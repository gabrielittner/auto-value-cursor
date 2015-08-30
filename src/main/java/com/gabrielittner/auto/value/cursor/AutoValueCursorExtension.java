package com.gabrielittner.auto.value.cursor;

import com.google.auto.service.AutoService;
import com.google.auto.value.AutoValueExtension;
import com.google.common.collect.Lists;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static javax.lang.model.element.Modifier.STATIC;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.ABSTRACT;

@AutoService(AutoValueExtension.class)
public class AutoValueCursorExtension implements AutoValueExtension {

  public static class Property {
    String name;
    ExecutableElement element;
    TypeName type;

    public Property() {}

    public Property(String name, ExecutableElement element) {
      this.name = name;
      this.element = element;

      type = TypeName.get(element.getReturnType());
    }

    public String columnName() {
      ColumnName columnName = element.getAnnotation(ColumnName.class);
      if (columnName != null) {
        return columnName.value();
      } else {
        return name;
      }
    }

    public boolean hasColumnName() {
      return element.getAnnotation(ColumnName.class) != null;
    }
  }

  @Override
  public boolean applicable(Context context) {
    return true;
  }

  @Override
  public boolean mustBeAtEnd(Context context) {
    return false;
  }

  @Override
  public String generateClass(Context context, String className, String classToExtend, boolean isFinal) {
    List<Property> properties = readProperties(context.properties());

    String fqAutoValueClass = context.autoValueClass().getQualifiedName().toString();
    Map<String, TypeName> types = convertPropertiesToTypes(context.properties());

    MethodSpec readMethod = createReadMethod(className, fqAutoValueClass, properties);

    TypeSpec.Builder subclass = TypeSpec.classBuilder(className)
        .superclass(TypeVariableName.get(classToExtend))
        .addMethod(generateConstructor(types))
        .addMethod(readMethod);

    if (isFinal) {
      subclass.addModifiers(FINAL);
    } else {
      subclass.addModifiers(ABSTRACT);
    }

    return JavaFile.builder(context.packageName(), subclass.build()).skipJavaLangImports(true).build().toString();
  }

  public List<Property> readProperties(Map<String, ExecutableElement> properties) {
    List<Property> values = new LinkedList<>();
    for (Map.Entry<String, ExecutableElement> entry : properties.entrySet()) {
      values.add(new Property(entry.getKey(), entry.getValue()));
    }
    return values;
  }

  MethodSpec generateConstructor(Map<String, TypeName> properties) {
    List<ParameterSpec> params = Lists.newArrayList();
    for (Map.Entry<String, TypeName> entry : properties.entrySet()) {
      params.add(ParameterSpec.builder(entry.getValue(), entry.getKey()).build());
    }

    MethodSpec.Builder builder = MethodSpec.constructorBuilder()
        .addParameters(params);

    StringBuilder superFormat = new StringBuilder("super(");
    for (int i = properties.size(); i > 0; i--) {
      superFormat.append("$N");
      if (i > 1) superFormat.append(", ");
    }
    superFormat.append(")");
    builder.addStatement(superFormat.toString(), properties.keySet().toArray());

    return builder.build();
  }

  /**
   * Converts the ExecutableElement properties to TypeName properties
   */
  Map<String, TypeName> convertPropertiesToTypes(Map<String, ExecutableElement> properties) {
    Map<String, TypeName> types = new LinkedHashMap<>();
    for (Map.Entry<String, ExecutableElement> entry : properties.entrySet()) {
      ExecutableElement el = entry.getValue();
      types.put(entry.getKey(), TypeName.get(el.getReturnType()));
    }
    return types;
  }

  public MethodSpec createReadMethod(String className,
                                     String autoValueClassName, List<Property> properties) {
    ClassName cursorClass = ClassName.get("android.database", "Cursor");
    ParameterSpec jsonReader = ParameterSpec.builder(cursorClass, "cursor").build();
    ClassName annotatedClass = ClassName.bestGuess(autoValueClassName);
    MethodSpec.Builder readMethod = MethodSpec.methodBuilder("createFromCursor")
        .addModifiers(STATIC)
        .returns(annotatedClass)
        .addParameter(jsonReader);

    Property unsupportedNotNullableProp = null;
    boolean hasColumnName = false;
    // add the properties
    Map<Property, FieldSpec> fields = new LinkedHashMap<>(properties.size());
    for (Property prop : properties) {
      FieldSpec field = FieldSpec.builder(prop.type, prop.name).build();
      fields.put(prop, field);

      String cursorMethod = getCursorMethod(prop);
      if (cursorMethod != null) {
        String suffix = isBoolean(prop) ? " == 1" : "";
        readMethod.addStatement("$T $N = cursor.$L(cursor.getColumnIndexOrThrow($S))$L",
                field.type, field, cursorMethod, prop.columnName(), suffix);
      } else {
        if (prop.hasColumnName()) {
          throw new IllegalArgumentException("Property " + prop.name + " is annotated with "
                  + "@ColumnName but a " + prop.type + " can't be read from Cursor.");
        }
        if (!hasAnnotationWithName(prop.element, "Nullable")) {
          unsupportedNotNullableProp = prop;
        }

        readMethod.addCode("$T $N = null; // type can't be read from cursor\n", field.type, field);
      }
      if (prop.hasColumnName()) hasColumnName = true;
    }

    if (hasColumnName && unsupportedNotNullableProp != null) {
      throw new IllegalArgumentException("Property " + unsupportedNotNullableProp.name
              + " is annotated with @ColumnName but a " + unsupportedNotNullableProp.type
              + " can't be read from Cursor.");
    }

    StringBuilder format = new StringBuilder("return new ");
    format.append(className.replaceAll("\\$", ""));
    format.append("(");
    Iterator<FieldSpec> iterator = fields.values().iterator();
    while (iterator.hasNext()) {
      iterator.next();
      format.append("$N");
      if (iterator.hasNext()) format.append(", ");
    }
    format.append(")");
    readMethod.addStatement(format.toString(), fields.values().toArray());

    return readMethod.build();
  }

  private static String getCursorMethod(Property prop) {
    if (prop.type.equals(TypeName.get(byte[].class)) || prop.type.equals(TypeName.get(Byte[].class))) {
      return "getBlob";
    } else if (prop.type.equals(TypeName.DOUBLE) || prop.type.equals(TypeName.DOUBLE.box())) {
      return "getDouble";
    } else if (prop.type.equals(TypeName.FLOAT) || prop.type.equals(TypeName.FLOAT.box())) {
      return "getFloat";
    } else if (prop.type.equals(TypeName.INT) || prop.type.equals(TypeName.INT.box())) {
      return "getInt";
    } else if (prop.type.equals(TypeName.LONG) || prop.type.equals(TypeName.LONG.box())) {
      return "getLong";
    } else if (prop.type.equals(TypeName.SHORT) || prop.type.equals(TypeName.SHORT.box())) {
      return "getShort";
    } else if (prop.type.equals(TypeName.get(String.class))) {
      return "getString";
    } else if (isBoolean(prop)) {
      return "getInt";
    }
    return null;
  }

  private static boolean isBoolean(Property prop) {
    return prop.type.equals(TypeName.BOOLEAN) || prop.type.equals(TypeName.BOOLEAN.box());
  }

  private static boolean hasAnnotationWithName(Element element, String simpleName) {
    for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
      String annotationName = mirror.getAnnotationType().asElement().getSimpleName().toString();
      if (simpleName.equals(annotationName)) {
        return true;
      }
    }
    return false;
  }
}

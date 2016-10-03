package com.gabrielittner.auto.value.cursor;

import com.gabrielittner.auto.value.ColumnProperty;
import com.gabrielittner.auto.value.util.ElementUtil;
import com.gabrielittner.auto.value.util.Property;
import com.google.auto.service.AutoService;
import com.google.auto.value.extension.AutoValueExtension;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.NameAllocator;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.TypeElement;

import static com.gabrielittner.auto.value.util.AutoValueUtil.error;
import static com.gabrielittner.auto.value.util.AutoValueUtil.getAutoValueClassClassName;
import static com.gabrielittner.auto.value.util.AutoValueUtil.getFinalClassClassName;
import static com.gabrielittner.auto.value.util.AutoValueUtil.newFinalClassConstructorCall;
import static com.gabrielittner.auto.value.util.AutoValueUtil.newTypeSpecBuilder;
import static com.gabrielittner.auto.value.util.ElementUtil.getMatchingStaticMethod;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

@AutoService(AutoValueExtension.class)
public class AutoValueCursorExtension extends AutoValueExtension {

  private static final ClassName CURSOR = ClassName.get("android.database", "Cursor");
  private static final ClassName LIST = ClassName.get("java.util", "List");
  private static final ClassName FUNC1 = ClassName.get("rx.functions", "Func1");

  private static final String SINGULAR_CREATE_METHOD_NAME = "createFromCursor";
  private static final String COLLECTION_CREATE_METHOD_NAME = "createListFromCursor";
  private static final String FUNC1_FIELD_NAME = "MAPPER";
  private static final String FUNC1_METHOD_NAME = "call";

  @Override
  public boolean applicable(Context context) {
    TypeElement valueClass = context.autoValueClass();
    return getMatchingStaticMethod(valueClass, ClassName.get(valueClass), CURSOR).isPresent()
            || getMatchingStaticMethod(valueClass, getFunc1TypeName(context)).isPresent()
            || getMatchingStaticMethod(valueClass, ParameterizedTypeName.get(LIST, ClassName.get(valueClass)), CURSOR).isPresent();
  }

  @Override
  public String generateClass(
          Context context, String className, String classToExtend, boolean isFinal) {
    ImmutableList<ColumnProperty> properties = ColumnProperty.from(context);

    TypeSpec.Builder subclass = newTypeSpecBuilder(context, className, classToExtend, isFinal)
            .addMethod(createReadMethod(context, properties))
            .addMethod(createReadListMethod(context, properties));

    if (ElementUtil.typeExists(context.processingEnvironment().getElementUtils(), FUNC1)) {
      subclass.addField(createMapper(context));
    }

    return JavaFile.builder(context.packageName(), subclass.build()).build().toString();
  }

  private MethodSpec createReadMethod(Context context, ImmutableList<ColumnProperty> properties) {
    MethodSpec.Builder readMethod =
            MethodSpec.methodBuilder(SINGULAR_CREATE_METHOD_NAME)
                    .addModifiers(STATIC)
                    .returns(getFinalClassClassName(context))
                    .addParameter(CURSOR, "cursor");

    ImmutableMap<Property, FieldSpec> columnAdapters = getColumnAdapters(properties);
    addColumnAdaptersToMethod(readMethod, properties, columnAdapters);

    String[] names = new String[properties.size()];
    for (int i = 0; i < properties.size(); i++) {
      ColumnProperty property = properties.get(i);
      names[i] = property.humanName();

      if (property.columnAdapter() != null) {
        readMethod.addStatement(
                "$T $N = $N.fromCursor(cursor, $S)",
                property.type(),
                property.humanName(),
                columnAdapters.get(property),
                property.columnName());
      } else if (property.supportedType()) {
        if (property.nullable()) {
          readMethod.addCode(readNullableProperty(property));
        } else {
          readMethod.addCode(readProperty(property));
        }
      } else if (property.nullable()) {
        readMethod.addCode(
                "$T $N = null; // can't be read from cursor\n",
                property.type(),
                property.humanName());
      } else {
        error(context, property, "Property has type that can't be read from Cursor.");
      }
    }
    return readMethod
            .addCode("return ")
            .addCode(newFinalClassConstructorCall(context, names))
            .build();
  }

  private MethodSpec createReadListMethod(Context context, ImmutableList<ColumnProperty> properties) {
    MethodSpec.Builder readMethod =
            MethodSpec.methodBuilder(COLLECTION_CREATE_METHOD_NAME)
                    .addModifiers(STATIC)
                    .returns()
                    .addParameter(CURSOR, "cursor");

    ImmutableMap<Property, FieldSpec> columnAdapters = getColumnAdapters(properties);
    addColumnAdaptersToMethod(readMethod, properties, columnAdapters);

    String[] names = new String[properties.size()];
    for (int i = 0; i < properties.size(); i++) {
      ColumnProperty property = properties.get(i);
      names[i] = property.humanName();

      if (property.columnAdapter() != null) {
        readMethod.addStatement(
                "$T $N = $N.fromCursor(cursor, $S)",
                property.type(),
                property.humanName(),
                columnAdapters.get(property),
                property.columnName());
      } else if (property.supportedType()) {
        if (property.nullable()) {
          readMethod.addCode(readNullableProperty(property));
        } else {
          readMethod.addCode(readProperty(property));
        }
      } else if (property.nullable()) {
        readMethod.addCode(
                "$T $N = null; // can't be read from cursor\n",
                property.type(),
                property.humanName());
      } else {
        error(context, property, "Property has type that can't be read from Cursor.");
      }
    } return null;
  }

  private CodeBlock readProperty(ColumnProperty property) {
    CodeBlock getValue = CodeBlock.of(property.cursorMethod(), getColumnIndex(property));
    return CodeBlock.builder()
            .addStatement("$T $N = $L", property.type(), property.humanName(), getValue)
            .build();
  }

  private CodeBlock readNullableProperty(ColumnProperty property) {
    String columnIndexVar = property.humanName() + "ColumnIndex";
    CodeBlock getValue =
            CodeBlock.builder()
                    .add("cursor.isNull($L) ? null : ", columnIndexVar)
                    .add(property.cursorMethod(), columnIndexVar)
                    .build();
    return CodeBlock.builder()
            .addStatement("int $L = $L", columnIndexVar, getColumnIndex(property))
            .addStatement("$T $N = $L", property.type(), property.humanName(), getValue)
            .build();
  }

  private CodeBlock getColumnIndex(ColumnProperty property) {
    return CodeBlock.of("cursor.getColumnIndexOrThrow($S)", property.columnName());
  }

  private FieldSpec createMapper(Context context) {
    TypeName func1Name = getFunc1TypeName(context);
    MethodSpec func1Method =
            MethodSpec.methodBuilder(FUNC1_METHOD_NAME)
                    .addAnnotation(Override.class)
                    .addModifiers(PUBLIC)
                    .addParameter(CURSOR, "c")
                    .returns(getFinalClassClassName(context))
                    .addStatement("return $L($N)", SINGULAR_CREATE_METHOD_NAME, "c")
                    .build();
    TypeSpec func1 =
            TypeSpec.anonymousClassBuilder("")
                    .addSuperinterface(func1Name)
                    .addMethod(func1Method)
                    .build();
    return FieldSpec.builder(func1Name, FUNC1_FIELD_NAME, STATIC, FINAL)
            .initializer("$L", func1)
            .build();
  }

  private TypeName getFunc1TypeName(Context context) {
    return ParameterizedTypeName.get(FUNC1, CURSOR, getAutoValueClassClassName(context));
  }

  public static ImmutableMap<Property, FieldSpec> getColumnAdapters(
          List<ColumnProperty> properties) {
    Map<Property, FieldSpec> columnAdapters = new HashMap<>();
    for (ColumnProperty property : properties) {
      if (property.columnAdapter() != null && !columnAdapters.containsKey(property)) {
        ClassName clsName = (ClassName) TypeName.get(property.columnAdapter());
        String name = NameAllocator.toJavaIdentifier(toLowerCase(clsName.simpleName()));
        FieldSpec field = FieldSpec.builder(clsName, name).build();
        columnAdapters.put(property, field);
      }
    }
    return ImmutableMap.copyOf(columnAdapters);
  }

  private static String toLowerCase(String s) {
    return Character.toLowerCase(s.charAt(0)) + s.substring(1);
  }

  public static void addColumnAdaptersToMethod(
          MethodSpec.Builder method,
          List<ColumnProperty> properties,
          ImmutableMap<Property, FieldSpec> columnAdapters) {
    if (columnAdapters.size() == 0) {
      return;
    }

    List<FieldSpec> handledAdapters = new ArrayList<>(columnAdapters.size());
    for (Property property : properties) {
      FieldSpec adapter = columnAdapters.get(property);
      if (adapter != null && !handledAdapters.contains(adapter)) {
        method.addStatement("$1T $2N = new $1T()", adapter.type, adapter);
        handledAdapters.add(adapter);
      }
    }
  }
}

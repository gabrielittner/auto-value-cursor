package com.gabrielittner.auto.value.cursor;

import com.google.auto.service.AutoService;
import com.google.auto.value.extension.AutoValueExtension;
import com.google.common.collect.Lists;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.Map.Entry;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.TypeElement;

import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

@AutoService(AutoValueExtension.class)
public class AutoValueCursorExtension extends AutoValueExtension {

    private static final ClassName CURSOR = ClassName.get("android.database", "Cursor");
    private static final ClassName FUNC1 = ClassName.get("rx.functions", "Func1");
    private static final String NULLABLE = "Nullable";

    private static final String METHOD_NAME = "createFromCursor";
    private static final String FIELD_NAME = "MAPPER";

    @Override public boolean applicable(Context context) {
        return true;
    }

    @Override public String generateClass(Context context, String className, String classToExtend,
            boolean isFinal) {
        String packageName = context.packageName();
        ClassName autoValueClassName = ClassName.get(packageName,
                context.autoValueClass().getSimpleName().toString());
        Map<String, ExecutableElement> properties = context.properties();

        TypeSpec.Builder subclass = TypeSpec.classBuilder(className)
                .addModifiers(isFinal ? FINAL : ABSTRACT)
                .superclass(ClassName.get(packageName, classToExtend))
                .addMethod(generateConstructor(properties))
                .addMethod(createReadMethod(className, autoValueClassName, properties));

        if (projectUsesRxJava(context)) {
            subclass.addField(createMapper(autoValueClassName));
        }

        return JavaFile.builder(packageName, subclass.build())
                .skipJavaLangImports(true)
                .build()
                .toString();
    }

    private MethodSpec generateConstructor(Map<String, ExecutableElement> properties) {
        List<ParameterSpec> params = Lists.newArrayList();
        for (Entry<String, ExecutableElement> entry : properties.entrySet()) {
            TypeName typeName = TypeName.get(entry.getValue().getReturnType());
            params.add(ParameterSpec.builder(typeName, entry.getKey()).build());
        }

        StringBuilder superFormat = new StringBuilder("super(");
        for (int i = properties.size(); i > 0; i--) {
            superFormat.append("$N");
            if (i > 1) superFormat.append(", ");
        }
        superFormat.append(")");

        return MethodSpec.constructorBuilder()
                .addParameters(params)
                .addStatement(superFormat.toString(), properties.keySet().toArray())
                .build();
    }

    private MethodSpec createReadMethod(String className, ClassName autoValueClassName,
            Map<String, ExecutableElement> properties) {
        MethodSpec.Builder readMethod = MethodSpec.methodBuilder(METHOD_NAME)
                .addModifiers(STATIC)
                .returns(autoValueClassName)
                .addParameter(CURSOR, "cursor");

        String[] propertyNames = new String[properties.keySet().size()];
        propertyNames = properties.keySet().toArray(propertyNames);
        String unsupportedNotNullableProp = null;
        boolean hasFieldWithColumnName = false;
        for (String name : propertyNames) {
            ExecutableElement element = properties.get(name);
            TypeName type = TypeName.get(element.getReturnType());

            String cursorMethod = getCursorMethod(type);
            String columnName = getColumnName(element);
            if (cursorMethod != null) {
                readMethod.addStatement(cursorMethod, type, name,
                        columnName != null ? columnName : name);
            } else {
                readMethod.addCode("$T $N = null; // type can't be read from cursor\n", type, name);

                if (columnName != null || !hasAnnotationWithName(element, NULLABLE)) {
                    // a) user wanted to explicitly map this unsupported field
                    // b) unsupported field can't be null
                    // TODO fail here immediately
                    // not doing it right now because there is no opt in to auto-value-cursor
                    unsupportedNotNullableProp = name;
                }
            }
            if (columnName != null) hasFieldWithColumnName = true;
        }

        if (hasFieldWithColumnName && unsupportedNotNullableProp != null) {
            throw new IllegalArgumentException(String.format("Property %s has a type that can't be"
                    + " read from Cursor.", unsupportedNotNullableProp));
        }

        StringBuilder format = new StringBuilder("return new ");
        format.append(className.replaceAll("\\$", ""));
        format.append("(");
        for (int i = 0; i < propertyNames.length; i++) {
            if (i > 0) format.append(", ");
            format.append("$L");
        }
        format.append(")");
        readMethod.addStatement(format.toString(), (Object[]) propertyNames);

        return readMethod.build();
    }

    private static String getCursorMethod(TypeName type) {
        if (type.equals(TypeName.get(byte[].class)) || type.equals(
                TypeName.get(Byte[].class))) {
            return "$T $N = cursor.getBlob(cursor.getColumnIndexOrThrow($S))";
        } else if (type.equals(TypeName.DOUBLE) || type.equals(TypeName.DOUBLE.box())) {
            return "$T $N = cursor.getDouble(cursor.getColumnIndexOrThrow($S))";
        } else if (type.equals(TypeName.FLOAT) || type.equals(TypeName.FLOAT.box())) {
            return "$T $N = cursor.getFloat(cursor.getColumnIndexOrThrow($S))";
        } else if (type.equals(TypeName.INT) || type.equals(TypeName.INT.box())) {
            return "$T $N = cursor.getInt(cursor.getColumnIndexOrThrow($S))";
        } else if (type.equals(TypeName.LONG) || type.equals(TypeName.LONG.box())) {
            return "$T $N = cursor.getLong(cursor.getColumnIndexOrThrow($S))";
        } else if (type.equals(TypeName.SHORT) || type.equals(TypeName.SHORT.box())) {
            return "$T $N = cursor.getShort(cursor.getColumnIndexOrThrow($S))";
        } else if (type.equals(TypeName.get(String.class))) {
            return "$T $N = cursor.getString(cursor.getColumnIndexOrThrow($S))";
        } else if (type.equals(TypeName.BOOLEAN) || type.equals(TypeName.BOOLEAN.box())) {
            return "$T $N = cursor.getInt(cursor.getColumnIndexOrThrow($S)) == 1";
        }
        return null;
    }

    private FieldSpec createMapper(ClassName autoValueClassName) {
        TypeName func1Type = ParameterizedTypeName.get(FUNC1, CURSOR, autoValueClassName);
        TypeSpec func1 = TypeSpec.anonymousClassBuilder("")
                .addSuperinterface(func1Type)
                .addMethod(MethodSpec.methodBuilder("call")
                        .addAnnotation(Override.class)
                        .addModifiers(PUBLIC)
                        .addParameter(CURSOR, "c")
                        .returns(autoValueClassName)
                        .addStatement("return $L($N)", METHOD_NAME, "c")
                        .build())
                .build();
        return FieldSpec.builder(func1Type, FIELD_NAME, STATIC, FINAL)
                .initializer("$L", func1)
                .build();
    }

    private static String getColumnName(ExecutableElement element) {
        ColumnName columnName = element.getAnnotation(ColumnName.class);
        return columnName != null ? columnName.value() : null;
    }

    private static boolean hasAnnotationWithName(Element element, String simpleName) {
        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
            String name = mirror.getAnnotationType().asElement().getSimpleName().toString();
            if (simpleName.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static boolean projectUsesRxJava(Context context) {
        TypeElement func1 = context.processingEnvironment().getElementUtils()
                .getTypeElement(FUNC1.packageName() + "." + FUNC1.simpleName());
        return func1 != null;
    }
}

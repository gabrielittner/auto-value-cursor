package com.gabrielittner.auto.value.cursor;

import com.google.auto.service.AutoService;
import com.google.auto.value.extension.AutoValueExtension;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.Map;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

import static com.gabrielittner.auto.value.util.AnnotationUtil.getAnnotationMirror;
import static com.gabrielittner.auto.value.util.AnnotationUtil.getAnnotationValue;
import static com.gabrielittner.auto.value.util.AnnotationUtil.hasAnnotationWithName;
import static com.gabrielittner.auto.value.util.AutoValueUtil.generateFinalClassConstructorCall;
import static com.gabrielittner.auto.value.util.AutoValueUtil.getAutoValueClassClassName;
import static com.gabrielittner.auto.value.util.AutoValueUtil.getFinalClassClassName;
import static com.gabrielittner.auto.value.util.AutoValueUtil.typeSpecBuilder;
import static com.gabrielittner.auto.value.util.ElementUtil.getMethod;
import static com.gabrielittner.auto.value.util.ElementUtil.hasMethod;
import static com.gabrielittner.auto.value.util.ElementUtil.typeExists;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

@AutoService(AutoValueExtension.class)
public class AutoValueCursorExtension extends AutoValueExtension {

    private static final ClassName CURSOR = ClassName.get("android.database", "Cursor");
    private static final ClassName FUNC1 = ClassName.get("rx.functions", "Func1");
    private static final String NULLABLE = "Nullable";

    private static final String METHOD_NAME = "createFromCursor";
    private static final String FUNC1_FIELD_NAME = "MAPPER";
    private static final String FUNC1_METHOD_NAME = "call";

    @Override
    public boolean applicable(Context context) {
        TypeElement valueClass = context.autoValueClass();
        return hasMethod(valueClass, false, true, CURSOR, ClassName.get(valueClass.asType()))
                || hasMethod(valueClass, false, true, CURSOR, getFunc1TypeName(context));
    }

    @Override
    public String generateClass(Context context, String className, String classToExtend,
            boolean isFinal) {
        Map<String, ExecutableElement> properties = context.properties();

        TypeSpec.Builder subclass = typeSpecBuilder(context, className, classToExtend, isFinal)
                .addMethod(createReadMethod(context, properties));

        if (typeExists(context.processingEnvironment().getElementUtils(), FUNC1)) {
            subclass.addField(createMapper(context));
        }

        return JavaFile.builder(context.packageName(), subclass.build())
                .skipJavaLangImports(true)
                .build()
                .toString();
    }

    private MethodSpec createReadMethod(Context context,
            Map<String, ExecutableElement> properties) {
        MethodSpec.Builder readMethod = MethodSpec.methodBuilder(METHOD_NAME)
                .addModifiers(STATIC)
                .returns(getFinalClassClassName(context))
                .addParameter(CURSOR, "cursor");

        Types typeUtils = context.processingEnvironment().getTypeUtils();
        for (Map.Entry<String, ExecutableElement> entry : properties.entrySet()) {
            ExecutableElement element = entry.getValue();
            TypeName type = TypeName.get(element.getReturnType());
            String name = entry.getKey();
            String cursorMethod = getCursorMethod(type);
            String columnName = getColumnName(element);
            if (cursorMethod != null) {
                readMethod.addStatement(cursorMethod, type, name,
                        columnName != null ? columnName : name);
            } else {
                TypeMirror factoryTypeMirror = getFactoryTypeMirror(element);
                if (factoryTypeMirror != null) {
                    String methodName = getFactoryMethodName(type,
                            (TypeElement) typeUtils.asElement(factoryTypeMirror));
                    TypeName factoryType = TypeName.get(factoryTypeMirror);
                    readMethod.addStatement("$T $N = $T.$N(cursor)", type, name,
                            factoryType, methodName);
                } else {
                    if (!hasAnnotationWithName(element, NULLABLE)) {
                        throw new IllegalArgumentException(String.format("Property %s has type %s "
                                + "that can't be read from Cursor.", name, type));
                    }
                    readMethod.addCode("$T $N = null; // can't be read from cursor\n", type, name);
                }
            }
        }

        Object[] propertyNames = properties.keySet().toArray();
        CodeBlock returnCall = generateFinalClassConstructorCall(context, propertyNames);
        return readMethod.addCode("return ").addCode(returnCall).build();
    }

    private static String getCursorMethod(TypeName type) {
        if (type.equals(TypeName.get(byte[].class)) || type.equals(
                TypeName.get(Byte[].class))) {
            return "$T $N = cursor.getBlob(cursor.getColumnIndexOrThrow($S))";
        }
        if (type.equals(TypeName.DOUBLE) || type.equals(TypeName.DOUBLE.box())) {
            return "$T $N = cursor.getDouble(cursor.getColumnIndexOrThrow($S))";
        }
        if (type.equals(TypeName.FLOAT) || type.equals(TypeName.FLOAT.box())) {
            return "$T $N = cursor.getFloat(cursor.getColumnIndexOrThrow($S))";
        }
        if (type.equals(TypeName.INT) || type.equals(TypeName.INT.box())) {
            return "$T $N = cursor.getInt(cursor.getColumnIndexOrThrow($S))";
        }
        if (type.equals(TypeName.LONG) || type.equals(TypeName.LONG.box())) {
            return "$T $N = cursor.getLong(cursor.getColumnIndexOrThrow($S))";
        }
        if (type.equals(TypeName.SHORT) || type.equals(TypeName.SHORT.box())) {
            return "$T $N = cursor.getShort(cursor.getColumnIndexOrThrow($S))";
        }
        if (type.equals(TypeName.get(String.class))) {
            return "$T $N = cursor.getString(cursor.getColumnIndexOrThrow($S))";
        }
        if (type.equals(TypeName.BOOLEAN) || type.equals(TypeName.BOOLEAN.box())) {
            return "$T $N = cursor.getInt(cursor.getColumnIndexOrThrow($S)) == 1";
        }
        return null;
    }

    private static String getColumnName(ExecutableElement element) {
        ColumnName columnName = element.getAnnotation(ColumnName.class);
        return columnName != null ? columnName.value() : null;
    }

    private TypeMirror getFactoryTypeMirror(Element element) {
        AnnotationMirror annotationMirror = getAnnotationMirror(element, CursorAdapter.class);
        if (annotationMirror == null) {
            return null;
        }
        AnnotationValue annotationValue = getAnnotationValue(annotationMirror, "value");
        return annotationValue == null ? null : (TypeMirror) annotationValue.getValue();
    }

    private String getFactoryMethodName(TypeName returnType, TypeElement factoryClass) {
        ExecutableElement method = getMethod(factoryClass, false, true, CURSOR, returnType);
        if (method != null) {
            return method.getSimpleName().toString();
        }
        throw new IllegalArgumentException(String.format("Class \"%s\" needs to define a "
                        + "public static method taking a \"Cursor\" and returning \"%s\"",
                factoryClass.getSimpleName(), returnType.toString()));
    }

    private FieldSpec createMapper(Context context) {
        TypeName func1Name = getFunc1TypeName(context);
        TypeSpec func1 = TypeSpec.anonymousClassBuilder("")
                .addSuperinterface(func1Name)
                .addMethod(MethodSpec.methodBuilder(FUNC1_METHOD_NAME)
                        .addAnnotation(Override.class)
                        .addModifiers(PUBLIC)
                        .addParameter(CURSOR, "c")
                        .returns(getFinalClassClassName(context))
                        .addStatement("return $L($N)", METHOD_NAME, "c")
                        .build())
                .build();
        return FieldSpec.builder(func1Name, FUNC1_FIELD_NAME, STATIC, FINAL)
                .initializer("$L", func1)
                .build();
    }

    private TypeName getFunc1TypeName(Context context) {
        return ParameterizedTypeName.get(FUNC1, CURSOR, getAutoValueClassClassName(context));
    }
}

package com.gabrielittner.auto.value.cursor;

import com.google.auto.service.AutoService;
import com.google.auto.value.extension.AutoValueExtension;
import com.google.common.collect.Lists;
import com.squareup.javapoet.*;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static javax.lang.model.element.Modifier.*;

@AutoService(AutoValueExtension.class)
public class AutoValueCursorExtension extends AutoValueExtension {

    private static final ClassName CURSOR = ClassName.get("android.database", "Cursor");
    private static final ClassName FUNC1 = ClassName.get("rx.functions", "Func1");
    private static final String NULLABLE = "Nullable";

    private static final String METHOD_NAME = "createFromCursor";
    private static final String FIELD_NAME = "MAPPER";

    @Override public boolean applicable(Context context) {
        TypeElement autoValueClass = context.autoValueClass();
        List<? extends Element> elements = autoValueClass.getEnclosedElements();
        for (Element element : elements) {
            // searching for a static method
            if (element.getKind() != ElementKind.METHOD
                    || !element.getModifiers().contains(Modifier.STATIC)) {
                continue;
            }
            ExecutableElement method = (ExecutableElement) element;
            // that method should return the annotated class and take a Cursor as parameter
            // or return a Func1<Cursor, "annotated class"> and not have any parameters
            if (methodTakesAndReturns(method, CURSOR, ClassName.get(autoValueClass.asType()))
                    || methodTakesAndReturns(method, null, getFunc1Name(context))) {
                return true;
            }
        }
        return false;
    }

    private boolean methodTakesAndReturns(ExecutableElement method, TypeName takes, TypeName returns) {
        List<? extends VariableElement> parameters = method.getParameters();
        if (takes != null) {
            if (parameters.size() != 1) {
                return false;
            }
            if (!takes.equals(ClassName.get(parameters.get(0).asType()))) {
                return false;
            }
        } else {
            if (parameters.size() > 0) {
                return false;
            }
        }

        return returns.equals(ClassName.get(method.getReturnType()));
    }

    @Override public String generateClass(Context context, String className, String classToExtend,
            boolean isFinal) {
        String packageName = context.packageName();
        Map<String, ExecutableElement> properties = context.properties();

        TypeSpec.Builder subclass = TypeSpec.classBuilder(className)
                .addModifiers(isFinal ? FINAL : ABSTRACT)
                .superclass(ClassName.get(packageName, classToExtend))
                .addMethod(generateConstructor(properties))
                .addMethod(createReadMethod(context, className, properties));

        if (projectUsesRxJava(context)) {
            subclass.addField(createMapper(context, className));
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

    private MethodSpec createReadMethod(Context context, String className,
            Map<String, ExecutableElement> properties) {
        ClassName returnType = getReturnType(context, className);
        MethodSpec.Builder readMethod = MethodSpec.methodBuilder(METHOD_NAME)
                .addModifiers(STATIC)
                .returns(returnType)
                .addParameter(CURSOR, "cursor");

        Set<String> keySet = properties.keySet();
        String[] propertyNames = new String[keySet.size()];
        propertyNames = keySet.toArray(propertyNames);
        for (String name : propertyNames) {
            ExecutableElement element = properties.get(name);
            TypeName type = TypeName.get(element.getReturnType());
            String cursorMethod = getCursorMethod(type);
            String columnName = getColumnName(element);
            TypeMirror factoryTypeMirror = getFactoryTypeMirror(element);
            Types typeUtils = context.processingEnvironment().getTypeUtils();
            if (cursorMethod != null) {
                readMethod.addStatement(cursorMethod, type, name,
                        columnName != null ? columnName : name);
            } else if (factoryTypeMirror != null) {
                String methodName = getFactoryMethodName(type,
                        (TypeElement) typeUtils.asElement(factoryTypeMirror));
                TypeName factoryType = TypeName.get(factoryTypeMirror);
                readMethod.addStatement("$T $N = $T.$N(cursor)", type, name, factoryType, methodName);
            } else {
                if (!hasAnnotationWithName(element, NULLABLE)) {
                    throw new IllegalArgumentException(String.format("Property %s has type %s that "
                            + "can't be read from Cursor.", name, type));
                }
                readMethod.addCode("$T $N = null; // type can't be read from cursor\n", type, name);
            }
        }

        StringBuilder format = new StringBuilder("return new ");
        format.append(returnType.simpleName());
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

    private FieldSpec createMapper(Context context, String className) {
        TypeName func1Name = getFunc1Name(context);
        TypeSpec func1 = TypeSpec.anonymousClassBuilder("")
                .addSuperinterface(func1Name)
                .addMethod(MethodSpec.methodBuilder("call")
                        .addAnnotation(Override.class)
                        .addModifiers(PUBLIC)
                        .addParameter(CURSOR, "c")
                        .returns(getReturnType(context, className))
                        .addStatement("return $L($N)", METHOD_NAME, "c")
                        .build())
                .build();
        return FieldSpec.builder(func1Name, FIELD_NAME, STATIC, FINAL)
                .initializer("$L", func1)
                .build();
    }

    private ClassName getReturnType(Context context, String className) {
        return ClassName.get(context.packageName(), className.replaceAll("\\$", ""));
    }

    private TypeName getFunc1Name(Context context) {
        ClassName autoValueClassName = ClassName.get(context.packageName(),
                context.autoValueClass().getSimpleName().toString());
        return ParameterizedTypeName.get(FUNC1, CURSOR, autoValueClassName);
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

    private static AnnotationMirror getAnnotationMirror(Element element, Class<?> clazz) {
        String clazzName = clazz.getName();
        for (AnnotationMirror m : element.getAnnotationMirrors()) {
            if (m.getAnnotationType().toString().equals(clazzName)) {
                return m;
            }
        }
        return null;
    }

    private static AnnotationValue getAnnotationValue(AnnotationMirror annotationMirror, String key) {
        for (Entry<? extends ExecutableElement, ? extends AnnotationValue> entry :
                annotationMirror.getElementValues().entrySet()) {
            if (entry.getKey().getSimpleName().toString().equals(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public TypeMirror getFactoryTypeMirror(Element element) {
        AnnotationMirror annotationMirror =
                getAnnotationMirror(element, CursorAdapter.class);
        if (annotationMirror == null) {
            return null;
        }
        AnnotationValue annotationValue = getAnnotationValue(annotationMirror, "value");
        return annotationValue == null ? null : (TypeMirror) annotationValue.getValue();
    }

    private String getFactoryMethodName(TypeName returnType, TypeElement factoryClass) {
        List<? extends Element> elements = factoryClass.getEnclosedElements();
        for (Element element : elements) {
            // searching for a static method
            if (element.getKind() != ElementKind.METHOD
                    || !element.getModifiers().contains(Modifier.STATIC)) {
                continue;
            }
            ExecutableElement method = (ExecutableElement) element;
            // that method should return the annotated class and take a Cursor as parameter
            if (methodTakesAndReturns(method, CURSOR, returnType)) {
                return method.getSimpleName().toString();
            }
        }
        throw new IllegalArgumentException(String.format("Class \"%s\" needs to define a "
                + "public static method taking a \"Cursor\" and returning \"%s\"",
                factoryClass.getSimpleName(), returnType.toString()));
    }

    private static boolean projectUsesRxJava(Context context) {
        TypeElement func1 = context.processingEnvironment().getElementUtils()
                .getTypeElement(FUNC1.packageName() + "." + FUNC1.simpleName());
        return func1 != null;
    }
}

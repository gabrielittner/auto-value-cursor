package com.gabrielittner.auto.value.cursor;

import com.gabrielittner.auto.value.ColumnProperty;
import com.gabrielittner.auto.value.util.ElementUtil;
import com.gabrielittner.auto.value.util.Property;
import com.google.auto.common.MoreElements;
import com.google.auto.service.AutoService;
import com.google.auto.value.extension.AutoValueExtension;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

import static com.gabrielittner.auto.value.util.AutoValueUtil.getAutoValueClassClassName;
import static com.gabrielittner.auto.value.util.AutoValueUtil.getFinalClassClassName;
import static com.gabrielittner.auto.value.util.AutoValueUtil.newFinalClassConstructorCall;
import static com.gabrielittner.auto.value.util.AutoValueUtil.newTypeSpecBuilder;
import static com.gabrielittner.auto.value.util.ElementUtil.getStaticMethod;
import static com.gabrielittner.auto.value.util.ElementUtil.hasStaticMethod;
import static com.gabrielittner.auto.value.util.ElementUtil.typeExists;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.tools.Diagnostic.Kind.ERROR;

@AutoService(AutoValueExtension.class)
public class AutoValueCursorExtension extends AutoValueExtension {

    private static final ClassName CURSOR = ClassName.get("android.database", "Cursor");
    private static final ClassName FUNC1 = ClassName.get("rx.functions", "Func1");

    private static final String METHOD_NAME = "createFromCursor";
    private static final String FUNC1_FIELD_NAME = "MAPPER";
    private static final String FUNC1_METHOD_NAME = "call";

    @Override
    public boolean applicable(Context context) {
        TypeElement valueClass = context.autoValueClass();
        return hasStaticMethod(valueClass, CURSOR, ClassName.get(valueClass.asType()))
                || hasStaticMethod(valueClass, null, getFunc1TypeName(context));
    }

    @Override
    public String generateClass(Context context, String className, String classToExtend,
            boolean isFinal) {
        ImmutableList<ColumnProperty> properties = ColumnProperty.from(context);

        TypeSpec.Builder subclass = newTypeSpecBuilder(context, className, classToExtend, isFinal)
                .addMethod(createReadMethod(context, properties));

        if (typeExists(context.processingEnvironment().getElementUtils(), FUNC1)) {
            subclass.addField(createMapper(context));
        }

        return JavaFile.builder(context.packageName(), subclass.build())
                .build()
                .toString();
    }

    private MethodSpec createReadMethod(Context context, ImmutableList<ColumnProperty> properties) {
        MethodSpec.Builder readMethod = MethodSpec.methodBuilder(METHOD_NAME)
                .addModifiers(STATIC)
                .returns(getFinalClassClassName(context))
                .addParameter(CURSOR, "cursor");

        Types typeUtils = context.processingEnvironment().getTypeUtils();
        String[] names = new String[properties.size()];
        for (int i = 0; i < properties.size(); i++) {
            ColumnProperty property = properties.get(i);
            names[i] = property.humanName();

            TypeMirror factory = property.cursorAdapter();
            if (factory != null) {
                CodeBlock readProperty = readProperty(property, factory, typeUtils, context);
                if (readProperty != null) {
                    readMethod.addCode(readProperty);
                }
            } else if (property.supportedType()) {
                if (property.nullable()) {
                    readMethod.addCode(readNullableProperty(property));
                } else {
                    readMethod.addCode(readProperty(property));
                }
            } else if (property.nullable()) {
                readMethod.addCode("$T $N = null; // can't be read from cursor\n", property.type(),
                        property.humanName());
            } else {
                error(context, property, "Property has type that can't be read from Cursor.");
            }
        }
        return readMethod.addCode("return ")
                .addCode(newFinalClassConstructorCall(context, names))
                .build();
    }

    private CodeBlock readProperty(ColumnProperty property) {
        CodeBlock getValue = CodeBlock.of(property.cursorMethod(), getColumnIndex(property));
        return CodeBlock.builder()
                .addStatement("$T $N = $L", property.type(), property.humanName(), getValue)
                .build();
    }

    private CodeBlock readNullableProperty(ColumnProperty property) {
        String columnIndexVar = property.humanName() + "ColumnIndex";
        CodeBlock getValue = CodeBlock.builder()
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

    private CodeBlock readProperty(ColumnProperty property, TypeMirror factory, Types typeUtils,
            Context context) {
        TypeElement factoryType = (TypeElement) typeUtils.asElement(factory);
        ExecutableElement method = getStaticMethod(factoryType, CURSOR, property.type());
        if (method != null) {
            return CodeBlock.builder()
                    .addStatement("$T $N = $T.$N(cursor)", property.type(), property.humanName(),
                            TypeName.get(factory), method.getSimpleName())
                    .build();
        }
        error(context, property, "Class \"%s\" needs to define a public static method taking a"
                + " \"Cursor\" and returning \"%s\"", factoryType, property.type().toString());
        return null;
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

    public static void error(Context context, Property property, String message) {
        context.processingEnvironment().getMessager()
                .printMessage(ERROR, message, property.element());
    }

    public static void error(Context context, Property property, String message, Object... args) {
        error(context, property, String.format(message, args));
    }
}

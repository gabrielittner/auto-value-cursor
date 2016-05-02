package com.gabrielittner.auto.value.cursor;

import com.gabrielittner.auto.value.ColumnProperty;
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

    private MethodSpec createReadMethod(Context context,
            ImmutableList<ColumnProperty> properties) {
        MethodSpec.Builder readMethod = MethodSpec.methodBuilder(METHOD_NAME)
                .addModifiers(STATIC)
                .returns(getFinalClassClassName(context))
                .addParameter(CURSOR, "cursor");

        Types typeUtils = context.processingEnvironment().getTypeUtils();
        String[] names = new String[properties.size()];
        for (int i = 0; i < properties.size(); i++) {
            ColumnProperty property = properties.get(i);
            names[i] = property.humanName();

            String cursorMethod = getCursorMethod(property.type());
            TypeMirror factoryTypeMirror = property.cursorAdapter();
            if (factoryTypeMirror != null) {
                TypeElement factoryType = (TypeElement) typeUtils.asElement(factoryTypeMirror);
                ExecutableElement method = getStaticMethod(factoryType, CURSOR, property.type());
                if (method == null) {
                    String message = String.format("Class \"%s\" needs to define a public"
                                    + " static method taking a \"Cursor\" and returning \"%s\"",
                            factoryType, property.type().toString());
                    context.processingEnvironment().getMessager()
                            .printMessage(ERROR, message, context.autoValueClass());
                    continue;
                }
                readMethod.addStatement("$T $N = $T.$N(cursor)", property.type(),
                        property.humanName(), TypeName.get(factoryTypeMirror),
                        method.getSimpleName().toString());
            } else if (cursorMethod != null) {
                CodeBlock getColumnIndex =
                        CodeBlock.of("cursor.getColumnIndexOrThrow($S)", property.columnName());
                CodeBlock getValue;
                if (property.nullable()) {
                    String columnIndexVar = property.humanName() + "ColumnIndex";
                    readMethod.addStatement("int $L = $L", columnIndexVar, getColumnIndex);
                    getValue = CodeBlock.builder()
                            .add("cursor.isNull($L) ? null : ", columnIndexVar)
                            .add(cursorMethod, columnIndexVar)
                            .build();
                } else {
                    getValue = CodeBlock.of(cursorMethod, getColumnIndex);
                }
                readMethod.addStatement("$T $N = $L", property.type(), property.humanName(),
                        getValue);
            } else {
                if (property.nullable()) {
                    readMethod.addCode("$T $N = null; // can't be read from cursor\n",
                            property.type(), property.humanName());
                } else {
                    String message = String.format("ColumnProperty \"%s\" has type \"%s\" that can't "
                            + "be read from Cursor.", property.humanName(), property.type());
                    context.processingEnvironment().getMessager()
                            .printMessage(ERROR, message, context.autoValueClass());
                }
            }
        }

        CodeBlock returnCall = newFinalClassConstructorCall(context, names);
        return readMethod.addCode("return ").addCode(returnCall).build();
    }

    public static String getCursorMethod(TypeName type) {
        if (type.equals(TypeName.get(byte[].class)) || type.equals(
                TypeName.get(Byte[].class))) {
            return "cursor.getBlob($L)";
        }
        if (type.equals(TypeName.DOUBLE) || type.equals(TypeName.DOUBLE.box())) {
            return "cursor.getDouble($L)";
        }
        if (type.equals(TypeName.FLOAT) || type.equals(TypeName.FLOAT.box())) {
            return "cursor.getFloat($L)";
        }
        if (type.equals(TypeName.INT) || type.equals(TypeName.INT.box())) {
            return "cursor.getInt($L)";
        }
        if (type.equals(TypeName.LONG) || type.equals(TypeName.LONG.box())) {
            return "cursor.getLong($L)";
        }
        if (type.equals(TypeName.SHORT) || type.equals(TypeName.SHORT.box())) {
            return "cursor.getShort($L)";
        }
        if (type.equals(TypeName.get(String.class))) {
            return "cursor.getString($L)";
        }
        if (type.equals(TypeName.BOOLEAN) || type.equals(TypeName.BOOLEAN.box())) {
            return "cursor.getInt($L) == 1";
        }
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
}

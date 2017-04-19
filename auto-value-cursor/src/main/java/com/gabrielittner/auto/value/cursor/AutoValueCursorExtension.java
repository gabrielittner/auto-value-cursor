package com.gabrielittner.auto.value.cursor;

import com.gabrielittner.auto.value.ColumnProperty;
import com.gabrielittner.auto.value.util.ElementUtil;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.TypeElement;

import static com.gabrielittner.auto.value.util.AutoValueUtil.error;
import static com.gabrielittner.auto.value.util.AutoValueUtil.getAutoValueClassClassName;
import static com.gabrielittner.auto.value.util.AutoValueUtil.getFinalClassClassName;
import static com.gabrielittner.auto.value.util.AutoValueUtil.newFinalClassConstructorCall;
import static com.gabrielittner.auto.value.util.AutoValueUtil.newTypeSpecBuilder;
import static com.gabrielittner.auto.value.util.ElementUtil.getMatchingStaticMethod;
import static com.google.common.base.Preconditions.checkNotNull;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

@AutoService(AutoValueExtension.class)
public class AutoValueCursorExtension extends AutoValueExtension {

    private static final ClassName CURSOR = ClassName.get("android.database", "Cursor");
    private static final ClassName FUNC1 = ClassName.get("rx.functions", "Func1");
    private static final ClassName FUNCTION = ClassName.get("io.reactivex.functions", "Function");

    private static final String METHOD_NAME = "createFromCursor";
    private static final String FUNC1_FIELD_NAME = "MAPPER";
    private static final String FUNC1_METHOD_NAME = "call";
    private static final String FUNCTION_FIELD_NAME = "MAPPER_FUNCTION";
    private static final String FUNCTION_METHOD_NAME = "apply";

    @Override
    public boolean applicable(Context context) {
        TypeElement valueClass = context.autoValueClass();
        return getMatchingStaticMethod(valueClass, ClassName.get(valueClass), CURSOR).isPresent()
                || getMatchingStaticMethod(valueClass, getFunc1TypeName(context)).isPresent()
                || getMatchingStaticMethod(valueClass, getFunctionTypeName(context)).isPresent();
    }

    @Override
    public String generateClass(
            Context context, String className, String classToExtend, boolean isFinal) {
        ImmutableList<ColumnProperty> properties = ColumnProperty.from(context);

        TypeSpec.Builder subclass =
                newTypeSpecBuilder(context, className, classToExtend, isFinal)
                        .addMethod(createReadMethod(context, properties));

        if (ElementUtil.typeExists(context.processingEnvironment().getElementUtils(), FUNC1)) {
            subclass.addField(createRxJava1Mapper(context));
        }

        if (ElementUtil.typeExists(context.processingEnvironment().getElementUtils(), FUNCTION)) {
            subclass.addField(createRxJava2Mapper(context));
        }

        return JavaFile.builder(context.packageName(), subclass.build()).build().toString();
    }

    private MethodSpec createReadMethod(Context context, ImmutableList<ColumnProperty> properties) {
        MethodSpec.Builder readMethod =
                MethodSpec.methodBuilder(METHOD_NAME)
                        .addModifiers(STATIC)
                        .returns(getFinalClassClassName(context))
                        .addParameter(CURSOR, "cursor");

        ImmutableMap<ClassName, String> columnAdapters = addColumnAdaptersToMethod(readMethod, properties);

        String[] names = new String[properties.size()];
        for (int i = 0; i < properties.size(); i++) {
            ColumnProperty property = properties.get(i);
            names[i] = property.humanName();

            if (property.columnAdapter() != null) {
                readMethod.addStatement(
                        "$T $N = $L.fromCursor(cursor, $S)",
                        property.type(),
                        property.humanName(),
                        columnAdapters.get(property.columnAdapter()),
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

    private CodeBlock readProperty(ColumnProperty property) {
        CodeBlock getValue = CodeBlock.of(checkNotNull(property.cursorMethod()), getColumnIndexOrThrow(property));
        return CodeBlock.builder()
                .addStatement("$T $N = $L", property.type(), property.humanName(), getValue)
                .build();
    }

    private CodeBlock readNullableProperty(ColumnProperty property) {
        String columnIndexVar = property.humanName() + "ColumnIndex";
        String cursorMethod = checkNotNull(property.cursorMethod());
        CodeBlock getValue =
                CodeBlock.builder()
                        .add("($L == -1 || cursor.isNull($L)) ? null : ", columnIndexVar, columnIndexVar)
                        .add(cursorMethod, columnIndexVar)
                        .build();
        return CodeBlock.builder()
                .addStatement("int $L = $L", columnIndexVar, getColumnIndex(property))
                .addStatement("$T $N = $L", property.type(), property.humanName(), getValue)
                .build();
    }

    private CodeBlock getColumnIndexOrThrow(ColumnProperty property) {
        return CodeBlock.of("cursor.getColumnIndexOrThrow($S)", property.columnName());
    }

    private CodeBlock getColumnIndex(ColumnProperty property) {
        return CodeBlock.of("cursor.getColumnIndex($S)", property.columnName());
    }

    private FieldSpec createRxJava1Mapper(Context context) {
        TypeName func1Name = getFunc1TypeName(context);
        MethodSpec func1Method =
                MethodSpec.methodBuilder(FUNC1_METHOD_NAME)
                        .addAnnotation(Override.class)
                        .addModifiers(PUBLIC)
                        .addParameter(CURSOR, "c")
                        .returns(getFinalClassClassName(context))
                        .addStatement("return $L($N)", METHOD_NAME, "c")
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

    private FieldSpec createRxJava2Mapper(Context context) {
        TypeName functionName = getFunctionTypeName(context);
        MethodSpec functionMethod =
                MethodSpec.methodBuilder(FUNCTION_METHOD_NAME)
                        .addAnnotation(Override.class)
                        .addModifiers(PUBLIC)
                        .addParameter(CURSOR, "c")
                        .returns(getFinalClassClassName(context))
                        .addStatement("return $L($N)", METHOD_NAME, "c")
                        .build();
        TypeSpec function =
                TypeSpec.anonymousClassBuilder("")
                        .addSuperinterface(functionName)
                        .addMethod(functionMethod)
                        .build();
        return FieldSpec.builder(functionName, FUNCTION_FIELD_NAME, STATIC, FINAL)
                .initializer("$L", function)
                .build();
    }

    private TypeName getFunc1TypeName(Context context) {
        return ParameterizedTypeName.get(FUNC1, CURSOR, getAutoValueClassClassName(context));
    }

    private TypeName getFunctionTypeName(Context context) {
        return ParameterizedTypeName.get(FUNCTION, CURSOR, getAutoValueClassClassName(context));
    }

    public static ImmutableMap<ClassName, String> addColumnAdaptersToMethod(
            MethodSpec.Builder method,
            List<ColumnProperty> properties) {
        Map<ClassName, String> columnAdapters = new LinkedHashMap<>();
        NameAllocator nameAllocator = new NameAllocator();
        for (ColumnProperty property : properties) {
            ClassName adapter = property.columnAdapter();
            if (adapter != null && !columnAdapters.containsKey(adapter)) {
                String name = nameAllocator.newName(toLowerCase(adapter.simpleName()));
                method.addStatement("$1T $2L = new $1T()", adapter, name);
                columnAdapters.put(adapter, name);
            }
        }
        return ImmutableMap.copyOf(columnAdapters);
    }

    private static String toLowerCase(String s) {
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }
}

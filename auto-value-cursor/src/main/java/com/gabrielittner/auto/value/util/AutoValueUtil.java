package com.gabrielittner.auto.value.util;

import com.google.auto.value.extension.AutoValueExtension.Context;
import com.google.common.collect.Lists;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeParameterElement;

import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.FINAL;

public final class AutoValueUtil {

    private static String getAutoValueClassSimpleName(Context context) {
        return context.autoValueClass().getSimpleName().toString();
    }

    public static ClassName getAutoValueClassClassName(Context context) {
        return ClassName.get(context.packageName(), getAutoValueClassSimpleName(context));
    }

    private static String getFinalClassSimpleName(Context context) {
        return "AutoValue_" + context.autoValueClass().getSimpleName().toString();
    }

    public static ClassName getFinalClassClassName(Context context) {
        return ClassName.get(context.packageName(), getFinalClassSimpleName(context));
    }

    public static TypeSpec.Builder typeSpecBuilder(Context context, String className, String classToExtend, boolean isFinal) {
        List<? extends TypeParameterElement> typeParameters = context.autoValueClass().getTypeParameters();
        TypeVariableName[] typeVariables = new TypeVariableName[typeParameters.size()];
        for (int i = 0; i < typeParameters.size(); i++) {
            typeVariables[i] = TypeVariableName.get(typeParameters.get(i));
        }

        return TypeSpec.classBuilder(className)
                .addModifiers(isFinal ? FINAL : ABSTRACT)
                .addTypeVariables(Arrays.asList(typeVariables))
                .superclass(superClass(context, classToExtend, typeVariables))
                .addMethod(generateConstructor(context.properties()));
    }

    private static TypeName superClass(Context context, String classToExtend, TypeVariableName[] typeVariables) {
        ClassName superClassWithoutParameters = ClassName.get(context.packageName(), classToExtend);
        if (typeVariables.length > 0) {
            return ParameterizedTypeName.get(superClassWithoutParameters, (TypeName[]) typeVariables);
        } else {
            return superClassWithoutParameters;
        }
    }

    private static MethodSpec generateConstructor(Map<String, ExecutableElement> properties) {
        List<ParameterSpec> params = Lists.newArrayList();
        for (Map.Entry<String, ExecutableElement> entry : properties.entrySet()) {
            TypeName typeName = TypeName.get(entry.getValue().getReturnType());
            params.add(ParameterSpec.builder(typeName, entry.getKey()).build());
        }

        CodeBlock code = generateConstructorCall("super", properties.keySet().toArray());

        return MethodSpec.constructorBuilder()
                .addParameters(params)
                .addCode(code)
                .build();
    }

    public static CodeBlock generateFinalClassConstructorCall(Context context, Object[] properties) {
        String constructorName = "new " + getFinalClassSimpleName(context);
        return generateConstructorCall(constructorName, properties);
    }

    private static CodeBlock generateConstructorCall(String constructorName, Object[] properties) {
        StringBuilder format = new StringBuilder(constructorName)
                .append("(");
        for (int i = properties.length; i > 0; i--) {
            format.append("$N");
            if (i > 1) format.append(", ");
        }
        format.append(")");
        return CodeBlock.builder()
                .addStatement(format.toString(), properties)
                .build();
    }

    private AutoValueUtil() {
        throw new AssertionError("No instances.");
    }
}

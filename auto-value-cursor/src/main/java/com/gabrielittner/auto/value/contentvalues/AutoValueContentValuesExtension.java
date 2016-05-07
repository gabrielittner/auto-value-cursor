package com.gabrielittner.auto.value.contentvalues;

import com.gabrielittner.auto.value.ColumnProperty;
import com.gabrielittner.auto.value.util.Property;
import com.google.auto.service.AutoService;
import com.google.auto.value.extension.AutoValueExtension;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.Collections;
import java.util.Set;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import static com.gabrielittner.auto.value.cursor.AutoValueCursorExtension.getCursorMethod;
import static com.gabrielittner.auto.value.util.AutoValueUtil.newTypeSpecBuilder;
import static com.gabrielittner.auto.value.util.ElementUtil.getAbstractMethod;
import static com.gabrielittner.auto.value.util.ElementUtil.getStaticMethod;
import static com.gabrielittner.auto.value.util.ElementUtil.hasAbstractMethod;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.tools.Diagnostic.Kind.ERROR;

@AutoService(AutoValueExtension.class)
public class AutoValueContentValuesExtension extends AutoValueExtension {

    private static final ClassName CONTENT_VALUES = ClassName.get("android.content",
            "ContentValues");

    @Override
    public boolean applicable(Context context) {
        Elements elements = context.processingEnvironment().getElementUtils();
        TypeElement valueClass = context.autoValueClass();
        return hasAbstractMethod(elements, valueClass, null, CONTENT_VALUES);
    }

    @Override
    public Set<String> consumeProperties(Context context) {
        Elements elements = context.processingEnvironment().getElementUtils();
        TypeElement valueClass = context.autoValueClass();
        ExecutableElement method = getAbstractMethod(elements, valueClass, null, CONTENT_VALUES);
        String methodName = method.getSimpleName().toString();
        for (Property property :  ColumnProperty.from(context)) {
            if (property.methodName().equals(methodName)) {
                return ImmutableSet.of(methodName, property.humanName());
            }
        }
        return Collections.singleton(methodName);
    }

    @Override
    public String generateClass(Context context, String className, String classToExtend,
            boolean isFinal) {
        Elements elements = context.processingEnvironment().getElementUtils();
        TypeElement valueClass = context.autoValueClass();
        ExecutableElement method = getAbstractMethod(elements, valueClass, null, CONTENT_VALUES);
        ImmutableList<ColumnProperty> properties = ColumnProperty.from(context);

        TypeSpec.Builder subclass = newTypeSpecBuilder(context, className, classToExtend, isFinal)
                .addMethod(createToContentValuesMethod(context, method, properties));

        return JavaFile.builder(context.packageName(), subclass.build())
                .build()
                .toString();
    }

    private MethodSpec createToContentValuesMethod(Context context,
            ExecutableElement methodToImplement, ImmutableList<ColumnProperty> properties) {
        String methodName = methodToImplement.getSimpleName().toString();
        MethodSpec.Builder writeMethod = MethodSpec.methodBuilder(methodName)
                .addModifiers(PUBLIC)
                .returns(CONTENT_VALUES)
                .addStatement("$1T values = new $1T($2L)", CONTENT_VALUES, properties.size());

        Types typeUtils = context.processingEnvironment().getTypeUtils();
        for (ColumnProperty property : properties) {
            TypeMirror factoryTypeMirror = property.valuesAdapter();
            if (factoryTypeMirror != null) {
                TypeElement factoryType = (TypeElement) typeUtils.asElement(factoryTypeMirror);
                ExecutableElement method = getStaticMethod(factoryType, property.type(),
                        CONTENT_VALUES);
                if (method == null) {
                    String message = String.format("Class \"%s\" needs to define a public static "
                            + "method taking \"%s\" and returning \"ContentValues\"",
                            factoryType, property.type().toString());
                    context.processingEnvironment().getMessager()
                            .printMessage(ERROR, message, context.autoValueClass());
                    continue;
                }

                writeMethod.addStatement("$T $LValues = $T.$N($L())", CONTENT_VALUES,
                        property.humanName(), TypeName.get(factoryTypeMirror),
                        method.getSimpleName().toString(), property.methodName());
                writeMethod.addStatement("if ($1LValues != null) values.putAll($1LValues)",
                        property.humanName());
            } else if (getCursorMethod(property.type()) != null) {
                writeMethod.addStatement("values.put($S, $L())", property.columnName(),
                        property.methodName());
            } else {
                String message = String.format("Property \"%s\" has type \"%s\" that can't "
                        + "be put into ContentValues.", property.humanName(), property.type());
                context.processingEnvironment().getMessager()
                        .printMessage(ERROR, message, context.autoValueClass());
            }
        }
        return writeMethod.addStatement("return values")
                .build();
    }
}

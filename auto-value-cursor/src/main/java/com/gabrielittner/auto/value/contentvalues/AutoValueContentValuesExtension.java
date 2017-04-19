package com.gabrielittner.auto.value.contentvalues;

import com.gabrielittner.auto.value.ColumnProperty;
import com.google.auto.service.AutoService;
import com.google.auto.value.extension.AutoValueExtension;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import java.util.Collections;
import java.util.Set;
import javax.lang.model.element.ExecutableElement;

import static com.gabrielittner.auto.value.cursor.AutoValueCursorExtension.addColumnAdaptersToMethod;
import static com.gabrielittner.auto.value.util.AutoValueUtil.error;
import static com.gabrielittner.auto.value.util.AutoValueUtil.newTypeSpecBuilder;
import static com.gabrielittner.auto.value.util.ElementUtil.getMatchingAbstractMethod;
import static javax.lang.model.element.Modifier.PUBLIC;

@AutoService(AutoValueExtension.class)
public class AutoValueContentValuesExtension extends AutoValueExtension {

    private static final ClassName CONTENT_VALUES =
            ClassName.get("android.content", "ContentValues");

    @Override
    public boolean applicable(Context context) {
        return getMatchingAbstractMethod(context.abstractMethods(), CONTENT_VALUES).isPresent();
    }

    @Override
    public Set<ExecutableElement> consumeMethods(Context context) {
        Optional<ExecutableElement> method =
                getMatchingAbstractMethod(context.abstractMethods(), CONTENT_VALUES);
        if (method.isPresent()) {
            return Collections.singleton(method.get());
        }
        return Collections.emptySet();
    }

    @Override
    public String generateClass(
            Context context, String className, String classToExtend,boolean isFinal) {
        Optional<ExecutableElement> method =
                getMatchingAbstractMethod(context.abstractMethods(), CONTENT_VALUES);
        if (!method.isPresent()) throw new AssertionError("Method is null");
        ImmutableList<ColumnProperty> properties = ColumnProperty.from(context);

        TypeSpec.Builder subclass =
                newTypeSpecBuilder(context, className, classToExtend, isFinal)
                        .addMethod(createToContentValuesMethod(context, method.get(), properties));

        return JavaFile.builder(context.packageName(), subclass.build()).build().toString();
    }

    private MethodSpec createToContentValuesMethod(
            Context context,
            ExecutableElement methodToImplement,
            ImmutableList<ColumnProperty> properties) {
        String methodName = methodToImplement.getSimpleName().toString();

        MethodSpec.Builder writeMethod =
                MethodSpec.methodBuilder(methodName)
                        .addAnnotation(Override.class)
                        .addModifiers(PUBLIC)
                        .returns(CONTENT_VALUES)
                        .addStatement(
                                "$1T values = new $1T($2L)", CONTENT_VALUES, properties.size());

        ImmutableMap<ClassName, String> columnAdapters = addColumnAdaptersToMethod(writeMethod, properties);

        for (ColumnProperty property : properties) {
            if (property.columnAdapter() != null) {
                writeMethod.addStatement(
                        "$L.toContentValues(values, $S, $L())",
                        columnAdapters.get(property.columnAdapter()),
                        property.columnName(),
                        property.methodName());
            } else if (property.supportedType()) {
                writeMethod.addStatement(
                        "values.put($S, $L())", property.columnName(), property.methodName());
            } else {
                error(context, property, "Property has type that can't be put into ContentValues.");
            }
        }
        return writeMethod.addStatement("return values").build();
    }
}

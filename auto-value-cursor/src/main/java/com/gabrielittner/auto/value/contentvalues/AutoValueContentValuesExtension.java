package com.gabrielittner.auto.value.contentvalues;

import com.gabrielittner.auto.value.ColumnProperty;
import com.gabrielittner.auto.value.util.Property;
import com.google.auto.service.AutoService;
import com.google.auto.value.extension.AutoValueExtension;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import java.util.Collections;
import java.util.Set;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;

import static com.gabrielittner.auto.value.cursor.AutoValueCursorExtension.addColumnAdaptersToMethod;
import static com.gabrielittner.auto.value.cursor.AutoValueCursorExtension.error;
import static com.gabrielittner.auto.value.cursor.AutoValueCursorExtension.getColumnAdapters;
import static com.gabrielittner.auto.value.util.AutoValueUtil.newTypeSpecBuilder;
import static com.gabrielittner.auto.value.util.ElementUtil.getAbstractMethod;
import static com.gabrielittner.auto.value.util.ElementUtil.hasAbstractMethod;
import static javax.lang.model.element.Modifier.PUBLIC;

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
        for (Property property : ColumnProperty.from(context)) {
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
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .returns(CONTENT_VALUES)
                .addStatement("$1T values = new $1T($2L)", CONTENT_VALUES, properties.size());

        ImmutableMap<Property, FieldSpec> columnAdapters = getColumnAdapters(properties);
        addColumnAdaptersToMethod(writeMethod, properties, columnAdapters);

        for (ColumnProperty property : properties) {
            TypeMirror factory = property.columnAdapter();
            if (factory != null) {
                writeMethod.addStatement("$N.toContentValues(values, $S, $L())",
                        columnAdapters.get(property), property.columnName(), property.methodName());
            } else if (property.supportedType()) {
                writeMethod.addStatement("values.put($S, $L())", property.columnName(),
                        property.methodName());
            } else {
                error(context, property, "Property has type that can't be put into ContentValues.");
            }
        }
        return writeMethod.addStatement("return values")
                .build();
    }
}

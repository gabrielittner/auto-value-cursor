package com.gabrielittner.auto.value.contentvalues;

import com.google.auto.service.AutoService;
import com.google.auto.value.extension.AutoValueExtension;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

import static com.gabrielittner.auto.value.cursor.AutoValueCursorExtension.getColumnName;
import static com.gabrielittner.auto.value.cursor.AutoValueCursorExtension.getCursorMethod;
import static com.gabrielittner.auto.value.util.AutoValueUtil.typeSpecBuilder;
import static com.gabrielittner.auto.value.util.ElementUtil.getMethod;
import static com.gabrielittner.auto.value.util.ElementUtil.hasMethod;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.tools.Diagnostic.Kind.ERROR;

@AutoService(AutoValueExtension.class)
public class AutoValueContentValuesExtension extends AutoValueExtension {

    private static final ClassName CONTENT_VALUES = ClassName.get("android.content",
            "ContentValues");

    @Override
    public boolean applicable(Context context) {
        TypeElement valueClass = context.autoValueClass();
        return hasMethod(valueClass, true, false, null, CONTENT_VALUES);
    }

    @Override
    public Set<String> consumeProperties(Context context) {
        ExecutableElement method = getMethod(context.autoValueClass(), true, false, null,
                CONTENT_VALUES);
        return Collections.singleton(method.getSimpleName().toString());
    }

    @Override
    public String generateClass(Context context, String className, String classToExtend,
            boolean isFinal) {
        Map<String, ExecutableElement> properties = context.properties();
        ExecutableElement method = getMethod(context.autoValueClass(), true, false, null,
                CONTENT_VALUES);

        TypeSpec.Builder subclass = typeSpecBuilder(context, className, classToExtend, isFinal)
                .addMethod(createToContentValuesMethod(context, method, properties));

        return JavaFile.builder(context.packageName(), subclass.build())
                .build()
                .toString();
    }

    private MethodSpec createToContentValuesMethod(Context context,
            ExecutableElement method, Map<String, ExecutableElement> properties) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(method.getSimpleName().toString())
                .addModifiers(PUBLIC)
                .returns(CONTENT_VALUES)
                .addStatement("$1T values = new $1T($2L)", CONTENT_VALUES, properties.size());
        for (Map.Entry<String, ExecutableElement> entry : properties.entrySet()) {
            String name = entry.getKey();
            ExecutableElement element = entry.getValue();
            TypeName type = TypeName.get(element.getReturnType());

            if (getCursorMethod(type) == null) {
                String message = String.format("Property \"%s\" has type \"%s\" that can't be put "
                        + "into ContentValues.", name, type);
                context.processingEnvironment().getMessager()
                        .printMessage(ERROR, message, context.autoValueClass());
                continue;
            }

            String columnName = getColumnName(element);
            if (columnName == null) columnName = entry.getKey();

            builder.addStatement("values.put($S, $L())", columnName, name);
        }
        return builder.addStatement("return values")
                .build();
    }
}

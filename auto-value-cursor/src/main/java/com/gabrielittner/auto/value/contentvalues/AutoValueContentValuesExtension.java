package com.gabrielittner.auto.value.contentvalues;

import com.gabrielitter.auto.value.contentvalues.ValuesAdapter;
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
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

import static com.gabrielittner.auto.value.cursor.AutoValueCursorExtension.getColumnName;
import static com.gabrielittner.auto.value.cursor.AutoValueCursorExtension.getCursorMethod;
import static com.gabrielittner.auto.value.util.AnnotationUtil.getAnnotationMirror;
import static com.gabrielittner.auto.value.util.AnnotationUtil.getAnnotationValue;
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

    private MethodSpec createToContentValuesMethod(AutoValueExtension.Context context,
            ExecutableElement methodToImplement, Map<String, ExecutableElement> properties) {
        String methodName = methodToImplement.getSimpleName().toString();
        MethodSpec.Builder writeMethod = MethodSpec.methodBuilder(methodName)
                .addModifiers(PUBLIC)
                .returns(CONTENT_VALUES)
                .addStatement("$1T values = new $1T($2L)", CONTENT_VALUES, properties.size());

        Types typeUtils = context.processingEnvironment().getTypeUtils();
        for (Map.Entry<String, ExecutableElement> entry : properties.entrySet()) {
            String name = entry.getKey();
            ExecutableElement element = entry.getValue();
            TypeName type = TypeName.get(element.getReturnType());

            TypeMirror factoryTypeMirror = getFactoryTypeMirror(element);
            if (factoryTypeMirror != null) {
                TypeElement factoryType = (TypeElement) typeUtils.asElement(factoryTypeMirror);
                ExecutableElement method = getMethod(factoryType, false, true, type,
                        CONTENT_VALUES);
                if (method == null) {
                    String message = String.format("Class \"%s\" needs to define a public"
                                    + " static method taking \"%s\" and returning \"ContentValues\"",
                            factoryType, type.toString());
                    context.processingEnvironment().getMessager()
                            .printMessage(ERROR, message, context.autoValueClass());
                    continue;
                }

                writeMethod.addStatement("$1T $2LValues = $3T.$4N($2L())", CONTENT_VALUES, name,
                        TypeName.get(factoryTypeMirror), method.getSimpleName().toString());
                writeMethod.addStatement("if ($1LValues != null) values.putAll($1LValues)", name);
            } else if (getCursorMethod(type) != null) {
                String columnName = getColumnName(element);
                columnName = columnName != null ? columnName : name;
                writeMethod.addStatement("values.put($S, $L())", columnName, name);
            } else {
                String message = String.format("Property \"%s\" has type \"%s\" that can't "
                        + "be put into ContentValues.", name, type);
                context.processingEnvironment().getMessager()
                        .printMessage(ERROR, message, context.autoValueClass());
            }
        }
        return writeMethod.addStatement("return values")
                .build();
    }

    private static TypeMirror getFactoryTypeMirror(Element element) {
        AnnotationMirror annotationMirror = getAnnotationMirror(element, ValuesAdapter.class);
        if (annotationMirror == null) {
            return null;
        }
        AnnotationValue annotationValue = getAnnotationValue(annotationMirror, "value");
        return annotationValue == null ? null : (TypeMirror) annotationValue.getValue();
    }
}

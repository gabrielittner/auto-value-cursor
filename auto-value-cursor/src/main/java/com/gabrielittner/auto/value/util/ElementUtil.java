package com.gabrielittner.auto.value.util;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;

public final class ElementUtil {

    public static boolean hasMethod(TypeElement cls, boolean isAbstract, boolean isStatic,
            TypeName takes, TypeName returns) {
        return getMethod(cls, isAbstract, isStatic, takes, returns) != null;
    }

    public static ExecutableElement getMethod(TypeElement cls, boolean isAbstract, boolean isStatic,
            TypeName takes, TypeName returns) {
        List<? extends Element> elements = cls.getEnclosedElements();
        for (Element element : elements) {
            if (element.getKind() != ElementKind.METHOD) {
                continue;
            }
            if (isAbstract != element.getModifiers().contains(Modifier.ABSTRACT)) {
                continue;
            }
            if (isStatic != element.getModifiers().contains(Modifier.STATIC)) {
                continue;
            }

            ExecutableElement method = (ExecutableElement) element;
            if (methodTakes(method, takes) && methodReturns(method, returns)) {
                return method;
            }
        }
        return null;
    }

    private static boolean methodTakes(ExecutableElement method, TypeName takes) {
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
        return true;
    }

    private static boolean methodReturns(ExecutableElement method, TypeName returns) {
        return returns.equals(ClassName.get(method.getReturnType()));
    }

    public static boolean typeExists(Elements elements, ClassName className) {
        String name = className.toString();
        return elements.getTypeElement(name) != null;
    }

    private ElementUtil() {
        throw new AssertionError("No instances.");
    }
}

package com.gabrielitter.auto.value.contentvalues;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

@Retention(SOURCE)
@Target({METHOD, FIELD})
public @interface ValuesAdapter {
    Class<?> value();
}

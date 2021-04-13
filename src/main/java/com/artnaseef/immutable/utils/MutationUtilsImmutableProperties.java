package com.artnaseef.immutable.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MutationUtilsImmutableProperties {
    /**
     * List of property names, in the order they are passed to the constructor.
     *
     * The listed properties, and only the listed properties, are processed by ImmutableUtils.
     *
     * @return
     */
    String[] properties();
}

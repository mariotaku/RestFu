package org.mariotaku.restfu.annotation.param;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by mariotaku on 16/1/17.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Queries {
    KeyValue[] value() default {};

    /**
     * Library will trying to load {@link Queries#value()} from template class
     *
     * @return Template class
     */
    Class<?> template() default void.class;
}

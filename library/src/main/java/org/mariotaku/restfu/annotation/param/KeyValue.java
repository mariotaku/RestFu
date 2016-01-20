package org.mariotaku.restfu.annotation.param;

/**
 * Created by mariotaku on 16/1/17.
 */
public @interface KeyValue {
    String key();

    String value() default "";

    String valueKey() default "";

    char arrayDelimiter() default '\0';
}

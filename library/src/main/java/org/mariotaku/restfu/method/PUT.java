package org.mariotaku.restfu.method;

import org.mariotaku.restfu.RestMethod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by mariotaku on 15/2/7.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@RestMethod(value = "PUT", hasBody = true)
public @interface PUT {
    String METHOD = "PUT";

    String value();
}

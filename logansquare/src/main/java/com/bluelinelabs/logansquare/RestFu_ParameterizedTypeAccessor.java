package com.bluelinelabs.logansquare;

import com.bluelinelabs.logansquare.ParameterizedType.ConcreteParameterizedType;

import java.lang.reflect.Type;

public class RestFu_ParameterizedTypeAccessor {
    private RestFu_ParameterizedTypeAccessor() {
    }

    public static <T> ParameterizedType<T> create(Type type) {
        return new ConcreteParameterizedType<T>(type);
    }
}

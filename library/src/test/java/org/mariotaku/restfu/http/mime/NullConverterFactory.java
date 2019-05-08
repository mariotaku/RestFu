package org.mariotaku.restfu.http.mime;

import org.jetbrains.annotations.NotNull;
import org.mariotaku.restfu.RestConverter;
import org.mariotaku.restfu.http.HttpResponse;

import java.lang.reflect.Type;

/**
 * Created by mariotaku on 2017/3/25.
 */
class NullConverterFactory<E extends Exception> extends RestConverter.SimpleFactory<E> {
    @NotNull
    @Override
    public RestConverter<HttpResponse, ?, E> forResponse(@NotNull Type toType) {
        return null;
    }

}
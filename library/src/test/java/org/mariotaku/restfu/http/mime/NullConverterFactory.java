package org.mariotaku.restfu.http.mime;

import org.mariotaku.restfu.RestConverter;
import org.mariotaku.restfu.http.HttpResponse;

import java.lang.reflect.Type;

/**
 * Created by mariotaku on 2017/3/25.
 */
class NullConverterFactory<E extends Exception> implements RestConverter.Factory<E> {
    @Override
    public RestConverter<HttpResponse, ?, E> forResponse(Type toType) {
        return null;
    }

    @Override
    public RestConverter<?, Body, E> forRequest(Type fromType) {
        return null;
    }
}

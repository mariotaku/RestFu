package org.mariotaku.restfu.moshi;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import org.jetbrains.annotations.NotNull;
import org.mariotaku.restfu.RestConverter;
import org.mariotaku.restfu.http.ContentType;
import org.mariotaku.restfu.http.HttpResponse;
import org.mariotaku.restfu.http.mime.Body;
import org.mariotaku.restfu.http.mime.SimpleBody;
import org.mariotaku.restfu.http.mime.StringBody;

import java.io.IOException;
import java.lang.reflect.Type;

import okio.Okio;

/**
 * Created by mariotaku on 2019/5/8.
 */
public class MoshiConverterFactory<E extends Exception> extends RestConverter.SimpleFactory<E> {

    private final Moshi moshi;

    public MoshiConverterFactory(Moshi moshi) {
        this.moshi = moshi;
    }

    @NotNull
    @Override
    public RestConverter<HttpResponse, ?, E> forResponse(@NotNull Type type) throws RestConverter.ConvertException {
        return new JsonResponseConverter<>(this, type);
    }

    @NotNull
    @Override
    public RestConverter<?, Body, E> forRequest(@NotNull Type type) throws RestConverter.ConvertException {
        if (SimpleBody.supports(type)) {
            return new SimpleBodyConverter<>(type);
        }
        return new JsonRequestConverter<>(this, type);
    }

    protected <T> JsonAdapter<T> adapterFor(@NotNull Type type) {
        return moshi.adapter(type);
    }

    protected <T> JsonAdapter<T> adapterFor(@NotNull Class<T> type) {
        return moshi.adapter(type);
    }

    protected void processParsedObject(@NotNull Object parsed, @NotNull HttpResponse httpResponse) {

    }

    @NotNull
    private Object parseOrThrow(@NotNull HttpResponse response, @NotNull Type type)
            throws IOException, RestConverter.ConvertException {
        try {
            final Object parsed = adapterFor(type).fromJson(Okio.buffer(Okio.source(response.getBody().stream())));
            if (parsed == null) {
                throw new IOException("Empty data");
            }
            return parsed;
        } catch (IOException e) {
            throw new RestConverter.ConvertException("Malformed JSON Data", e);
        }
    }

    private static class JsonResponseConverter<E extends Exception> implements RestConverter<HttpResponse, Object, E> {
        private final MoshiConverterFactory<E> factory;
        private final Type type;

        JsonResponseConverter(MoshiConverterFactory<E> factory, Type type) {
            this.factory = factory;
            this.type = type;
        }

        @NotNull
        @Override
        public Object convert(@NotNull HttpResponse httpResponse) throws IOException, ConvertException, E {
            final Object object = factory.parseOrThrow(httpResponse, type);
            factory.processParsedObject(object, httpResponse);
            return object;
        }
    }

    private static class JsonRequestConverter<E extends Exception> implements RestConverter<Object, Body, E> {
        private final MoshiConverterFactory<E> factory;
        private final Type type;

        JsonRequestConverter(MoshiConverterFactory<E> factory, Type type) {
            this.factory = factory;
            this.type = type;
        }

        @NotNull
        @Override
        public Body convert(@NotNull Object request) {
            final String json = factory.adapterFor(type).toJson(request);
            return new StringBody(json, ContentType.parse("application/json"));
        }
    }
}

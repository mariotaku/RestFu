/*
 * Copyright (c) 2015 mariotaku
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mariotaku.restfu;


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mariotaku.restfu.http.BodyType;
import org.mariotaku.restfu.http.MultiValueMap;
import org.mariotaku.restfu.http.RawValue;
import org.mariotaku.restfu.http.ValueMap;
import org.mariotaku.restfu.http.mime.*;

import java.io.IOException;
import java.util.Map;

@SuppressWarnings("WeakerAccess")
public final class RestRequest {

    @NotNull
    private final String method;
    private final boolean hasBody;
    @NotNull
    private final String path;

    @NotNull
    private final MultiValueMap<String> headers;
    @NotNull
    private final MultiValueMap<String> queries;
    @Nullable
    private final MultiValueMap<Body> params;
    private final RawValue file;
    private final BodyType bodyType;
    private final Map<String, Object> extras;

    private Body bodyCache;
    private String inferredBodyType;

    public RestRequest(@NotNull String method, boolean hasBody, @NotNull String path,
            @NotNull MultiValueMap<String> headers, @NotNull MultiValueMap<String> queries,
            @Nullable MultiValueMap<Body> params, @Nullable RawValue file, @Nullable BodyType bodyType,
            @NotNull Map<String, Object> extras) {
        this.method = method;
        this.hasBody = hasBody;
        this.path = path;
        this.queries = queries;
        this.params = params;
        this.headers = headers;
        this.extras = extras;
        this.file = file;
        this.bodyType = bodyType;
    }

    public String getBodyType() {
        if (!hasBody) return null;
        if (inferredBodyType != null) return inferredBodyType;
        if (bodyType != null) return bodyType.value();
        return inferredBodyType = inferBodyType();
    }

    private String inferBodyType() {
        if (file != null) return BodyType.RAW;
        final MultiValueMap<Body> params = getParams();
        if (params == null) return BodyType.FORM;
        for (Pair<String, Body> pair : params.toList()) {
            if (pair.second == null) continue;
            if (!(pair.second instanceof StringBody)) return BodyType.MULTIPART;
        }
        return BodyType.FORM;
    }

    @NotNull
    public MultiValueMap<String> getQueries() {
        return queries;
    }

    @Nullable
    public MultiValueMap<Body> getParams() {
        return params;
    }

    @NotNull
    public MultiValueMap<String> getHeaders() {
        return headers;
    }

    public Map<String, Object> getExtras() {
        return extras;
    }

    public <E extends Exception> Body getBody(RestConverter.Factory<E> converterFactory) throws E,
            RestConverter.ConvertException, IOException {
        if (bodyCache != null) return bodyCache;
        final String bodyType = getBodyType();
        if (bodyType == null) return null;
        final MultiValueMap<Body> params = getParams();
        switch (bodyType) {
            case BodyType.FORM: {
                if (params == null) return null;
                bodyCache = FormBody.wrap(params);
                break;
            }
            case BodyType.MULTIPART: {
                if (params == null) return null;
                bodyCache = new MultipartBody(params.toList());
                break;
            }
            case BodyType.RAW: {
                if (file == null) return null;
                bodyCache = file.body(converterFactory);
                break;
            }
            case BodyType.CUSTOM: {
                if (params == null) return null;
                assert this.bodyType != null;
                bodyCache = getBodyConverter().convert(params, this.bodyType.converterArgs());
                break;
            }
        }
        return bodyCache;
    }

    @NotNull
    public String getPath() {
        return path;
    }

    @NotNull
    public String getMethod() {
        return method;
    }

    @NotNull
    private BodyConverter getBodyConverter() {
        assert this.bodyType != null;
        try {
            return bodyType.converter().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * <p>
     * Creates {@link RestRequest} from {@link RestMethod}
     * </p>
     * <p>
     * Use this method if you want to modify requests <b>before</b> normal HTTP request created.
     * </p>
     * <br>
     * <p>
     * When using OAuth authorization, this would be very useful, because normal HTTP request cannot
     * be modified once OAuth signature generated.
     * </p>
     */
    public interface Factory<E extends Exception> {

        RestRequest create(@NotNull RestMethod<E> restMethod, @NotNull RestConverter.Factory<E> factory,
                @Nullable ValueMap valuePool)
                throws RestConverter.ConvertException, IOException, E;
    }

    public static class DefaultFactory<E extends Exception> implements Factory<E> {
        @Override
        public RestRequest create(@NotNull RestMethod<E> restMethod, @NotNull RestConverter.Factory<E> factory,
                @Nullable ValueMap valuePool)
                throws RestConverter.ConvertException, IOException, E {
            return restMethod.toRestRequest(factory, valuePool);
        }
    }
}

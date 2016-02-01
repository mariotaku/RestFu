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


import org.mariotaku.restfu.http.BodyType;
import org.mariotaku.restfu.http.MultiValueMap;
import org.mariotaku.restfu.http.RawValue;
import org.mariotaku.restfu.http.ValueMap;
import org.mariotaku.restfu.http.mime.Body;
import org.mariotaku.restfu.http.mime.FormBody;
import org.mariotaku.restfu.http.mime.MultipartBody;
import org.mariotaku.restfu.http.mime.StringBody;

import java.io.IOException;
import java.util.Map;

public final class RestRequest {

    private final String method;
    private final boolean hasBody;
    private final String path;

    private final MultiValueMap<String> headers;
    private final MultiValueMap<String> queries;
    private final MultiValueMap<Body> params;
    private final RawValue file;
    private final BodyType bodyType;
    private final Map<String, Object> extras;

    private Body bodyCache;
    private String inferredBodyType;

    public RestRequest(String method, boolean hasBody, String path,
                       MultiValueMap<String> headers,
                       MultiValueMap<String> queries,
                       MultiValueMap<Body> params,
                       RawValue file, BodyType bodyType, Map<String, Object> extras) {
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
        for (Pair<String, Body> pair : getParams().toList()) {
            if (!(pair.second instanceof StringBody)) return BodyType.MULTIPART;
        }
        return BodyType.FORM;
    }

    public MultiValueMap<String> getQueries() {
        return queries;
    }

    public MultiValueMap<Body> getParams() {
        return params;
    }

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
        switch (bodyType) {
            case BodyType.FORM: {
                bodyCache = FormBody.wrap(getParams());
                break;
            }
            case BodyType.MULTIPART: {
                bodyCache = new MultipartBody(getParams().toList());
                break;
            }
            case BodyType.RAW: {
                bodyCache = file.body(converterFactory);
                break;
            }
        }
        return bodyCache;
    }

    public String getPath() {
        return path;
    }

    public String getMethod() {
        return method;
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

        RestRequest create(RestMethod<E> restMethod, RestConverter.Factory<E> factory, ValueMap valuePool)
                throws RestConverter.ConvertException, IOException, E;
    }

    public static class DefaultFactory<E extends Exception> implements Factory<E> {
        @Override
        public RestRequest create(RestMethod<E> restMethod, RestConverter.Factory<E> factory, ValueMap valuePool)
                throws RestConverter.ConvertException, IOException, E {
            return restMethod.toRestRequest(factory, valuePool);
        }
    }
}

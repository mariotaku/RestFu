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

package org.mariotaku.restfu.http;


import org.mariotaku.restfu.HttpRequestFactory;
import org.mariotaku.restfu.Pair;
import org.mariotaku.restfu.RestRequestInfo;
import org.mariotaku.restfu.http.mime.TypedData;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mariotaku on 15/2/7.
 */
public final class RestHttpRequest {

    private final String method;
    private final String url;
    private final List<Pair<String, String>> headers;
    private final TypedData body;
    private final Object extra;

    public String getMethod() {
        return method;
    }

    public String getUrl() {
        return url;
    }

    public List<Pair<String, String>> getHeaders() {
        return headers;
    }

    public TypedData getBody() {
        return body;
    }

    public Object getExtra() {
        return extra;
    }

    @Override
    public String toString() {
        return "RestRequest{" +
                "method='" + method + '\'' +
                ", url='" + url + '\'' +
                ", headers=" + headers +
                ", body=" + body +
                '}';
    }

    public RestHttpRequest(String method, String url, List<Pair<String, String>> headers, TypedData body, Object extra) {
        this.method = method;
        this.url = url;
        this.headers = headers;
        this.body = body;
        this.extra = extra;
    }

    public static final class Builder {
        private String method;
        private String url;
        private List<Pair<String, String>> headers;
        private TypedData body;
        private Object extra;

        public Builder() {
        }

        public Builder method(String method) {
            this.method = method;
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder headers(List<Pair<String, String>> headers) {
            this.headers = headers;
            return this;
        }

        public Builder body(TypedData body) {
            this.body = body;
            return this;
        }

        public Builder extra(Object extra) {
            this.extra = extra;
            return this;
        }

        public RestHttpRequest build() {
            return new RestHttpRequest(method, url, headers, body, extra);
        }
    }


    public static final class DefaultFactory implements HttpRequestFactory {

        @Override
        public RestHttpRequest create(Endpoint endpoint, RestRequestInfo requestInfo, Authorization authorization) {
            final String url = Endpoint.constructUrl(endpoint.getUrl(), requestInfo);
            final ArrayList<Pair<String, String>> headers = new ArrayList<>(requestInfo.getHeaders());

            if (authorization != null && authorization.hasAuthorization()) {
                headers.add(Pair.create("Authorization", authorization.getHeader(endpoint, requestInfo)));
            }
            return new RestHttpRequest(requestInfo.getMethod(), url, headers, requestInfo.getBody(), null);
        }
    }
}

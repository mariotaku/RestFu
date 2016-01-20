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


import org.mariotaku.restfu.RestRequest;
import org.mariotaku.restfu.http.mime.Body;

/**
 * Created by mariotaku on 15/2/7.
 */
public final class HttpRequest {

    private final String method;
    private final String url;
    private final MultiValueMap<String> headers;
    private final Body body;
    private final Object tag;

    public String getMethod() {
        return method;
    }

    public String getUrl() {
        return url;
    }

    public MultiValueMap<String> getHeaders() {
        return headers;
    }

    public Body getBody() {
        return body;
    }

    public Object getTag() {
        return tag;
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

    public HttpRequest(String method, String url, MultiValueMap<String> headers, Body body, Object tag) {
        this.method = method;
        this.url = url;
        this.headers = headers;
        this.body = body;
        this.tag = tag;
    }

    public static final class Builder {
        private String method;
        private String url;
        private MultiValueMap<String> headers;
        private Body body;
        private Object tag;

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

        public Builder headers(MultiValueMap<String> headers) {
            this.headers = headers;
            return this;
        }

        public Builder body(Body body) {
            this.body = body;
            return this;
        }

        public Builder tag(Object tag) {
            this.tag = tag;
            return this;
        }

        public HttpRequest build() {
            return new HttpRequest(method, url, headers, body, tag);
        }
    }


    public static class DefaultFactory implements Factory {

        @Override
        public HttpRequest create(Endpoint endpoint, RestRequest requestInfo, Authorization authorization) {
            final String url = Endpoint.constructUrl(endpoint.getUrl(), requestInfo);
            final MultiValueMap<String> headers = requestInfo.getHeaders();

            if (authorization != null && authorization.hasAuthorization()) {
                headers.add("Authorization", authorization.getHeader(endpoint, requestInfo));
            }
            return new HttpRequest(requestInfo.getMethod(), url, headers, requestInfo.getBody(), null);
        }
    }

    /**
     * Created by mariotaku on 15/5/25.
     */
    public interface Factory {
        HttpRequest create(Endpoint endpoint, RestRequest info, Authorization authorization);
    }
}

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

package org.mariotaku.restfu.okhttp;

import okhttp3.*;
import okio.BufferedSink;
import okio.Okio;
import org.mariotaku.restfu.Pair;
import org.mariotaku.restfu.http.*;
import org.mariotaku.restfu.http.mime.Body;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Created by mariotaku on 16/2/4.
 */
public class OkHttpRestClient implements RestHttpClient {

    private OkHttpClient client;

    public OkHttpRestClient(OkHttpClient client) {
        setClient(client);
    }

    @Override
    public HttpCall newCall(final HttpRequest httpRequest) {
        final Request.Builder builder = new Request.Builder();
        builder.method(httpRequest.getMethod(), RestToOkBody.wrap(httpRequest.getBody()));
        builder.url(httpRequest.getUrl());
        final MultiValueMap<String> headers = httpRequest.getHeaders();
        if (headers != null) {
            for (Pair<String, String> header : headers.toList()) {
                builder.addHeader(header.first, header.second);
            }
        }
        return new OkToRestCall(client.newCall(builder.build()));
    }

    @Override
    public void enqueue(final HttpCall call, final HttpCallback callback) {
        call.enqueue(callback);
    }

    public void setClient(OkHttpClient client) {
        if (client == null) throw new NullPointerException();
        this.client = client;
    }

    public OkHttpClient getClient() {
        return client;
    }

    private static class RestToOkBody extends RequestBody {
        private final Body body;

        public RestToOkBody(Body body) {
            this.body = body;
        }

        public static RequestBody wrap(Body body) {
            if (body == null) return null;
            return new RestToOkBody(body);
        }

        @Override
        public MediaType contentType() {
            final ContentType contentType = body.contentType();
            if (contentType == null) return null;
            return MediaType.parse(contentType.toHeader());
        }

        @Override
        public void writeTo(BufferedSink sink) throws IOException {
            body.writeTo(sink.outputStream());
        }

        @Override
        public long contentLength() throws IOException {
            return body.length();
        }
    }

    private static class OkResponse extends HttpResponse {
        private final Response response;
        private Body body;

        public OkResponse(Response response) {
            this.response = response;
        }

        @Override
        public int getStatus() {
            return response.code();
        }

        @Override
        public MultiValueMap<String> getHeaders() {
            final Headers headers = response.headers();
            return new MultiValueMap<>(headers.toMultimap(), true);
        }

        @Override
        public String getHeader(String name) {
            return response.header(name);
        }

        @Override
        public List<String> getHeaders(String name) {
            return response.headers(name);
        }

        @Override
        public Body getBody() {
            if (body != null) return body;
            return body = new OkResponseBody(response.body());
        }

        @Override
        public void close() throws IOException {
            if (body != null) {
                body.close();
                body = null;
            }
        }
    }

    private static class OkResponseBody implements Body {

        private final ResponseBody body;

        public OkResponseBody(ResponseBody body) {
            this.body = body;
        }

        @Override
        public ContentType contentType() {
            final MediaType mediaType = body.contentType();
            if (mediaType == null) return null;
            return ContentType.parse(mediaType.toString());
        }

        @Override
        public String contentEncoding() {
            return null;
        }

        @Override
        public long length() throws IOException {
            return body.contentLength();
        }

        @Override
        public long writeTo(OutputStream os) throws IOException {
            final BufferedSink sink = Okio.buffer(Okio.sink(os));
            final long result = sink.writeAll(body.source());
            sink.flush();
            return result;
        }

        @Override
        public InputStream stream() throws IOException {
            return body.byteStream();
        }

        @Override
        public void close() throws IOException {
            body.close();
        }
    }

    private static class OkToRestCall implements HttpCall {
        private final Call call;

        public OkToRestCall(Call call) {
            this.call = call;
        }

        @Override
        public HttpResponse execute() throws IOException {
            return new OkResponse(call.execute());
        }

        @Override
        public void enqueue(HttpCallback callback) {
            call.enqueue(new OkCallback(callback));
        }

        public void cancel() {
            call.cancel();
        }

        @Override
        public boolean isCanceled() {
            return call.isCanceled();
        }

        @Override
        public void close() throws IOException {
        }
    }

    private static class OkCallback implements Callback {
        private final HttpCallback callback;

        public OkCallback(HttpCallback callback) {
            this.callback = callback;
        }

        @Override
        public void onFailure(Call call, IOException e) {
            this.callback.failure(e);
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            this.callback.response(new OkResponse(response));
        }
    }
}

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

import android.os.Looper;
import com.squareup.okhttp.*;
import okio.BufferedSink;
import okio.Okio;
import org.mariotaku.restfu.Pair;
import org.mariotaku.restfu.http.*;
import org.mariotaku.restfu.http.mime.TypedData;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mariotaku on 15/5/5.
 */
public class OkHttpRestClient implements RestHttpClient {

    private final OkHttpClient client;

    public OkHttpRestClient(OkHttpClient client) {
        this.client = client;
    }

    @Override
    public RestHttpResponse execute(RestHttpRequest restHttpRequest) throws IOException {
        final Call call = newCall(restHttpRequest);
        return new OkRestHttpResponse(call.execute());
    }

    private Call newCall(final RestHttpRequest restHttpRequest) {
        final Request.Builder builder = new Request.Builder();
        builder.method(restHttpRequest.getMethod(), RestToOkBody.wrap(restHttpRequest.getBody()));
        builder.url(restHttpRequest.getUrl());
        final List<Pair<String, String>> headers = restHttpRequest.getHeaders();
        if (headers != null) {
            for (Pair<String, String> header : headers) {
                builder.addHeader(header.first, header.second);
            }
        }
        builder.tag(restHttpRequest.getExtra());
        return client.newCall(builder.build());
    }

    @Override
    public RestQueuedRequest enqueue(final RestHttpRequest request, final RestHttpCallback callback) {
        final Call call = newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(final Request request, final IOException e) {
                if (call.isCanceled()) {
                    callback.cancelled();
                    return;
                }
                callback.exception(e);
            }

            @Override
            public void onResponse(final Response response) throws IOException {
                if (call.isCanceled()) {
                    callback.cancelled();
                    return;
                }
                callback.callback(new OkRestHttpResponse(response));
            }
        });
        return OkHttpQueuedRequest.create(client, call);
    }

    private static class RestToOkBody extends RequestBody {
        private final TypedData body;

        public RestToOkBody(TypedData body) {
            this.body = body;
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

        public static RequestBody wrap(TypedData body) {
            if (body == null) return null;
            return new RestToOkBody(body);
        }
    }

    private static class OkRestHttpResponse extends RestHttpResponse {
        private final Response response;
        private TypedData body;

        public OkRestHttpResponse(Response response) {
            this.response = response;
        }

        @Override
        public int getStatus() {
            return response.code();
        }

        @Override
        public List<Pair<String, String>> getHeaders() {
            final Headers headers = response.headers();
            final ArrayList<Pair<String, String>> headersList = new ArrayList<>();
            for (int i = 0, j = headers.size(); i < j; i++) {
                headersList.add(Pair.create(headers.name(i), headers.value(i)));
            }
            return headersList;
        }

        @Override
        public String getHeader(String name) {
            return response.header(name);
        }

        @Override
        public String[] getHeaders(String name) {
            final List<String> values = response.headers(name);
            return values.toArray(new String[values.size()]);
        }

        @Override
        public TypedData getBody() {
            if (body != null) return body;
            return body = new OkToRestBody(response.body());
        }

        @Override
        public void close() throws IOException {
            if (body != null) {
                body.close();
                body = null;
            }
        }
    }

    private static class OkToRestBody implements TypedData {

        private final ResponseBody body;

        public OkToRestBody(ResponseBody body) {
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

    static abstract class OkHttpQueuedRequest implements RestQueuedRequest {
        final OkHttpClient client;
        final Call call;
        boolean cancelled;

        public OkHttpQueuedRequest(final OkHttpClient client, final Call call) {
            this.client = client;
            this.call = call;
        }

        @Override
        public boolean isCancelled() {
            return cancelled || call.isCanceled();
        }

        @Override
        public void cancel() {
            cancelled = true;
            cancelImpl();
        }

        protected abstract void cancelImpl();

        private static class Android extends OkHttpQueuedRequest {

            public Android(OkHttpClient client, Call call) {
                super(client, call);
            }

            @Override
            protected void cancelImpl() {
                if (Looper.myLooper() != Looper.getMainLooper()) {
                    call.cancel();
                } else {
                    client.getDispatcher().getExecutorService().execute(new Runnable() {
                        @Override
                        public void run() {
                            call.cancel();
                        }
                    });
                }
            }
        }

        public static RestQueuedRequest create(OkHttpClient client, Call call) {
            try {
                Class.forName("android.os.Build");
                return new Android(client, call);
            } catch (Exception e) {
                return new Base(client, call);
            }
        }

        private static class Base extends OkHttpQueuedRequest {
            public Base(OkHttpClient client, Call call) {
                super(client, call);
            }

            @Override
            protected void cancelImpl() {
                call.cancel();
            }
        }
    }
}

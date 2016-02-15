package org.mariotaku.restfu.urlconnection;

import org.mariotaku.restfu.Pair;
import org.mariotaku.restfu.RestFuUtils;
import org.mariotaku.restfu.http.*;
import org.mariotaku.restfu.http.mime.Body;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * Created by mariotaku on 16/2/12.
 */
public class URLConnectionRestClient implements RestHttpClient {
    @Override
    public HttpCall newCall(HttpRequest request) {
        return new URLConnectionCall(request);
    }

    @Override
    public void enqueue(HttpCall call, HttpCallback callback) {
        call.enqueue(callback);
    }

    class URLConnectionCall implements HttpCall {

        private final HttpRequest request;

        public URLConnectionCall(HttpRequest request) {
            this.request = request;
        }

        @Override
        public HttpResponse execute() throws IOException {
            URL url = new URL(request.getUrl());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(request.getMethod());
            for (Pair<String, String> pair : request.getHeaders().toList()) {
                conn.addRequestProperty(pair.first, pair.second);
            }
            conn.getResponseCode();
            return new URLConnectionResponse(conn);
        }

        @Override
        public void enqueue(HttpCallback callback) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void cancel() {

        }

        @Override
        public boolean isCanceled() {
            return false;
        }

        @Override
        public void close() throws IOException {

        }

        private class URLConnectionResponse extends HttpResponse {
            private final HttpURLConnection conn;

            public URLConnectionResponse(HttpURLConnection conn) {
                this.conn = conn;
            }

            @Override
            public int getStatus() {
                try {
                    return conn.getResponseCode();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public MultiValueMap<String> getHeaders() {
                MultiValueMap<String> headers = new MultiValueMap<>(true);
                for (Map.Entry<String, List<String>> entry : conn.getHeaderFields().entrySet()) {
                    final String key = entry.getKey();
                    for (String value : entry.getValue()) {
                        headers.add(key, value);
                    }
                }
                return headers;
            }

            @Override
            public Body getBody() {
                return new URLConnectionBody(conn);
            }

            @Override
            public void close() throws IOException {
                conn.disconnect();
            }

            private class URLConnectionBody implements Body {
                private final HttpURLConnection conn;

                public URLConnectionBody(HttpURLConnection conn) {
                    this.conn = conn;
                }

                @Override
                public ContentType contentType() {
                    return null;
                }

                @Override
                public String contentEncoding() {
                    return conn.getContentEncoding();
                }

                @Override
                public long length() throws IOException {
                    return conn.getContentLengthLong();
                }

                @Override
                public long writeTo(OutputStream os) throws IOException {
                    return RestFuUtils.copyStream(stream(), os);
                }

                @Override
                public InputStream stream() throws IOException {
                    InputStream is = conn.getErrorStream();
                    if (is == null) {
                        is = conn.getInputStream();
                    }
                    return is;
                }

                @Override
                public void close() throws IOException {
                    conn.disconnect();
                }
            }
        }
    }
}

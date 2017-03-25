package org.mariotaku.restfu.http.mime;

import org.jetbrains.annotations.NotNull;
import org.mariotaku.restfu.http.HttpCall;
import org.mariotaku.restfu.http.HttpCallback;
import org.mariotaku.restfu.http.HttpRequest;
import org.mariotaku.restfu.http.RestHttpClient;

import java.io.IOException;

/**
 * Created by mariotaku on 2017/3/25.
 */
class DirectThrowRestHttpClient implements RestHttpClient {
    @NotNull
    @Override
    public HttpCall newCall(@NotNull HttpRequest request) {
        return new DirectThrowCall();
    }

    @Override
    public void enqueue(@NotNull HttpCall call, @NotNull HttpCallback callback) {
        callback.failure(new IOException("Must throw"));
    }
}

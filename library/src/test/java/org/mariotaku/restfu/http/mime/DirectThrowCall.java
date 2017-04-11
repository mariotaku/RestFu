package org.mariotaku.restfu.http.mime;

import org.jetbrains.annotations.NotNull;
import org.mariotaku.restfu.http.HttpCall;
import org.mariotaku.restfu.http.HttpCallback;
import org.mariotaku.restfu.http.HttpResponse;

import java.io.IOException;

/**
 * Created by mariotaku on 2017/3/25.
 */
public class DirectThrowCall implements HttpCall {
    @NotNull
    @Override
    public HttpResponse execute() throws IOException {
        throw new IOException("Must throw");
    }

    @Override
    public void enqueue(@NotNull HttpCallback callback) {

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
}

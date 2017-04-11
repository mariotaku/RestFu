package org.mariotaku.restfu.http;

import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by mariotaku on 16/1/17.
 */
public interface HttpCall extends Closeable {

    @NotNull
    HttpResponse execute() throws IOException;

    void enqueue(@NotNull HttpCallback callback);

    void cancel();

    boolean isCanceled();
}

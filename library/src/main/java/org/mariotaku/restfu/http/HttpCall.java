package org.mariotaku.restfu.http;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by mariotaku on 16/1/17.
 */
public interface HttpCall extends Closeable {

    HttpResponse execute() throws IOException;

    void enqueue(HttpCallback callback);

    void cancel();

    boolean isCanceled();
}

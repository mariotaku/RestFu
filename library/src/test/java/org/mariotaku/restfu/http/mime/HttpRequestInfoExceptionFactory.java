package org.mariotaku.restfu.http.mime;

import org.jetbrains.annotations.NotNull;
import org.mariotaku.restfu.ExceptionFactory;
import org.mariotaku.restfu.http.HttpRequest;
import org.mariotaku.restfu.http.HttpResponse;

/**
 * Created by mariotaku on 2017/3/25.
 */
class HttpRequestInfoExceptionFactory implements ExceptionFactory<HttpRequestInfoException> {
    @NotNull
    @Override
    public HttpRequestInfoException newException(Throwable cause, HttpRequest request, HttpResponse response) {
        return new HttpRequestInfoException(request);
    }
}

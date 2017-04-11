package org.mariotaku.restfu.http.mime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mariotaku.restfu.ExceptionFactory;
import org.mariotaku.restfu.http.HttpRequest;
import org.mariotaku.restfu.http.HttpResponse;

/**
 * Created by mariotaku on 2017/3/25.
 */
class HttpRequestInfoExceptionFactory implements ExceptionFactory<HttpRequestInfoException> {
    @NotNull
    @Override
    public HttpRequestInfoException newException(@Nullable Throwable cause, @Nullable HttpRequest request, @Nullable HttpResponse response) {
        return new HttpRequestInfoException(request);
    }
}

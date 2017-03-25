package org.mariotaku.restfu.http.mime;

import org.mariotaku.restfu.http.HttpRequest;

/**
 * Created by mariotaku on 2017/3/25.
 */
public class HttpRequestInfoException extends Exception {
    HttpRequest request;

    public HttpRequestInfoException(HttpRequest request) {
        this.request = request;
    }
}

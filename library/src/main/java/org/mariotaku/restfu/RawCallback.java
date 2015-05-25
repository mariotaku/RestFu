package org.mariotaku.restfu;

import org.mariotaku.restfu.http.RestHttpResponse;

import java.io.IOException;

/**
 * Created by mariotaku on 15/2/7.
 */
public interface RawCallback extends ErrorCallback {
    void result(RestHttpResponse result) throws IOException;

}

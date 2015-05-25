package org.mariotaku.restfu;

import org.mariotaku.restfu.http.Authorization;
import org.mariotaku.restfu.http.Endpoint;
import org.mariotaku.restfu.http.RestHttpClient;

/**
 * Created by mariotaku on 15/4/19.
 */
public interface RestClient {
    Endpoint getEndpoint();

    RestHttpClient getRestClient();

    Converter getConverter();

    Authorization getAuthorization();

}

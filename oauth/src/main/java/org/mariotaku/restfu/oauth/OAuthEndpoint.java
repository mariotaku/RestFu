package org.mariotaku.restfu.oauth;

import org.mariotaku.restfu.http.Endpoint;

/**
 * Created by mariotaku on 15/2/6.
 */
public class OAuthEndpoint extends Endpoint {
    private final String signUrl;

    public OAuthEndpoint(String url) {
        this(url, url);
    }

    public OAuthEndpoint(String url, String signUrl) {
        super(url);
        this.signUrl = signUrl != null ? signUrl : url;
    }

    public String getSignUrl() {
        return signUrl;
    }
}

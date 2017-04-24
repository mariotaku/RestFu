package org.mariotaku.restfu.oauth2;

import org.jetbrains.annotations.NotNull;
import org.mariotaku.restfu.RestRequest;
import org.mariotaku.restfu.http.Authorization;
import org.mariotaku.restfu.http.Endpoint;

/**
 * Created by mariotaku on 2017/4/18.
 */
public class OAuth2Authorization implements Authorization {

    private final String accessToken;

    public OAuth2Authorization(@NotNull String accessToken) {
        this.accessToken = accessToken;
    }

    @Override
    public String getHeader(@NotNull Endpoint endpoint, @NotNull RestRequest info) {
        return "Bearer " + accessToken;
    }

    @Override
    public boolean hasAuthorization() {
        return true;
    }

    public String getAccessToken() {
        return accessToken;
    }
}

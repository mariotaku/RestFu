package org.mariotaku.restfu.oauth;


import org.jetbrains.annotations.NotNull;
import org.mariotaku.restfu.RestFuUtils;
import org.mariotaku.restfu.http.ValueMap;

import java.nio.charset.Charset;
import java.text.ParseException;

/**
 * Created by mariotaku on 15/2/4.
 */
public class OAuthToken implements ValueMap {

    private String screenName;
    private String userId;

    private String oauthToken, oauthTokenSecret;

    public String getScreenName() {
        return screenName;
    }

    public String getUserId() {
        return userId;
    }

    public String getOauthTokenSecret() {
        return oauthTokenSecret;
    }

    public String getOauthToken() {
        return oauthToken;
    }

    public OAuthToken(String oauthToken, String oauthTokenSecret) {
        this.oauthToken = oauthToken;
        this.oauthTokenSecret = oauthTokenSecret;
    }

    public OAuthToken(String body, Charset charset) throws ParseException {
        RestFuUtils.parseQuery(body, charset.name(), new RestFuUtils.KeyValueConsumer() {

            @Override
            public void consume(String key, String value) {
                switch (key) {
                    case "oauth_token": {
                        oauthToken = value;
                        break;
                    }
                    case "oauth_token_secret": {
                        oauthTokenSecret = value;
                        break;
                    }
                    case "user_id": {
                        userId = value;
                        break;
                    }
                    case "screen_name": {
                        screenName = value;
                        break;
                    }
                }
            }
        });
        if (oauthToken == null || oauthTokenSecret == null) {
            throw new ParseException("Unable to parse request token", -1);
        }
    }

    @Override
    public boolean has(@NotNull String key) {
        return "oauth_token".equals(key) || "oauth_token_secret".equals(key);
    }

    @Override
    public String toString() {
        return "OAuthToken{" +
                "screenName='" + screenName + '\'' +
                ", userId=" + userId +
                ", oauthToken='" + oauthToken + '\'' +
                ", oauthTokenSecret='" + oauthTokenSecret + '\'' +
                '}';
    }

    @Override
    public String get(@NotNull String key) {
        if ("oauth_token".equals(key)) {
            return oauthToken;
        } else if ("oauth_token_secret".equals(key)) {
            return oauthTokenSecret;
        }
        return null;
    }

    @NotNull
    @Override
    public String[] keys() {
        return new String[]{"oauth_token", "oauth_token_secret"};
    }

}

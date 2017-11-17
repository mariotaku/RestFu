package org.mariotaku.restfu.oauth;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mariotaku.commons.collection.Pair;
import org.mariotaku.restfu.RestRequest;
import org.mariotaku.restfu.http.Authorization;
import org.mariotaku.restfu.http.BodyType;
import org.mariotaku.restfu.http.Endpoint;
import org.mariotaku.restfu.http.MultiValueMap;
import org.mariotaku.restfu.http.mime.Body;
import org.mariotaku.restfu.http.mime.StringBody;
import org.mariotaku.restfu.http.mime.UrlSerialization;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;

/**
 * Created by mariotaku on 15/2/4.
 */
@SuppressWarnings("unused")
public class OAuthAuthorization implements Authorization {

    private static final UrlSerialization OAUTH_ENCODING = new UrlSerialization() {
        final BitSet allowedSet;

        {
            allowedSet = new BitSet(0xFF);
            allowedSet.set('-', true);
            allowedSet.set('.', true);
            allowedSet.set('_', true);
            allowedSet.set('~', true);
            for (int i = '0'; i <= '9'; i++) {
                allowedSet.set(i, true);
            }
            for (int i = 'A'; i <= 'Z'; i++) {
                allowedSet.set(i, true);
            }
            for (int i = 'a'; i <= 'z'; i++) {
                allowedSet.set(i, true);
            }
        }

        @Override
        protected void appendEscape(int codePoint, Charset charset, StringBuilder target) {
            if (codePoint <= 0xFF && allowedSet.get(codePoint)) {
                target.appendCodePoint(codePoint);
            } else {
                percentEncode(codePoint, charset, target);
            }
        }
    };

    private static final String DEFAULT_ENCODING = "UTF-8";
    private static final Charset DEFAULT_CHARSET = Charset.forName(DEFAULT_ENCODING);
    private static final String OAUTH_SIGNATURE_METHOD = "HMAC-SHA1";
    private static final String OAUTH_VERSION = "1.0";

    private final SecureRandom secureRandom = new SecureRandom();

    @NotNull
    private final String consumerKey, consumerSecret;
    @Nullable
    private final OAuthToken oauthToken;
    @Nullable
    private final String realm;

    @SuppressWarnings({"WeakerAccess", "unused"})
    public OAuthAuthorization(@NotNull String consumerKey, @NotNull String consumerSecret) {
        this(consumerKey, consumerSecret, null);
    }

    @SuppressWarnings("WeakerAccess")
    public OAuthAuthorization(@NotNull String consumerKey, @NotNull String consumerSecret,
            @Nullable OAuthToken oauthToken) {
        this(consumerKey, consumerSecret, oauthToken, null);
    }

    @SuppressWarnings("WeakerAccess")
    public OAuthAuthorization(@NotNull String consumerKey, @NotNull String consumerSecret,
            @Nullable OAuthToken oauthToken, @Nullable String realm) {
        this.consumerKey = consumerKey;
        this.consumerSecret = consumerSecret;
        this.oauthToken = oauthToken;
        this.realm = realm;
    }

    @SuppressWarnings("unused")
    @NotNull
    public String getConsumerKey() {
        return consumerKey;
    }

    @SuppressWarnings("unused")
    @NotNull
    public String getConsumerSecret() {
        return consumerSecret;
    }

    @SuppressWarnings("unused")
    @Nullable
    public OAuthToken getOauthToken() {
        return oauthToken;
    }

    @SuppressWarnings("unused")
    @Nullable
    public String getRealm() {
        return realm;
    }

    @Override
    public String getHeader(@NotNull Endpoint endpoint, @NotNull RestRequest request) {
        if (!(endpoint instanceof OAuthEndpoint))
            throw new IllegalArgumentException("OAuthEndpoint required");
        final Map<String, Object> extras = request.getExtras();
        String oauthToken = null, oauthTokenSecret = null;
        if (this.oauthToken != null) {
            oauthToken = this.oauthToken.getOauthToken();
            oauthTokenSecret = this.oauthToken.getOauthTokenSecret();
        } else if (extras != null) {
            oauthToken = (String) extras.get("oauth_token");
            oauthTokenSecret = (String) extras.get("oauth_token_secret");
        }
        final OAuthEndpoint oauthEndpoint = (OAuthEndpoint) endpoint;
        final String method = request.getMethod();
        final String url = Endpoint.constructUrl(oauthEndpoint.getSignUrl(), request);
        final MultiValueMap<String> queries = request.getQueries();
        final MultiValueMap<Body> params = request.getParams();
        final List<Pair<String, String>> encodeParams = generateOAuthParams(oauthToken, oauthTokenSecret,
                method, url, queries, params, request.getBodyType());
        final StringBuilder headerBuilder = new StringBuilder();
        headerBuilder.append("OAuth ");
        if (realm != null) {
            headerBuilder.append("realm=");
            headerBuilder.append(realm);
            headerBuilder.append(" ");
        }
        for (int i = 0, j = encodeParams.size(); i < j; i++) {
            if (i != 0) {
                headerBuilder.append(", ");
            }
            final Pair<String, String> keyValuePair = encodeParams.get(i);
            headerBuilder.append(keyValuePair.first);
            headerBuilder.append("=\"");
            headerBuilder.append(keyValuePair.second);
            headerBuilder.append('\"');
        }
        return headerBuilder.toString();
    }

    @Override
    public boolean hasAuthorization() {
        return true;
    }

    private String generateOAuthSignature(@NotNull String method, @NotNull String url, @NotNull String oauthNonce,
            long timestamp, @Nullable String oauthToken, @Nullable String oauthTokenSecret,
            @Nullable MultiValueMap<String> queries, @Nullable MultiValueMap<Body> params, @Nullable String bodyType) {
        final List<String> encodeParams = new ArrayList<>();
        encodeParams.add(encodeOAuthParameter("oauth_consumer_key", consumerKey));
        encodeParams.add(encodeOAuthParameter("oauth_nonce", oauthNonce));
        encodeParams.add(encodeOAuthParameter("oauth_signature_method", OAUTH_SIGNATURE_METHOD));
        encodeParams.add(encodeOAuthParameter("oauth_timestamp", String.valueOf(timestamp)));
        encodeParams.add(encodeOAuthParameter("oauth_version", OAUTH_VERSION));
        if (oauthToken != null) {
            encodeParams.add(encodeOAuthParameter("oauth_token", oauthToken));
        }
        if (queries != null) {
            for (Pair<String, String> query : queries.toList()) {
                encodeParams.add(encodeOAuthParameter(query.first, query.second));
            }
        }
        if (params != null && BodyType.FORM.equals(bodyType)) {
            for (Pair<String, Body> form : params.toList()) {
                final StringBody second = (StringBody) form.second;
                if (second != null) {
                    encodeParams.add(encodeOAuthParameter(form.first, second.value()));
                } else {
                    encodeParams.add(encodeOAuthParameter(form.first, null));
                }
            }
        }
        Collections.sort(encodeParams);
        final StringBuilder paramBuilder = new StringBuilder();
        for (int i = 0, j = encodeParams.size(); i < j; i++) {
            if (i != 0) {
                paramBuilder.append('&');
            }
            paramBuilder.append(encodeParams.get(i));
        }
        final StringBuilder signingKey = new StringBuilder();
        encodeOAuth(consumerSecret, signingKey);
        signingKey.append('&');
        if (oauthTokenSecret != null) {
            encodeOAuth(oauthTokenSecret, signingKey);
        }
        try {
            final Mac mac = Mac.getInstance("HmacSHA1");
            SecretKeySpec secret = new SecretKeySpec(signingKey.toString().getBytes(), mac.getAlgorithm());
            mac.init(secret);
            String urlNoQuery = url.indexOf('?') != -1 ? url.substring(0, url.indexOf('?')) : url;
            final StringBuilder baseString = new StringBuilder();
            encodeOAuth(method, baseString);
            baseString.append('&');
            encodeOAuth(urlNoQuery, baseString);
            baseString.append('&');
            encodeOAuth(paramBuilder.toString(), baseString);
            final byte[] signature = mac.doFinal(baseString.toString().getBytes(DEFAULT_ENCODING));
            return Base64.encodeNoWrap(signature);
        } catch (NoSuchAlgorithmException e) {
            throw new UnsupportedOperationException(e);
        } catch (InvalidKeyException | UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
    }

    private List<Pair<String, String>> generateOAuthParams(@Nullable final String oauthToken,
            @Nullable final String oauthTokenSecret, @NotNull final String method, @NotNull final String url,
            @Nullable final MultiValueMap<String> queries, @Nullable final MultiValueMap<Body> params,
            @Nullable final String bodyType) {
        final String oauthNonce = generateOAuthNonce();
        final long timestamp = System.currentTimeMillis() / 1000;
        final String oauthSignature = generateOAuthSignature(method, url, oauthNonce, timestamp, oauthToken,
                oauthTokenSecret, queries, params, bodyType);
        final List<Pair<String, String>> encodeParams = new ArrayList<>();
        encodeParams.add(Pair.create("oauth_consumer_key", consumerKey));
        encodeParams.add(Pair.create("oauth_nonce", oauthNonce));
        encodeParams.add(Pair.create("oauth_signature", encodeOAuth(oauthSignature)));
        encodeParams.add(Pair.create("oauth_signature_method", OAUTH_SIGNATURE_METHOD));
        encodeParams.add(Pair.create("oauth_timestamp", String.valueOf(timestamp)));
        encodeParams.add(Pair.create("oauth_version", OAUTH_VERSION));
        if (oauthToken != null) {
            encodeParams.add(Pair.create("oauth_token", oauthToken));
        }
        Collections.sort(encodeParams, new Comparator<Pair<String, String>>() {
            @Override
            public int compare(Pair<String, String> lhs, Pair<String, String> rhs) {
                return lhs.first.compareTo(rhs.first);
            }
        });
        return encodeParams;
    }

    private String encodeOAuthParameter(String key, String value) {
        final StringBuilder sb = new StringBuilder();
        encodeOAuth(key, sb);
        if (value != null) {
            sb.append('=');
            encodeOAuth(value, sb);
        }
        return sb.toString();
    }

    private static String encodeOAuth(final String value) {
        return OAUTH_ENCODING.serialize(value, DEFAULT_CHARSET);
    }

    private static void encodeOAuth(final String value, final StringBuilder sb) {
        OAUTH_ENCODING.serialize(value, DEFAULT_CHARSET, sb);
    }

    private static final char[] VALID_NONCE_CHARACTERS = {'0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o',
            'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F',
            'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W',
            'X', 'Y', 'Z'};

    private String generateOAuthNonce() {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 32; i++) {
            sb.append(VALID_NONCE_CHARACTERS[secureRandom.nextInt(VALID_NONCE_CHARACTERS.length)]);
        }
        return sb.toString();
    }

}

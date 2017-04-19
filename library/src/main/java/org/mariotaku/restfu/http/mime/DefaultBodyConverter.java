package org.mariotaku.restfu.http.mime;

import org.jetbrains.annotations.NotNull;
import org.mariotaku.restfu.http.MultiValueMap;

/**
 * Created by mariotaku on 2017/4/19.
 */
public final class DefaultBodyConverter implements BodyConverter {
    @NotNull
    @Override
    public Body convert(@NotNull MultiValueMap<Body> params, @NotNull String[] args) {
        throw new UnsupportedOperationException();
    }
}

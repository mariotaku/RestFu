/*
 * Copyright (c) 2015 mariotaku
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mariotaku.restfu.http;

import org.mariotaku.restfu.RestConverter;
import org.mariotaku.restfu.annotation.param.Raw;
import org.mariotaku.restfu.http.mime.SimpleBody;
import org.mariotaku.restfu.http.mime.Body;

import java.io.IOException;

/**
 * Created by mariotaku on 15/2/6.
 */
public final class RawValue {
    private final Raw annotation;
    private final Object value;

    @Override
    public String toString() {
        return "FileValue{" +
                "annotation=" + annotation +
                ", value=" + value +
                '}';
    }

    public RawValue(Raw annotation, Object value) {
        this.annotation = annotation;
        this.value = value;
    }

    public <E extends Exception> Body body(RestConverter.Factory<E> converterFactory) throws E, IOException,
            RestConverter.ConvertException {
        if (value == null) return null;
        //noinspection unchecked
        final RestConverter<Object, Body, E> converter =
                (RestConverter<Object, Body, E>) converterFactory.forRequest(value.getClass());
        if (converter != null) return converter.convert(value);
        return SimpleBody.wrap(value);
    }
}

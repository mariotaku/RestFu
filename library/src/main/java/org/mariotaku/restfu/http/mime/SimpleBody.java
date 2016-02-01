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

package org.mariotaku.restfu.http.mime;

import org.mariotaku.restfu.Utils;
import org.mariotaku.restfu.http.ContentType;
import org.mariotaku.restfu.io.StreamingGZIPInputStream;

import java.io.*;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

/**
 * Created by mariotaku on 15/2/7.
 */
public class SimpleBody implements Body {

    private final ContentType contentType;
    private final long contentLength;
    private final InputStream stream;
    private final String contentEncoding;

    public SimpleBody(ContentType contentType, String contentEncoding, long contentLength, InputStream stream) throws IOException {
        this.contentType = contentType;
        this.contentEncoding = contentEncoding;
        this.contentLength = contentLength;
        if ("gzip".equals(contentEncoding)) {
            this.stream = new StreamingGZIPInputStream(stream);
        } else {
            this.stream = stream;
        }
    }

    @Override
    public ContentType contentType() {
        return contentType;
    }

    @Override
    public String contentEncoding() {
        return contentEncoding;
    }

    @Override
    public long length() {
        return contentLength;
    }

    @Override
    public String toString() {
        return "BaseTypedData{" +
                "contentType=" + contentType +
                ", contentLength=" + contentLength +
                ", stream=" + stream +
                ", contentEncoding='" + contentEncoding + '\'' +
                '}';
    }

    @Override
    public long writeTo(OutputStream os) throws IOException {
        return Utils.copyStream(stream(), os);
    }


    @Override
    public InputStream stream() {
        return stream;
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }

    public static Body wrap(Object value) {
        if (value == null) return null;
        if (value instanceof Body) {
            return (Body) value;
        } else if (value instanceof File) {
            return new FileBody((File) value);
        } else if (value instanceof String) {
            return new StringBody((String) value, Charset.defaultCharset());
        } else if (value instanceof Number) {
            return new StringBody(value.toString(), Charset.defaultCharset());
        } else if (value instanceof Character) {
            return new StringBody(value.toString(), Charset.defaultCharset());
        } else if (value instanceof Boolean) {
            return new StringBody(value.toString(), Charset.defaultCharset());
        }
        throw new UnsupportedOperationException(value.getClass().toString());
    }

    public static boolean supports(Type value) {
        if (value instanceof Class) {
            return supportsClass((Class<?>) value);
        } else if (value instanceof ParameterizedType) {
            Type rawType = ((ParameterizedType) value).getRawType();
            if (rawType instanceof Class) {
                return supportsClass((Class<?>) rawType);
            }
        }
        return false;
    }

    private static boolean supportsClass(Class<?> value) {
        if (Body.class.isAssignableFrom(value)) {
            return true;
        } else if (value == File.class) {
            return true;
        } else if (value == String.class) {
            return true;
        } else if (Number.class.isAssignableFrom(value)) {
            return true;
        } else if (value == Character.class) {
            return true;
        } else if (value == Boolean.class) {
            return true;
        }
        return false;
    }

    public static Reader reader(Body data) throws IOException {
        final ContentType contentType = data.contentType();
        final Charset charset = contentType != null ? contentType.getCharset() : null;
        return new InputStreamReader(data.stream(), charset != null ? charset : Charset.defaultCharset());
    }
}

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

import org.mariotaku.restfu.RestFuUtils;
import org.mariotaku.restfu.http.ContentType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

/**
 * An immutable string body
 */
public final class StringBody implements Body {

    private final ContentType contentType;
    private final String value;
    private ByteArrayInputStream is;

    public StringBody(String string, Charset charset) {
        this(string, ContentType.parse("text/plain").charset(charset));
    }

    public StringBody(String value, final ContentType contentType) {
        this.value = value;
        this.contentType = contentType;
    }


    @Override
    public ContentType contentType() {
        return contentType;
    }

    @Override
    public String contentEncoding() {
        return null;
    }

    @Override
    public long length() throws IOException {
        return getBytes().length;
    }


    public String value() {
        return value;
    }

    private byte[] getBytes() {
        if (contentType != null) {
            final Charset charset = contentType.getCharset();
            if (charset != null) {
                return value.getBytes(charset);
            }
        }
        return value.getBytes();
    }

    @Override
    public long writeTo(OutputStream os) throws IOException {
        os.write(getBytes());
        return getBytes().length;
    }


    @Override
    public InputStream stream() throws IOException {
        if (is != null) return is;
        return is = new ByteArrayInputStream(getBytes());
    }

    @Override
    public void close() throws IOException {
        RestFuUtils.closeSilently(is);
    }
}

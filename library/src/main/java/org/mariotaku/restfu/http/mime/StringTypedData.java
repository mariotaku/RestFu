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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.mariotaku.restfu.Utils;
import org.mariotaku.restfu.http.ContentType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

/**
 * An immutable string body
 */
public final class StringTypedData implements TypedData {

    private final ContentType contentType;
    private final byte[] data;
    private ByteArrayInputStream is;

    public StringTypedData(@NonNull String string, @NonNull Charset charset) {
        this(string, ContentType.parse("text/plain").charset(charset));
    }

    public StringTypedData(@NonNull String string, @NonNull final ContentType contentType) {
        this.contentType = contentType;
        final Charset charset = contentType.getCharset();
        if (charset == null)
            throw new NullPointerException("Charset must be specified in ContentType");
        try {
            this.data = string.getBytes(charset.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
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
        return data.length;
    }

    @Override
    public long writeTo(@NonNull OutputStream os) throws IOException {
        os.write(data);
        return data.length;
    }

    @NonNull
    @Override
    public InputStream stream() throws IOException {
        if (is != null) return is;
        return is = new ByteArrayInputStream(data);
    }

    @Override
    public void close() throws IOException {
        Utils.closeSilently(is);
    }
}

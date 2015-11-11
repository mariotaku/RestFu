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

import okio.BufferedSink;
import okio.Okio;
import org.mariotaku.restfu.http.ContentType;
import org.mariotaku.restfu.io.StreamingGZIPInputStream;

import java.io.*;
import java.nio.charset.Charset;

/**
 * Created by mariotaku on 15/2/7.
 */
public class BaseTypedData implements TypedData {

    private final ContentType contentType;
    private final long contentLength;
    private final InputStream stream;
    private final String contentEncoding;

    public BaseTypedData(ContentType contentType, String contentEncoding, long contentLength, InputStream stream) throws IOException {
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
        final BufferedSink sink = Okio.buffer(Okio.sink(os));
        long result = sink.writeAll(Okio.source(stream));
        sink.flush();
        return result;
    }


    @Override
    public InputStream stream() {
        return stream;
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }

    public static TypedData wrap(Object value) {
        if (value instanceof TypedData) {
            return (TypedData) value;
        } else if (value instanceof java.io.File) {
            return new FileTypedData((java.io.File) value);
        } else if (value instanceof String) {
            return new StringTypedData((String) value, Charset.defaultCharset());
        }
        throw new UnsupportedOperationException();
    }

    public static Reader reader(TypedData data) throws IOException {
        final ContentType contentType = data.contentType();
        final Charset charset = contentType != null ? contentType.getCharset() : null;
        return new InputStreamReader(data.stream(), charset != null ? charset : Charset.defaultCharset());
    }
}

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
import android.util.Pair;

import org.mariotaku.restfu.Utils;
import org.mariotaku.restfu.http.ContentType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by mariotaku on 15/5/5.
 */
public class MultipartTypedBody implements TypedData {
    private final List<Pair<String, TypedData>> parts;
    private final ContentType contentType;


    private static final byte[] COLONSPACE = {':', ' '};
    private static final byte[] CRLF = {'\r', '\n'};
    private static final byte[] DASHDASH = {'-', '-'};
    private final String boundary;

    private boolean lengthSet;
    private long length;

    public MultipartTypedBody(List<Pair<String, TypedData>> parts) {
        this.parts = parts;
        this.contentType = ContentType.parse("multipart/form-data");
        boundary = Utils.bytesToHex(UUID.randomUUID().toString().getBytes());
        contentType.addParameter("boundary", boundary);
    }

    public MultipartTypedBody() {
        this(new ArrayList<Pair<String, TypedData>>());
    }

    public void add(@NonNull String name, @NonNull TypedData data) {
        parts.add(Pair.create(name, data));
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
        if (!lengthSet) {
            length = 0;
            for (Pair<String, TypedData> part : parts) {
                length += part.second.length();
            }
            lengthSet = true;
        }
        return length;
    }

    @Override
    public void writeTo(@NonNull OutputStream os) throws IOException {
        for (Pair<String, TypedData> part : parts) {
            os.write(DASHDASH);
            os.write(boundary.getBytes());
            os.write(CRLF);
            final ContentType contentDisposition = new ContentType("form-data").parameter("name", part.first);
            final ContentType contentType = part.second.contentType();
            final long contentLength = part.second.length();
            if (part.second instanceof FileTypedData) {
                contentDisposition.addParameter("filename", ((FileTypedData) part.second).fileName());
            }
            os.write("Content-Disposition".getBytes());
            os.write(COLONSPACE);
            os.write(contentDisposition.toHeader().getBytes());
            os.write(CRLF);
            if (contentType != null) {
                os.write("Content-Type".getBytes());
                os.write(COLONSPACE);
                os.write(contentType.toHeader().getBytes());
                os.write(CRLF);
            }
            if (contentLength != -1) {
                os.write("Content-Length".getBytes());
                os.write(COLONSPACE);
                os.write(String.valueOf(contentLength).getBytes());
                os.write(CRLF);
            }
            os.write(CRLF);
            part.second.writeTo(os);
            os.write(CRLF);
        }
        os.write(DASHDASH);
        os.write(boundary.getBytes());
        os.write(DASHDASH);
        os.write(CRLF);
    }

    @NonNull
    @Override
    public InputStream stream() throws IOException {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        writeTo(os);
        return new ByteArrayInputStream(os.toByteArray());
    }

    @Override
    public void close() throws IOException {
        for (Pair<String, TypedData> part : parts) {
            part.second.close();
        }
    }
}

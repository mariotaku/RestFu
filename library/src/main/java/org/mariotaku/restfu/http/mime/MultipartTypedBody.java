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
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by mariotaku on 15/5/5.
 */
public class MultipartTypedBody implements TypedData {

    public static final byte[] CONTENT_DISPOSITION;
    public static final byte[] CONTENT_TYPE;
    public static final byte[] CONTENT_LENGTH;

    static {
        try {
            CONTENT_DISPOSITION = "Content-Disposition".getBytes("ASCII");
            CONTENT_TYPE = "Content-Type".getBytes("ASCII");
            CONTENT_LENGTH = "Content-Length".getBytes("ASCII");
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError();
        }
    }

    private final List<Pair<String, TypedData>> parts;
    private final ContentType contentType;


    private static final byte[] COLONSPACE = {':', ' '};
    private static final byte[] CRLF = {'\r', '\n'};
    private static final byte[] DASHDASH = {'-', '-'};
    private final byte[] boundaryBytes;

    private boolean lengthSet;
    private long length;

    public MultipartTypedBody(List<Pair<String, TypedData>> parts) {
        this.parts = parts;
        this.contentType = ContentType.parse("multipart/form-data");
        final String boundary = Utils.bytesToHex(UUID.randomUUID().toString().getBytes());
        contentType.addParameter("boundary", boundary);
        boundaryBytes = boundary.getBytes();
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
            long tempLength = 0;
            for (Pair<String, TypedData> part : parts) {
                tempLength += DASHDASH.length;
                tempLength += boundaryBytes.length;
                tempLength += CRLF.length;
                final ContentType contentDisposition = new ContentType("form-data").parameter("name", part.first);
                final ContentType contentType = part.second.contentType();
                final long contentLength = part.second.length();
                if (contentLength < 0) return -1;
                if (part.second instanceof FileTypedData) {
                    contentDisposition.addParameter("filename", ((FileTypedData) part.second).fileName());
                }
                tempLength += CONTENT_DISPOSITION.length;
                tempLength += COLONSPACE.length;
                tempLength += contentDisposition.toHeader().getBytes().length;
                tempLength += CRLF.length;
                if (contentType != null) {
                    tempLength += CONTENT_TYPE.length;
                    tempLength += COLONSPACE.length;
                    tempLength += contentType.toHeader().getBytes().length;
                    tempLength += CRLF.length;
                }
                if (contentLength != -1) {
                    tempLength += CONTENT_LENGTH.length;
                    tempLength += COLONSPACE.length;
                    tempLength += String.valueOf(contentLength).getBytes().length;
                    tempLength += CRLF.length;
                }
                tempLength += CRLF.length;
                tempLength += contentLength;
                tempLength += CRLF.length;
            }
            tempLength += DASHDASH.length;
            tempLength += boundaryBytes.length;
            tempLength += DASHDASH.length;
            tempLength += CRLF.length;
            length = tempLength;
            lengthSet = true;
        }
        return length;
    }

    @Override
    public void writeTo(@NonNull OutputStream os) throws IOException {
        for (Pair<String, TypedData> part : parts) {
            os.write(DASHDASH);
            os.write(boundaryBytes);
            os.write(CRLF);
            final ContentType contentDisposition = new ContentType("form-data").parameter("name", part.first);
            final ContentType contentType = part.second.contentType();
            final long contentLength = part.second.length();
            if (part.second instanceof FileTypedData) {
                contentDisposition.addParameter("filename", ((FileTypedData) part.second).fileName());
            }
            os.write(CONTENT_DISPOSITION);
            os.write(COLONSPACE);
            os.write(contentDisposition.toHeader().getBytes());
            os.write(CRLF);
            if (contentType != null) {
                os.write(CONTENT_TYPE);
                os.write(COLONSPACE);
                os.write(contentType.toHeader().getBytes());
                os.write(CRLF);
            }
            if (contentLength != -1) {
                os.write(CONTENT_LENGTH);
                os.write(COLONSPACE);
                os.write(String.valueOf(contentLength).getBytes());
                os.write(CRLF);
            }
            os.write(CRLF);
            part.second.writeTo(os);
            os.write(CRLF);
        }
        os.write(DASHDASH);
        os.write(boundaryBytes);
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

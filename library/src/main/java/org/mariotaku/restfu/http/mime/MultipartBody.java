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


import org.mariotaku.restfu.Pair;
import org.mariotaku.restfu.RestFuUtils;
import org.mariotaku.restfu.http.ContentType;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by mariotaku on 15/5/5.
 */
public class MultipartBody implements Body {

    private static final byte[] CONTENT_DISPOSITION = {'C', 'o', 'n', 't', 'e', 'n', 't', '-', 'D',
            'i', 's', 'p', 'o', 's', 'i', 't', 'i', 'o', 'n'};
    private static final byte[] CONTENT_TYPE = {'C', 'o', 'n', 't', 'e', 'n', 't', '-', 'T',
            'y', 'p', 'e'};
    private static final byte[] CONTENT_LENGTH = {'C', 'o', 'n', 't', 'e', 'n', 't', '-', 'L',
            'e', 'n', 'g', 't', 'h'};
    private static final byte[] COLONSPACE = {':', ' '};
    private static final byte[] CRLF = {'\r', '\n'};
    private static final byte[] DASHDASH = {'-', '-'};

    private final List<Pair<String, Body>> parts;
    private final ContentType contentType;
    private final byte[] boundaryBytes;

    private boolean lengthSet;
    private long length;

    public MultipartBody(List<Pair<String, Body>> parts) {
        this.parts = parts;
        this.contentType = ContentType.parse("multipart/form-data");
        final String boundary = RestFuUtils.bytesToHex(UUID.randomUUID().toString().getBytes());
        contentType.addParameter("boundary", boundary);
        boundaryBytes = boundary.getBytes();
    }

    public MultipartBody() {
        this(new ArrayList<Pair<String, Body>>());
    }

    public void add(final String name, final Body data) {
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
            final LengthCountOutputStream os = new LengthCountOutputStream();
            writeBody(os);
            length = os.length();
            lengthSet = true;
        }
        return length;
    }

    @Override
    public long writeTo(OutputStream os) throws IOException {
        return writeBody(os);
    }

    @Override
    public InputStream stream() throws IOException {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        writeTo(os);
        return new ByteArrayInputStream(os.toByteArray());
    }

    @Override
    public void close() throws IOException {
        for (Pair<String, Body> part : parts) {
            part.second.close();
        }
    }

    private long writeBody(final OutputStream os) throws IOException {
        long totalLength = 0;
        for (Pair<String, Body> part : parts) {
            totalLength += write(os, DASHDASH);
            totalLength += write(os, boundaryBytes);
            totalLength += write(os, CRLF);
            final ContentType contentDisposition = new ContentType("form-data").parameter("name", part.first);
            final ContentType contentType = part.second.contentType();
            final long contentLength = part.second.length();
            if (part.second instanceof FileBody) {
                contentDisposition.addParameter("filename", ((FileBody) part.second).fileName());
            }
            totalLength += write(os, CONTENT_DISPOSITION);
            totalLength += write(os, COLONSPACE);
            totalLength += write(os, contentDisposition.toHeader().getBytes());
            totalLength += write(os, CRLF);
            if (contentType != null) {
                totalLength += write(os, CONTENT_TYPE);
                totalLength += write(os, COLONSPACE);
                totalLength += write(os, contentType.toHeader().getBytes());
                totalLength += write(os, CRLF);
            }
            if (contentLength != -1) {
                totalLength += write(os, CONTENT_LENGTH);
                totalLength += write(os, COLONSPACE);
                totalLength += write(os, String.valueOf(contentLength).getBytes());
                totalLength += write(os, CRLF);
            }
            totalLength += write(os, CRLF);
            if (os instanceof LengthCountOutputStream) {
                final LengthCountOutputStream lcos = (LengthCountOutputStream) os;
                if (contentLength == -1) {
                    lcos.markNoLength();
                }
                lcos.add(contentLength);
                totalLength += contentLength;
            } else {
                totalLength += part.second.writeTo(os);
            }
            totalLength += write(os, CRLF);
        }
        totalLength += write(os, DASHDASH);
        totalLength += write(os, boundaryBytes);
        totalLength += write(os, DASHDASH);
        totalLength += write(os, CRLF);
        return totalLength;
    }

    private long write(final OutputStream os, final byte[] bytes) throws IOException {
        os.write(bytes);
        return bytes.length;
    }


    private static class LengthCountOutputStream extends OutputStream {

        private boolean noLength;
        private long length;

        LengthCountOutputStream() {
            reset();
        }

        @Override
        public void write(final byte[] buffer, final int offset, final int count) throws IOException {
            RestFuUtils.checkOffsetAndCount(buffer.length, offset, count);
            add(count);
        }

        @Override
        public void write(final int oneByte) throws IOException {
            add(1);
        }

        public void add(long added) {
            length += added;
        }

        public void markNoLength() {
            noLength = true;
        }

        public void reset() {
            length = 0;
            noLength = false;
        }

        public long length() {
            if (noLength) return -1;
            return length;
        }
    }
}

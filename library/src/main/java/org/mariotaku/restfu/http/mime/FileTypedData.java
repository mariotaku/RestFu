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

import org.mariotaku.restfu.Utils;
import org.mariotaku.restfu.http.ContentType;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by mariotaku on 15/5/6.
 */
public class FileTypedData implements TypedData {

    private long length = -1;
    private File file;
    private ContentType contentType;
    private String fileName;
    private InputStream stream;

    public FileTypedData(File file, ContentType contentType) {
        this.file = file;
        this.contentType = contentType;
    }

    public FileTypedData(InputStream stream, String fileName, long length, ContentType contentType) {
        this.stream = stream;
        this.fileName = fileName;
        this.length = length;
        this.contentType = contentType;
    }

    public FileTypedData(File file) {
        this(file, null);
    }

    @Override
    public long length() {
        if (length != -1) return length;
        if (file == null) return -1;
        return length = file.length();
    }

    @Override
    public void writeTo(@NonNull OutputStream os) throws IOException {
        Utils.copyStream(stream(), os);
    }

    @NonNull
    @Override
    public InputStream stream() throws IOException {
        if (stream != null) return stream;
        return stream = new FileInputStream(file);
    }

    @Override
    public void close() throws IOException {
        if (stream != null) {
            stream.close();
        }
    }

    @Override
    public ContentType contentType() {
        if (contentType == null) {
            return ContentType.OCTET_STREAM;
        }
        return contentType;
    }

    @Override
    public String contentEncoding() {
        return null;
    }

    public String fileName() {
        if (fileName != null) return fileName;
        return fileName = file.getName();
    }
}

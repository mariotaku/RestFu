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
import org.mariotaku.restfu.http.MultiValueMap;

import java.io.*;
import java.nio.charset.Charset;

/**
 * Created by mariotaku on 15/2/6.
 */
public class FormBody implements Body {

    private final MultiValueMap<String> forms;
    private final Charset charset;

    private byte[] bytes;

    public FormBody(MultiValueMap<String> forms) {
        this.forms = forms;
        this.charset = Charset.forName("UTF-8");
    }

    @Override
    public ContentType contentType() {
        return new ContentType("application/x-www-form-urlencoded", charset);
    }

    @Override
    public String contentEncoding() {
        return null;
    }

    private void toRawBytes() {
        if (bytes != null) return;
        final StringBuilder sb = new StringBuilder();
        RestFuUtils.append(sb, forms, charset);
        try {
            bytes = sb.toString().getBytes(charset.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long length() {
        toRawBytes();
        return bytes.length;
    }

    @Override
    public long writeTo(OutputStream os) throws IOException {
        toRawBytes();
        os.write(bytes);
        return bytes.length;
    }

    @Override
    public InputStream stream() {
        toRawBytes();
        return new ByteArrayInputStream(bytes);
    }

    @Override
    public void close() throws IOException {
        // No-op
    }

    public static Body wrap(MultiValueMap<Body> params) {
        final MultiValueMap<String> forms = new MultiValueMap<>();
        for (Pair<String, Body> pair : params.toList()) {
            final StringBody second = (StringBody) pair.second;
            if (second != null) {
                forms.add(pair.first, second.value());
            } else {
                forms.add(pair.first, null);
            }
        }
        return new FormBody(forms);
    }
}

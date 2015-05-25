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

package org.mariotaku.restfu;

import android.support.annotation.Nullable;
import android.util.Pair;

import org.mariotaku.restfu.http.FileValue;
import org.mariotaku.restfu.http.mime.FormTypedBody;
import org.mariotaku.restfu.http.mime.MultipartTypedBody;
import org.mariotaku.restfu.http.mime.TypedData;
import org.mariotaku.restfu.annotation.param.Body;

import java.util.List;
import java.util.Map;

public final class RestRequestInfo {

    private String method;
    private String path;

    private List<Pair<String, String>> queries, forms, headers;
    private List<Pair<String, TypedData>> parts;
    private Map<String, Object> extras;
    private FileValue file;
    private Body body;

    private TypedData bodyCache;

    public RestRequestInfo(String method, String path, List<Pair<String, String>> queries,
                           List<Pair<String, String>> forms, List<Pair<String, String>> headers,
                           List<Pair<String, TypedData>> parts, FileValue file, Body body, Map<String, Object> extras) {
        this.method = method;
        this.path = path;
        this.queries = queries;
        this.forms = forms;
        this.headers = headers;
        this.parts = parts;
        this.extras = extras;
        this.file = file;
        this.body = body;
    }

    public List<Pair<String, String>> getQueries() {
        return queries;
    }

    public List<Pair<String, String>> getForms() {
        return forms;
    }

    public List<Pair<String, String>> getHeaders() {
        return headers;
    }

    public List<Pair<String, TypedData>> getParts() {
        return parts;
    }

    public Map<String, Object> getExtras() {
        return extras;
    }

    @Nullable
    public TypedData getBody() {
        if (bodyCache != null) return bodyCache;
        if (body == null) return null;
        switch (body.value()) {
            case FORM: {
                bodyCache = new FormTypedBody(getForms());
                break;
            }
            case MULTIPART: {
                bodyCache = new MultipartTypedBody(getParts());
                break;
            }
            case FILE: {
                bodyCache = file.body();
                break;
            }
        }
        return bodyCache;
    }

    public String getPath() {
        return path;
    }

    public String getMethod() {
        return method;
    }

}

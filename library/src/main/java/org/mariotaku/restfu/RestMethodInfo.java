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

import android.support.annotation.NonNull;
import android.util.Pair;

import org.mariotaku.restfu.annotation.RestMethod;
import org.mariotaku.restfu.annotation.param.Body;
import org.mariotaku.restfu.annotation.param.Extra;
import org.mariotaku.restfu.annotation.param.File;
import org.mariotaku.restfu.annotation.param.Form;
import org.mariotaku.restfu.annotation.param.Header;
import org.mariotaku.restfu.annotation.param.Part;
import org.mariotaku.restfu.annotation.param.Path;
import org.mariotaku.restfu.annotation.param.Query;
import org.mariotaku.restfu.exception.MethodNotImplementedException;
import org.mariotaku.restfu.http.FileValue;
import org.mariotaku.restfu.http.ValueMap;
import org.mariotaku.restfu.http.mime.BaseTypedData;
import org.mariotaku.restfu.http.mime.TypedData;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RestMethodInfo {

    private final RestMethod method;
    private final String path;
    private final Body body;

    private final ArrayList<Pair<Path, Object>> paths;
    private final ArrayList<Pair<Query, Object>> queries;
    private final ArrayList<Pair<Header, Object>> headers;
    private final ArrayList<Pair<Form, Object>> forms;
    private final ArrayList<Pair<Part, Object>> parts;
    private final ArrayList<Pair<Extra, Object>> extras;
    private final FileValue file;

    private ArrayList<Pair<String, String>> queriesCache, formsCache, headersCache;
    private ArrayList<Pair<String, TypedData>> partsCache;
    private Map<String, Object> extrasCache;

    RestMethodInfo(final RestMethod method, String path, final Body body, final ArrayList<Pair<Path, Object>> paths,
                   final ArrayList<Pair<Query, Object>> queries, final ArrayList<Pair<Header, Object>> headers,
                   final ArrayList<Pair<Form, Object>> forms, final ArrayList<Pair<Part, Object>> parts,
                   final FileValue file, ArrayList<Pair<Extra, Object>> extras) {
        this.method = method;
        this.path = path;
        this.body = body;
        this.paths = paths;
        this.queries = queries;
        this.headers = headers;
        this.forms = forms;
        this.parts = parts;
        this.extras = extras;
        this.file = file;
    }

    static RestMethodInfo get(Method method, Object[] args) {
        RestMethod restMethod = null;
        String pathFormat = null;
        for (Annotation annotation : method.getAnnotations()) {
            final Class<?> annotationType = annotation.annotationType();
            restMethod = annotationType.getAnnotation(RestMethod.class);
            if (restMethod != null) {
                try {
                    pathFormat = (String) annotationType.getMethod("value").invoke(annotation);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                break;
            }
        }
        final Body body = method.getAnnotation(Body.class);
        final ArrayList<Pair<Path, Object>> paths = new ArrayList<>();
        final ArrayList<Pair<Query, Object>> queries = new ArrayList<>();
        final ArrayList<Pair<Header, Object>> headers = new ArrayList<>();
        final ArrayList<Pair<Form, Object>> forms = new ArrayList<>();
        final ArrayList<Pair<Part, Object>> parts = new ArrayList<>();
        final ArrayList<Pair<Extra, Object>> extras = new ArrayList<>();
        FileValue file = null;
        final Annotation[][] annotations = method.getParameterAnnotations();
        for (int i = 0, j = annotations.length; i < j; i++) {
            final Path path = getAnnotation(annotations[i], Path.class);
            if (path != null) {
                paths.add(Pair.create(path, args[i]));
            }
            final Query query = getAnnotation(annotations[i], Query.class);
            if (query != null) {
                queries.add(Pair.create(query, args[i]));
            }
            final Header header = getAnnotation(annotations[i], Header.class);
            if (header != null) {
                headers.add(Pair.create(header, args[i]));
            }
            final Form form = getAnnotation(annotations[i], Form.class);
            if (form != null) {
                forms.add(Pair.create(form, args[i]));
            }
            final Part part = getAnnotation(annotations[i], Part.class);
            if (part != null) {
                parts.add(Pair.create(part, args[i]));
            }
            final File paramFile = getAnnotation(annotations[i], File.class);
            if (paramFile != null) {
                if (file == null) {
                    file = new FileValue(paramFile, args[i]);
                } else {
                    throw new IllegalArgumentException();
                }
            }
            final Extra extra = getAnnotation(annotations[i], Extra.class);
            if (extra != null) {
                extras.add(Pair.create(extra, args[i]));
            }
        }
        checkMethod(restMethod, body, forms, parts, file);
        return new RestMethodInfo(restMethod, pathFormat, body, paths, queries, headers, forms, parts, file, extras);
    }

    private static String[] getValueMapKeys(String[] annotationValue, ValueMap valueMap) {
        return annotationValue != null && annotationValue.length > 0 ? annotationValue : valueMap.keys();
    }

    private static void checkMethod(RestMethod restMethod, Body body, ArrayList<Pair<Form, Object>> forms,
                                    ArrayList<Pair<Part, Object>> parts, FileValue file) {
        if (restMethod == null)
            throw new MethodNotImplementedException("Method must has annotation annotated with @RestMethod");
        if (restMethod.hasBody() && body == null && (!forms.isEmpty() || !parts.isEmpty() || file != null)) {
            throw new IllegalArgumentException("@Body required for method " + restMethod.value());
        } else if (!restMethod.hasBody() && body != null) {
            throw new IllegalArgumentException(restMethod.value() + " does not allow body");
        }
        if (body == null) return;
        switch (body.value()) {
            case FILE: {
                if (file == null) {
                    throw new NullPointerException("@File annotation is required");
                }
                if (!forms.isEmpty() || !parts.isEmpty()) {
                    throw new IllegalArgumentException("Only arguments with @File annotation allowed");
                }
                break;
            }
            case MULTIPART: {
                if (!forms.isEmpty() || file != null) {
                    throw new IllegalArgumentException("Only arguments with @Part annotation allowed");
                }
                break;
            }
            case FORM: {
                if (file != null || !parts.isEmpty()) {
                    throw new IllegalArgumentException("Only arguments with @Form annotation allowed");
                }
                break;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Annotation> T getAnnotation(Annotation[] annotations, Class<T> annotationClass) {
        for (Annotation annotation : annotations) {
            if (annotationClass.isAssignableFrom(annotation.annotationType())) {
                return (T) annotation;
            }
        }
        return null;
    }

    @NonNull
    public Map<String, Object> getExtras() {
        if (extrasCache != null) return extrasCache;
        final Map<String, Object> map = new HashMap<>();
        for (Pair<Extra, Object> entry : extras) {
            final Extra extra = entry.first;
            final Object value = entry.second;
            if (value instanceof ValueMap) {
                final ValueMap valueMap = (ValueMap) value;
                for (String key : getValueMapKeys(extra.value(), valueMap)) {
                    if (valueMap.has(key)) {
                        map.put(key, valueMap.get(key));
                    }
                }
            } else if (value != null) {
                for (String key : extra.value()) {
                    map.put(key, value);
                }
            }
        }
        return extrasCache = map;
    }

    @NonNull
    public List<Pair<String, String>> getForms() {
        if (formsCache != null) return formsCache;
        final ArrayList<Pair<String, String>> list = new ArrayList<>();
        for (Pair<Form, Object> entry : forms) {
            final Form form = entry.first;
            final Object value = entry.second;
            if (value == null) continue;
            if (value instanceof ValueMap) {
                final ValueMap valueMap = (ValueMap) value;
                for (String key : getValueMapKeys(form.value(), valueMap)) {
                    if (valueMap.has(key)) {
                        list.add(Pair.create(key, String.valueOf(valueMap.get(key))));
                    }
                }
            } else {
                final char delimiter = form.arrayDelimiter();
                String valueString = Utils.toString(value, delimiter);
                for (String key : form.value()) {
                    list.add(Pair.create(key, valueString));
                }
            }
        }
        return formsCache = list;
    }

    @NonNull
    public List<Pair<String, TypedData>> getParts() {
        if (partsCache != null) return partsCache;
        final ArrayList<Pair<String, TypedData>> list = new ArrayList<>();
        for (Pair<Part, Object> entry : parts) {
            final Part part = entry.first;
            final String[] names = part.value();
            final Object value = entry.second;
            if (value instanceof TypedData) {
                list.add(Pair.create(names[0], (TypedData) value));
            } else if (value != null) {
                list.add(Pair.create(names[0], BaseTypedData.wrap(value)));
            }
        }
        return partsCache = list;
    }

    @NonNull
    public List<Pair<String, String>> getHeaders() {
        if (headersCache != null) return headersCache;
        final ArrayList<Pair<String, String>> list = new ArrayList<>();
        for (Pair<Header, Object> entry : headers) {
            final Header header = entry.first;
            final Object value = entry.second;
            if (value instanceof ValueMap) {
                final ValueMap valueMap = (ValueMap) value;
                for (String key : getValueMapKeys(header.value(), valueMap)) {
                    if (valueMap.has(key)) {
                        list.add(Pair.create(key, String.valueOf(valueMap.get(key))));
                    }
                }
            } else if (value != null) {
                for (String key : header.value()) {
                    list.add(Pair.create(key, String.valueOf(value)));
                }
            }
        }
        return headersCache = list;
    }

    public RestMethod getMethod() {
        return method;
    }

    @NonNull
    public String getPath() {
        final int queryIndex = path.indexOf('?');
        final StringBuilder sb = new StringBuilder();
        int start, end, prevEnd = -1;
        while ((start = path.indexOf('{', prevEnd)) != -1 && (end = path.indexOf('}', start)) != -1) {
            if (queryIndex != -1 && start >= queryIndex) break;
            sb.append(path.substring(prevEnd + 1, start));
            final String key = path.substring(start + 1, end);
            final String replacement = findPathReplacement(key);
            if (replacement == null)
                throw new IllegalArgumentException("Path key {" + key + "} not bound");
            sb.append(replacement);
            prevEnd = end;
        }
        if (queryIndex != -1) {
            sb.append(path.substring(prevEnd + 1, queryIndex));
        } else {
            sb.append(path.substring(prevEnd + 1));
        }
        return sb.toString();
    }

    @NonNull
    public List<Pair<String, String>> getQueries() {
        if (queriesCache != null) return queriesCache;
        final ArrayList<Pair<String, String>> list = new ArrayList<>();
        final int queryIndex = path.indexOf('?');
        if (queryIndex != -1) {
            Utils.parseGetParameters(path.substring(queryIndex + 1), list, Charset.defaultCharset().name());
        }
        for (Pair<Query, Object> entry : queries) {
            if (entry.second == null) continue;
            if (entry.second instanceof ValueMap) {
                final ValueMap valueMap = (ValueMap) entry.second;
                for (String key : getValueMapKeys(entry.first.value(), valueMap)) {
                    if (valueMap.has(key)) {
                        final Object mapValue = valueMap.get(key);
                        if (mapValue.getClass().isArray()) {
                            for (int i = 0, j = Array.getLength(mapValue); i < j; i++) {
                                list.add(Pair.create(key, String.valueOf(Array.get(mapValue, i))));
                            }
                        } else {
                            list.add(Pair.create(key, String.valueOf(mapValue)));
                        }
                    }
                }
            } else {
                final char delimiter = entry.first.arrayDelimiter();
                String valueString = Utils.toString(entry.second, delimiter);
                for (String key : entry.first.value()) {
                    list.add(Pair.create(key, valueString));
                }
            }
        }
        return queriesCache = list;
    }

    private String findPathReplacement(String key) {
        for (Pair<Path, Object> entry : paths) {
            if (key.equals(entry.first.value())) {
                if (entry.first.encoded()) {
                    return String.valueOf(entry.second);
                } else {
                    return Utils.encode(String.valueOf(entry.second), "UTF-8");
                }
            }
        }
        return null;
    }

    public RestRequestInfo toRequestInfo() {
        return new RestRequestInfo(getMethod().value(), getPath(), getQueries(), getForms(),
                getHeaders(), getParts(), getFile(), getBody(), getExtras());
    }

    public Body getBody() {
        return body;
    }

    public FileValue getFile() {
        return file;
    }
}
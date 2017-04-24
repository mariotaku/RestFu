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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mariotaku.restfu.annotation.HttpMethod;
import org.mariotaku.restfu.annotation.param.*;
import org.mariotaku.restfu.http.*;
import org.mariotaku.restfu.http.mime.Body;
import org.mariotaku.restfu.http.mime.UrlSerialization;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("WeakerAccess")
public final class RestMethod<E extends Exception> {

    @NotNull
    private final HttpMethod method;
    @NotNull
    private final String path;
    @Nullable
    private final BodyType bodyType;

    @NotNull
    private final ArrayList<Pair<Path, Object>> paths;
    @NotNull
    private final ArrayList<Pair<Header, Object>> headers;
    @NotNull
    private final ArrayList<Pair<Query, Object>> queries;
    @NotNull
    private final ArrayList<Pair<Param, Object>> params;
    @NotNull
    private final ArrayList<Pair<Extra, Object>> extras;

    private final Headers headerConstants;
    private final Queries queryConstants;
    private final Params paramConstants;

    private final RawValue rawValue;

    private MultiValueMap<String> headersCache;
    private MultiValueMap<String> queriesCache;
    private MultiValueMap<Body> paramsCache;
    private Map<String, Object> extrasCache;

    private RestMethod(@NotNull HttpMethod method, @NotNull String path, @Nullable BodyType bodyType,
            @NotNull ArrayList<Pair<Path, Object>> paths, @NotNull ArrayList<Pair<Header, Object>> headers,
            @NotNull ArrayList<Pair<Query, Object>> queries, @NotNull ArrayList<Pair<Param, Object>> params,
            @NotNull ArrayList<Pair<Extra, Object>> extras, Headers headerConstants, Queries queryConstants,
            Params paramConstants, RawValue rawValue) {
        this.method = method;
        this.path = path;
        this.bodyType = bodyType;
        this.paths = paths;
        this.headers = headers;
        this.queries = queries;
        this.params = params;
        this.extras = extras;
        this.headerConstants = headerConstants;
        this.queryConstants = queryConstants;
        this.paramConstants = paramConstants;
        this.rawValue = rawValue;
    }

    static <E extends Exception> RestMethod<E> get(Method method, Object[] args) {
        HttpMethod httpMethod = null;
        String pathFormat = null;
        for (Annotation annotation : method.getAnnotations()) {
            final Class<?> annotationType = annotation.annotationType();
            httpMethod = annotationType.getAnnotation(HttpMethod.class);
            if (httpMethod != null) {
                try {
                    pathFormat = (String) annotationType.getMethod("value").invoke(annotation);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                break;
            }
        }
        if (httpMethod == null) {
            throw new IllegalArgumentException("Must be annotated with @HttpMethod or @GET, @POST...");
        }
        final BodyType bodyType = method.getAnnotation(BodyType.class);
        final ArrayList<Pair<Path, Object>> paths = new ArrayList<>();
        final ArrayList<Pair<Query, Object>> queries = new ArrayList<>();
        final ArrayList<Pair<Header, Object>> headers = new ArrayList<>();
        final ArrayList<Pair<Param, Object>> params = new ArrayList<>();
        final ArrayList<Pair<Extra, Object>> extras = new ArrayList<>();
        RawValue rawValue = null;
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
            final Param param = getAnnotation(annotations[i], Param.class);
            if (param != null) {
                params.add(Pair.create(param, args[i]));
            }
            final Raw paramRaw = getAnnotation(annotations[i], Raw.class);
            if (paramRaw != null) {
                if (rawValue == null) {
                    rawValue = new RawValue(paramRaw, args[i]);
                } else {
                    throw new IllegalArgumentException();
                }
            }
            final Extra extra = getAnnotation(annotations[i], Extra.class);
            if (extra != null) {
                extras.add(Pair.create(extra, args[i]));
            }
        }
        final Headers headerConstants = getAnnotation(method, Headers.class);
        final Queries queryConstants = getAnnotation(method, Queries.class);
        final Params paramConstants = getAnnotation(method, Params.class);
        return new RestMethod<>(httpMethod, pathFormat, bodyType, paths, headers, queries, params, extras,
                headerConstants, queryConstants, paramConstants, rawValue);
    }

    @NotNull
    public HttpMethod getMethod() {
        return method;
    }

    @NotNull
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

    @NotNull
    public MultiValueMap<String> getHeaders(@Nullable final ValueMap valuesPool) throws RestConverter.ConvertException,
            IOException, E {
        if (headersCache != null) return headersCache;
        final MultiValueMap<String> map = new MultiValueMap<>(true);
        final Converter<String, E> converter = new Converter<String, E>() {
            @Override
            public String[] convert(Object from, char arrayDelimiter, int booleanEncoding) throws RestConverter.ConvertException, IOException {
                final String header;
                if (from == null) {
                    header = null;
                } else if (from instanceof HeaderValue) {
                    header = ((HeaderValue) from).toHeaderValue();
                } else if (from.getClass().isArray()) {
                    if (arrayDelimiter == '\0') {
                        return RestFuUtils.toStringArray(from);
                    }
                    header = RestFuUtils.toString(from, arrayDelimiter);
                } else {
                    header = from.toString();
                }
                return new String[]{header};
            }
        };
        final HeaderSanitizer sanitizer = new HeaderSanitizer();
        addArgumentsToMap(headers, map, converter, sanitizer);
        addConstants(headerConstants, valuesPool, converter, map, sanitizer);
        return headersCache = map;
    }

    @NotNull
    public MultiValueMap<String> getQueries(@Nullable final ValueMap valuesPool) throws RestConverter.ConvertException,
            IOException, E {
        if (queriesCache != null) return queriesCache;
        final MultiValueMap<String> list = new MultiValueMap<>();
        final int queryIndex = path.indexOf('?');
        if (queryIndex != -1) {
            RestFuUtils.parseQuery(path.substring(queryIndex + 1), Charset.defaultCharset().name(), list);
        }
        final Converter<String, E> converter = new Converter<String, E>() {
            @Override
            public String[] convert(Object from, char arrayDelimiter, int booleanEncoding) throws RestConverter.ConvertException, IOException {
                if (from == null) return new String[]{null};
                if (arrayDelimiter == '\0' && from.getClass().isArray()) {
                    return RestFuUtils.toStringArray(from);
                }
                return RestMethod.toString(from, arrayDelimiter, booleanEncoding);
            }
        };
        final SimpleSanitizer<String> sanitizer = new SimpleSanitizer<>();
        addArgumentsToMap(queries, list, converter, sanitizer);
        addConstants(queryConstants, valuesPool, converter, list, sanitizer);
        if (!method.allowBody()) {
            addArgumentsToMap(params, list, converter, sanitizer);
            addConstants(paramConstants, valuesPool, converter, list, sanitizer);
        }
        return queriesCache = list;
    }

    @Nullable
    public MultiValueMap<Body> getParams(final RestConverter.Factory<E> factory, @Nullable final ValueMap valuesPool)
            throws RestConverter.ConvertException, IOException, E {
        if (!method.allowBody()) return null;
        if (paramsCache != null) return paramsCache;
        final MultiValueMap<Body> map = new MultiValueMap<>();
        final Converter<Body, E> converter = new Converter<Body, E>() {
            @Override
            public Body[] convert(Object argument, char arrayDelimiter, int booleanEncoding) throws IOException, RestConverter.ConvertException, E {
                return RestFuUtils.toBodies(argument, factory, arrayDelimiter);
            }
        };
        final SimpleSanitizer<Body> sanitizer = new SimpleSanitizer<>();
        addArgumentsToMap(params, map, converter, sanitizer);
        addConstants(paramConstants, valuesPool, converter, map, sanitizer);
        return paramsCache = map;
    }

    @Nullable
    public RawValue getRawValue() {
        return rawValue;
    }

    @Nullable
    public BodyType getBodyType() {
        return bodyType;
    }

    @NotNull
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

    @NotNull
    public RestRequest toRestRequest(@NotNull RestConverter.Factory<E> factory, @Nullable final ValueMap valuesPool)
            throws RestConverter.ConvertException, IOException, E {
        final HttpMethod method = getMethod();
        final MultiValueMap<Body> params = getParams(factory, valuesPool);
        final RawValue fileValue = getRawValue();
        checkMethod(method, fileValue);
        return new RestRequest(method.value(), method.allowBody(), getPath(), getHeaders(valuesPool),
                getQueries(valuesPool), params, fileValue, getBodyType(), getExtras());
    }

    private String findPathReplacement(String key) {
        final Charset charset = Charset.forName("UTF-8");
        for (Pair<Path, Object> entry : paths) {
            if (key.equals(entry.first.value())) {
                if (entry.first.encoded()) {
                    return String.valueOf(entry.second);
                } else {
                    return UrlSerialization.PATH.serialize(String.valueOf(entry.second), charset);
                }
            }
        }
        return null;
    }

    private static String[] getValueMapKeys(String[] annotationValue, ValueMap valueMap) {
        return annotationValue != null && annotationValue.length > 0 ? annotationValue : valueMap.keys();
    }

    private static void checkMethod(HttpMethod httpMethod, RawValue fileValue) {
        final boolean hasBody = fileValue != null;
        if (!httpMethod.allowBody() && hasBody) {
            throw new IllegalArgumentException(httpMethod.value() + " does not allow body");
        }
    }

    private static <A extends Annotation, T, E extends Exception> void addConstants(final A annotation,
            @Nullable  final ValueMap valuesPool, final Converter<T, E> converter,
            final MultiValueMap<T> target, final Sanitizer<T> sanitizer) throws RestConverter.ConvertException,
            IOException, E {
        consumeConstants(annotation, new ConstantIterateConsumer<E>() {
            @Override
            public void consume(KeyValue item) throws RestConverter.ConvertException, IOException, E {
                final String key = item.key(), value = item.value(), valueKey = item.valueKey();
                if (valueKey.length() > 0) {
                    if (valuesPool != null && valuesPool.has(valueKey)) {
                        final String sanitizedKey = sanitizer.sanitizeKey(key);
                        final T[] keyValue = converter.convert(valuesPool.get(valueKey), item.arrayDelimiter(),
                                BooleanEncoding.TRUE_FALSE);
                        target.addAll(sanitizedKey, sanitizer.sanitizeValue(keyValue));
                    }
                } else {
                    final String sanitizedKey = sanitizer.sanitizeKey(key);
                    final T[] convertedValue = converter.convert(value, item.arrayDelimiter(),
                            BooleanEncoding.TRUE_FALSE);
                    target.addAll(sanitizedKey, sanitizer.sanitizeValue(convertedValue));
                }
            }
        });
    }

    private static <T extends Annotation, E extends Exception> void consumeArguments(final ArrayList<Pair<T, Object>> list,
            final ArgumentIterateConsumer<E> consumer) throws RestConverter.ConvertException, IOException, E {
        if (list == null) return;
        for (Pair<?, Object> pair : list) {
            if (pair.first instanceof Header) {
                final Header h = (Header) pair.first;
                consumer.consume(h.value(), h.arrayDelimiter(), h.ignoreOnNull(), h.booleanEncoding(), pair.second);
            } else if (pair.first instanceof Param) {
                final Param p = (Param) pair.first;
                consumer.consume(p.value(), p.arrayDelimiter(), p.ignoreOnNull(), p.booleanEncoding(), pair.second);
            } else if (pair.first instanceof Query) {
                final Query q = (Query) pair.first;
                consumer.consume(q.value(), q.arrayDelimiter(), q.ignoreOnNull(), q.booleanEncoding(), pair.second);
            }
        }

    }

    private static <T extends Annotation, E extends Exception> void consumeConstants(T annotation,
            ConstantIterateConsumer<E> consumer) throws RestConverter.ConvertException, IOException, E {
        if (annotation == null) return;
        KeyValue[] items;
        if (annotation instanceof Headers) {
            Headers h = (Headers) annotation;
            if (h.template() != void.class) {
                h = h.template().getAnnotation(Headers.class);
                if (h == null) {
                    throw new UnsupportedOperationException("Template class must be annotated");
                } else if (h.template() != void.class) {
                    throw new UnsupportedOperationException("Template class can't use template reference");
                }
            }
            items = h.value();
        } else if (annotation instanceof Queries) {
            Queries q = (Queries) annotation;
            if (q.template() != void.class) {
                q = q.template().getAnnotation(Queries.class);
                if (q == null) {
                    throw new UnsupportedOperationException("Template class must be annotated");
                } else if (q.template() != void.class) {
                    throw new UnsupportedOperationException("Template class can't use template reference");
                }
            }
            items = q.value();
        } else if (annotation instanceof Params) {
            Params p = (Params) annotation;
            if (p.template() != void.class) {
                p = p.template().getAnnotation(Params.class);
                if (p == null) {
                    throw new UnsupportedOperationException("Template class must be annotated");
                } else if (p.template() != void.class) {
                    throw new UnsupportedOperationException("Template class can't use template reference");
                }
            }
            items = p.value();
        } else {
            throw new UnsupportedOperationException(annotation.getClass().toString());
        }
        for (KeyValue item : items) {
            consumer.consume(item);
        }
    }

    private static <A extends Annotation, O, E extends Exception> void addArgumentsToMap(ArrayList<Pair<A, Object>> list,
            @NotNull final MultiValueMap<O> map, final Converter<O, E> converter, final Sanitizer<O> sanitizer)
            throws RestConverter.ConvertException, IOException, E {
        consumeArguments(list, new ArgumentIterateConsumer<E>() {
            @Override
            public void consume(@NotNull String[] names, char arrayDelimiter, boolean ignoreOnNull, int booleanEncoding,
                    @Nullable Object object) throws RestConverter.ConvertException, IOException, E {
                if (ignoreOnNull && object == null) return;
                addToMap(names, object, map, arrayDelimiter, booleanEncoding, converter, sanitizer);
            }
        });
    }

    private static <T, E extends Exception> void addToMap(@NotNull final String[] names, @Nullable final Object object,
            final MultiValueMap<T> map, final char arrayDelimiter, final int booleanEncoding,
            final Converter<T, E> converter, final Sanitizer<T> sanitizer)
            throws RestConverter.ConvertException, IOException, E {
        if (object == null) {
            for (String name : names) {
                map.addAll(sanitizer.sanitizeKey(name), sanitizer.sanitizeValue(null));
            }
        } else if (object instanceof ValueMap) {
            final ValueMap valueMap = (ValueMap) object;
            for (String key : getValueMapKeys(names, valueMap)) {
                final String sanitized = sanitizer.sanitizeKey(key);
                if (valueMap.has(sanitized)) {
                    final Object value = valueMap.get(key);
                    if (value == null) {
                        map.addAll(sanitized, sanitizer.sanitizeValue(null));
                    } else {
                        map.addAll(sanitized, sanitizer.sanitizeValue(converter.convert(value, arrayDelimiter,
                                booleanEncoding)));
                    }
                }
            }
        } else for (String name : names) {
            map.addAll(sanitizer.sanitizeKey(name), sanitizer.sanitizeValue(converter.convert(object, arrayDelimiter,
                    booleanEncoding)));
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

    private static <T extends Annotation> T getAnnotation(Method method, Class<T> annotationClass) {
        T annotation = method.getAnnotation(annotationClass);
        if (annotation == null) {
            annotation = method.getDeclaringClass().getAnnotation(annotationClass);
        }
        return annotation;
    }

    private static String[] toString(Object value, char delimiter, int booleanEncoding) {
        if (value instanceof Boolean) {
            switch (booleanEncoding) {
                case BooleanEncoding.ONE_ZERO:
                    return new String[]{value == Boolean.TRUE ? "1" : "0"};
                case BooleanEncoding.KEY_IF_TRUE:
                    return value == Boolean.TRUE ? new String[]{null} : new String[0];
                case BooleanEncoding.IGNORE_IF_FALSE:
                    return value == Boolean.TRUE ? new String[]{"true"} : new String[0];
                case BooleanEncoding.IGNORE_IF_TRUE:
                    return value == Boolean.FALSE ? new String[]{"false"} : new String[0];
            }
        }
        return new String[]{RestFuUtils.toString(value, delimiter)};
    }

    interface Converter<T, E extends Exception> {
        T[] convert(Object object, char arrayDelimiter, int booleanEncoding) throws RestConverter.ConvertException, IOException, E;
    }

    interface ArgumentIterateConsumer<E extends Exception> {
        void consume(@NotNull String[] names, char arrayDelimiter, boolean ignoreOnNull, int booleanEncoding,
                @Nullable Object object) throws RestConverter.ConvertException, IOException, E;
    }

    interface ConstantIterateConsumer<E extends Exception> {
        void consume(KeyValue item) throws RestConverter.ConvertException, IOException, E;
    }

    interface Sanitizer<E> {
        String sanitizeKey(String in);

        E[] sanitizeValue(E[] in);
    }

    static class SimpleSanitizer<E> implements Sanitizer<E> {

        @Override
        public String sanitizeKey(String in) {
            return in;
        }

        @Override
        public E[] sanitizeValue(E[] in) {
            return in;
        }
    }

    static class HeaderSanitizer implements Sanitizer<String> {
        @Override
        public String sanitizeKey(String in) {
            return RestFuUtils.sanitizeHeader(in);
        }

        @Override
        public String[] sanitizeValue(String[] in) {
            if (in == null) return null;
            final String[] out = new String[in.length];
            for (int i = 0, j = in.length; i < j; i++) {
                out[i] = RestFuUtils.sanitizeHeader(in[i]);
            }
            return out;
        }

    }


}
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

import org.mariotaku.restfu.annotation.HttpMethod;
import org.mariotaku.restfu.annotation.param.*;
import org.mariotaku.restfu.exception.MethodNotImplementedException;
import org.mariotaku.restfu.http.*;
import org.mariotaku.restfu.http.mime.Body;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public final class RestMethod<E extends Exception> {

    private final HttpMethod method;
    private final String path;
    private final BodyType bodyType;

    private final ArrayList<Pair<Path, Object>> paths;
    private final ArrayList<Pair<Header, Object>> headers;
    private final ArrayList<Pair<Query, Object>> queries;
    private final ArrayList<Pair<Param, Object>> params;
    private final ArrayList<Pair<Extra, Object>> extras;

    private final Headers headerConstants;
    private final Queries queryConstants;
    private final Params paramConstants;

    private final RawValue rawValue;


    private MultiValueMap<String> headersCache;
    private MultiValueMap<String> queriesCache;
    private MultiValueMap<Body> paramsCache;
    private Map<String, Object> extrasCache;

    public RestMethod(HttpMethod method, String path, BodyType bodyType, ArrayList<Pair<Path, Object>> paths,
                      ArrayList<Pair<Header, Object>> headers, ArrayList<Pair<Query, Object>> queries,
                      ArrayList<Pair<Param, Object>> params, ArrayList<Pair<Extra, Object>> extras,
                      Headers headerConstants, Queries queryConstants, Params paramConstants, RawValue rawValue) {
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
        checkMethod(httpMethod, bodyType, params, rawValue);
        return new RestMethod<>(httpMethod, pathFormat, bodyType, paths, headers, queries, params, extras,
                headerConstants, queryConstants, paramConstants, rawValue);
    }

    private static String[] getValueMapKeys(String[] annotationValue, ValueMap valueMap) {
        return annotationValue != null && annotationValue.length > 0 ? annotationValue : valueMap.keys();
    }

    private static void checkMethod(HttpMethod httpMethod, BodyType bodyType, ArrayList<Pair<Param, Object>> params,
                                    RawValue fileValue) {
        if (httpMethod == null)
            throw new MethodNotImplementedException("Method must has annotation annotated with @" +
                    HttpMethod.class.getSimpleName());
        final boolean hasBody = !params.isEmpty() || fileValue != null;
        if (!httpMethod.allowBody() && hasBody) {
            throw new IllegalArgumentException(httpMethod.value() + " does not allow body");
        }
    }

    private static <A extends Annotation, T, E extends Exception> void addConstants(final A annotation,
                                                                                    final ValueMap valuesPool,
                                                                                    final Converter<T, E> converter,
                                                                                    final MultiValueMap<T> target)
            throws RestConverter.ConvertException, IOException, E {
        consumeConstants(annotation, new ConstantIterateConsumer<E>() {
            @Override
            public void consume(KeyValue item) throws RestConverter.ConvertException, IOException, E {
                final String key = item.key(), value = item.value(), valueKey = item.valueKey();
                if (valueKey.length() > 0 && valuesPool != null) {
                    if (valuesPool.has(valueKey)) {
                        target.addAll(key, converter.convert(valuesPool.get(valueKey), item.arrayDelimiter()));
                    }
                } else {
                    target.addAll(key, converter.convert(value, item.arrayDelimiter()));
                }
            }
        });
    }

    private static <T extends Annotation, E extends Exception> void consumeArguments(final ArrayList<Pair<T, Object>> list,
                                                                                     final ArgumentIterateConsumer<E> consumer)
            throws RestConverter.ConvertException, IOException, E {
        if (list == null) return;
        for (Pair<?, Object> pair : list) {
            if (pair.first instanceof Header) {
                consumer.consume(((Header) pair.first).value(), ((Header) pair.first).arrayDelimiter(), pair.second);
            } else if (pair.first instanceof Param) {
                consumer.consume(((Param) pair.first).value(), ((Param) pair.first).arrayDelimiter(), pair.second);
            } else if (pair.first instanceof Query) {
                consumer.consume(((Query) pair.first).value(), ((Query) pair.first).arrayDelimiter(), pair.second);
            }
        }

    }

    private static <T extends Annotation, E extends Exception> void consumeConstants(T annotation, ConstantIterateConsumer<E> consumer)
            throws RestConverter.ConvertException, IOException, E {
        if (annotation == null) return;
        KeyValue[] items;
        if (annotation instanceof Headers) {
            items = ((Headers) annotation).value();
        } else if (annotation instanceof Queries) {
            items = ((Queries) annotation).value();
        } else if (annotation instanceof Params) {
            items = ((Params) annotation).value();
        } else {
            throw new UnsupportedOperationException(annotation.getClass().toString());
        }
        for (KeyValue item : items) {
            consumer.consume(item);
        }
    }

    private static <A extends Annotation, O, E extends Exception> void addArgumentsToMap(ArrayList<Pair<A, Object>> list,
                                                                                         final MultiValueMap<O> map,
                                                                                         final Converter<O, E> converter)
            throws RestConverter.ConvertException, IOException, E {
        consumeArguments(list, new ArgumentIterateConsumer<E>() {
            @Override
            public void consume(String[] names, char arrayDelimiter, Object object) throws RestConverter.ConvertException,
                    IOException, E {
                addToMap(names, object, map, arrayDelimiter, converter);
            }
        });
    }

    private static <T, E extends Exception> void addToMap(final String[] names, final Object object,
                                                          final MultiValueMap<T> map, final char arrayDelimiter,
                                                          final Converter<T, E> converter)
            throws RestConverter.ConvertException, IOException, E {
        if (object == null) {
            for (String name : names) {
                map.add(name, null);
            }
        } else if (object instanceof ValueMap) {
            final ValueMap valueMap = (ValueMap) object;
            for (String key : getValueMapKeys(names, valueMap)) {
                if (valueMap.has(key)) {
                    map.addAll(key, converter.convert(valueMap.get(key), arrayDelimiter));
                }
            }
        } else {
            for (String name : names) {
                map.addAll(name, converter.convert(object, arrayDelimiter));
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

    private static <T extends Annotation> T getAnnotation(Method method, Class<T> annotationClass) {
        T annotation = method.getAnnotation(annotationClass);
        if (annotation == null) {
            annotation = method.getDeclaringClass().getAnnotation(annotationClass);
        }
        return annotation;
    }

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

    public HttpMethod getMethod() {
        return method;
    }

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

    public MultiValueMap<String> getHeaders(final ValueMap valuesPool) throws RestConverter.ConvertException, IOException, E {
        if (headersCache != null) return headersCache;
        final MultiValueMap<String> map = new MultiValueMap<>(true);
        final Converter<String, E> converter = new Converter<String, E>() {
            @Override
            public String[] convert(Object from, char arrayDelimiter) throws RestConverter.ConvertException, IOException {
                if (from == null) return new String[]{null};
                else if (from instanceof HeaderValue) return new String[]{((HeaderValue) from).toHeaderValue()};
                else return new String[]{from.toString()};
            }
        };
        addArgumentsToMap(headers, map, converter);
        addConstants(headerConstants, valuesPool, converter, map);
        return headersCache = map;
    }

    public MultiValueMap<String> getQueries(ValueMap valuesPool) throws RestConverter.ConvertException, IOException, E {
        if (queriesCache != null) return queriesCache;
        final MultiValueMap<String> list = new MultiValueMap<>();
        final int queryIndex = path.indexOf('?');
        if (queryIndex != -1) {
            Utils.parseQuery(path.substring(queryIndex + 1), Charset.defaultCharset().name(), list);
        }
        final Converter<String, E> converter = new Converter<String, E>() {
            @Override
            public String[] convert(Object from, char arrayDelimiter) throws RestConverter.ConvertException, IOException {
                if (from == null) return new String[]{null};
                return new String[]{from.toString()};
            }
        };
        addArgumentsToMap(queries, list, converter);
        addConstants(queryConstants, valuesPool, converter, list);
        return queriesCache = list;
    }

    public MultiValueMap<Body> getParams(final RestConverter.Factory<E> factory, ValueMap valuesPool)
            throws RestConverter.ConvertException, IOException, E {
        if (paramsCache != null) return paramsCache;
        final MultiValueMap<Body> map = new MultiValueMap<>();
        final Converter<Body, E> converter = new Converter<Body, E>() {
            @Override
            public Body[] convert(Object argument, char arrayDelimiter) throws IOException, RestConverter.ConvertException, E {
                return Utils.toBodies(argument, factory, arrayDelimiter);
            }
        };
        addArgumentsToMap(params, map, converter);
        addConstants(paramConstants, valuesPool, converter, map);
        return paramsCache = map;
    }

    public RestRequest toRestRequest(RestConverter.Factory<E> factory, ValueMap valuesPool)
            throws RestConverter.ConvertException, IOException, E {
        final HttpMethod method = getMethod();
        return new RestRequest(method.value(), method.allowBody(), getPath(), getHeaders(valuesPool), getQueries(valuesPool),
                getParams(factory, valuesPool), getRawValue(), getBodyType(), getExtras());
    }

    public BodyType getBodyType() {
        return bodyType;
    }

    public RawValue getRawValue() {
        return rawValue;
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

    interface Converter<T, E extends Exception> {
        T[] convert(Object object, char arrayDelimiter) throws RestConverter.ConvertException, IOException, E;
    }

    interface ArgumentIterateConsumer<E extends Exception> {
        void consume(String[] names, char arrayDelimiter, Object object) throws RestConverter.ConvertException, IOException, E;
    }

    interface ConstantIterateConsumer<E extends Exception> {
        void consume(KeyValue item) throws RestConverter.ConvertException, IOException, E;
    }
}
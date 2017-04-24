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

import org.mariotaku.restfu.annotation.param.BooleanEncoding;
import org.mariotaku.restfu.http.MultiValueMap;
import org.mariotaku.restfu.http.mime.Body;
import org.mariotaku.restfu.http.mime.StringBody;
import org.mariotaku.restfu.http.mime.UrlSerialization;

import java.io.Closeable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mariotaku on 15/2/4.
 */
public class RestFuUtils {
    private static final char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    public static String[] split(final String str, final String separator) {
        String[] returnValue;
        int index = str.indexOf(separator);
        if (index == -1) {
            returnValue = new String[]{str};
        } else {
            final List<String> strList = new ArrayList<>();
            int oldIndex = 0;
            while (index != -1) {
                final String subStr = str.substring(oldIndex, index);
                strList.add(subStr);
                oldIndex = index + separator.length();
                index = str.indexOf(separator, oldIndex);
            }
            if (oldIndex != str.length()) {
                strList.add(str.substring(oldIndex));
            }
            returnValue = strList.toArray(new String[strList.size()]);
        }

        return returnValue;
    }

    public static void closeSilently(Closeable c) {
        if (c == null) return;
        try {
            c.close();
        } catch (IOException ignore) {
        }
    }

    public static void parseQuery(final String queryString, final String encoding, final KeyValueConsumer consumer) {
        final String[] queryStrings = split(queryString, "&");
        try {
            for (final String query : queryStrings) {
                final String[] split = split(query, "=");
                final String key = URLDecoder.decode(split[0], encoding);
                if (split.length == 2) {
                    consumer.consume(key, URLDecoder.decode(split[1], encoding));
                } else {
                    consumer.consume(key, null);
                }
            }
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static void parseQuery(final String queryString, final String encoding, final MultiValueMap<String> params) {
        parseQuery(queryString, encoding, new KeyValueConsumer() {

            @Override
            public void consume(String key, String value) {
                params.add(key, value);
            }
        });
    }

    public static <T> T assertNotNull(T obj) {
        if (obj == null) throw new NullPointerException();
        return obj;
    }

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }


    public static String toString(Object value, char delimiter) {
        final Class<?> valueClass = value.getClass();
        if (valueClass.isArray() && delimiter != '\0') {
            final StringBuilder sb = new StringBuilder();
            for (int i = 0, j = Array.getLength(value); i < j; i++) {
                if (i != 0) {
                    sb.append(delimiter);
                }
                sb.append(Array.get(value, i));
            }
            return sb.toString();
        } else {
            return value.toString();
        }
    }

    public static String[] toStringArray(Object value) {
        final int length = Array.getLength(value);
        final String[] out = new String[length];
        for (int i = 0; i < length; i++) {
            Object item = Array.get(value, i);
            if (item != null) {
                out[i] = item.toString();
            } else {
                out[i] = null;
            }
        }
        return out;
    }

    public static <E extends Exception> Body[] toBodies(Object value, RestConverter.Factory<E> factory, char delimiter)
            throws RestConverter.ConvertException, IOException, E {
        if (value == null) throw new NullPointerException();
        final Class<?> valueClass = value.getClass();
        if (valueClass.isArray()) {
            if (delimiter != '\0') {
                // If delimiter specified, all array elements should be treated as string
                final StringBuilder sb = new StringBuilder();
                for (int i = 0, j = Array.getLength(value); i < j; i++) {
                    if (i != 0) {
                        sb.append(delimiter);
                    }
                    sb.append(Array.get(value, i));
                }
                return new Body[]{new StringBody(sb.toString(), Charset.forName("UTF-8"))};
            } else {
                final int length = Array.getLength(value);
                final Body[] bodies = new Body[length];
                for (int i = 0; i < length; i++) {
                    bodies[i] = toBody(Array.get(value, i), factory);
                }
                return bodies;
            }
        } else {
            return new Body[]{toBody(value, factory)};
        }
    }


    public static <E extends Exception> Body toBody(Object obj, RestConverter.Factory<E> factory)
            throws RestConverter.ConvertException, IOException, E {
        if (obj == null)
            return null;
        final Class<?> cls = obj.getClass();
        //noinspection unchecked
        final RestConverter<Object, Body, E> converter = (RestConverter<Object, Body, E>)
                factory.forRequest(cls);
        if (converter == null) throw new RestConverter.UnsupportedTypeException(cls);
        return converter.convert(obj);
    }

    public static void checkOffsetAndCount(int arrayLength, int offset, int count) {
        if ((offset | count) < 0 || offset > arrayLength || arrayLength - offset < count) {
            throw new ArrayIndexOutOfBoundsException("length=" + arrayLength + "; regionStart="
                    + offset + "; regionLength=" + count);
        }
    }

    public static int append(StringBuilder sb, MultiValueMap<String> queries, Charset charset) {
        int numAdded = 0;
        final List<Pair<String, String>> list = queries.toList();
        for (int i = 0, j = list.size(); i < j; i++) {
            if (i != 0) {
                sb.append('&');
            }
            final Pair<String, String> form = list.get(i);
            UrlSerialization.QUERY.serialize(form.first, charset, sb);
            if (form.second != null) {
                sb.append('=');
                UrlSerialization.QUERY.serialize(form.second, charset, sb);
            }
            numAdded++;
        }
        return numAdded;
    }

    public static String sanitizeHeader(String header) {
        if (header == null) return null;
        final char[] chars = header.toCharArray();
        for (int i = 0, j = chars.length; i < j; i++) {
            if (!isAsciiPrintable(chars[i])) {
                chars[i] = '.';
            }
        }
        return new String(chars);
    }

    private static boolean isAsciiPrintable(char ch) {
        return ch >= 0x20 && ch < 0x7f;
    }

    public interface KeyValueConsumer {
        void consume(String key, String value);
    }
}

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

package org.mariotaku.restfu.http;

import android.text.TextUtils;
import android.util.Pair;

import org.mariotaku.restfu.RestRequestInfo;
import org.mariotaku.restfu.Utils;

import java.util.Arrays;
import java.util.List;

/**
 * Created by mariotaku on 15/2/6.
 */
public class Endpoint {

    private String url;

    public String getUrl() {
        return url;
    }

    public Endpoint(String url) {
        this.url = url;
    }

    public static String constructUrl(String endpoint, RestRequestInfo requestInfo) {
        return constructUrl(endpoint, requestInfo.getPath(), requestInfo.getQueries());
    }

    public String construct(String path, List<Pair<String, String>> queries) {
        return constructUrl(url, path, queries);
    }

    @SafeVarargs
    public final String construct(String path, Pair<String, String>... queries) {
        return constructUrl(url, path, Arrays.asList(queries));
    }

    public static String constructUrl(String endpoint, String path, List<Pair<String, String>> queries) {
        if (endpoint == null) throw new NullPointerException("Endpoint is null");
        final StringBuilder urlBuilder = new StringBuilder();
        if (endpoint.charAt(endpoint.length() - 1) == '/') {
            urlBuilder.append(endpoint.substring(0, endpoint.length() - 1));
        } else {
            urlBuilder.append(endpoint);
        }
        if (path != null) {
            if (path.charAt(0) != '/') {
                urlBuilder.append('/');
            }
            urlBuilder.append(path);
        }
        return constructUrl(urlBuilder.toString(), queries);
    }

    public static String constructUrl(String url, List<Pair<String, String>> queries) {
        if (queries == null || queries.isEmpty()) return url;
        final StringBuilder urlBuilder = new StringBuilder(url);
        for (int i = 0, j = queries.size(); i < j; i++) {
            final Pair<String, String> item = queries.get(i);
            urlBuilder.append(i != 0 ? '&' : '?');
            urlBuilder.append(Utils.encode(item.first, "UTF-8"));
            if (!TextUtils.isEmpty(item.second)) {
                urlBuilder.append('=');
                urlBuilder.append(Utils.encode(item.second, "UTF-8"));
            }
        }
        return urlBuilder.toString();
    }

    @SafeVarargs
    public static String constructUrl(String url, Pair<String, String>... queries) {
        return constructUrl(url, Arrays.asList(queries));
    }
}

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


import org.mariotaku.restfu.RestRequest;
import org.mariotaku.restfu.Utils;

import java.nio.charset.Charset;

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

    public static String constructUrl(String endpoint, RestRequest restRequest) {
        return constructUrl(endpoint, restRequest.getPath(), restRequest.getQueries());
    }

    public String construct(String path, MultiValueMap<String> queries) {
        return constructUrl(url, path, queries);
    }

    public String construct(String path, String[]... queries) {
        MultiValueMap<String> map = new MultiValueMap<>();
        for (String[] query : queries) {
            if (query.length != 2) throw new IllegalArgumentException();
            map.add(query[0], query[1]);
        }
        return constructUrl(url, path, map);
    }

    public boolean checkEndpoint(String that) {
        return that != null && that.startsWith(url);
    }

    public static String constructUrl(String endpoint, String path, MultiValueMap<String> queries) {
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

    public static String constructUrl(String url, String[]... queries) {
        MultiValueMap<String> map = new MultiValueMap<>();
        for (String[] query : queries) {
            if (query.length != 2) throw new IllegalArgumentException();
            map.add(query[0], query[1]);
        }
        return constructUrl(url, map);
    }

    public static String constructUrl(String url, MultiValueMap<String> queries) {
        if (queries == null || queries.isEmpty()) return url;
        final StringBuilder urlBuilder = new StringBuilder(url);
        urlBuilder.append('?');
        Utils.append(urlBuilder, queries, Charset.forName("UTF-8"));
        return urlBuilder.toString();
    }

}

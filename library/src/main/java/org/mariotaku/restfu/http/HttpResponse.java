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


import org.mariotaku.restfu.http.mime.Body;

import java.io.Closeable;
import java.util.List;

/**
 * Created by mariotaku on 15/2/7.
 */
public abstract class HttpResponse implements Closeable {
    public abstract int getStatus();

    public abstract MultiValueMap<String> getHeaders();

    public abstract Body getBody();

    public String getHeader(String name) {
        if (name == null) throw new NullPointerException();
        return getHeaders().getFirst(name);
    }

    public List<String> getHeaders(String name) {
        if (name == null) throw new NullPointerException();
        return getHeaders().get(name);
    }

    /**
     * Returns true if the code is in [200..300), which means the request was
     * successfully received, understood, and accepted.
     *
     * @return True if HTTP response is successful response
     */
    public boolean isSuccessful() {
        final int status = getStatus();
        return status >= 200 && status < 300;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("HTTP ");
        sb.append(Integer.toString(getStatus()));
        return sb.toString();
    }
}

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

import org.mariotaku.restfu.http.RestHttpResponse;

/**
 * Created by mariotaku on 15/2/7.
 */
public class RestException extends RuntimeException {
    public RestException(String message, Throwable cause) {
        super(message, cause);
    }

    public RestException(String message) {
        super(message);
    }

    public RestException() {
    }

    public RestException(Throwable cause) {
        super(cause);
    }

    private RestHttpResponse response;

    @Override
    public String toString() {
        return "RestException{" +
                "response=" + response +
                "} " + super.toString();
    }

    public RestHttpResponse getResponse() {
        return response;
    }

    public void setResponse(RestHttpResponse response) {
        this.response = response;
    }
}

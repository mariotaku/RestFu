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

import org.mariotaku.restfu.http.HttpResponse;
import org.mariotaku.restfu.http.mime.BaseBody;
import org.mariotaku.restfu.http.mime.Body;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * Created by mariotaku on 15/2/6.
 */
public interface RestConverter<F, T, E extends Exception> {

    T convert(F from) throws ConvertException, IOException, E;

    interface Factory<E extends Exception> {

        RestConverter<HttpResponse, ?, E> forResponse(Type toType);

        RestConverter<?, Body, E> forRequest(Type fromType);
    }

    abstract class SimpleFactory<E extends Exception> implements Factory<E> {
        @Override
        public RestConverter<?, Body, E> forRequest(Type fromType) {
            return new SimpleBodyConverter<>(fromType);
        }

        public static class SimpleBodyConverter<E extends Exception> implements RestConverter<Object, Body, E> {
            private final Type fromType;

            public SimpleBodyConverter(Type fromType) {
                this.fromType = fromType;
            }

            @Override
            public Body convert(Object from) throws ConvertException, IOException {
                return BaseBody.wrap(from);
            }
        }
    }

    class ConvertException extends Exception {
        public ConvertException() {
            super();
        }

        public ConvertException(String message) {
            super(message);
        }

        public ConvertException(String message, Throwable cause) {
            super(message, cause);
        }

        public ConvertException(Throwable cause) {
            super(cause);
        }

        protected ConvertException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
        }
    }

    class UnsupportedTypeException extends RuntimeException {
        public UnsupportedTypeException(Type type) {
            super(type.toString());
        }
    }
}

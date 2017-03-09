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
import org.mariotaku.restfu.callback.Callback;
import org.mariotaku.restfu.callback.RawCallback;
import org.mariotaku.restfu.http.*;

import java.io.IOException;
import java.lang.reflect.*;

public class RestAPIFactory<E extends Exception> {

    private Endpoint endpoint;
    private Authorization authorization;
    private ValueMap constantPool;

    private RestHttpClient httpClient;

    private HttpRequest.Factory httpRequestFactory = new HttpRequest.DefaultFactory();
    private RestRequest.Factory<E> restRequestFactory = new RestRequest.DefaultFactory<>();
    private RestConverter.Factory<E> restConverterFactory;
    private ExceptionFactory<E> exceptionFactory;
    private ResultDispatcher<E> resultDispatcher = new ResultDispatcher.Default<>();

    public RestAPIFactory() {

    }

    public void setConstantPool(ValueMap constantPool) {
        this.constantPool = constantPool;
    }

    public void setEndpoint(@NotNull Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    public void setAuthorization(Authorization authorization) {
        this.authorization = authorization;
    }

    public void setHttpClient(@NotNull RestHttpClient restClient) {
        this.httpClient = restClient;
    }

    public void setHttpRequestFactory(@NotNull HttpRequest.Factory factory) {
        this.httpRequestFactory = factory;
    }

    public void setExceptionFactory(@NotNull ExceptionFactory<E> factory) {
        this.exceptionFactory = factory;
    }

    public void setRestConverterFactory(@NotNull RestConverter.Factory<E> restConverterFactory) {
        this.restConverterFactory = restConverterFactory;
    }

    public void setRestRequestFactory(@NotNull RestRequest.Factory<E> restRequestFactory) {
        this.restRequestFactory = restRequestFactory;
    }

    public void setResultDispatcher(@NotNull ResultDispatcher<E> resultDispatcher) {
        this.resultDispatcher = resultDispatcher;
    }

    public static RestClient getRestClient(Object obj) {
        final InvocationHandler handler = Proxy.getInvocationHandler(obj);
        if (!(handler instanceof RestClient)) throw new IllegalArgumentException();
        return (RestClient) handler;
    }

    @SuppressWarnings("unchecked")
    public <T> T build(Class<T> cls) {
        final ClassLoader classLoader = cls.getClassLoader();
        final Class[] interfaces = new Class[]{cls};
        checkNotNull(endpoint, "Endpoint");
        checkNotNull(httpClient, "HttpClient");
        checkNotNull(restConverterFactory, "RestConverter.Factory");
        checkNotNull(restRequestFactory, "RestRequest.Factory");
        checkNotNull(httpRequestFactory, "HttpRequest.Factory");
        checkNotNull(exceptionFactory, "ExceptionFactory");
        checkNotNull(resultDispatcher, "ResultDispatcher");
        return (T) Proxy.newProxyInstance(classLoader, interfaces, new RestInvocationHandler(endpoint,
                authorization, httpClient, restConverterFactory, restRequestFactory, httpRequestFactory,
                exceptionFactory, constantPool, resultDispatcher));
    }

    private static void checkNotNull(Object object, String name) {
        if (object == null) throw new NullPointerException(name + " must not be null");
    }

    private static class RestInvocationHandler<E extends Exception> implements InvocationHandler, RestClient {
        private final Endpoint endpoint;
        private final Authorization authorization;

        private final RestConverter.Factory<E> converterFactory;
        private final RestRequest.Factory<E> requestInfoFactory;
        private final HttpRequest.Factory requestFactory;
        private final ExceptionFactory<E> exceptionFactory;
        private final ResultDispatcher<E> resultDispatcher;
        private final RestHttpClient restClient;
        private final ValueMap constantPoll;

        public RestInvocationHandler(Endpoint endpoint, Authorization authorization,
                                     RestHttpClient restClient,
                                     RestConverter.Factory<E> converterFactory,
                                     RestRequest.Factory<E> restRequestFactory,
                                     HttpRequest.Factory httpRequestFactory,
                                     ExceptionFactory<E> exceptionFactory,
                                     ValueMap constantPoll, ResultDispatcher<E> resultDispatcher) {
            this.endpoint = endpoint;
            this.authorization = authorization;
            this.restClient = restClient;
            this.converterFactory = converterFactory;
            this.requestInfoFactory = restRequestFactory;
            this.requestFactory = httpRequestFactory;
            this.exceptionFactory = exceptionFactory;
            this.constantPoll = constantPoll;
            this.resultDispatcher = resultDispatcher;
        }

        @Override
        public Endpoint getEndpoint() {
            return endpoint;
        }

        @Override
        public RestHttpClient getRestClient() {
            return restClient;
        }

        @Override
        public Authorization getAuthorization() {
            return authorization;
        }

        @SuppressWarnings({"TryWithIdenticalCatches"})
        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws E {
            try {
                if (method.getDeclaringClass() == Object.class) {
                    return method.invoke(this, args);
                } else if (method.getDeclaringClass() == RestClient.class) {
                    return method.invoke(this, args);
                }
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            final RestMethod<E> restMethod = RestMethod.get(method, args);
            final Callback<?, E> callback = findCallback(args);
            RestRequest restRequest;
            HttpCall httpCall = null;
            HttpRequest httpRequest = null;
            HttpResponse httpResponse = null;
            try {
                // Get converter before network requests, https://github.com/TwidereProject/Twidere-Android/issues/378
                // We can throw exceptions before network requests sent
                final Type returnType = method.getGenericReturnType();
                final RestConverter<HttpResponse, ?, E> converter = converterFactory.forResponse(returnType);
                restRequest = requestInfoFactory.create(restMethod, converterFactory, constantPoll);
                httpRequest = requestFactory.create(endpoint, restRequest, authorization, converterFactory);
                httpCall = restClient.newCall(httpRequest);
                httpResponse = httpCall.execute();
                if (!httpResponse.isSuccessful()) {
                    return onError(null, httpRequest, httpResponse, callback);
                }
                return onResult(converter, httpResponse, callback);
            } catch (IOException e) {
                return onError(e, httpRequest, httpResponse, callback);
            } catch (RestConverter.ConvertException e) {
                return onError(e, httpRequest, httpResponse, callback);
            } finally {
                RestFuUtils.closeSilently(httpResponse);
                RestFuUtils.closeSilently(httpCall);
            }
        }

        private <T> Object onResult(RestConverter<HttpResponse, T, E> converter, HttpResponse httpResponse,
                                    @Nullable final Callback<?, E> callback) throws RestConverter.ConvertException, E,
                IOException {
            if (callback == null) {
                return converter.convert(httpResponse);
            }
            if (callback instanceof RawCallback) {
                //noinspection unchecked
                RawCallback<E> rawCallback = (RawCallback<E>) callback;
                resultDispatcher.dispatchResult(rawCallback, httpResponse);
            } else {
                //noinspection unchecked
                Callback<T, E> typedCallback = (Callback<T, E>) callback;
                resultDispatcher.dispatchResult(typedCallback, converter.convert(httpResponse));
            }
            return null;
        }


        private Object onError(final Throwable cause, final HttpRequest httpRequest, final HttpResponse response,
                               @Nullable final Callback<?, E> callback) throws E {
            final E exception = exceptionFactory.newException(cause, httpRequest, response);
            if (callback == null) {
                throw exception;
            }
            //noinspection unchecked
            resultDispatcher.dispatchException(callback, exception);
            return null;
        }

        @Nullable
        private <T> Callback<T, E> findCallback(@Nullable Object[] args) {
            if (args == null) return null;
            for (Object arg : args) {
                if (arg instanceof Callback) {
                    //noinspection unchecked
                    return (Callback<T, E>) arg;
                }
            }
            return null;
        }

    }

}

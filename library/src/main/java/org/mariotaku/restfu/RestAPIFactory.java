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

import android.support.annotation.Nullable;

import org.mariotaku.restfu.callback.ErrorCallback;
import org.mariotaku.restfu.callback.RawCallback;
import org.mariotaku.restfu.callback.RestCallback;
import org.mariotaku.restfu.exception.RestException;
import org.mariotaku.restfu.http.Authorization;
import org.mariotaku.restfu.http.Endpoint;
import org.mariotaku.restfu.http.RestHttpCallback;
import org.mariotaku.restfu.http.RestHttpClient;
import org.mariotaku.restfu.http.RestHttpRequest;
import org.mariotaku.restfu.http.RestHttpResponse;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Created by mariotaku on 15/2/6.
 */
public class RestAPIFactory {

    private Endpoint endpoint;
    private Authorization authorization;
    private Converter converter;
    private RestHttpClient restClient;
    private HttpRequestFactory httpRequestFactory;
    private ExceptionFactory exceptionFactory;
    private RequestInfoFactory requestInfoFactory;

    public void setEndpoint(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    public void setAuthorization(Authorization authorization) {
        this.authorization = authorization;
    }

    public void setConverter(Converter converter) {
        this.converter = converter;
    }

    public void setClient(RestHttpClient restClient) {
        this.restClient = restClient;
    }

    public void setHttpRequestFactory(HttpRequestFactory factory) {
        this.httpRequestFactory = factory;
    }

    public void setExceptionFactory(ExceptionFactory factory) {
        this.exceptionFactory = factory;
    }

    public RestAPIFactory() {

    }

    @SuppressWarnings("unchecked")
    public <T> T build(Class<T> cls) {
        final ClassLoader classLoader = cls.getClassLoader();
        final Class[] interfaces = new Class[]{cls};
        return (T) Proxy.newProxyInstance(classLoader, interfaces, new RestInvocationHandler(endpoint,
                authorization, restClient, converter, requestInfoFactory, httpRequestFactory, exceptionFactory));
    }

    public static RestClient getRestClient(Object obj) {
        final InvocationHandler handler = Proxy.getInvocationHandler(obj);
        if (!(handler instanceof RestClient)) throw new IllegalArgumentException();
        return (RestClient) handler;
    }

    public void setRequestInfoFactory(RequestInfoFactory requestInfoFactory) {
        this.requestInfoFactory = requestInfoFactory;
    }

    private static class RestInvocationHandler implements InvocationHandler, RestClient {
        private final Endpoint endpoint;
        private final Authorization authorization;
        private final Converter converter;
        private final HttpRequestFactory requestFactory;
        private final ExceptionFactory exceptionFactory;
        private final RequestInfoFactory requestInfoFactory;

        @Override
        public Endpoint getEndpoint() {
            return endpoint;
        }

        @Override
        public RestHttpClient getRestClient() {
            return restClient;
        }

        @Override
        public Converter getConverter() {
            return converter;
        }

        @Override
        public Authorization getAuthorization() {
            return authorization;
        }

        private final RestHttpClient restClient;

        public RestInvocationHandler(Endpoint endpoint, Authorization authorization,
                                     RestHttpClient restClient, Converter converter,
                                     RequestInfoFactory requestInfoFactory, HttpRequestFactory requestFactory, ExceptionFactory exceptionFactory) {
            this.endpoint = endpoint;
            this.authorization = authorization;
            this.restClient = restClient;
            this.converter = converter;
            this.requestInfoFactory = requestInfoFactory;
            this.requestFactory = requestFactory != null ? requestFactory : new RestHttpRequest.DefaultFactory();
            this.exceptionFactory = exceptionFactory != null ? exceptionFactory : new DefaultExceptionFactory();
        }

        @SuppressWarnings({"TryWithIdenticalCatches"})
        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Exception {
            final RestMethodInfo methodInfo = RestMethodInfo.get(method, args);
            final RestRequestInfo requestInfo;
            if (requestInfoFactory != null) {
                requestInfo = requestInfoFactory.create(methodInfo);
            } else {
                requestInfo = methodInfo.toRequestInfo();
            }
            final RestHttpRequest restHttpRequest = requestFactory.create(endpoint, requestInfo, authorization);
            final Class<?>[] parameterTypes = method.getParameterTypes();
            RestHttpResponse response = null;
            try {
                response = restClient.execute(restHttpRequest);
                if (parameterTypes.length > 0) {
                    final Class<?> lastParameterType = parameterTypes[parameterTypes.length - 1];
                    if (RestCallback.class.isAssignableFrom(lastParameterType)) {
                        final RestCallback<?> callback = (RestCallback<?>) args[args.length - 1];
                        restClient.enqueue(restHttpRequest, new RestHttpCallback() {
                            @Override
                            public void callback(final RestHttpResponse response) {
                                if (callback != null) {
                                    try {
                                        invokeCallback(callback, converter.convert(response, method.getGenericReturnType()));
                                    } catch (Exception e) {
                                        callback.error(e);
                                    }
                                }
                            }

                            @Override
                            public void exception(final IOException ioe) {
                                callback.error(ioe);
                            }

                            @Override
                            public void cancelled() {

                            }
                        });
                        return null;
                    } else if (RawCallback.class.isAssignableFrom(lastParameterType)) {
                        final RawCallback callback = (RawCallback) args[args.length - 1];
                        restClient.enqueue(restHttpRequest, new RestHttpCallback() {
                            @Override
                            public void callback(final RestHttpResponse response) {
                                try {
                                    callback.result(response);
                                } catch (IOException e) {
                                    callback.error(e);
                                }
                            }

                            @Override
                            public void exception(final IOException ioe) {
                                callback.error(ioe);
                            }

                            @Override
                            public void cancelled() {

                            }
                        });
                        return null;
                    }
                }
                if (!response.isSuccessful()) {
                    onError(null, args, restHttpRequest, parameterTypes, response);
                    return null;
                }
                return converter.convert(response, method.getGenericReturnType());
            } catch (IOException e) {
                onError(e, args, restHttpRequest, parameterTypes, response);
                return null;
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } finally {
                Utils.closeSilently(response);
            }
        }

        private void onError(@Nullable final IOException cause, final Object[] args,
                             final RestHttpRequest restHttpRequest, final Class<?>[] parameterTypes,
                             final RestHttpResponse response) throws Exception {
            final Exception re = exceptionFactory.newException(cause, restHttpRequest, response);
            if (parameterTypes.length > 0) {
                final Class<?> lastParameterType = parameterTypes[parameterTypes.length - 1];
                if (ErrorCallback.class.isAssignableFrom(lastParameterType)) {
                    final ErrorCallback callback = (ErrorCallback) args[args.length - 1];
                    if (callback != null) {
                        callback.error(re);
                        return;
                    }
                }
            }
            throw re;
        }

        private static <T> void invokeCallback(final RestCallback<?> callback, final T result) {
            //noinspection unchecked
            ((RestCallback<T>) callback).result(result);
        }
    }

    public static final class DefaultExceptionFactory implements ExceptionFactory {

        @Override
        public Exception newException(@Nullable Throwable cause, @Nullable final RestHttpRequest request,
                                      @Nullable final RestHttpResponse response) {
            final RestException e = new RestException(cause);
            e.setRequest(request);
            e.setResponse(response);
            return e;
        }
    }
}

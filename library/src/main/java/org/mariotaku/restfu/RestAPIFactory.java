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

import org.mariotaku.restfu.callback.ErrorCallback;
import org.mariotaku.restfu.callback.RestCallback;
import org.mariotaku.restfu.exception.RestException;
import org.mariotaku.restfu.http.*;

import java.io.IOException;
import java.lang.reflect.*;

/**
 * Created by mariotaku on 15/2/6.
 */
public class RestAPIFactory<E extends Exception> {

    private Endpoint endpoint;
    private Authorization authorization;
    private RestHttpClient httpClient;

    private HttpRequest.Factory httpRequestFactory = new HttpRequest.DefaultFactory();
    private RestRequest.Factory<E> restRequestFactory = new RestRequest.DefaultFactory<>();
    private RestConverter.Factory<E> restConverterFactory;
    @SuppressWarnings("unchecked")
    private ExceptionFactory<E> exceptionFactory = new DefaultExceptionFactory();
    private ValueMap constantPool;

    public RestAPIFactory() {

    }

    public void setConstantPool(ValueMap constantPool) {
        this.constantPool = constantPool;
    }

    public void setEndpoint(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    public void setAuthorization(Authorization authorization) {
        this.authorization = authorization;
    }

    public void setHttpClient(RestHttpClient restClient) {
        this.httpClient = restClient;
    }

    public void setHttpRequestFactory(HttpRequest.Factory factory) {
        this.httpRequestFactory = factory;
    }

    public void setExceptionFactory(ExceptionFactory<E> factory) {
        this.exceptionFactory = factory;
    }

    public void setRestConverterFactory(RestConverter.Factory<E> restConverterFactory) {
        this.restConverterFactory = restConverterFactory;
    }

    public void setRestRequestFactory(RestRequest.Factory<E> restRequestFactory) {
        this.restRequestFactory = restRequestFactory;
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
        return (T) Proxy.newProxyInstance(classLoader, interfaces, new RestInvocationHandler(endpoint,
                authorization, httpClient, restConverterFactory, restRequestFactory, httpRequestFactory,
                exceptionFactory, constantPool));
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
        private final ExceptionFactory exceptionFactory;
        private final ValueMap constantPoll;
        private final RestHttpClient restClient;

        public RestInvocationHandler(Endpoint endpoint, Authorization authorization,
                                     RestHttpClient restClient,
                                     RestConverter.Factory<E> converterFactory,
                                     RestRequest.Factory<E> restRequestFactory,
                                     HttpRequest.Factory httpRequestFactory,
                                     ExceptionFactory exceptionFactory,
                                     ValueMap constantPoll) {
            this.endpoint = endpoint;
            this.authorization = authorization;
            this.restClient = restClient;
            this.converterFactory = converterFactory;
            this.requestInfoFactory = restRequestFactory;
            this.requestFactory = httpRequestFactory;
            this.exceptionFactory = exceptionFactory;
            this.constantPoll = constantPoll;
        }

        private static <T> void invokeCallback(final RestCallback<?> callback, final T result) {
            //noinspection unchecked
            ((RestCallback<T>) callback).result(result);
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
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Exception {
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(this, args);
            } else if (method.getDeclaringClass() == RestClient.class) {
                return method.invoke(this, args);
            }
            final RestMethod<E> restMethod = RestMethod.get(method, args);
            final RestRequest restRequest = requestInfoFactory.create(restMethod, converterFactory, constantPoll);
            final HttpRequest httpRequest = requestFactory.create(endpoint, restRequest, authorization, converterFactory);
            HttpCall httpCall = null;
            HttpResponse httpResponse = null;
            try {
                httpCall = restClient.newCall(httpRequest);
                httpResponse = httpCall.execute();
                if (!httpResponse.isSuccessful()) {
                    return onError(null, httpRequest, httpResponse, args);
                }
                final Type returnType = method.getGenericReturnType();
                return converterFactory.forResponse(returnType).convert(httpResponse);
            } catch (IOException e) {
                return onError(e, httpRequest, httpResponse, args);
            } catch (RestConverter.ConvertException e) {
                return onError(e, httpRequest, httpResponse, args);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } finally {
                RestFuUtils.closeSilently(httpResponse);
                RestFuUtils.closeSilently(httpCall);
            }
        }

        private Object onError(final Throwable cause, final HttpRequest httpRequest, final HttpResponse response,
                               final Object[] args) throws Exception {
            final Exception exception = exceptionFactory.newException(cause, httpRequest, response);
            if (args != null) {
                for (Object arg : args) {
                    if (arg instanceof ErrorCallback) {
                        ((ErrorCallback) arg).error(exception);
                        return null;
                    }
                }
            }
            throw exception;
        }
    }

    public static final class DefaultExceptionFactory implements ExceptionFactory {

        @Override
        public RestException newException(final Throwable cause, final HttpRequest request,
                                          final HttpResponse response) {
            final RestException e = new RestException(cause);
            e.setRequest(request);
            e.setResponse(response);
            return e;
        }
    }
}

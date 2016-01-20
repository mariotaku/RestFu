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
public class RestAPIFactory {

    private Endpoint endpoint;
    private Authorization authorization;
    private RestHttpClient httpClient;

    private RestConverter.Factory restConverterFactory;
    private HttpRequest.Factory httpRequestFactory;
    private ExceptionFactory exceptionFactory;
    private RestRequest.Factory restRequestFactory;
    private ValueMap constantPool;

    public RestAPIFactory() {

    }

    public static RestClient getRestClient(Object obj) {
        final InvocationHandler handler = Proxy.getInvocationHandler(obj);
        if (!(handler instanceof RestClient)) throw new IllegalArgumentException();
        return (RestClient) handler;
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

    public void setRestConverterFactory(RestConverter.Factory restConverterFactory) {
        this.restConverterFactory = restConverterFactory;
    }

    public void setHttpClient(RestHttpClient restClient) {
        this.httpClient = restClient;
    }

    public void setHttpRequestFactory(HttpRequest.Factory factory) {
        this.httpRequestFactory = factory;
    }

    public void setExceptionFactory(ExceptionFactory factory) {
        this.exceptionFactory = factory;
    }

    @SuppressWarnings("unchecked")
    public <T> T build(Class<T> cls) {
        final ClassLoader classLoader = cls.getClassLoader();
        final Class[] interfaces = new Class[]{cls};
        return (T) Proxy.newProxyInstance(classLoader, interfaces, new RestInvocationHandler(endpoint,
                authorization, httpClient, restConverterFactory, restRequestFactory, httpRequestFactory,
                exceptionFactory, constantPool));
    }

    public void setRestRequestFactory(RestRequest.Factory restRequestFactory) {
        this.restRequestFactory = restRequestFactory;
    }

    private static class RestInvocationHandler implements InvocationHandler, RestClient {
        private final Endpoint endpoint;
        private final Authorization authorization;

        private final RestConverter.Factory responseConverterFactory;
        private final HttpRequest.Factory requestFactory;
        private final ExceptionFactory exceptionFactory;
        private final RestRequest.Factory requestInfoFactory;
        private final ValueMap constantPoll;
        private final RestHttpClient restClient;

        public RestInvocationHandler(Endpoint endpoint, Authorization authorization,
                                     RestHttpClient restClient,
                                     RestConverter.Factory restConverterFactory,
                                     RestRequest.Factory restRequestFactory,
                                     HttpRequest.Factory httpRequestFactory,
                                     ExceptionFactory exceptionFactory,
                                     ValueMap constantPoll) {
            this.endpoint = endpoint;
            this.authorization = authorization;
            this.restClient = restClient;
            this.responseConverterFactory = restConverterFactory;
            this.requestInfoFactory = restRequestFactory != null ? restRequestFactory : new RestRequest.DefaultFactory();
            this.requestFactory = httpRequestFactory != null ? httpRequestFactory : new HttpRequest.DefaultFactory();
            this.exceptionFactory = exceptionFactory != null ? exceptionFactory : new DefaultExceptionFactory();
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
                return method.invoke(proxy, args);
            } else if (method.getDeclaringClass() == RestClient.class) {
                return method.invoke(this, args);
            }
            final RestMethod restMethod = RestMethod.get(method, args);
            final RestRequest restRequest = requestInfoFactory.create(restMethod, responseConverterFactory, constantPoll);
            final HttpRequest httpRequest = requestFactory.create(endpoint, restRequest, authorization);
            final Class<?>[] parameterTypes = method.getParameterTypes();
            HttpCall httpCall = null;
            HttpResponse httpResponse = null;
            try {
                httpCall = restClient.newCall(httpRequest);
                httpResponse = httpCall.execute();
                if (!httpResponse.isSuccessful()) {
                    onError(null, args, httpRequest, parameterTypes, httpResponse);
                    return null;
                }
                final Type returnType = method.getGenericReturnType();
                return responseConverterFactory.fromResponse(returnType).convert(httpResponse);
            } catch (IOException e) {
                onError(e, args, httpRequest, parameterTypes, httpResponse);
                return null;
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } finally {
                Utils.closeSilently(httpResponse);
                Utils.closeSilently(httpCall);
            }
        }

        private void onError(final IOException cause, final Object[] args,
                             final HttpRequest httpRequest, final Class<?>[] parameterTypes,
                             final HttpResponse response) throws Exception {
            final Exception re = exceptionFactory.newException(cause, httpRequest, response);
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
    }

    public static final class DefaultExceptionFactory implements ExceptionFactory {

        @Override
        public Exception newException(final Throwable cause, final HttpRequest request,
                                      final HttpResponse response) {
            final RestException e = new RestException(cause);
            e.setRequest(request);
            e.setResponse(response);
            return e;
        }
    }
}

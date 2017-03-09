package org.mariotaku.restfu;

import org.mariotaku.restfu.callback.Callback;

import java.io.IOException;

/**
 * Created by mariotaku on 16/6/6.
 */
public interface ResultDispatcher<E extends Exception> {
    <T> void dispatchResult(Callback<T, E> callback, T obj) throws IOException, E;

    <T> void dispatchException(Callback<T, E> callback, E ex);

    final class Default<E extends Exception> implements ResultDispatcher<E> {

        @Override
        public <T> void dispatchResult(Callback<T, E> callback, T result) throws IOException, E {
            callback.result(result);
        }

        @Override
        public <T> void dispatchException(Callback<T, E> callback, E exception) {
            callback.error(exception);
        }
    }
}

package org.mariotaku.restfu;

import org.mariotaku.restfu.callback.ErrorCallback;
import org.mariotaku.restfu.callback.RestCallback;

/**
 * Created by mariotaku on 16/6/6.
 */
public interface ResultDispatcher {
    <T> void dispatchResult(RestCallback<T> callback, T obj);

    <E extends Exception> void dispatchException(ErrorCallback<E> callback, E ex);

    final class Default implements ResultDispatcher {

        @Override
        public <T> void dispatchResult(RestCallback<T> callback, T result) {
            callback.result(result);
        }

        @Override
        public <E extends Exception> void dispatchException(ErrorCallback<E> callback, E exception) {
            callback.error(exception);
        }
    }
}

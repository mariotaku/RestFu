package org.mariotaku.restfu;

import org.jetbrains.annotations.NotNull;
import org.mariotaku.restfu.callback.Callback;

import java.io.IOException;

/**
 * Created by mariotaku on 16/6/6.
 */
public interface ResultDispatcher<E extends Exception> {
    <T> void dispatchResult(@NotNull Callback<T, E> callback, @NotNull T obj) throws IOException, E;

    <T> void dispatchException(@NotNull Callback<T, E> callback, @NotNull E ex);

    final class Default<E extends Exception> implements ResultDispatcher<E> {

        @Override
        public <T> void dispatchResult(@NotNull Callback<T, E> callback, @NotNull T result) throws IOException, E {
            callback.result(result);
        }

        @Override
        public <T> void dispatchException(@NotNull Callback<T, E> callback, @NotNull E exception) {
            callback.error(exception);
        }
    }
}

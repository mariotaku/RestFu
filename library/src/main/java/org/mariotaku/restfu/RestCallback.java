package org.mariotaku.restfu;

/**
 * Created by mariotaku on 15/2/7.
 */
public interface RestCallback<T> extends ErrorCallback {
    void result(T result);

}

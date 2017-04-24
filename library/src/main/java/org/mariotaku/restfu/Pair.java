package org.mariotaku.restfu;

/**
 * A convenience class to represent tuple.
 * <p>
 * Created by mariotaku on 15/11/11.
 */
public class Pair<F, S> {
    public F first;
    public S second;

    public Pair(F first, S second) {
        this.first = first;
        this.second = second;
    }

    public static <F, S> Pair<F, S> create(F first, S second) {
        return new Pair<>(first, second);
    }
}

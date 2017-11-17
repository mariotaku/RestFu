package org.mariotaku.restfu.http;

import org.mariotaku.commons.collection.MultiMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by mariotaku on 16/1/17.
 */
public final class MultiValueMap<V> extends MultiMap<String, V> {

    public MultiValueMap() {
        this(new HashMap<String, List<V>>(), false);
    }

    public MultiValueMap(Map<String, List<V>> map) {
        this(map, false);
    }

    public MultiValueMap(boolean ignoreCase) {
        this(null, ignoreCase);
    }

    public MultiValueMap(Map<String, List<V>> map, boolean ignoreCase) {
        super(map, new StringEquatable(ignoreCase));
    }

    private static class StringEquatable implements Equatable<String> {
        private boolean ignoreCase;

        StringEquatable(boolean ignoreCase) {
            this.ignoreCase = ignoreCase;
        }

        @Override
        public boolean equals(String s1, String s2) {
            if (s1 == null || s2 == null) {
                //noinspection StringEquality
                return s1 == s2;
            }
            if (ignoreCase) {
                return s1.equalsIgnoreCase(s2);
            }
            return s1.equals(s2);
        }
    }
}

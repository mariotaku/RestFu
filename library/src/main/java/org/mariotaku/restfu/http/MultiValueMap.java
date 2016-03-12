package org.mariotaku.restfu.http;

import org.mariotaku.restfu.Pair;

import java.util.*;

/**
 * Created by mariotaku on 16/1/17.
 */
public final class MultiValueMap<V> {

    private final Map<String, List<V>> map;
    private final boolean ignoreCase;

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
        this.map = map != null ? map : new HashMap<String, List<V>>();
        this.ignoreCase = ignoreCase;
    }

    public V getFirst(String key) {
        final List<V> values = get(key);
        if (values == null || values.isEmpty()) return null;
        return values.get(0);
    }

    public List<V> get(String key) {
        if (ignoreCase) {
            for (Map.Entry<String, List<V>> entry : map.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(key)) {
                    return entry.getValue();
                }
            }
        }
        return map.get(key);
    }

    public List<Pair<String, V>> toList() {
        final ArrayList<Pair<String, V>> list = new ArrayList<>();
        for (Map.Entry<String, List<V>> entry : map.entrySet()) {
            for (V value : entry.getValue()) {
                list.add(Pair.create(entry.getKey(), value));
            }
        }
        return list;
    }

    public final void add(String key, V value) {
        List<V> list = get(key);
        if (list == null) {
            list = new ArrayList<>();
            map.put(key, list);
        }
        list.add(value);
    }

    public final void addAll(String key, V[] values) {
        List<V> list = get(key);
        if (list == null) {
            list = new ArrayList<>();
            map.put(key, list);
        }
        Collections.addAll(list, values);
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public Set<String> keySet() {
        return map.keySet();
    }
}

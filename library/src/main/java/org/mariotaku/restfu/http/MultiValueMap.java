package org.mariotaku.restfu.http;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

    public V getFirst(@NotNull String key) {
        final List<V> values = get(key);
        if (values == null || values.isEmpty()) return null;
        return values.get(0);
    }

    public List<V> get(@NotNull String key) {
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

    public final void add(@NotNull String key, V value) {
        List<V> list = get(key);
        if (list == null) {
            list = new ArrayList<>();
            map.put(key, list);
        }
        list.add(value);
    }

    public final void addAll(@NotNull String key, V[] values) {
        List<V> list = get(key);
        if (list == null) {
            list = new ArrayList<>();
            map.put(key, list);
        }
        if (values != null) {
            Collections.addAll(list, values);
        } else {
            list.add(null);
        }
    }

    public final void addAll(@NotNull String key, @Nullable Collection<V> values) {
        List<V> list = get(key);
        if (list == null) {
            list = new ArrayList<>();
            map.put(key, list);
        }
        if (values != null) {
            list.addAll(values);
        } else {
            list.add(null);
        }
    }

    public final void removeAll(@NotNull String key) {
        map.remove(key);
    }

    public final void clear() {
        map.clear();
    }

    public void addFrom(@NotNull MultiValueMap<V> another) {
        for (Map.Entry<String, List<V>> entry : another.map.entrySet()) {
            addAll(entry.getKey(), entry.getValue());
        }
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public Set<String> keySet() {
        return map.keySet();
    }

    public Set<Map.Entry<String, List<V>>> entrySet() {
        return map.entrySet();
    }
}

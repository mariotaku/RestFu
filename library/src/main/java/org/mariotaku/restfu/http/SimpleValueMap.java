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

package org.mariotaku.restfu.http;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by mariotaku on 15/5/8.
 */
public class SimpleValueMap implements ValueMap {

    private final Map<String, Object> internalMap = new HashMap<>();

    @Override
    public boolean has(String key) {
        return internalMap.containsKey(key);
    }

    @Override
    public Object get(String key) {
        return internalMap.get(key);
    }

    public void put(String key, Object value) {
        internalMap.put(key, value);
    }

    public void remove(String key) {
        internalMap.remove(key);
    }

    @Override
    public String[] keys() {
        final Set<String> keySet = internalMap.keySet();
        return keySet.toArray(new String[keySet.size()]);
    }

    protected void copyValue(ValueMap from, String key) {
        if (from.has(key)) {
            put(key, from.get(key));
        }
    }

    public final HashMap<String, Object> asMap() {
        return new HashMap<>(internalMap);
    }

}

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

import org.mariotaku.restfu.http.mime.BaseTypedData;
import org.mariotaku.restfu.http.mime.TypedData;
import org.mariotaku.restfu.param.File;

/**
 * Created by mariotaku on 15/2/6.
 */
public final class FileValue {
    private final File annotation;
    private final Object value;

    @Override
    public String toString() {
        return "FileValue{" +
                "annotation=" + annotation +
                ", value=" + value +
                '}';
    }

    public FileValue(File annotation, Object value) {
        this.annotation = annotation;
        this.value = value;
    }

    public TypedData body() {
        return BaseTypedData.wrap(value);
    }
}

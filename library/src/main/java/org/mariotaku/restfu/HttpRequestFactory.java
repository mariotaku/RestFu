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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.mariotaku.restfu.http.Authorization;
import org.mariotaku.restfu.http.Endpoint;
import org.mariotaku.restfu.http.RestHttpRequest;

/**
 * Created by mariotaku on 15/5/25.
 */
public interface HttpRequestFactory {
    RestHttpRequest create(@NonNull Endpoint endpoint, @NonNull RestRequestInfo info, @Nullable Authorization authorization);
}

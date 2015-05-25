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

/**
 * Creates {@link RestRequestInfo} from {@link RestMethodInfo}
 * <p/>
 * Use this method if you want to modify requests <b>before</b> normal HTTP request created.
 *
 * <br/>
 *
 * When using OAuth authorization, this would be very useful, because normal HTTP request cannot
 * be modified once OAuth signature generated.
 */
public interface RequestInfoFactory {
    RestRequestInfo create(RestMethodInfo methodInfo);
}

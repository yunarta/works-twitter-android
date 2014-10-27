/*
 * Copyright 2014-present Yunarta
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mobilesolutionworks.android.twitter.limit;

import org.json.JSONTokener;

/**
 * Created by yunarta on 27/10/14.
 */
public class AccessLimit {

    public class Resource {
        String uri;

        int limit;

        int remaining;

        long reset;
    }

    public AccessLimit(String json) {
        JSONTokener tokener = new JSONTokener(json);

    }
}

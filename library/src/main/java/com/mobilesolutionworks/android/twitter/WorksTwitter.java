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

package com.mobilesolutionworks.android.twitter;

import android.support.annotation.NonNull;

import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.auth.AccessToken;

/**
 * Created by yunarta on 20/10/14.
 */
public interface WorksTwitter {

    int FACEBOOK_DIALOG_REQUEST_MASK = 0xe000;

    interface Callback {

        void onCancelled();

        void onSessionOpened(AccessToken accessToken);
    }

    interface ResponseCallback {

        void onCancelled();

        void onCompleted(Status status);
    }

    void open(@NonNull WorksTwitter.Callback callback);

    void validate(@NonNull WorksTwitter.Callback callback);

    void tweets(StatusUpdate latestStatus, @NonNull WorksTwitter.ResponseCallback callback);

//    void readRequest(Request request, @NonNull WorksTwitter.ResponseCallback callback, String... newPermissions);

//    void publishRequest(Request request, @NonNull WorksTwitter.ResponseCallback callback, String... newPermissions);

    void close();

}

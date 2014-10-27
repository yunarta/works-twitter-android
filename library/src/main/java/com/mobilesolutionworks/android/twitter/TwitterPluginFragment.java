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

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import org.apache.commons.lang3.SerializationUtils;

import java.util.Map;

import bolts.Continuation;
import bolts.Task;
import twitter4j.RateLimitStatus;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

/**
 * Created by yunarta on 20/10/14.
 */
public class TwitterPluginFragment extends Fragment implements WorksTwitter {

    public static TwitterPluginFragment create(String consumerKey, String consumerSecret) {
        Bundle args = new Bundle();
        args.putString("consumerKey", consumerKey);
        args.putString("consumerSecret", consumerSecret);

        TwitterPluginFragment fragment = new TwitterPluginFragment();
        fragment.setArguments(args);

        return fragment;
    }

    protected String mConsumerKey;

    protected String mConsumerSecret;

    protected AccessToken mAccessToken;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle arguments = getArguments();

        if (arguments == null) {
            throw new IllegalArgumentException("arguments is not set");
        }

        mConsumerKey = arguments.getString("consumerKey");
        mConsumerSecret = arguments.getString("consumerSecret");
        if (TextUtils.isEmpty(mConsumerKey) || TextUtils.isEmpty(mConsumerSecret)) {
            throw new IllegalArgumentException("both consumerKey and consumerSecret is required");
        }

        SharedPreferences preferences = getActivity().getSharedPreferences("twitter", Activity.MODE_PRIVATE);
        if (preferences.contains("access_token_str")) {
            String ats = preferences.getString("access_token_str", "");
            if (!TextUtils.isEmpty(ats)) {
                byte[] decode = Base64.decode(ats, Base64.DEFAULT);
                mAccessToken = SerializationUtils.deserialize(decode);
            }
        }

    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void open(@NonNull final Callback callback) {
        Configuration configuration = new ConfigurationBuilder().
                setOAuthConsumerKey(mConsumerKey).
                setOAuthConsumerSecret(mConsumerSecret).
                build();

        final Twitter instance = new TwitterFactory(configuration).getInstance();
        if (mAccessToken != null) {
            instance.setOAuthAccessToken(mAccessToken);
        }

        doValidate(mAccessToken, instance).continueWithTask(new Continuation<User, Task<AccessToken>>() {
            @Override
            public Task<AccessToken> then(Task<User> task) throws Exception {
                if (task.isFaulted()) {
                    SharedPreferences preferences = getActivity().getSharedPreferences("twitter", Activity.MODE_PRIVATE);
                    preferences.edit().clear().apply();

                    mAccessToken = null;
                    instance.setOAuthAccessToken(null);
                    return doGetAuthenticationURL(instance).onSuccessTask(new Continuation<RequestToken, Task<Bundle>>() {
                        @Override
                        public Task<Bundle> then(Task<RequestToken> task) throws Exception {
                            return doDialogAuthentication(task.getResult());
                        }
                    }).onSuccessTask(new Continuation<Bundle, Task<AccessToken>>() {
                        @Override
                        public Task<AccessToken> then(Task<Bundle> task) throws Exception {
                            return doGetAccessToken(instance, task.getResult());
                        }
                    }).continueWith(new Continuation<AccessToken, AccessToken>() {
                        @Override
                        public AccessToken then(Task<AccessToken> task) throws Exception {
                            if (task.isFaulted()) {
                                Log.d(BuildConfig.DEBUG_TAG, "Failed", task.getError());
                                Toast.makeText(getActivity(), task.getError().getMessage(), Toast.LENGTH_LONG).show();
                                callback.onCancelled();
                            } else if (task.isCompleted()) {
                                AccessToken accessToken = task.getResult();
                                String serialized = Base64.encodeToString(SerializationUtils.serialize(accessToken), Base64.DEFAULT);

                                SharedPreferences preferences = getActivity().getSharedPreferences("twitter", Activity.MODE_PRIVATE);
                                preferences.edit().putString("access_token_str", serialized).apply();
                                callback.onSessionOpened(accessToken);
                                mAccessToken = accessToken;

                                return accessToken;
                            }

                            return null;
                        }
                    });
                } else {
                    callback.onSessionOpened(mAccessToken);
                    return Task.forResult(mAccessToken);
                }
            }
        });
    }

    @Override
    public void validate(@NonNull final Callback callback) {
        Configuration configuration = new ConfigurationBuilder().
                setOAuthConsumerKey(mConsumerKey).
                setOAuthConsumerSecret(mConsumerSecret).
                build();

        final Twitter instance = new TwitterFactory(configuration).getInstance();
        if (mAccessToken != null) {
            instance.setOAuthAccessToken(mAccessToken);
        }

        doValidate(mAccessToken, instance).continueWith(new Continuation<User, AccessToken>() {
            @Override
            public AccessToken then(Task<User> task) throws Exception {
                if (task.isFaulted()) {
                    SharedPreferences preferences = getActivity().getSharedPreferences("twitter", Activity.MODE_PRIVATE);
                    preferences.edit().clear().apply();
                    mAccessToken = null;

                    callback.onSessionOpened(null);
                    return null;
                } else {
                    callback.onSessionOpened(mAccessToken);
                    return mAccessToken;
                }
            }
        });
    }

    @Override
    public void tweets(final StatusUpdate latestStatus, @NonNull final ResponseCallback callback) {
        Configuration configuration = new ConfigurationBuilder().
                setOAuthConsumerKey(mConsumerKey).
                setOAuthConsumerSecret(mConsumerSecret).
                build();

        final Twitter instance = new TwitterFactory(configuration).getInstance();
        if (mAccessToken != null) {
            instance.setOAuthAccessToken(mAccessToken);
        }

        doValidate(mAccessToken, instance).continueWithTask(new Continuation<User, Task<AccessToken>>() {
            @Override
            public Task<AccessToken> then(Task<User> task) throws Exception {
                if (task.isFaulted()) {
                    SharedPreferences preferences = getActivity().getSharedPreferences("twitter", Activity.MODE_PRIVATE);
                    preferences.edit().clear().apply();

                    mAccessToken = null;
                    instance.setOAuthAccessToken(null);
                    return doGetAuthenticationURL(instance).onSuccessTask(new Continuation<RequestToken, Task<Bundle>>() {
                        @Override
                        public Task<Bundle> then(Task<RequestToken> task) throws Exception {
                            return doDialogAuthentication(task.getResult());
                        }
                    }).onSuccessTask(new Continuation<Bundle, Task<AccessToken>>() {
                        @Override
                        public Task<AccessToken> then(Task<Bundle> task) throws Exception {
                            return doGetAccessToken(instance, task.getResult());
                        }
                    }).continueWith(new Continuation<AccessToken, AccessToken>() {
                        @Override
                        public AccessToken then(Task<AccessToken> task) throws Exception {
                            if (task.isFaulted()) {
                                Log.d(BuildConfig.DEBUG_TAG, "Failed", task.getError());
                                Toast.makeText(getActivity(), task.getError().getMessage(), Toast.LENGTH_LONG).show();
                                callback.onCancelled();
                            } else if (task.isCompleted()) {
                                AccessToken accessToken = task.getResult();
                                String serialized = Base64.encodeToString(SerializationUtils.serialize(accessToken), Base64.DEFAULT);

                                SharedPreferences preferences = getActivity().getSharedPreferences("twitter", Activity.MODE_PRIVATE);
                                preferences.edit().putString("access_token_str", serialized).apply();
                                instance.setOAuthAccessToken(accessToken);

                                mAccessToken = accessToken;

                                return accessToken;
                            }

                            return null;
                        }
                    });
                } else {
                    return Task.forResult(mAccessToken);
                }
//            }
//        }).onSuccessTask(new Continuation<AccessToken, Task<Map<String, RateLimitStatus>>>() {
//            @Override
//            public Task<Map<String, RateLimitStatus>> then(Task<AccessToken> task) throws Exception {
//                return doCheckStatus(instance);
            }
        }).onSuccessTask(new Continuation<AccessToken, Task<Status>>() {
            @Override
            public Task<Status> then(Task<AccessToken> task) throws Exception {
//                Map<String, RateLimitStatus> result = task.getResult();

                return doTweet(instance, latestStatus);
            }
        }).continueWith(new Continuation<Status, Object>() {
            @Override
            public Object then(Task<Status> task) throws Exception {
                if (task.isFaulted()) {
                    Log.d(BuildConfig.DEBUG_TAG, "Failed", task.getError());
                    Toast.makeText(getActivity(), task.getError().getMessage(), Toast.LENGTH_LONG).show();
                    callback.onCancelled();
                } else if (task.isCompleted()) {
                    callback.onCompleted(task.getResult());
                }

                return null;
            }
        });
    }

    @Override
    public void close() {
        SharedPreferences preferences = getActivity().getSharedPreferences("twitter", Activity.MODE_PRIVATE);
        preferences.edit().clear().apply();

        mAccessToken = null;
    }

    @SuppressWarnings("unchecked")
    protected Task<User> doValidate(final AccessToken accessToken, final Twitter instance) {
        final Task<User>.TaskCompletionSource source = Task.create();
        if (accessToken == null) {
            source.trySetError(new IllegalStateException());
        } else {
            source.trySetResult(null);
//            new AsyncTask<Task<User>.TaskCompletionSource, Void, Object>() {
//                @Override
//                protected Object doInBackground(Task<User>.TaskCompletionSource... params) {
//                    try {
//                        return instance.verifyCredentials();
//                    } catch (TwitterException e) {
//                        return e;
//                    }
//                }
//
//                @Override
//                protected void onPostExecute(Object s) {
//                    super.onPostExecute(s);
//                    if (s instanceof User) {
//                        source.trySetResult((User) s);
//                    } else {
//                        source.trySetError((Exception) s);
//                    }
//                }
//            }.execute(source);
        }

        return source.getTask();
    }

    @SuppressWarnings("unchecked")
    protected Task<RequestToken> doGetAuthenticationURL(final Twitter instance) {
        final Task<RequestToken>.TaskCompletionSource source = Task.create();

        new AsyncTask<Task<RequestToken>.TaskCompletionSource, Void, Object>() {
            @Override
            protected Object doInBackground(Task<RequestToken>.TaskCompletionSource... params) {
                try {
                    return instance.getOAuthRequestToken("oauth://works-twitter");
                } catch (TwitterException e) {
                    return e;
                }
            }

            @Override
            protected void onPostExecute(Object s) {
                super.onPostExecute(s);
                if (s instanceof RequestToken) {
                    source.trySetResult((RequestToken) s);
                } else {
                    source.trySetError((Exception) s);
                }
            }
        }.execute(source);
        return source.getTask();
    }

    protected Task<Bundle> doDialogAuthentication(RequestToken token) {
        final Task<Bundle>.TaskCompletionSource source = Task.create();

        WebDialog dialog = new WebDialog(getActivity(), WebDialog.DEFAULT_THEME, token, new WebDialog.OnCompleteListener() {
            @Override
            public void onComplete(Bundle values, com.mobilesolutionworks.android.twitter.TwitterException error) {
                if (error != null) {
                    source.trySetError(error);
                } else {
                    source.trySetResult(values);
                }
            }
        });
        dialog.show();
        return source.getTask();
    }

    protected Task<AccessToken> doGetAccessToken(final Twitter instance, Bundle bundle) {
        final Task<AccessToken>.TaskCompletionSource source = Task.create();

        new AsyncTask<Bundle, Void, Object>() {

            @Override
            protected Object doInBackground(Bundle... params) {
                try {
                    Bundle result = params[0];

                    RequestToken token = (RequestToken) result.getSerializable("request_token");
                    String verifier = result.getString("oauth_verifier");

                    AccessToken accessToken = instance.getOAuthAccessToken(token, verifier);
                    Log.d(BuildConfig.DEBUG_TAG, "accessToken.getScreenName() = " + accessToken.getScreenName());
                    Log.d(BuildConfig.DEBUG_TAG, "accessToken.getUserId() = " + accessToken.getUserId());
                    Log.d(BuildConfig.DEBUG_TAG, "accessToken.getToken() = " + accessToken.getToken());
                    Log.d(BuildConfig.DEBUG_TAG, "accessToken.getTokenSecret() = " + accessToken.getTokenSecret());

                    return accessToken;
                } catch (TwitterException e) {
                    return e;
                }
            }

            @Override
            protected void onPostExecute(Object o) {
                super.onPostExecute(o);
                if (o instanceof AccessToken) {
                    source.trySetResult((AccessToken) o);
                } else {
                    source.trySetError((Exception) o);
                }

            }
        }.execute(bundle);
        return source.getTask();
    }

    protected Task<twitter4j.Status> doTweet(final Twitter instance, final StatusUpdate latestStatus) {
        final Task<twitter4j.Status>.TaskCompletionSource source = Task.create();

        new AsyncTask<StatusUpdate, Void, Object>() {

            @Override
            protected Object doInBackground(StatusUpdate... params) {
                try {
                    return instance.updateStatus(latestStatus);
                } catch (TwitterException e) {
                    return e;
                }
            }

            @Override
            protected void onPostExecute(Object o) {
                super.onPostExecute(o);
                if (o instanceof AccessToken) {
                    source.trySetResult((twitter4j.Status) o);
                } else {
                    source.trySetError((Exception) o);
                }

            }
        }.execute(latestStatus);
        return source.getTask();
    }

    protected Task<Map<String, RateLimitStatus>> doCheckStatus(final Twitter instance) {
        final Task<Map<String, RateLimitStatus>>.TaskCompletionSource source = Task.create();

        new AsyncTask<Void, Void, Object>() {

            @Override
            protected Object doInBackground(Void... params) {
                try {
                    return instance.getRateLimitStatus();
                } catch (TwitterException e) {
                    return e;
                }
            }

            @Override
            protected void onPostExecute(Object o) {
                super.onPostExecute(o);
                if (o instanceof Map) {
                    source.trySetResult((Map<String, RateLimitStatus>) o);
                } else {
                    source.trySetError((Exception) o);
                }

            }
        }.execute();
        return source.getTask();
    }
}

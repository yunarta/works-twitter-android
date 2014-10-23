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

package com.mobilesolutionworks.android.twitter.test;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.mobilesolutionworks.android.twitter.TwitterPluginFragment;
import com.mobilesolutionworks.android.twitter.WorksTwitter;

import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.auth.AccessToken;

public class MainActivity extends FragmentActivity implements View.OnClickListener {

    WorksTwitter mTwitter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_twitter_test);

        findViewById(R.id.btn_open).setOnClickListener(this);
        findViewById(R.id.btn_validate).setOnClickListener(this);
        findViewById(R.id.btn_close).setOnClickListener(this);
        findViewById(R.id.btn_request1).setOnClickListener(this);
        findViewById(R.id.btn_request2).setOnClickListener(this);
        findViewById(R.id.btn_request3).setOnClickListener(this);

        if (savedInstanceState == null) {
            TwitterPluginFragment fragment = TwitterPluginFragment.create("NHaDFIy56kdB6aFbvyLEeAX9Q", "KdJFIsbahBI2qNJ1U84csnwgM6EIvawpqJYTSYzFm0aQvF5j0U");
            mTwitter = fragment;

            getSupportFragmentManager().beginTransaction().add(fragment, "twitter").commit();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_open: {
                mTwitter.open(new StatusCallback());
                break;
            }

            case R.id.btn_validate: {
                mTwitter.validate(new StatusCallback());
                break;
            }

            case R.id.btn_close: {
                mTwitter.close();
                break;
            }

            case R.id.btn_request1: {
                EditText id = (EditText) findViewById(R.id.status_update);

                StatusUpdate request = new StatusUpdate(id.getText().toString());
                mTwitter.tweets(request, new WorksTwitter.ResponseCallback() {
                    @Override
                    public void onCancelled() {

                    }

                    @Override
                    public void onCompleted(Status status) {
                        Toast.makeText(MainActivity.this, "test response = " + status, Toast.LENGTH_SHORT).show();
                    }

                });
                break;
            }

//            case R.id.btn_request2: {
//                Request request = new Request(null, "/804889722866833/likes", null, HttpMethod.GET, null);
//                mFacebook.readRequest(request, new WorksFacebook.ResponseCallback() {
//                    @Override
//                    public void onCancelled() {
//
//                    }
//
//                    @Override
//                    public void onCompleted(Response response) {
//                        Toast.makeText(FacebookTestActivity.this, "test response = " + response, Toast.LENGTH_SHORT).show();
//                    }
//                });
//                break;
//            }
//
//            case R.id.btn_request3: {
//                mFacebook.requestMe(new WorksFacebook.ResponseCallback() {
//                    @Override
//                    public void onCancelled() {
//
//                    }
//
//
//                    @Override
//                    public void onCompleted(Response response) {
//                        Toast.makeText(FacebookTestActivity.this, "test response = " + response, Toast.LENGTH_SHORT).show();
//                    }
//                });
//                break;
//            }
        }
    }

    private class StatusCallback implements WorksTwitter.Callback {

        @Override
        public void onCancelled() {

        }

        @Override
        public void onSessionOpened(AccessToken accessToken) {
            TextView textView = (TextView) findViewById(R.id.status);
            if (accessToken != null) {
                textView.setText("Twitter Connected");
            } else {
                textView.setText("Facebook Disconnected");
            }
        }
    }
}

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

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import twitter4j.RateLimitStatus;

/**
 * Created by yunarta on 27/10/14.
 */
public class TwitterLimitDatabase {

    public static final String TWITTER_LIMITS = "twitter_limits";

    private final SQLiteDatabase mDb;

    public TwitterLimitDatabase(Context context) {
        mDb = new SQLiteOpenHelper(context, "twitter-limit", null, 1) {

            @Override
            public void onCreate(SQLiteDatabase db) {
                db.execSQL("CREATE TABLE IF NOT EXISTS " + TWITTER_LIMITS + " (" +
                        /* local uri    */ "uri TEXT," +
                        /* remote uri   */ "limit TEXT," +
                        /* data content */ "remaining TEXT," +
                        /* expiry time  */ "reset INTEGER," +
                        "PRIMARY KEY (uri) ON CONFLICT REPLACE" +
                        ")");

            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                db.execSQL("DROP TABLE IF EXISTS " + TWITTER_LIMITS);
                onCreate(db);
            }
        }.getWritableDatabase();
    }

    public void updateAll(Map<String, RateLimitStatus> limits) {
        List<ContentValues> bulk = new ArrayList<ContentValues>(limits.size());
        for (String key : limits.keySet()) {
            RateLimitStatus status = limits.get(key);

            ContentValues object = new ContentValues();
            object.put("uri", key);
            object.put("limit", status.getLimit());
            object.put("remaining", status.getRemaining());
            object.put("reset", System.currentTimeMillis() + status.getSecondsUntilReset() * 1000);

            mDb.insert(TWITTER_LIMITS, null, object);
        }
    }

//    public int getLimit(String uri) {
//        Cursor cursor = mDb.query(TWITTER_LIMITS, new String[]{"uri", "limit", "remaining", "reset"}, "uri = ?", new String[]{uri}, null, null, null);
//        if (cursor != null) {
//            if (cursor.moveToFirst()) {
//                int limit = cursor.getInt(1);
//                int remaining = cursor.getInt(2);
//                int reset = (int) cursor.getLong(3);
//            }
//            cursor.close();
//        }
//    }
}

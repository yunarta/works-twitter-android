package com.mobilesolutionworks.android.twitter;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by yunarta on 27/10/14.
 */
public class TwitterUtil {

    public static void close(Context context) {
        SharedPreferences preferences = context.getSharedPreferences("twitter", Activity.MODE_PRIVATE);
        preferences.edit().clear().apply();
    }
}

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

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import twitter4j.auth.RequestToken;

/**
 * Created by yunarta on 21/10/14.
 */
public class WebDialog extends Dialog {

    static final        String  REDIRECT_URI                  = "oauth://works-twitter";
    static final        boolean DISABLE_SSL_CHECK_FOR_TESTING = false;
    public static final int     DEFAULT_THEME                 = android.R.style.Theme_Translucent_NoTitleBar;

    // width below which there are no extra margins
    private static final int NO_PADDING_SCREEN_WIDTH   = 480;
    // width beyond which we're always using the MIN_SCALE_FACTOR
    private static final int MAX_PADDING_SCREEN_WIDTH  = 800;
    // height below which there are no extra margins
    private static final int NO_PADDING_SCREEN_HEIGHT  = 800;
    // height beyond which we're always using the MIN_SCALE_FACTOR
    private static final int MAX_PADDING_SCREEN_HEIGHT = 1280;

    // the minimum scaling factor for the web dialog (50% of screen size)
    private static final double MIN_SCALE_FACTOR = 0.5;
    // translucent border around the webview
    private static final int    BACKGROUND_GRAY  = 0xCC000000;

    /**
     * Interface that implements a listener to be called when the user's interaction with the
     * dialog completes, whether because the dialog finished successfully, or it was cancelled,
     * or an error was encountered.
     */
    public interface OnCompleteListener {
        /**
         * Called when the dialog completes.
         *
         * @param values on success, contains the values returned by the dialog
         * @param error  on an error, contains an exception describing the error
         */
        void onComplete(Bundle values, TwitterException error);
    }


    private WebView webView;

    private boolean isDetached;

    private ProgressDialog spinner;

    private FrameLayout contentFrameLayout;

    private ImageView crossImageView;

    private final RequestToken token;

    private OnCompleteListener listener;

    public WebDialog(Context context, int theme, RequestToken token, OnCompleteListener listener) {
        super(context, theme);
        this.token = token;
        this.listener = listener;
    }

    @Override
    public void dismiss() {
        if (webView != null) {
            webView.stopLoading();
        }
        if (!isDetached) {
            if (spinner.isShowing()) {
                spinner.dismiss();
            }
            super.dismiss();
        }
    }

    @Override
    public void onDetachedFromWindow() {
        isDetached = true;
        super.onDetachedFromWindow();
    }

    @Override
    public void onAttachedToWindow() {
        isDetached = false;
        super.onAttachedToWindow();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                sendCancelToListener();
            }
        });

        spinner = new ProgressDialog(getContext());
        spinner.requestWindowFeature(Window.FEATURE_NO_TITLE);
        spinner.setMessage(getContext().getString(R.string.com_twitter_loading));
        spinner.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                sendCancelToListener();
                WebDialog.this.dismiss();
            }
        });

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        contentFrameLayout = new FrameLayout(getContext());

        // First calculate how big the frame layout should be
        calculateSize();
        getWindow().setGravity(Gravity.CENTER);

        // resize the dialog if the soft keyboard comes up
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        /* Create the 'x' image, but don't add to the contentFrameLayout layout yet
         * at this point, we only need to know its drawable width and height
         * to place the webview
         */
        createCrossImage();

        /* Now we know 'x' drawable width and height,
         * layout the webview and add it the contentFrameLayout layout
         */
        int crossWidth = crossImageView.getDrawable().getIntrinsicWidth();

        setUpWebView(crossWidth / 2 + 1);

        /* Finally add the 'x' image to the contentFrameLayout layout and
        * add contentFrameLayout to the Dialog view
        */
        contentFrameLayout.addView(crossImageView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(contentFrameLayout);
    }

    private void calculateSize() {
        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        // always use the portrait dimensions to do the scaling calculations so we always get a portrait shaped
        // web dialog
        int width = metrics.widthPixels < metrics.heightPixels ? metrics.widthPixels : metrics.heightPixels;
        int height = metrics.widthPixels < metrics.heightPixels ? metrics.heightPixels : metrics.widthPixels;

        int dialogWidth = Math.min(
                getScaledSize(width, metrics.density, NO_PADDING_SCREEN_WIDTH, MAX_PADDING_SCREEN_WIDTH),
                metrics.widthPixels);
        int dialogHeight = Math.min(
                getScaledSize(height, metrics.density, NO_PADDING_SCREEN_HEIGHT, MAX_PADDING_SCREEN_HEIGHT),
                metrics.heightPixels);

        getWindow().setLayout(dialogWidth, dialogHeight);
    }

    /**
     * Returns a scaled size (either width or height) based on the parameters passed.
     *
     * @param screenSize     a pixel dimension of the screen (either width or height)
     * @param density        density of the screen
     * @param noPaddingSize  the size at which there's no padding for the dialog
     * @param maxPaddingSize the size at which to apply maximum padding for the dialog
     * @return a scaled size.
     */
    private int getScaledSize(int screenSize, float density, int noPaddingSize, int maxPaddingSize) {
        int scaledSize = (int) ((float) screenSize / density);
        double scaleFactor;
        if (scaledSize <= noPaddingSize) {
            scaleFactor = 1.0;
        } else if (scaledSize >= maxPaddingSize) {
            scaleFactor = MIN_SCALE_FACTOR;
        } else {
            // between the noPadding and maxPadding widths, we take a linear reduction to go from 100%
            // of screen size down to MIN_SCALE_FACTOR
            scaleFactor = MIN_SCALE_FACTOR +
                    ((double) (maxPaddingSize - scaledSize))
                            / ((double) (maxPaddingSize - noPaddingSize))
                            * (1.0 - MIN_SCALE_FACTOR);
        }
        return (int) (screenSize * scaleFactor);
    }

    private void createCrossImage() {
        crossImageView = new ImageView(getContext());
        // Dismiss the dialog when user click on the 'x'
        crossImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCancelToListener();
                WebDialog.this.dismiss();
            }
        });
        Drawable crossDrawable = getContext().getResources().getDrawable(R.drawable.com_twitter_close);
        crossImageView.setImageDrawable(crossDrawable);
        /* 'x' should not be visible while webview is loading
         * make it visible only after webview has fully loaded
         */
        crossImageView.setVisibility(View.INVISIBLE);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setUpWebView(int margin) {
        LinearLayout webViewContainer = new LinearLayout(getContext());
        webView = new WebView(getContext());
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setWebViewClient(new DialogWebViewClient());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl(token.getAuthenticationURL());
        webView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        webView.setVisibility(View.INVISIBLE);
        webView.getSettings().setSavePassword(false);
        webView.getSettings().setSaveFormData(false);

        webViewContainer.setPadding(margin, margin, margin, margin);
        webViewContainer.addView(webView);
        webViewContainer.setBackgroundColor(BACKGROUND_GRAY);
        contentFrameLayout.addView(webViewContainer);
    }

    private void sendCancelToListener() {
        sendErrorToListener(new TwitterOperationCanceledException());
    }

    private void sendErrorToListener(TwitterException e) {
        listener.onComplete(null, e);
    }

    private void sendSuccessToListener(String token, String verifier) {
        Bundle bundle = new Bundle();
        bundle.putString("oauth_token", token);
        bundle.putString("oauth_verifier", verifier);
        bundle.putSerializable("request_token", this.token);

        listener.onComplete(bundle, null);
    }

    private class DialogWebViewClient extends WebViewClient {
        @Override
        @SuppressWarnings("deprecation")
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.startsWith(WebDialog.REDIRECT_URI)) {
                // oauth://works-twitter?oauth_token=XhEJrHwUe6PsLREwy8BUWSVnSAYop68K&oauth_verifier=dWjUrCgPu2RTLwz0Raabh2UVK37SbFB2
                // oauth://works-twitter?denied=mxDBmW41xa71CaZcvybfalPLDzp859n0
                Log.d(BuildConfig.DEBUG_TAG, "url = " + url);

                Uri uri = Uri.parse(url);
                String token = uri.getQueryParameter("oauth_token");
                String verifier = uri.getQueryParameter("oauth_verifier");
                if (!TextUtils.isEmpty(token) && !TextUtils.isEmpty(verifier)) {
                    sendSuccessToListener(token, verifier);
                } else {
                    sendErrorToListener(new TwitterDialogException("denied", 0, url));
                }
//                Bundle values = Util.parseUrl(url);
//
//                String error = values.getString("error");
//                if (error == null) {
//                    error = values.getString("error_type");
//                }
//
//                String errorMessage = values.getString("error_msg");
//                if (errorMessage == null) {
//                    errorMessage = values.getString("error_description");
//                }
//                String errorCodeString = values.getString("error_code");
//                int errorCode = FacebookRequestError.INVALID_ERROR_CODE;
//                if (!Utility.isNullOrEmpty(errorCodeString)) {
//                    try {
//                        errorCode = Integer.parseInt(errorCodeString);
//                    } catch (NumberFormatException ex) {
//                        errorCode = FacebookRequestError.INVALID_ERROR_CODE;
//                    }
//                }
//
//                if (Utility.isNullOrEmpty(error) && Utility
//                        .isNullOrEmpty(errorMessage) && errorCode == FacebookRequestError.INVALID_ERROR_CODE) {
//                    sendSuccessToListener(values);
//                } else if (error != null && (error.equals("access_denied") ||
//                        error.equals("OAuthAccessDeniedException"))) {
//                    sendCancelToListener();
//                } else {
//                    FacebookRequestError requestError = new FacebookRequestError(errorCode, error, errorMessage);
//                    sendErrorToListener(new FacebookServiceException(requestError, errorMessage));
//                }

                WebDialog.this.dismiss();
                return true;
            }
//            else if (url.startsWith(WebDialog.CANCEL_URI)) {
//                sendCancelToListener();
//                WebDialog.this.dismiss();
//                return true;
//            }
            else if (url.contains("api.twitter.com")) {
                return false;
            }

            // launch non-dialog URLs in a full browser
            getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            return true;
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            sendErrorToListener(new TwitterDialogException(description, errorCode, failingUrl));
            WebDialog.this.dismiss();
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            if (DISABLE_SSL_CHECK_FOR_TESTING) {
                handler.proceed();
            } else {
                super.onReceivedSslError(view, handler, error);

                sendErrorToListener(new TwitterDialogException(null, ERROR_FAILED_SSL_HANDSHAKE, null));
                handler.cancel();
                WebDialog.this.dismiss();
            }
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            if (!isDetached) {
                spinner.show();
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            if (!isDetached) {
                spinner.dismiss();
            }
            /*
             * Once web view is fully loaded, set the contentFrameLayout background to be transparent
             * and make visible the 'x' image.
             */
            contentFrameLayout.setBackgroundColor(Color.TRANSPARENT);
            webView.setVisibility(View.VISIBLE);
            crossImageView.setVisibility(View.VISIBLE);
        }
    }
}

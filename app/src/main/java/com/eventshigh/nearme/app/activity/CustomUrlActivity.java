package com.eventshigh.nearme.app.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CustomUrlActivity extends BaseActivity {
    public static final String BLOG_HOST = "blog.eventshigh.com";
    public static final String EXTRA_TITLE_KEY = CustomUrlActivity.class.getName() + ".title";

    private WebView webView;
    // private static boolean isPayment;
    private ValueCallback<Uri> mUploadMessage;
    private ValueCallback<Uri[]> mUploadMessageArray;
    private final static int FILECHOOSER_RESULTCODE = 1;
    private String mCM;

    private static final String TAG = CustomUrlActivity.class.getSimpleName();


    public static void launchCustomUrl(Context context, Uri webUri, @Nullable String title) {
        Intent intent = new Intent(context,
                CustomUrlActivity.class);
        intent.setData(webUri);
        if (title != null) {
            intent.putExtra(CustomUrlActivity.EXTRA_TITLE_KEY, title);
        }

        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_url);
        webView = (WebView) findViewById(R.id.web_view);

        setupNewWebView(webView, this, true);

        // Set title.
        String title = getIntent().getStringExtra(EXTRA_TITLE_KEY);

        if (Utils.checkIfUnknown(getIntent().getDataString()) == null) {
            // nothing to do
            finish();
            return;
        }

        if (title != null) {
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(title);
            }
        }
        // If its notification action, report it accordingly.
        String action = getIntent().getAction();
        if (action != null && action.startsWith(NOTIFICATION_ACTION)) {
            reportActionToAnalytics("openNotification");
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, getIntent().getData()));
                finish();
                return;
            } catch (Exception e) {
                // do nothing
                Crashlytics.getInstance().core.logException(e);
            }
        }

        // Load the url in the web view.
        webView.loadUrl(getIntent().getDataString());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_custom_url, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_open_in_browser) {
            reportActionToAnalytics("openInBrowser");
            Intent intent = new Intent(Intent.ACTION_VIEW, getIntent().getData());
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                // No application to open url. ignore.
                Crashlytics.getInstance().core.logException(e);
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public View getViewForSnackbar() {
        return webView;
    }

    @Override
    public void onBackPressed() {
        // Handle web view back button before exiting the activity
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }


    @SuppressWarnings("ResultOfMethodCallIgnored")
    @SuppressLint("SetJavaScriptEnabled")
    public void setupNewWebView(WebView webView, final BaseActivity activity, boolean useProgressBar) {
        // Enable Javascript
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setSupportZoom(false);
        webSettings.setBuiltInZoomControls(false);
        WebView.setWebContentsDebuggingEnabled(true);

        // Enable Caching.
        File dir = activity.getCacheDir();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        webSettings.setAppCachePath(dir.getPath());
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setAppCacheEnabled(true);
        webSettings.setAllowFileAccess(true);

        // Enable rich content.
        webSettings.setAllowContentAccess(true);
        webSettings.setBlockNetworkImage(false);
        webSettings.setBlockNetworkLoads(false);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadsImagesAutomatically(true);

        // Setup a new web view client so we can listen in on events and also customize
        // web view behavior.
        webView.setWebViewClient(new EHWebViewClient(activity,
                useProgressBar ? activity.findViewById(R.id.top_progress_bar) : null));
        webView.setWebChromeClient(new WebChromeClient() {
            //The undocumented magic method override
            //Eclipse will swear at you if you try to put @Override here
            // For Android 3.0+
            public void openFileChooser(ValueCallback<Uri> uploadMsg) {
                mUploadMessage = uploadMsg;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("image/*");
                activity.startActivityForResult(Intent.createChooser(i, "File Chooser"), FILECHOOSER_RESULTCODE);
            }

            // For Android 3.0+, above method not supported in some android 3+ versions, in such case we use this
            public void openFileChooser(ValueCallback uploadMsg, String acceptType) {
                mUploadMessage = uploadMsg;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                activity.startActivityForResult(
                        Intent.createChooser(i, "File Browser"),
                        FILECHOOSER_RESULTCODE);
            }

            //For Android 4.1+
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
                mUploadMessage = uploadMsg;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("image/*");
                activity.startActivityForResult(Intent.createChooser(i, "File Chooser"), FILECHOOSER_RESULTCODE);
            }

            public boolean onShowFileChooser(
                    WebView webView, ValueCallback<Uri[]> filePathCallback,
                    WebChromeClient.FileChooserParams fileChooserParams) {
                /*if (mUploadMessageArray != null) {
                    mUploadMessageArray.onReceiveValue(null);
                }*/
                mUploadMessageArray = filePathCallback;
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(activity.getPackageManager()) != null) {
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                        takePictureIntent.putExtra("PhotoPath", mCM);
                    } catch (IOException ex) {
                        Log.e(TAG, "Image file creation failed", ex);
                    }
                    if (photoFile != null) {
                        mCM = "file:" + photoFile.getAbsolutePath();
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                    } else {
                        takePictureIntent = null;
                    }
                }
                Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                contentSelectionIntent.setType("image/*");
                Intent[] intentArray;
                if (takePictureIntent != null) {
                    intentArray = new Intent[]{takePictureIntent};
                } else {
                    intentArray = new Intent[0];
                }

                Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
                activity.startActivityForResult(chooserIntent, FILECHOOSER_RESULTCODE);
                return true;
            }

        });
        webSettings.setPluginState(PluginState.ON);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @SuppressLint("SetJavaScriptEnabled")
    public static void setupWebView(WebView webView, final BaseActivity activity, boolean useProgressBar) {
        // Enable Javascript
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setSupportZoom(false);
        webSettings.setBuiltInZoomControls(false);
        WebView.setWebContentsDebuggingEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);


        // Enable Caching.
        File dir = activity.getCacheDir();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        webSettings.setAppCachePath(dir.getPath());
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setAppCacheEnabled(true);
        webSettings.setAllowFileAccess(true);

        // Enable rich content.
        webSettings.setAllowContentAccess(true);
        webSettings.setBlockNetworkImage(false);
        webSettings.setBlockNetworkLoads(false);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadsImagesAutomatically(true);

        // Setup a new web view client so we can listen in on events and also customize
        // web view behavior.
        //  webView.setInitialScale(1);

        webView.setWebViewClient(new EHWebViewClient(activity,
                useProgressBar ? activity.findViewById(R.id.top_progress_bar) : null));
        webView.setWebChromeClient(new WebChromeClient());
        webSettings.setPluginState(PluginState.ON);
    }

    private File createImageFile() throws IOException {
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "img_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {
        if (Build.VERSION.SDK_INT >= 21) {
            Uri[] results = null;
            //Check if response is positive
            if (resultCode == Activity.RESULT_OK) {
                if (requestCode == FILECHOOSER_RESULTCODE) {
                    if (null == mUploadMessageArray) {
                        return;
                    }
                    if (intent == null) {
                        //Capture Photo if no image available
                        if (mCM != null) {
                            results = new Uri[]{Uri.parse(mCM)};
                        }
                    } else {
                        String dataString = intent.getDataString();
                        if (dataString != null) {
                            results = new Uri[]{Uri.parse(dataString)};
                        }
                    }
                }
            }
            mUploadMessageArray.onReceiveValue(results);
            mUploadMessage = null;


        } else {
            if (requestCode == FILECHOOSER_RESULTCODE) {
                if (null == mUploadMessage) return;
                Uri result = intent == null || resultCode != RESULT_OK ? null : intent.getData();
                mUploadMessage.onReceiveValue(result);
                mUploadMessage = null;
            }
        }
    }

    public static class EHWebViewClient extends WebViewClient {
        private BaseActivity activity;
        @Nullable
        private View progressBar;

        public EHWebViewClient(BaseActivity activity, @Nullable View progressBar) {
            this.activity = activity;
            this.progressBar = progressBar;

        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            if (progressBar != null) {
                activity.reportActionToAnalytics("startLoading");
                progressBar.setVisibility(View.VISIBLE);
            }

        }

        @Override
        public void onPageFinished(WebView view, String url) {
            if (progressBar != null) {
                activity.reportActionToAnalytics("finishLoading");
                progressBar.setVisibility(View.GONE);
            }
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // Returning true here means that when opening new links from this page will open
            // the default app that can handle the link.
            if (url.contains("www.eventshigh.com")) {
                activity.reportActionToAnalytics("openEhLink", url);
                Intent intent = new Intent(activity, LaunchActivity.class);
                intent.setAction(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                activity.startActivity(intent);
                return true;
            } else if (url.contains("twitter.com") || url.contains("facebook.com")) {
                activity.reportActionToAnalytics("openLink", url);
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                activity.startActivity(intent);
                return true;
            }
            return false;
        }
    }
}

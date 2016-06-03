package com.eventshigh.nearme.app.network;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.squareup.okhttp.OkHttpClient;

/**
 * Volley Helper which provide the simple methods to manage VolleyRequestQueue
 * and submit requests in queue for parallel processing.
 */
public class VolleyHelper {

    private static VolleyHelper instance;

    public static synchronized VolleyHelper getInstance(Context context) {
        if (instance == null) {
            instance = new VolleyHelper(context);
        }
        return instance;
    }

    private final RequestQueue requestQueue;

    private VolleyHelper(Context context) {
        requestQueue = Volley.newRequestQueue(context.getApplicationContext(), new OkHttpStack(new OkHttpClient()));
    }

    public static RequestQueue getRequestQueue(Context context){
        return getInstance(context).requestQueue;
    }

    public static <T> void addToRequestQueue(Context context, Request<T> req){
        getRequestQueue(context).add(req);
    }

    public static void log(BaseActivity activity, VolleyError volleyError) {
        String logTag = activity.getClass().getSimpleName();
        Throwable cause = volleyError.getCause();
        if (cause != null) {
            Crashlytics.getInstance().core.logException(cause);
            Log.w(logTag, "Volley Error: " + volleyError.getMessage(), cause);
            activity.reportActionToAnalytics("failedRequest", cause.getClass().getSimpleName());
        } else {
            Log.w(logTag, "Volley Error: " + volleyError.getMessage());
            activity.reportActionToAnalytics("failedRequest");
        }
    }

}

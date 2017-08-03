package com.eventshigh.nearme.app.network;

import android.content.Context;
import android.net.Uri;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * @author shubham
 * @since 10/6/16.
 */

public class ReportUserRequest extends StringRequest{


    private static String TAG = "ReportUserRequest";

    public static void submit(Context context,Uri uri,Priority priority,Response.Listener<String> listener, Response.ErrorListener errorListener) {

            //RequestFuture<String> future = RequestFuture.newFuture();
            ReportUserRequest request =
                    new ReportUserRequest(uri.toString(), priority, listener,errorListener);
            request.setRetryPolicy(new DefaultRetryPolicy(60_000,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            VolleyHelper.addToRequestQueue(context, request);

    }


    private final Priority priority;

    public ReportUserRequest(String url, Priority priority,Response.Listener<String>listener, Response.ErrorListener errorListener){
        super(Method.GET,url,listener,errorListener);
        this.priority = priority;
    }


    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        Map<String, String> mapHeader = new HashMap<String,String>();
        mapHeader.put("Content-Type","application/x-www-form-urlencoded");
        return mapHeader;
    }

    @Override
    public String getBodyContentType() {
        return "application/x-www-form-urlencoded";
    }

    @Override
    public Priority getPriority() {
        return priority;
    }

}

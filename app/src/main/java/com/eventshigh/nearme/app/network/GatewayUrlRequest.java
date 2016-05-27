package com.eventshigh.nearme.app.network;

/**
 * @author shubham
 * @since 26/5/16.
 */

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;


/**
 * @author shubham
 * @since 17/5/16.
 */


public class GatewayUrlRequest extends StringRequest{


    private Map<String,String> params;

    public static void submit(Context context,Map<String,String> params,
                              Priority priority, Response.Listener<String> listener, Response.ErrorListener errorListener) {

        try {
            URL url = new URL(EventsHighEndpoints.GATEWAY_URI_BASE +"gateway");
            Uri uri = Uri.parse(url.toURI().toString());

            GatewayUrlRequest request =
                    new GatewayUrlRequest(uri.toString(), priority, listener, errorListener);
            request.params = params;

            request.setRetryPolicy(new DefaultRetryPolicy(60_000,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

            VolleyHelper.addToRequestQueue(context, request);
        }catch(MalformedURLException e){
            Log.e("Payment Gateway Error", e.toString());
        }catch (URISyntaxException e){
            Log.e("Payment Gateway Error",e.toString());
        }
    }


    private final Priority priority;

    public GatewayUrlRequest(String url,  Priority priority,
                                    Response.Listener<String> listener, Response.ErrorListener errorListener) {
        super(Request.Method.POST,url,listener,errorListener);
        this.priority = priority;
    }

    @Override
    public Map<String, String> getParams() {
        return params;
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        Map<String, String> mapHeader = new HashMap<String,String>();
        mapHeader.put("Content-Type","multipart/form-data");
        return mapHeader;
    }

    @Override
    public String getBodyContentType() {
        return "multipart/form-data";
    }

    @Override
    public Priority getPriority() {
        return priority;
    }

    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response) {
        String urlLocation = response.headers.get("Location");
            return Response.success(urlLocation,
                    HttpHeaderParser.parseCacheHeaders(response));
    }
}

package com.eventshigh.nearme.app.network;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.Signer;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.GeneralSecurityException;

/**
 * @author shubham
 * @since 27/6/16.
 */
public class AddFacebookUserInfoRequest extends JsonObjectRequest {

    public static void submit(Context context,JSONObject data,
                              Priority priority, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {

        try {
            URL url = new URL(EventsHighEndpoints.API_URI_BASE +"mobileapp/record_user_fb_info");
            Uri uri = Uri.parse(url.toURI().toString());
            AddFacebookUserInfoRequest request = new AddFacebookUserInfoRequest(
                    Signer.sign(uri).toString(), data, priority, listener, errorListener);
            request.setRetryPolicy(new DefaultRetryPolicy(60_000,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            VolleyHelper.addToRequestQueue(context, request);
        }catch(MalformedURLException e){
            Log.e("Add FbUser Info Error", e.toString());
        }catch (URISyntaxException e){
            Log.e("Add FbUser Info Error",e.toString());
        }catch (UnsupportedEncodingException usee){
            Log.e("Add FbUser Info Error",usee.toString());
        }catch (GeneralSecurityException gse){
            Log.e("Add FbUser Info Error",gse.toString());
        }
    }

    private final Priority priority;

    public AddFacebookUserInfoRequest(String url, JSONObject jsonRequest, Priority priority,
                                    Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        super(Request.Method.POST, url, jsonRequest, listener, errorListener);
        this.priority = priority;
    }

    @Override
    public Priority getPriority() {
        return priority;
    }

}


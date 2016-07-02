package com.eventshigh.nearme.app.network;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;
import com.eventshigh.nearme.app.data.ProfileInfo;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.Signer;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.GeneralSecurityException;

public class FetchProfileRequest extends JsonRequest<ProfileInfo>{

    public static void submit(Context context,String mobileNo,
                              Priority priority, Response.Listener<ProfileInfo> listener, Response.ErrorListener errorListener) {

        try {
            URL url = new URL(EventsHighEndpoints.API_URI_BASE +"api/user_info_for_mobile_no/"+mobileNo);
            Uri uri = Uri.parse(url.toURI().toString());
            FetchProfileRequest request = new FetchProfileRequest(mobileNo,
                    Signer.sign(uri).toString(),priority,context, listener, errorListener);
            request.setRetryPolicy(new DefaultRetryPolicy(60_000,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            VolleyHelper.addToRequestQueue(context, request);
        }catch(MalformedURLException e){
            Log.e("Fetch fbInfo Error", e.toString());
        }catch (URISyntaxException e){
            Log.e("Fetch fbInfo Error",e.toString());
        }catch (UnsupportedEncodingException usee){
            Log.e("Add FbUser Info Error",usee.toString());
        }catch (GeneralSecurityException gse){
            Log.e("Add FbUser Info Error",gse.toString());
        }
    }

    private final Priority priority;
    private final Context context;
    private final String profileId;

    public FetchProfileRequest(String profileId, String url, Priority priority,Context context,
                                      Response.Listener<ProfileInfo> listener, Response.ErrorListener errorListener) {
        super(Method.GET, url,null,listener, errorListener);
        this.priority = priority;
        this.context = context;
        this.profileId = profileId;
    }

    @Override
    public Priority getPriority() {
        return priority;
    }

    @Override
    protected Response<ProfileInfo> parseNetworkResponse(NetworkResponse networkResponse){
        try {
            String jsonString = new String(networkResponse.data, "UTF-8");
            JSONObject eventsJson = new JSONObject(jsonString);
            ProfileInfo profileInfo = ProfileInfo.fromJson(eventsJson,context);
            profileInfo.setProfileId(profileId);
            return Response.success(profileInfo,HttpHeaderParser.parseCacheHeaders(networkResponse));
        }catch (UnsupportedEncodingException uee){
            Log.e("Fetch Profile Info",uee.toString());
        }catch (JSONException jse){
            Log.e("Fetch Profile Info",jse.toString());
        }
        return null;
    }
}


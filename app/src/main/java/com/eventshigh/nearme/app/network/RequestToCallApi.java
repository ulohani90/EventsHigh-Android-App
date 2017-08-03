package com.eventshigh.nearme.app.network;

import android.content.Context;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by umesh on 23/05/17.
 */

public class RequestToCallApi extends JsonObjectRequest{

    public static void submit(Context context, String phoneNo,String organizerPhoneNo,String eventId, String organizerAccountName, Priority priority,
                              Response.Listener<JSONObject> listener,
                              Response.ErrorListener errorListener) {

        String url = EventsHighEndpoints.API_ENDPOINT_REQUEST_CALLBACK;

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("user_phone",phoneNo);
            jsonObject.put("organizer_phone",organizerPhoneNo);
            jsonObject.put("eid",eventId);
            if(organizerAccountName!=null){
                jsonObject.put("organizer_account",organizerAccountName);
            }else{
                jsonObject.put("organizer_account","");
            }
            RequestToCallApi request = new RequestToCallApi(
                    context, url,jsonObject, priority, listener, errorListener);
            request.setRetryPolicy(new DefaultRetryPolicy(60_000,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            VolleyHelper.addToRequestQueue(context, request);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


    Context context;
    Priority priority;

    public RequestToCallApi(Context context, String url, JSONObject jsonRequest, Priority priority, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        super(Method.POST, url, jsonRequest, listener, errorListener);
        setShouldAllowStaleResponse(false);
        this.context = context;
        this.priority = priority;
    }

    @Override
    public Priority getPriority() {
        return priority;
    }

}

package com.eventshigh.nearme.app.user;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.eventshigh.nearme.app.network.RecordUserAction;
import com.eventshigh.nearme.app.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

public class UserActionHelper {
    public static final String JSON_KEY_ANDROID_ID = "android_id";
    public static final String JSON_KEY_TIMESTAMP = "timestamp";
    public static final String JSON_KEY_ACTION = "action";
    public static final String JSON_KEY_DATA = "data";
    private static final String JSON_KEY_EVENT_ID = "event_id";
    private static final String JSON_KEY_INTEREST = "interest";

    public enum EventAction {
        ADD_FAVORITE,
        REMOVE_FAVORITE,
        BOOK,
        CALL,
        SAVE,
        SHARE,
        VIEW_EVENT,
    }

    public enum FollowingAction {
        FOLLOW,
        UN_FOLLOW,
    }

    private final Context context;

    public UserActionHelper(Context context) {
        this.context = context;
    }

    public void recordShareAction(String eventId,String eventTitle, @Nullable String appName, @Nullable String link) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(JSON_KEY_EVENT_ID, eventId);
            if (appName != null) {
                jsonObject.put("app_name", appName);
            }
            if (link != null) {
                jsonObject.put("link", link);
            }
            if(eventTitle!=null){
                jsonObject.put("event_title",eventTitle);
            }
            recordServerUserAction(EventAction.SHARE.name().toLowerCase(),jsonObject);
            recordAction(EventAction.SHARE.name().toLowerCase(), jsonObject.toString());
        } catch (JSONException e) {

        }
    }

    public void recordAction(EventAction action, String eventId) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(JSON_KEY_EVENT_ID, eventId);
            recordServerUserAction(action.name().toString(),jsonObject);
            recordAction(action.name().toLowerCase(), jsonObject.toString());
        } catch (JSONException e) {

        }
    }

    public void recordAction(FollowingAction action, String interest) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(JSON_KEY_INTEREST, interest);
            recordServerUserAction(action.name().toString(),jsonObject);
            recordAction(action.name().toLowerCase(), jsonObject.toString());
        } catch (JSONException e) {

        }
    }

    private void recordAction(final String action, final String data) {
        //recordAction(action, data, System.currentTimeMillis());

    }

    public void recordServerUserAction(String action,JSONObject data){
        RecordUserAction.submit(context, action, data, Request.Priority.HIGH, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject, boolean b) {
                Log.i("Message Success","true");
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.i("Message failure","true");
            }
        });
    }

    /*private void recordAction(final String action, final String data, final long timestamp) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Bundle bundle = new Bundle();
                bundle.putString(JSON_KEY_ANDROID_ID, Utils.getAndroidId(context));
                bundle.putString(JSON_KEY_TIMESTAMP, Long.toString(timestamp));
                bundle.putString(JSON_KEY_ACTION, action);
                bundle.putString(JSON_KEY_DATA, data);
                GcmRegistration.sendUpstream(context, bundle);
            }
        });
        thread.start();
    }
*/

}

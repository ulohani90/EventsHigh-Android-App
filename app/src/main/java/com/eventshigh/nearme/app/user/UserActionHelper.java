package com.eventshigh.nearme.app.user;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.eventshigh.nearme.app.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

public class UserActionHelper {
    private static final String JSON_KEY_ANDROID_ID = "android_id";
    private static final String JSON_KEY_TIMESTAMP = "timestamp";
    private static final String JSON_KEY_ACTION = "action";
    private static final String JSON_KEY_DATA = "data";
    private static final String JSON_KEY_EVENT_ID = "event_id";
    private static final String JSON_KEY_INTEREST = "interest";

    public enum EventAction {
        ADD_FAVORITE,
        REMOVE_FAVORITE,
        BOOK,
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

    public void recordShareAction(String eventId, @Nullable String appName, @Nullable String postId) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(JSON_KEY_EVENT_ID, eventId);
            if (appName != null) {
                jsonObject.put("app_name", appName);
            }
            if (postId != null) {
                jsonObject.put("post_id", postId);
            }
            recordAction(EventAction.SHARE.name().toLowerCase(), jsonObject.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void recordAction(EventAction action, String eventId) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(JSON_KEY_EVENT_ID, eventId);
            recordAction(action.name().toLowerCase(), jsonObject.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void recordAction(FollowingAction action, String interest) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(JSON_KEY_INTEREST, interest);
            recordAction(action.name().toLowerCase(), jsonObject.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void recordAction(final String action, final String data) {
        recordAction(action, data, System.currentTimeMillis());
    }

    private void recordAction(final String action, final String data, final long timestamp) {
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
}

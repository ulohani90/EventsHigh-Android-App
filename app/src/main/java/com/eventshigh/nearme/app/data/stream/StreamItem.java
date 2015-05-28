package com.eventshigh.nearme.app.data.stream;

import android.support.annotation.Nullable;

import com.eventshigh.nearme.app.data.StreamDbHelper.StreamType;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Base class for StreamItem. This class also captures the base fields for each stream item like
 * timestamp, title, message.
 */
public abstract class StreamItem {
    public final long timestamp;
    public final String title;
    public final String message;
    @Nullable public final String imgUrl;

    protected StreamItem(long timestamp, String title, String message, @Nullable String imgUrl) {
        this.timestamp = timestamp;
        this.title = title;
        this.message = message;
        this.imgUrl = imgUrl;
    }

    protected StreamItem(long timestamp, JSONObject json) throws JSONException {
        this(timestamp, json.getString("title"), json.getString("message"),
                json.optString("img_url", null));
    }

    public abstract StreamType getStreamType();

    public JSONObject toJSON() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("title", title);
        jsonObject.put("message", message);
        if (imgUrl != null) {
            jsonObject.put("img__url", imgUrl);
        }

        return jsonObject;
    }

    public String toString() {
        try {
            return toJSON().toString();
        } catch (JSONException e) {
            return super.toString();
        }
    }
}

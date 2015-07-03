package com.eventshigh.nearme.app.data.stream;

import android.support.annotation.Nullable;

import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.data.StreamDbHelper.StreamType;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Base class for StreamItem. This class also captures the base fields for each stream item like
 * timestamp, title, message.
 */
public abstract class StreamItem {
    private static final String TITLE_KEY = "title";
    private static final String MESSAGE_KEY = "message";
    private static final String IMG_URL_KEY = "img_url";
    private static final String MOBILE_NO_KEY = "mobile_no";

    public final long timestamp;
    public final String title;
    public final String message;
    @Nullable public final String imgUrl;
    @Nullable public final String mobileNo;

    protected StreamItem(long timestamp, String title, String message, @Nullable String imgUrl,
            @Nullable String mobileNo) {
        this.timestamp = timestamp;
        this.title = title;
        this.message = message;
        this.imgUrl = imgUrl;
        this.mobileNo = mobileNo;
    }

    protected StreamItem(long timestamp, JSONObject json) throws JSONException {
        this(timestamp, json.getString(TITLE_KEY), json.getString(MESSAGE_KEY),
                json.optString(IMG_URL_KEY, null), json.optString(MOBILE_NO_KEY, null));
    }

    public abstract StreamType getStreamType();

    public abstract void launch(BaseContextActivity activity);

    public JSONObject toJSON() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(TITLE_KEY, title);
        jsonObject.put(MESSAGE_KEY, message);
        if (imgUrl != null) {
            jsonObject.put(IMG_URL_KEY, imgUrl);
        }
        if (mobileNo != null) {
            jsonObject.put(MOBILE_NO_KEY, mobileNo);
        }

        return jsonObject;
    }

    @Override
    public String toString() {
        try {
            return toJSON().toString();
        } catch (JSONException e) {
            return super.toString();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof  StreamItem)) {
            return false;
        }

        StreamItem other = (StreamItem) o;
        return other.title.equals(title) && other.message.equals(message);
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }
}

package com.eventshigh.nearme.app.data.stream;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;

import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.activity.FeedbackActivity;
import com.eventshigh.nearme.app.data.StreamDbHelper;
import com.eventshigh.nearme.app.data.StreamDbHelper.StreamType;
import com.eventshigh.nearme.app.utils.ZendeskUtils;

import org.json.JSONException;
import org.json.JSONObject;

public class TicketNotificationStreamItem extends StreamItem {
    public final String ticket;

    // Record event notification as stream item.
    public static void record(Context context, String title, String message,
                              @Nullable String imgUrl, String ticket) {
        TicketNotificationStreamItem streamItem = new TicketNotificationStreamItem(
                System.currentTimeMillis(), title, message, imgUrl, ticket);
        try {
            StreamDbHelper.addStreamToDB(context, streamItem);
        } catch (JSONException e) {
            // Ignore.
        }
    }

    public TicketNotificationStreamItem(long timestamp, String title, String message,
                                       @Nullable String imgUrl, String ticket) {
        super(timestamp, title, message, imgUrl, null);

        this.ticket = ticket;
    }

    public TicketNotificationStreamItem(long timestamp, JSONObject json) throws JSONException {
        super(timestamp, json);

        this.ticket = json.getString("ticket");
    }

    public StreamType getStreamType() {
        return StreamType.TICKET_NOTIFICATION;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject jsonObject = super.toJSON();
        jsonObject.put("ticket", ticket);
        return jsonObject;
    }

    public void launch(BaseContextActivity activity) {
        ZendeskUtils.initZendesk(activity);
        Intent intent = new Intent(activity, FeedbackActivity.class);
        intent.setAction(BaseActivity.NOTIFICATION_ACTION);
        activity.startActivity(intent);
    }
}

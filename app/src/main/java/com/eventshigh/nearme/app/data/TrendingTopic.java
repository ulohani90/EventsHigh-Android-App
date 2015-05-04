package com.eventshigh.nearme.app.data;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;

import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.activity.CustomUrlActivity;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;

import org.json.JSONObject;

/**
 * Defines a trending topic, which has title and image.
 */
public final class TrendingTopic {
    public final String tagName;
    public final String imgUrl;
    public final String action;

    public TrendingTopic(String tagName, String imgUrl, String action) {
        this.tagName = tagName;
        this.imgUrl = imgUrl;
        this.action = action;
    }

    public static @Nullable TrendingTopic parse(@Nullable JSONObject json) {
        if (json == null) {
            return null;
        }

        String name = json.optString("display_name");
        String imgUrl = json.optString("img_url");
        if (name == null || name.isEmpty() || imgUrl == null || imgUrl.isEmpty()) {
            return null;
        }

        return new TrendingTopic(name, imgUrl, json.optString("action"));
    }

    public void launch(BaseContextActivity activity) {
        if (action != null) {
            if (action.startsWith("view:")) {
                String[] actionParts = action.split(":", 3);
                if (actionParts.length == 3) {
                    CustomUrlActivity.launchCustomUrl(activity, Uri.parse(actionParts[2]),
                            actionParts[1]);
                    return;
                }
            }
            if (action.startsWith("event:")) {
                String[] actionParts = action.split(":", 2);
                if (actionParts.length == 2) {
                    activity.showEventDetails(EventsHighEndpoints.getEventDetailsURI(
                            City.BANGALORE, actionParts[1]));
                    return;
                }
            }
            if (action.startsWith("q:")) {
                String[] actionParts = action.split(":", 2);
                if (actionParts.length == 2) {
                    activity.showSearchView(actionParts[1]);
                    return;
                }
            }
            if (action.startsWith("target:")) {
                String[] actionParts = action.split(":", 2);
                try {
                    Class<?> cls = activity.getClassLoader().loadClass(actionParts[1]);
                    Intent intent = new Intent(activity, cls);
                    activity.startActivity(intent);
                    return;
                } catch (Exception e) {
                     // Ignore.
                }
            }
        }

        activity.showSearchView(tagName);
    }
}

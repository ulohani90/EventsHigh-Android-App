package com.eventshigh.nearme.app.data;

import android.support.annotation.Nullable;

import org.json.JSONObject;

/**
 * Defines a trending topic, which has title and image.
 */
public final class TrendingTopic {
    public final String tagName;
    public final String imgUrl;

    public TrendingTopic(String tagName, String imgUrl) {
        this.tagName = tagName;
        this.imgUrl = imgUrl;
    }

    public static @Nullable TrendingTopic parse(@Nullable JSONObject json) {
        if (json == null) {
            return null;
        }

        String name = json.optString("display_name");
        String imgUrl = json.optString("img_url");
        if (name == null || imgUrl == null) {
            return null;
        }

        return new TrendingTopic(name, imgUrl);
    }
}

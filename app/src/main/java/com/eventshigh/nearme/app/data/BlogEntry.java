package com.eventshigh.nearme.app.data;

import android.os.Parcel;
import android.os.Parcelable;

import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BlogEntry implements Parcelable {
    public final String title;
    public final String thumbnail;
    public final Date pubDate;
    public final String url;
    public String description;

    public BlogEntry(String title, String thumbnail, Date pubDate, String url, String description) {
        this.title = title;
        this.thumbnail = thumbnail;
        this.pubDate = pubDate;
        this.url = url;
        this.description = description;
    }

    /**********************************
     * Parcel management methods.
     *********************************/
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(thumbnail);
        dest.writeLong(pubDate.getTime());
        dest.writeString(url);
        dest.writeString(description);
    }

    // This is used to regenerate your object. All Parcelables must have
    // a CREATOR that implements these two methods
    public static final Parcelable.Creator<BlogEntry> CREATOR =
            new Parcelable.Creator<BlogEntry>() {
                public BlogEntry createFromParcel(Parcel in) {
                    return new BlogEntry(in.readString(),
                            in.readString(),
                            new Date(in.readLong()),
                            in.readString(),
                            in.readString()
                    );
                }

                public BlogEntry[] newArray(int size) {
                    return new BlogEntry[size];
                }
            };

    /**********************************
     * Helper static methods, used for JSON parsing
     *********************************/
    public static BlogEntry parse(JSONObject blogEntryJson) throws JSONException, ParseException {
        return new BlogEntry(blogEntryJson.optString("title"),
                blogEntryJson.optString("featured_image_url"),
                DateTimeUtils.parseBlogDate(blogEntryJson.optString("updated_at")),
                EventsHighEndpoints.WEB_URI_BASE + "post/" + blogEntryJson.optInt("id"),
                blogEntryJson.optString("description")
        );
    }

    public static List<BlogEntry> parse(JSONArray blogEntriesJson) {
        List<BlogEntry> blogEntries = new ArrayList<>(blogEntriesJson.length());
        for (int i = 0; i < blogEntriesJson.length(); i++) {
            try {
                blogEntries.add(parse(blogEntriesJson.getJSONObject(i)));
            } catch (JSONException | ParseException e) {
                Crashlytics.getInstance().core.logException(e);
            }
        }
        return blogEntries;
    }
}

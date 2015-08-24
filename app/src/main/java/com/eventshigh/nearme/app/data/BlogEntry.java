package com.eventshigh.nearme.app.data;

import android.os.Parcel;
import android.os.Parcelable;

import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.utils.DateTimeUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BlogEntry implements Parcelable {
    public final String title;
    public final String snippet;
    public final String contents;
    public final String imgUrl;
    public final String[] tags;
    public final String url;
    public final Date pubDate;

    public BlogEntry(String title, String snippet, String contents, String imgUrl, String[] tags,
                     String url, Date pubDate) {
        this.title = title;
        this.snippet = snippet;
        this.contents = contents;
        this.imgUrl = imgUrl;
        this.tags = tags;
        this.url = url;
        this.pubDate = pubDate;
    }

    /**********************************
     Parcel management methods.
     *********************************/
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(snippet);
        dest.writeString(contents);
        dest.writeString(imgUrl);
        dest.writeStringArray(tags);
        dest.writeString(url);
        dest.writeLong(pubDate.getTime());
    }

    // This is used to regenerate your object. All Parcelables must have
    // a CREATOR that implements these two methods
    public static final Parcelable.Creator<BlogEntry> CREATOR =
            new Parcelable.Creator<BlogEntry>() {
                public BlogEntry createFromParcel(Parcel in) {
                    return new BlogEntry(in.readString(),
                            in.readString(),
                            in.readString(),
                            in.readString(),
                            in.createStringArray(),
                            in.readString(),
                            new Date(in.readLong())
                    );
                }

                public BlogEntry[] newArray(int size) {
                    return new BlogEntry[size];
                }
            };

    /**********************************
     Helper static methods, used for JSON parsing
     *********************************/
    public static BlogEntry parse(JSONObject blogEntryJson) throws JSONException, ParseException {
        return new BlogEntry(blogEntryJson.getString("title"),
                blogEntryJson.getString("description"),
                blogEntryJson.getString("description_full"),
                blogEntryJson.getString("imgUrl"),
                blogEntryJson.getString("tags").split(" "),
                blogEntryJson.getString("url"),
                DateTimeUtils.parseBlogDate(blogEntryJson.getString("pub_date"))
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

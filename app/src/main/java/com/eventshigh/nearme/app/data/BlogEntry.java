package com.eventshigh.nearme.app.data;

import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.utils.DateTimeUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BlogEntry {
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

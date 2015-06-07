package com.eventshigh.nearme.app.network;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class TagsSuggestRequest extends JsonRequest<List<String>> {
    public TagsSuggestRequest(Listener<List<String>> listener, ErrorListener errorListener) {
        super(Method.GET, EventsHighEndpoints.getTagSuggestURI(), null, listener, errorListener);
    }

    @Override
    protected Response<List<String>> parseNetworkResponse(NetworkResponse response) {
        try {
            String jsonString = new String(response.data, "UTF-8");
            JSONArray tagsJSONArray = new JSONArray(jsonString);
            List<String> suggestTags = new ArrayList<>(tagsJSONArray.length());
            for (int i = 0; i < tagsJSONArray.length(); i++) {
                try {
                    suggestTags.add(tagsJSONArray.getJSONObject(i).getString("tag"));
                } catch (JSONException e) {
                    // Ignore.
                    Crashlytics.getInstance().core.logException(e);
                }
            }
            return Response.success(suggestTags, HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException | JSONException e) {
            Crashlytics.getInstance().core.logException(e);
            return Response.error(new ParseError(e));
        }
    }
}

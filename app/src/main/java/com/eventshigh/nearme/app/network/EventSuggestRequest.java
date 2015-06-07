package com.eventshigh.nearme.app.network;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.network.EventSuggestRequest.SuggestEvent;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Request to fetch titles for all upcoming events so that it can be used for
 * suggestions.
 */
public class EventSuggestRequest extends JsonRequest<List<SuggestEvent>> {
    public static class SuggestEvent {
        public final String id;
        public final String city;
        public final String name;

        public SuggestEvent(String id, String city, String name) {
            this.id = id;
            this.city = city;
            this.name = name;
        }

        public static SuggestEvent parse(JSONObject jsonObject) throws JSONException {
            return new SuggestEvent(jsonObject.getString("id"), jsonObject.getString("city"),
                    jsonObject.getString("name"));
        }
    }

    public EventSuggestRequest(City city, Listener<List<SuggestEvent>> listener,
                               ErrorListener errorListener) {
        super(Method.GET, EventsHighEndpoints.getEventSuggestURI(city), null, listener,
                errorListener);
    }

    @Override
    protected Response<List<SuggestEvent>> parseNetworkResponse(NetworkResponse response) {
        try {
            String jsonString = new String(response.data, "UTF-8");
            JSONArray suggestJSONArray = new JSONArray(jsonString);
            List<SuggestEvent> suggestEvents = new ArrayList<>(suggestJSONArray.length());
            for (int i = 0; i < suggestJSONArray.length(); i++) {
                try {
                    suggestEvents.add(SuggestEvent.parse(suggestJSONArray.getJSONObject(i)));
                } catch (JSONException e) {
                    // Ignore.
                    Crashlytics.getInstance().core.logException(e);
                }
            }
            return Response.success(suggestEvents, HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException | JSONException e) {
            Crashlytics.getInstance().core.logException(e);
            return Response.error(new ParseError(e));
        }
    }
}

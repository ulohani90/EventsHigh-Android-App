package com.eventshigh.nearme.app.network;

import android.content.Context;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by umesh on 07/04/16.
 */
public class MobileUserEventsRequest extends JsonRequest<List<MyEventsRequest.TopicEvents>> {


    Account account;

    /**
     * Helper method to submit a volley request to fetch Events information.
     *
     * @param listener      callback on success.
     * @param errorListener callback on failures.
     */
    public static void submit(Context context, EventsContext eventsContext,
                              Priority priority, Object tag, boolean shouldBypassCache, boolean includeWithoutLocation,
                              Response.Listener<List<MyEventsRequest.TopicEvents>> listener, Response.ErrorListener errorListener) {


        String url;
        try {
            url = EventsHighEndpoints.getApiEndpointEventsMobileUser(Utils.getAndroidId(context));
            if (shouldBypassCache) {
                url = url + "&cmode=override";
            }
        } catch (IllegalArgumentException e) {
            errorListener.onErrorResponse(new VolleyError("Invalid Query", e));
            return;
        }

        MobileUserEventsRequest request = new MobileUserEventsRequest(context, eventsContext, url, priority,
                shouldBypassCache, includeWithoutLocation, listener, errorListener);
        request.setTag(tag);
        VolleyHelper.addToRequestQueue(context, request);
    }


    private final EventsContext eventsContext;
    private final Priority priority;
    private final boolean includeWithoutLocation;
    private Context mContext;

    public MobileUserEventsRequest(Context context, EventsContext eventsContext, String url, Priority priority,
                                   boolean shouldBypassCache, boolean includeWithoutLocation,
                                   Response.Listener<List<MyEventsRequest.TopicEvents>> listener, Response.ErrorListener errorListener) {
        super(Method.GET, url, null, listener, errorListener);
        setShouldBypassCache(shouldBypassCache);
        setShouldAllowStaleResponse(true);

        this.eventsContext = eventsContext;
        this.priority = priority;
        this.includeWithoutLocation = includeWithoutLocation;
        this.mContext = context;
        account = new Account(context);
    }


    @Override
    public Priority getPriority() {
        return priority;
    }

    @Override
    protected Response<List<MyEventsRequest.TopicEvents>> parseNetworkResponse(NetworkResponse networkResponse) {
        try {
            List<MyEventsRequest.TopicEvents> events = new ArrayList<>();
            String jsonString = new String(networkResponse.data, "UTF-8");
            JSONObject eventsJson = new JSONObject(jsonString);
            if (eventsJson.has("events")) {
                JSONArray eventsJsonArray = eventsJson.getJSONArray("events");

                for (int i = 0; i < eventsJsonArray.length(); i++) {
                    List<Event> topicEvents = Event.fromJSON(eventsJsonArray.getJSONObject(i).getJSONArray("topic_events"), includeWithoutLocation, null);
                    account.setIsFollowing(eventsJsonArray.getJSONObject(i).getString("topic"), true);
                    MyEventsRequest.TopicEvents eventData = new MyEventsRequest.TopicEvents(eventsJsonArray.getJSONObject(i).getString("topic"), topicEvents, eventsJsonArray.getJSONObject(i).getInt("event_count"));
                    events.add(eventData);
                }
            }
            return Response.success(events, HttpHeaderParser.parseCacheHeaders(networkResponse));

        } catch (UnsupportedEncodingException | JSONException e) {
            Crashlytics.getInstance().core.logException(e);
            return Response.error(new ParseError(e));
        }
    }
}

package com.eventshigh.nearme.app.network;

import android.content.Context;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.data.SponsoredEventObj;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * Created by umesh on 10/11/17.
 */

public class GetSponsoredEventsRequest extends JsonRequest<List<SponsoredEventObj>> {

    public static void submit(Context context, String city, Priority priority,
                              Object tag, boolean shouldBypassCache, Response.Listener<List<SponsoredEventObj>> listener,
                              Response.ErrorListener errorListener) {


        String url = String.format(EventsHighEndpoints.API_ENDPOINT_GET_SPONSORED_EVENTS, city);
        GetSponsoredEventsRequest request = new GetSponsoredEventsRequest(
                context, url, shouldBypassCache, priority, listener, errorListener);
        request.setTag(tag);
        VolleyHelper.addToRequestQueue(context, request);
    }

    private final Context context;

    private final Priority priority;

    public GetSponsoredEventsRequest(Context context, String url,
                                     boolean shouldBypassCache, Priority priority,
                                     Response.Listener<List<SponsoredEventObj>> listener, Response.ErrorListener errorListener) {
        super(Method.GET, url, null, listener, errorListener);
        setShouldBypassCache(shouldBypassCache);
        setShouldAllowStaleResponse(true);

        this.context = context;
        this.priority = priority;
    }

    @Override
    public Priority getPriority() {
        return priority;
    }


    @Override
    protected Response<List<SponsoredEventObj>> parseNetworkResponse(NetworkResponse response) {
        try {
            String jsonString = new String(response.data, "UTF-8");
            JSONObject sponsoredJson = new JSONObject(jsonString);
            return Response.success(SponsoredEventObj.parseJsonArray(sponsoredJson.getJSONArray("sponsored_events")), HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException | JSONException e) {
            Crashlytics.getInstance().core.logException(e);
            return Response.error(new ParseError(e));
        }

    }
}

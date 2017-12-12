package com.eventshigh.nearme.app.network;

import android.content.Context;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.data.DealsObject;
import com.eventshigh.nearme.app.data.SponsoredEventObj;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * Created by umesh on 12/12/17.
 */

public class GetHotDealsRequest extends JsonRequest<DealsObject> {


    public static void submit(Context context, String city, Priority priority,
                              Object tag, boolean shouldBypassCache, Response.Listener<DealsObject> listener,
                              Response.ErrorListener errorListener) {

        String url = String.format(EventsHighEndpoints.API_ENDPOINT_GET_HOT_DEALS, city);
        GetHotDealsRequest request = new GetHotDealsRequest(
                context, url, shouldBypassCache, priority, listener, errorListener);
        request.setTag(tag);
        VolleyHelper.addToRequestQueue(context, request);
    }

    private final Context context;

    private final Priority priority;

    public GetHotDealsRequest(Context context, String url,
                              boolean shouldBypassCache, Priority priority,
                              Response.Listener<DealsObject> listener, Response.ErrorListener errorListener) {
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
    protected Response<DealsObject> parseNetworkResponse(NetworkResponse response) {
        try {
            String jsonString = new String(response.data, "UTF-8");
            JSONObject dealsJson = new JSONObject(jsonString);
            return Response.success(DealsObject.parseJson(dealsJson), HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException | JSONException e) {
            Crashlytics.getInstance().core.logException(e);
            return Response.error(new ParseError(e));
        }

    }
}
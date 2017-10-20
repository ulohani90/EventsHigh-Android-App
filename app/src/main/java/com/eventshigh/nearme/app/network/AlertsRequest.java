package com.eventshigh.nearme.app.network;

import android.content.Context;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.stream.CustomUrlNotificationStream;
import com.eventshigh.nearme.app.data.stream.EventNotificationStreamItem;
import com.eventshigh.nearme.app.data.stream.QueryNotificationStreamItem;
import com.eventshigh.nearme.app.data.stream.TicketNotificationStreamItem;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

/**
 * Created by umesh on 22/04/16.
 */
public class AlertsRequest extends JsonRequest<Boolean> {
    public static void submit(Context context, City city, Request.Priority priority,
                              Object tag, boolean shouldBypassCache, Response.Listener<Boolean> listener,
                              Response.ErrorListener errorListener) {

        String url = EventsHighEndpoints.getApiEndPointForAlerts(city.name());
        AlertsRequest request = new AlertsRequest(
                context, url, shouldBypassCache, priority, listener, errorListener);
        request.setTag(tag);
        VolleyHelper.addToRequestQueue(context, request);
    }


    Context context;
    Request.Priority priority;

    public AlertsRequest(Context context, String url, boolean shouldBypassCache, Request.Priority priority, Response.Listener<Boolean> listener, Response.ErrorListener errorListener) {
        super(Request.Method.GET, url, null, listener, errorListener);
        setShouldBypassCache(shouldBypassCache);
        setShouldAllowStaleResponse(false);
        this.context = context;
        this.priority = priority;
    }

    @Override
    public Request.Priority getPriority() {
        return priority;
    }

    @Override
    protected Response<Boolean> parseNetworkResponse(NetworkResponse response) {
        try {
            String jsonString = new String(response.data, "UTF-8");
            JSONObject jsonObject = new JSONObject(jsonString);
            boolean isDataReceived = false;
            JSONArray alertsArray = jsonObject.getJSONArray("alerts");
            City city = new Account(context).getLastCity();
            for (int i = 0; i < alertsArray.length(); i++) {
                JSONObject obj = alertsArray.getJSONObject(i);
                String title = obj.getString("title");
                String message = obj.getString("desc");

                String imgUrl = obj.getString("img_url");
                String query = null;
                if (obj.has("query"))
                    query = Utils.checkIfUnknown(obj.getString("query"));

                String eid = null;
                if (obj.has("eid"))
                    eid = Utils.checkIfUnknown(obj.getString("eid"));

                String ticket = null;
                if (obj.has("ticket"))
                    ticket = Utils.checkIfUnknown(obj.getString("ticket"));

                String targetUrl = null;
                if (obj.has("target_url")) {
                    targetUrl = Utils.checkIfUnknown(obj.getString("target_url"));
                }
                if (eid != null) {
                    EventNotificationStreamItem.record(context, title, message, imgUrl, null, eid, city.name());
                } else if (query != null) {
                    QueryNotificationStreamItem.record(context, title, message, imgUrl, null, query);
                } else if (ticket != null) {
                    TicketNotificationStreamItem.record(context, title, message, imgUrl, ticket);
                } else if (targetUrl != null) {
                    CustomUrlNotificationStream.record(context,title,message,imgUrl,null,targetUrl);
                }
                isDataReceived = true;
            }
            return Response.success(isDataReceived,
                    HttpHeaderParser.parseCacheHeaders(response));
        } catch (JSONException | UnsupportedEncodingException e) {
            Crashlytics.getInstance().core.logException(e);
            return Response.error(new ParseError(e));
        }

    }
}

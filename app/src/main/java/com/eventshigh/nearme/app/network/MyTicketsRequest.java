package com.eventshigh.nearme.app.network;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.MyTicketObject;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.Signer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author shubham
 * @since 15/6/16.
 */
public class MyTicketsRequest extends JsonRequest<List<MyTicketObject>> {

    private boolean reviewEvent = false;

    public static void submit(Context context, Priority priority,
                              Object tag, boolean shouldBypassCache, Response.Listener<List<MyTicketObject>> listener,
                              Response.ErrorListener errorListener, boolean reviewEvent) {

        String url = EventsHighEndpoints.API_URI_BASE + "mobileapp/my_tickets_for_email?email=";
        url += (new Account(context)).getUserInfo().email;
        if (shouldBypassCache) {
            url += "&cmode=bypass";
        }
        Uri uri = Uri.parse(url);
        try {
            uri = Signer.sign(uri);
        } catch (GeneralSecurityException gse) {
            Log.e("MyTicketsRequest", gse.toString());
        } catch (UnsupportedEncodingException uee) {
            Log.e("MyTicketsRequest", uee.toString());
        }
        MyTicketsRequest request = new MyTicketsRequest(
                context, uri, shouldBypassCache, priority, listener, errorListener, reviewEvent);
        request.setTag(tag);
        VolleyHelper.addToRequestQueue(context, request);
    }


    Context context;
    Priority priority;

    public MyTicketsRequest(Context context, Uri uri, boolean shouldBypassCache, Priority priority, Response.Listener<List<MyTicketObject>> listener, Response.ErrorListener errorListener, boolean reviewEvent) {
        super(Method.GET, uri.toString(), null, listener, errorListener);
        setShouldBypassCache(shouldBypassCache);
        setShouldAllowStaleResponse(false);
        this.reviewEvent = reviewEvent;
        this.context = context;
        this.priority = priority;
    }

    @Override
    public Priority getPriority() {
        return priority;
    }


    @Override
    protected Response<List<MyTicketObject>> parseNetworkResponse(NetworkResponse response) {
        try {
            String jsonString = new String(response.data,
                    HttpHeaderParser.parseCharset(response.headers));
            JSONObject eventJson = new JSONObject(jsonString);

            JSONArray itemsJson = new JSONArray();
            if (eventJson.has("items"))
                itemsJson = eventJson.getJSONArray("items");

            boolean hasSubmittedReview;

            hasSubmittedReview = eventJson.getBoolean("hasSubmittedReview");

            if (reviewEvent && hasSubmittedReview) {
                return Response.success(null, HttpHeaderParser.parseCacheHeaders(response));
            } else {

                return Response.success(MyTicketObject.fromJSON(itemsJson), HttpHeaderParser.parseCacheHeaders(response));
            }
        } catch (UnsupportedEncodingException | JSONException e) {
            Log.i("Exception", e.getMessage());
            Crashlytics.getInstance().core.logException(e);
            return Response.error(new ParseError(e));
        }
    }

}

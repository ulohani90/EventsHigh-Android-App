package com.eventshigh.nearme.app.network;

import android.content.Context;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.data.CityBannerObject;

import com.eventshigh.nearme.app.utils.EventsHighEndpoints;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.List;

public class CityBannerRequest extends JsonRequest<List<CityBannerObject>> {


    public static void submit(Context context, String city, Priority priority,
                              Object tag, boolean shouldBypassCache, Response.Listener<List<CityBannerObject>> listener,
                              Response.ErrorListener errorListener) {

        String url = String.format(EventsHighEndpoints.API_ENDPOINT_GET_CITY_BANNER, city.toLowerCase());
        CityBannerRequest request = new CityBannerRequest(
                context, url, shouldBypassCache, priority, listener, errorListener);
        request.setTag(tag);
        VolleyHelper.addToRequestQueue(context, request);
    }

    private final Context context;

    private final Priority priority;

    public CityBannerRequest(Context context, String url,
                             boolean shouldBypassCache, Priority priority,
                             Response.Listener<List<CityBannerObject>> listener, Response.ErrorListener errorListener) {
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
    protected Response<List<CityBannerObject>> parseNetworkResponse(NetworkResponse response) {
        try {
            String jsonString = new String(response.data, "UTF-8");
            JSONObject bannersObj = new JSONObject(jsonString);
            JSONArray bannersArray = bannersObj.getJSONArray("banners");
            return Response.success(CityBannerObject.parseFromJsonArray(bannersArray), HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException | JSONException e) {
            Crashlytics.getInstance().core.logException(e);
            return Response.error(new ParseError(e));
        }

    }
}

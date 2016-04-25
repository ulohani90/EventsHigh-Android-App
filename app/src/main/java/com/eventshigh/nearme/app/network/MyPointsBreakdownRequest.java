package com.eventshigh.nearme.app.network;

import android.content.Context;
import android.net.Uri;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.broadcast.UpdateAccountInfoService;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.stream.OfferObject;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.Signer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by umesh on 22/04/16.
 */
public class MyPointsBreakdownRequest extends JsonRequest<MyPointsBreakdownRequest.PointBreakdownBaseObj> {


    public static class PointBreakdownBaseObj{
        public final long totalPoints ;

        public final List<MyPointsBreakdownRequest.PointBreakDown> points;

        public PointBreakdownBaseObj(long totalPoints,List<PointBreakDown> points){
                this.totalPoints = totalPoints;
                this.points = points;
        }

    }

    public static class PointBreakDown {
        public final int points;
        public final String type;
        public final String message;
        public final String action;
        public final long addedOn;

        public PointBreakDown(int points, String type, String message, String action, long addedOn) {
            this.points = points;
            this.type = type;
            this.message = message;
            this.action = action;
            this.addedOn = addedOn;
        }

        public static PointBreakDown parse(JSONObject jsonObj) throws JSONException {
            int points = jsonObj.getInt("points");
            String type = jsonObj.getString("type");
            String message = jsonObj.getString("message");
            String action = jsonObj.getString("action");
            long addedOn = DateTimeUtils.parseOfferTime(jsonObj.getString("added_on"));
            return new PointBreakDown(points,type,message,action,addedOn);

        }
    }

    public static void submit(Context context,  Priority priority,
                              Object tag, boolean shouldBypassCache, Response.Listener<PointBreakdownBaseObj> listener,
                              Response.ErrorListener errorListener) {
        Uri uri = UpdateAccountInfoService.getBaseUri(context, "getWalletPointsBreakdown").build();
        try {
            String url = Signer.sign(uri).toString();

            MyPointsBreakdownRequest request = new MyPointsBreakdownRequest(
                    context, url, shouldBypassCache, priority, listener, errorListener);
            request.setTag(tag);
            VolleyHelper.addToRequestQueue(context, request);
        }catch(UnsupportedEncodingException| GeneralSecurityException e){
            Crashlytics.getInstance().core.logException(e);
        }
    }


    Context context;
    Priority priority;

    public MyPointsBreakdownRequest(Context context, String  url,boolean shouldBypassCache,Priority priority, Response.Listener<PointBreakdownBaseObj> listener, Response.ErrorListener errorListener) throws GeneralSecurityException, UnsupportedEncodingException {
        super(Method.GET, url, null, listener, errorListener);
        setShouldBypassCache(shouldBypassCache);
        setShouldAllowStaleResponse(false);
        this.context = context;
        this.priority = priority;
    }

    @Override
    public Priority getPriority() {
        return priority;
    }


    @Override
    protected Response<PointBreakdownBaseObj> parseNetworkResponse(NetworkResponse response) {
        try {
            PointBreakdownBaseObj obj;
            String jsonString = new String(response.data, "UTF-8");
            JSONObject jsonObject = new JSONObject(jsonString);
            List<PointBreakDown> pointBreakdown = new ArrayList<>();
            JSONArray breakDownArray = jsonObject.getJSONArray("transactions");
            if(breakDownArray!=null){
                for(int i =0;i<breakDownArray.length();i++){
                    pointBreakdown.add(PointBreakDown.parse(breakDownArray.getJSONObject(i)));
                }
            }
            long totalPoints = jsonObject.getLong("points");
            obj = new PointBreakdownBaseObj(totalPoints,pointBreakdown);
            return Response.success(obj,
                    HttpHeaderParser.parseCacheHeaders(response));
        }catch(UnsupportedEncodingException | JSONException e){
            Crashlytics.getInstance().core.logException(e);
            return Response.error(new ParseError(e));
        }

    }


}

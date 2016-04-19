package com.eventshigh.nearme.app.network;

import android.content.Context;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.data.stream.OfferObject;
import com.eventshigh.nearme.app.data.stream.PointsObject;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.text.ParseException;
import java.util.ArrayList;

/**
 * Created by umesh on 16/04/16.
 */
public class OffersRequest extends JsonRequest<OffersRequest.OffersPointsObject>{




    public static class OffersPointsObject{
        public final ArrayList<OfferObject> offers;

        public final ArrayList<PointsObject> points;

        public OffersPointsObject( ArrayList<OfferObject> offers,ArrayList<PointsObject> points){
            this.offers = offers;
            this.points = points;
        }
    }

    public static void submit(Context context, City city, Priority priority,
                              Object tag, boolean shouldBypassCache, Response.Listener<OffersPointsObject> listener,
                              Response.ErrorListener errorListener) {

        String url = EventsHighEndpoints.getApiEndPointForOffers(city.name());
        OffersRequest request = new OffersRequest(
                context, url,  shouldBypassCache, priority, listener, errorListener);
        request.setTag(tag);
        VolleyHelper.addToRequestQueue(context, request);
    }


    Context context;
    Priority priority;

    public OffersRequest(Context context, String url,boolean shouldBypassCache,Priority priority, Response.Listener<OffersPointsObject> listener, Response.ErrorListener errorListener) {
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
    protected Response<OffersPointsObject> parseNetworkResponse(NetworkResponse response) {

        try {


            String jsonString = new String(response.data, "UTF-8");
            JSONObject jsonObject = new JSONObject(jsonString);
            ArrayList<OfferObject> offers = new ArrayList<>();
            JSONArray offersArray = jsonObject.getJSONArray("offers");
            if(offersArray!=null){
                for(int i=0;i<offersArray.length();i++){
                    offers.add(OfferObject.parse(offersArray.getJSONObject(i)));
                }
            }
            removeInvalidOffers(offers);
            ArrayList<PointsObject> points = new ArrayList<>();
            JSONArray pointsArray = jsonObject.getJSONArray("points");
            if(pointsArray!=null){
                for(int i=0;i<pointsArray.length();i++){
                    points.add(PointsObject.parse(pointsArray.getJSONObject(i)));
                }
            }
            return Response.success(new OffersPointsObject(offers,points),
                    HttpHeaderParser.parseCacheHeaders(response));

        } catch (JSONException | ParseException |UnsupportedEncodingException e) {
            Crashlytics.getInstance().core.logException(e);
            return Response.error(new ParseError(e));
        }


    }

    public void removeInvalidOffers(ArrayList<OfferObject> offers){
        for(int i=0;i<offers.size();i++){
            if(offers.get(i).validTill!=0 && offers.get(i).validTill-System.currentTimeMillis()<=0){
                offers.remove(i);
            }
        }
    }

}

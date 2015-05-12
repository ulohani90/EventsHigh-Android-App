package com.eventshigh.nearme.app.network;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;
import com.eventshigh.nearme.app.data.Offer;
import com.eventshigh.nearme.app.security.Signer;
import com.eventshigh.nearme.app.task.ReportTimingTask;
import com.eventshigh.nearme.app.user.AccountStateReporter;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.List;

/**
 * Fetches the offers for current user.
 */
public class OffersRequest extends JsonRequest<List<Offer>> {
    private final Context context;
    private final Uri offerUri;
    private final Priority priority;

    /**
     * Helper method to submit a volley request to fetch a single offer.
     *
     * @param context an application eventsContext to initiate the volley.
     * @param listener callback on success.
     */
    public static void submit(Context context, Priority priority, Object tag,
                              final Listener<Offer> listener) {
        submit(context, priority, tag, false, new Listener<List<Offer>>() {
            @Override
            public void onResponse(List<Offer> offers, boolean isIntermediate) {
                for (final Offer offer : offers) {
                    if (offer.isGoodToShow()) {
                        listener.onResponse(offer, isIntermediate);
                        break;
                    }
                }
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.e(OffersRequest.class.getSimpleName(),
                        "failed to fetch offers: " + volleyError.getMessage(), volleyError.getCause());
            }
        });
    }

    /**
     * Helper method to submit a volley request to fetch all offers information.
     *
     * @param context an application eventsContext to initiate the volley.
     * @param listener callback on success.
     * @param errorListener callback on failures.
     */
    public static void submit(Context context, Priority priority, Object tag, boolean shouldBypassCache,
                              Listener<List<Offer>> listener, ErrorListener errorListener) {
        try {
            OffersRequest request = new OffersRequest(context,
                    AccountStateReporter.getBaseUri(context, "getOffers").build(),
                    priority, shouldBypassCache, listener, errorListener);
            request.setTag(tag);
            VolleyHelper.addToRequestQueue(context, request);
        } catch (Exception e) {
            errorListener.onErrorResponse(new VolleyError(e));
        }
    }

    /**
     * Creates a new request.
     *
     * @param context application context.
     * @param priority priority of request.
     * @param listener Listener to receive the JSON response
     * @param errorListener Error listener, or null to ignore errors.
     */
    public OffersRequest(Context context, Uri offerUri, Priority priority, boolean shouldBypassCache,
                         Listener<List<Offer>> listener, ErrorListener errorListener)
            throws GeneralSecurityException, UnsupportedEncodingException {
        super(Method.GET, Signer.sign(offerUri).toString(), null, listener, errorListener);
        setShouldBypassCache(shouldBypassCache);

        this.context = context;
        this.offerUri = offerUri;
        this.priority = priority;
    }

    @Override
    public Priority getPriority() {
        return priority;
    }

    @Override
    public String getCacheKey() {
        return offerUri.toString();
    }

    @Override
    protected Response<List<Offer>> parseNetworkResponse(NetworkResponse response) {
        new ReportTimingTask(context, "events").execute(response.networkTimeMs);

        try {
            String jsonString = new String(response.data, "UTF-8");
            JSONObject offersJSON = new JSONObject(jsonString);
            List<Offer> offers = Offer.parse(offersJSON.getJSONArray("offers"));
            return Response.success(offers, HttpHeaderParser.parseCacheHeaders(response));
        } catch (Exception e) {
            return Response.error(new ParseError(e));
        }
    }
}

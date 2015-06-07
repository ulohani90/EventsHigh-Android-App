package com.eventshigh.nearme.app.network;

import android.content.Context;
import android.net.Uri;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.data.Offer;
import com.eventshigh.nearme.app.network.OffersRequest.OffersResponse;
import com.eventshigh.nearme.app.security.Signer;
import com.eventshigh.nearme.app.task.ReportTimingTask;
import com.eventshigh.nearme.app.user.AccountStateReporter;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

/**
 * Fetches the offers for current user.
 */
public class OffersRequest extends JsonRequest<OffersResponse> {
    public static class OffersResponse {
        public final List<Offer> offers;
        public final int forClaim;
        public final int claimed;
        public final int totalInstalls;

        public OffersResponse(List<Offer> offers, int forClaim, int claimed, int totalInstalls) {
            this.offers = Collections.unmodifiableList(offers);
            this.forClaim = forClaim;
            this.claimed = claimed;
            this.totalInstalls = totalInstalls;
        }
    }

    private final Context context;
    private final Uri offerUri;
    private final Priority priority;

    /**
     * Helper method to submit a volley request to fetch all offers information.
     *
     * @param context an application eventsContext to initiate the volley.
     * @param listener callback on success.
     * @param errorListener callback on failures.
     */
    public static void submit(Context context, Priority priority, Object tag, boolean shouldBypassCache,
                              Listener<OffersResponse> listener, ErrorListener errorListener) {
        try {
            OffersRequest request = new OffersRequest(context,
                    AccountStateReporter.getBaseUri(context, "getOffersTab").build(),
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
                         Listener<OffersResponse> listener, ErrorListener errorListener)
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
    protected Response<OffersResponse> parseNetworkResponse(NetworkResponse response) {
        ReportTimingTask.report(context, "offers", response.networkTimeMs);

        try {
            String jsonString = new String(response.data, "UTF-8");
            JSONObject offersJSON = new JSONObject(jsonString);
            List<Offer> offers = Offer.parse(offersJSON.getJSONArray("offers"));
            OffersResponse res = new OffersResponse(offers, offersJSON.getInt("for_claim"),
                    offersJSON.getInt("claimed"), offersJSON.getInt("total"));
            return Response.success(res, HttpHeaderParser.parseCacheHeaders(response));
        } catch (Exception e) {
            Crashlytics.getInstance().core.logException(e);
            return Response.error(new ParseError(e));
        }
    }
}

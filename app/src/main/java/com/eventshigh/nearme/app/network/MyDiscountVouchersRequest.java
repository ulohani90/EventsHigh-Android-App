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
import com.eventshigh.nearme.app.broadcast.UpdateAccountInfoService;
import com.eventshigh.nearme.app.network.MyDiscountVouchersRequest.DiscountCode;
import com.eventshigh.nearme.app.utils.Signer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MyDiscountVouchersRequest extends JsonRequest<List<DiscountCode>> {
    public static void submit(Context context, Priority priority, Object tag, boolean shouldBypassCache,
                              Listener<List<DiscountCode>> listener, ErrorListener errorListener) {
        try {
            Uri getMyCouponsUri =
                    UpdateAccountInfoService.getBaseUri(context, "getMyCoupons").build();
            MyDiscountVouchersRequest request = new MyDiscountVouchersRequest(
                    getMyCouponsUri, priority, shouldBypassCache, listener, errorListener);
            request.setTag(tag);
            VolleyHelper.addToRequestQueue(context, request);
        } catch (GeneralSecurityException | UnsupportedEncodingException e) {
            errorListener.onErrorResponse(new VolleyError(e));
        }
    }

    private final Priority priority;
    private final Uri getMyCouponsUri;

    public MyDiscountVouchersRequest(Uri getMyCouponsUri, Priority priority,
                                     boolean shouldBypassCache, Listener<List<DiscountCode>> listener, ErrorListener errorListener)
            throws GeneralSecurityException, UnsupportedEncodingException {
        super(Method.GET, Signer.sign(getMyCouponsUri).toString(), null, listener, errorListener);
        setShouldBypassCache(shouldBypassCache);

        this.priority = priority;
        this.getMyCouponsUri = getMyCouponsUri;
    }

    @Override
    public Priority getPriority() {
        return priority;
    }

    public String getCacheKey() {
        return getMyCouponsUri.toString();
    }

    @Override
    protected Response<List<DiscountCode>> parseNetworkResponse(NetworkResponse response) {
        try {
            String jsonString = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            JSONObject resp = new JSONObject(jsonString);
            JSONArray codes = resp.getJSONArray("codes");

            List<DiscountCode> discountCodes = new ArrayList<>(codes.length());
            for (int i = 0; i < codes.length(); i++) {
                discountCodes.add(DiscountCode.parse(codes.getJSONObject(i)));
            }

            Collections.sort(discountCodes, new Comparator<DiscountCode>() {
                @Override
                public int compare(DiscountCode lhs, DiscountCode rhs) {
                    return Long.compare(lhs.validTillTimestamp, rhs.validTillTimestamp);
                }
            });

            return Response.success(discountCodes, HttpHeaderParser.parseCacheHeaders(response));
        } catch (Exception e) {
            Crashlytics.getInstance().core.logException(e);
            return Response.error(new ParseError(e));
        }
    }

    public static class DiscountCode {
        public final String code;
        public final long validTillTimestamp;
        public final int amount;
        public final boolean isUsed;

        public DiscountCode(String code, long validTillTimestamp, int amount, boolean isUsed) {
            this.code = code;
            this.validTillTimestamp = validTillTimestamp;
            this.amount = amount;
            this.isUsed = isUsed;
        }

        public static DiscountCode parse(JSONObject jsonObject) throws JSONException {
            return new DiscountCode(jsonObject.getString("code"),
                    Long.parseLong(jsonObject.getString("validTillTimestamp")),
                    jsonObject.getInt("amount"),
                    jsonObject.getBoolean("used")
            );
        }
    }
}

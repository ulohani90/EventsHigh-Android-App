package com.eventshigh.nearme.app.network;

import android.content.Context;
import android.net.Uri;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;


import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;
import com.eventshigh.nearme.app.broadcast.UpdateAccountInfoService;


import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.utils.Signer;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SocialInvitationsRequest extends JsonRequest<SocialInvitationsRequest.CommonInviteObject>  {

    public static class PlanInvite {
        public final String planId;

        public PlanInvite(String planId) {
            this.planId = planId;
        }

        public static PlanInvite fromJSON(JSONObject data) throws JSONException {
            return new PlanInvite(data.getString("plan_id"));
        }

        public static List<PlanInvite> fromJSON(JSONArray data) throws JSONException {
            List<PlanInvite> invites = new ArrayList<>(data.length());
            for (int i = 0; i < data.length(); i++) {
                invites.add(fromJSON(data.getJSONObject(i)));
            }
            return invites;
        }
    }
    public static class SpecialCoupons implements Parcelable{

        public final MyDiscountVouchersRequest.DiscountCode coupon;

        public final String  message;

        public final String title;

        public final String target;

        public SpecialCoupons(MyDiscountVouchersRequest.DiscountCode coupon,String message,String title,String target){
            this.coupon = coupon;
            this.message =message;
            this.title = title;
            this.target = target;
        }

        public static List<SpecialCoupons> fromJson(JSONArray array) throws JSONException{
            List<SpecialCoupons> specials = new ArrayList<>();
            for(int i=0;i<array.length();i++){
                specials.add(new SpecialCoupons(array.getJSONObject(i).has("coupon")?MyDiscountVouchersRequest.DiscountCode.parse(array.getJSONObject(i).getJSONObject("coupon")):null,array.getJSONObject(i).getString("m"),array.getJSONObject(i).getString("t"),array.getJSONObject(i).getString("target")));
            }
            return specials;
        }
        public static SpecialCoupons parseJson(String jsonString){
            try {
                JSONObject jsonObject = new JSONObject(jsonString);
                return new SpecialCoupons(jsonObject.has("coupon") ? MyDiscountVouchersRequest.DiscountCode.parse(jsonObject.getJSONObject("coupon")) : null, jsonObject.getString("m"), jsonObject.getString("t"), jsonObject.getString("target"));
            }catch(Exception e){
                return null;
            }
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeParcelable(coupon,flags);
            dest.writeString(message);
            dest.writeString(title);
            dest.writeString(target);

        }
        public static final Parcelable.Creator<SpecialCoupons> CREATOR =
                new Parcelable.Creator<SpecialCoupons>() {
                    public SpecialCoupons createFromParcel(Parcel in) {
                        return new SpecialCoupons(
                                (MyDiscountVouchersRequest.DiscountCode) in.readParcelable(MyDiscountVouchersRequest.DiscountCode.class.getClassLoader()),
                                in.readString(),
                                in.readString(),
                                in.readString()
                        );
                    }

                    public SpecialCoupons[] newArray(int size) {
                        return new SpecialCoupons[size];
                    }
                };


    }

    public static class SocialInvite {
        public final String eventId;
        public final List<PlanInvite> planInvites;


        public SocialInvite(String eventId, List<PlanInvite> planInvites) {
            this.eventId = eventId;
            this.planInvites = planInvites;

        }

        public static SocialInvite fromJSON(JSONObject data) throws JSONException {
            return new SocialInvite(data.getString("event_id"),
                    PlanInvite.fromJSON(data.getJSONArray("plans")));
        }

        public static List<SocialInvite> fromJSON(JSONArray data) throws JSONException {
            List<SocialInvite> invites = new ArrayList<>(data.length());
            for (int i = 0; i < data.length(); i++) {
                invites.add(fromJSON(data.getJSONObject(i)));
            }
            return invites;
        }
    }

    public static class CommonInviteObject {
        public Map<String ,SocialInvite> invites;
        public List<SpecialCoupons> specials;

        public Map<String, SocialInvite> getInvites() {
            return invites;
        }

        public void setInvites(Map<String, SocialInvite> invites) {
            this.invites = invites;
        }

        public List<SpecialCoupons> getSpecials() {
            return specials;
        }

        public void setSpecials(List<SpecialCoupons> specials) {
            this.specials = specials;
        }

    }



    public static void submit(Context context, Priority priority, Object tag, boolean shouldBypassCache,
            Listener<CommonInviteObject> listener, ErrorListener errorListener) {
        try {
            String mobileNo = new Account(context).getUserInfo().phoneNo;
            if (mobileNo == null) {
                errorListener.onErrorResponse(new VolleyError("user is not signed in"));
                return;
            }

            Uri getSocialInvitesUri = UpdateAccountInfoService.getBaseUri(context, "get_social_invites")
                    .appendQueryParameter("mobile_no", mobileNo).build();
            SocialInvitationsRequest request = new SocialInvitationsRequest(
                    getSocialInvitesUri, priority, shouldBypassCache, listener, errorListener);
            request.setTag(tag);
            VolleyHelper.addToRequestQueue(context, request);
        } catch (GeneralSecurityException | UnsupportedEncodingException e) {
            errorListener.onErrorResponse(new VolleyError(e));
        }
    }

    private final Priority priority;
    private final Uri getSocialInvitesUri;


    public SocialInvitationsRequest( Uri getSocialInvitesUri, Priority priority,
            boolean shouldBypassCache, Listener<CommonInviteObject> listener, ErrorListener errorListener) throws GeneralSecurityException, UnsupportedEncodingException {
        super(Method.GET, Signer.sign(getSocialInvitesUri).toString(), null, listener, errorListener);
        setShouldBypassCache(shouldBypassCache);

        this.priority = priority;
        this.getSocialInvitesUri = getSocialInvitesUri;
    }

    @Override
    public Priority getPriority() {
        return priority;
    }

    public String getCacheKey() {
        return getSocialInvitesUri.toString();
    }

    @Override
    protected Response<CommonInviteObject> parseNetworkResponse(NetworkResponse response) {
        try {
            CommonInviteObject commonObj = new CommonInviteObject();

            String jsonString = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            JSONObject resp = new JSONObject(jsonString);
            Map<String, SocialInvite> invites = new HashMap<>();
            for (SocialInvite invite :
                    SocialInvite.fromJSON(resp.getJSONArray("invitations"))) {
                invites.put(invite.eventId, invite);
            }
            commonObj.setInvites(invites);
            commonObj.setSpecials(SpecialCoupons.fromJson(resp.getJSONArray("specials")));

            return Response.success(commonObj, HttpHeaderParser.parseCacheHeaders(response));
        } catch (Exception e) {
            return Response.error(new ParseError(e));
        }
    }
}

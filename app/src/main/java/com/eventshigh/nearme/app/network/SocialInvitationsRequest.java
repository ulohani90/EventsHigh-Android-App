package com.eventshigh.nearme.app.network;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.Nullable;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.data.SocialFriend;
import com.eventshigh.nearme.app.network.SocialInvitationsRequest.SocialInvite;
import com.eventshigh.nearme.app.security.Signer;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.user.AccountStateReporter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

public class SocialInvitationsRequest extends JsonRequest<List<SocialInvite>>  {

    public static class PlanInvite {
        public final String planId;
        public final List<SocialFriend> invitedBy;

        public PlanInvite(String planId, List<SocialFriend> invitedBy) {
            this.planId = planId;
            this.invitedBy = invitedBy;
        }

        public static PlanInvite fromJSON(JSONObject data, Context context) throws JSONException {
            return new PlanInvite(data.getString("plan_id"),
                    SocialFriend.fromJSON(data.getJSONArray("invited_by"), context));
        }

        public static List<PlanInvite> fromJSON(JSONArray data, Context context) throws JSONException {
            List<PlanInvite> invites = new ArrayList<>(data.length());
            for (int i = 0; i < data.length(); i++) {
                invites.add(fromJSON(data.getJSONObject(i), context));
            }
            return invites;
        }
    }

    public static class SocialInvite {
        public final String eventId;
        public final List<PlanInvite> planInvites;

        public SocialInvite(String eventId, List<PlanInvite> planInvites) {
            this.eventId = eventId;
            this.planInvites = planInvites;
        }

        public @Nullable SocialFriend getInvitedBy() {
            for (PlanInvite invite : planInvites) {
                if (! invite.invitedBy.isEmpty()) {
                    return invite.invitedBy.get(0);
                }
            }

            return  null;
        }

        public static SocialInvite fromJSON(JSONObject data, Context context) throws JSONException {
            return new SocialInvite(data.getString("event_id"),
                    PlanInvite.fromJSON(data.getJSONArray("plans"), context));
        }

        public static List<SocialInvite> fromJSON(JSONArray data, Context context) throws JSONException {
            List<SocialInvite> invites = new ArrayList<>(data.length());
            for (int i = 0; i < data.length(); i++) {
                invites.add(fromJSON(data.getJSONObject(i), context));
            }
            return invites;
        }
    }

    public static void submit(Context context, Priority priority, Object tag, boolean shouldBypassCache,
            Listener<List<SocialInvite>> listener, ErrorListener errorListener) {
        try {
            String mobileNo = new Account(context).getUserInfo().phoneNo;
            if (mobileNo == null) {
                errorListener.onErrorResponse(new VolleyError("user is not signed in"));
                return;
            }

            Uri getSocialInvitesUri = AccountStateReporter.getBaseUri(context, "get_social_invites")
                    .appendQueryParameter("mobile_no", mobileNo).build();
            SocialInvitationsRequest request = new SocialInvitationsRequest(context,
                    getSocialInvitesUri, priority, shouldBypassCache, listener, errorListener);
            request.setTag(tag);
            VolleyHelper.addToRequestQueue(context, request);
        } catch (GeneralSecurityException | UnsupportedEncodingException e) {
            errorListener.onErrorResponse(new VolleyError(e));
        }
    }

    private final Context context;
    private final Priority priority;
    private final Uri getSocialInvitesUri;

    public SocialInvitationsRequest(Context context, Uri getSocialInvitesUri, Priority priority,
            boolean shouldBypassCache, Listener<List<SocialInvite>> listener, ErrorListener errorListener)
            throws GeneralSecurityException, UnsupportedEncodingException {
        super(Method.GET, Signer.sign(getSocialInvitesUri).toString(), null, listener, errorListener);
        setShouldBypassCache(shouldBypassCache);

        this.context = context;
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
    protected Response<List<SocialInvite>> parseNetworkResponse(NetworkResponse response) {
        try {
            String jsonString = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            JSONObject resp = new JSONObject(jsonString);
            return Response.success(SocialInvite.fromJSON(resp.getJSONArray("invitations"), context),
                    HttpHeaderParser.parseCacheHeaders(response));
        } catch (Exception e) {
            Crashlytics.getInstance().core.logException(e);
            return Response.error(new ParseError(e));
        }
    }
}

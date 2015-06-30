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
import com.eventshigh.nearme.app.data.EventCategory;
import com.eventshigh.nearme.app.network.SocialActionsRequest.SocialActions;
import com.eventshigh.nearme.app.security.Signer;
import com.eventshigh.nearme.app.user.AccountStateReporter;
import com.eventshigh.nearme.app.utils.ContactUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SocialActionsRequest extends JsonRequest<SocialActions> {

    public static class SocialActions {
        public final Map<String, Set<String>> eventFavourites;
        public final Map<String, Set<String>> tagFollowers;

        public SocialActions(Map<String, Set<String>> eventFavourites, Map<String, Set<String>> tagFollowers) {
            this.eventFavourites = eventFavourites;
            this.tagFollowers = tagFollowers;
        }
    }

    public static void submit(Context context, Priority priority, Object tag, boolean shouldBypassCache,
            Listener<SocialActions> listener, ErrorListener errorListener) {
        try {
            Uri getSocialFriendsUri =
                    AccountStateReporter.getBaseUri(context, "get_social_actions").build();
            String url = Signer.sign(getSocialFriendsUri).toString();
            SocialActionsRequest request = new SocialActionsRequest(
                    context, url, priority, shouldBypassCache, listener, errorListener);
            request.setTag(tag);
            VolleyHelper.addToRequestQueue(context, request);
        } catch (GeneralSecurityException | UnsupportedEncodingException e) {
            errorListener.onErrorResponse(new VolleyError(e));
        }
    }

    private final Context context;
    private final Priority priority;

    public SocialActionsRequest(Context context, String url, Priority priority, boolean shouldBypassCache,
                             Listener<SocialActions> listener, ErrorListener errorListener) {
        super(Method.GET, url, null, listener, errorListener);
        setShouldBypassCache(shouldBypassCache);

        this.context = context;
        this.priority = priority;
    }

    @Override
    public Priority getPriority() {
        return priority;
    }

    @Override
    protected Response<SocialActions> parseNetworkResponse(NetworkResponse response) {
        Map<String, Set<String>> eventFavourites  = new HashMap<>();
        Map<String, Set<String>> tagFollowers = new HashMap<>();

        try {
            String jsonString = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            JSONObject resp = new JSONObject(jsonString);
            JSONArray friends = resp.getJSONArray("social");

            for (int i = 0; i < friends.length(); i++) {
                String contactId = ContactUtils.getContactIdForServerPhone(context,
                        friends.getJSONObject(i).getString("mobile_no"));
                JSONArray favouritesArray = friends.getJSONObject(i).getJSONArray("favourites");
                for (int j = 0; j < favouritesArray.length(); j++) {
                    add(eventFavourites, favouritesArray.getString(j), contactId);
                }

                JSONArray followingsArray = friends.getJSONObject(i).getJSONArray("followings");
                for (int j = 0; j < followingsArray.length(); j++) {
                    String key = EventCategory.toCategoryParsableString(followingsArray.getString(j));
                    add(tagFollowers, key, contactId);
                }
            }

            return Response.success(new SocialActions(eventFavourites, tagFollowers),
                    HttpHeaderParser.parseCacheHeaders(response));
        } catch (Exception e) {
            Crashlytics.getInstance().core.logException(e);
            return Response.error(new ParseError(e));
        }
    }

    private static boolean add(Map<String, Set<String>> usersMap, String key, String name) {
        Set<String> users = usersMap.get(key);
        if (users == null) {
            users = new HashSet<>();
            usersMap.put(key, users);
        }

        return users.add(name);
    }
}

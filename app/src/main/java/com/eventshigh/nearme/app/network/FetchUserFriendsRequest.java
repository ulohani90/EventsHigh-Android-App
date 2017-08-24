package com.eventshigh.nearme.app.network;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.FriendsStore;
import com.eventshigh.nearme.app.data.NewSocialFriend;
import com.eventshigh.nearme.app.user.Account;
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
 * Created by umesh on 23/08/17.
 */

public class FetchUserFriendsRequest extends JsonRequest<List<NewSocialFriend>> {

    public static void submit(Context context, String email,
                              Priority priority, Response.Listener<List<NewSocialFriend>> listener, Response.ErrorListener errorListener, boolean shouldByPassCache) {

        try {
            String url = EventsHighEndpoints.API_URI_BASE + "api/user_friends_for_email/" + email;
            Uri uri = Uri.parse(url);
            url = Signer.sign(uri).toString();
            if (shouldByPassCache) {
                //url += "&cmode=bypass";
            }
            FetchUserFriendsRequest request = new FetchUserFriendsRequest(email,
                    url, priority, context, listener, errorListener, shouldByPassCache);
            request.setRetryPolicy(new DefaultRetryPolicy(60_000,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            VolleyHelper.addToRequestQueue(context, request);
        } catch (UnsupportedEncodingException usee) {
            Log.e("Add FbUser Info Error", usee.toString());
        } catch (GeneralSecurityException gse) {
            Log.e("Add FbUser Info Error", gse.toString());
        }
    }

    private final Priority priority;
    private final Context context;
    private final String profileId;

    public FetchUserFriendsRequest(String profileId, String url, Priority priority, Context context,
                                   Response.Listener<List<NewSocialFriend>> listener, Response.ErrorListener errorListener, boolean shouldBypassCache) {
        super(Method.GET, url, null, listener, errorListener);
        setShouldBypassCache(shouldBypassCache);
        this.priority = priority;
        this.context = context;
        this.profileId = profileId;
    }

    @Override
    public Priority getPriority() {
        return priority;
    }

    @Override
    protected Response<List<NewSocialFriend>> parseNetworkResponse(NetworkResponse networkResponse) {

        try {
            String jsonString = new String(networkResponse.data, "UTF-8");
            JSONObject friendsJson = new JSONObject(jsonString);
            List<NewSocialFriend> friendList = new ArrayList<>();

            if (friendsJson.has("friends")) {

                JSONArray friendsJsonArray = friendsJson.getJSONArray("friends");
                FriendsStore friendsStore = new FriendsStore(context);
                if (friendsJsonArray != null) {
                    for (int i = 0; i < friendsJsonArray.length(); i++) {

                        NewSocialFriend newFriend = NewSocialFriend.parseJsonObject(friendsJsonArray.getJSONObject(i));
                        friendList.add(newFriend);
                        if (profileId.equalsIgnoreCase(new Account(context).getUserInfo().email) && !friendsStore.isKeyExists(newFriend.getEmail()))
                            friendsStore.setFollowing(newFriend.getEmail(), null, true);
                    }
                }
            }

            return Response.success(friendList, HttpHeaderParser.parseCacheHeaders(networkResponse));

        } catch (UnsupportedEncodingException | JSONException e) {
            e.printStackTrace();
            return Response.error(new ParseError(e));
        }


    }
}

package com.eventshigh.nearme.app.data;

import org.json.JSONObject;

/**
 * Created by umesh on 23/08/17.
 */

public class BasicProfileInfo {

    private String name;
    private String lastCity;
    private String profilePic;
    private String email;
    private int numInterests;
    private int numFavourites;
    private int numTickets;
    private int numReviews;
    private int numFriends;


    public BasicProfileInfo(JSONObject jsonObject) {
        JSONObject userInfo = jsonObject.optJSONObject("user_info");
        if (userInfo != null) {
            name = userInfo.optString("name");
            lastCity = userInfo.optString("last_city");
            profilePic = userInfo.optString("profile_pic");
            email = userInfo.optString("email");

        }
        numInterests = jsonObject.optInt("num_interests");
        numFavourites = jsonObject.optInt("num_favourites");
        numTickets = jsonObject.optInt("num_tickets");
        numReviews = jsonObject.optInt("num_reviews");
        numFriends = jsonObject.optInt("num_friends");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLastCity() {
        return lastCity;
    }

    public void setLastCity(String lastCity) {
        this.lastCity = lastCity;
    }

    public String getProfilePic() {
        return profilePic;
    }

    public void setProfilePic(String profilePic) {
        this.profilePic = profilePic;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getNumInterests() {
        return numInterests;
    }

    public void setNumInterests(int numInterests) {
        this.numInterests = numInterests;
    }

    public int getNumFavourites() {
        return numFavourites;
    }

    public void setNumFavourites(int numFavourites) {
        this.numFavourites = numFavourites;
    }

    public int getNumTickets() {
        return numTickets;
    }

    public void setNumTickets(int numTickets) {
        this.numTickets = numTickets;
    }

    public int getNumReviews() {
        return numReviews;
    }

    public void setNumReviews(int numReviews) {
        this.numReviews = numReviews;
    }

    public int getNumFriends() {
        return numFriends;
    }

    public void setNumFriends(int numFriends) {
        this.numFriends = numFriends;
    }
}

package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.ViewGroup;

import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.data.FriendsStore;
import com.eventshigh.nearme.app.data.SocialFriend;
import com.eventshigh.nearme.app.data.UserContact;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ContactsAdapter extends Adapter<ViewHolder> {
    public enum FriendCardType {
        PLAIN,
        FOLLOW
    }

    private final BaseActivity activity;
    private final FriendsStore friendsStore;
    private List<AdapterData> dataToShow = new ArrayList<>();

    public ContactsAdapter(BaseActivity activity) {
        this.activity = activity;
        this.friendsStore = new FriendsStore(activity);
    }

    @Override
    public int getItemCount() {
        return dataToShow.size();
    }

    @Override
    public int getItemViewType(int position) {
        return dataToShow.get(position).getType().typeId;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int type) {
        return DataType.onCreateViewHolder(activity, parent, type);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        dataToShow.get(position).onBindViewHolder(holder, position);
    }

    public void setMyContacts(Collection<UserContact> contacts, FriendCardType cardType) {
        dataToShow.clear();

        for (UserContact contact : contacts){
            SocialFriend friend = new SocialFriend(contact);
            dataToShow.add(new SocialFriendData(activity, friendsStore, friend, cardType));
        }

        if (cardType == FriendCardType.FOLLOW && !contacts.isEmpty()) {
            dataToShow.add(new EhInviteData(activity));
        }

        notifyDataSetChanged();
    }

    public void setFriends(Collection<SocialFriend> friends) {
        dataToShow.clear();

        for (SocialFriend friend : friends) {
            dataToShow.add(new SocialFriendData(activity, friendsStore, friend, FriendCardType.PLAIN));
        }
        notifyDataSetChanged();
    }
}

package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView.ViewHolder;

import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.data.FriendsStore;
import com.eventshigh.nearme.app.data.SocialFriend;
import com.eventshigh.nearme.app.ui.adapter.ContactsAdapter.FriendCardType;

public class SocialFriendData implements AdapterData {
    public final SocialFriend friend;
    public final FriendCardType cardType;
    private final BaseActivity activity;
    private final FriendsStore friendsStore;

    SocialFriendData(BaseActivity activity, FriendsStore friendsStore,
                     SocialFriend friend, FriendCardType cardType) {
        this.friend = friend;
        this.cardType = cardType;
        this.activity = activity;
        this.friendsStore = friendsStore;
    }

    @Override
    public String getId() {
        return friend.getName();
    }

    @Override
    public DataType getType() {
        return DataType.SOCIAL_FRIEND;
    }

    @Override
    public void onBindViewHolder(ViewHolder card, int position) {
        ((SocialFriendCard) card).populate(activity, friendsStore, this);
    }
}

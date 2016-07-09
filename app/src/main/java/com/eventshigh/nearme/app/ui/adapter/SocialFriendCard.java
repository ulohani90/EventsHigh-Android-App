package com.eventshigh.nearme.app.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.UserProfileActivity;
import com.eventshigh.nearme.app.data.FriendsStore;
import com.eventshigh.nearme.app.data.SocialFriend;
import com.eventshigh.nearme.app.ui.adapter.ContactsAdapter.FriendCardType;

public class SocialFriendCard extends ViewHolder {
    private final TextView contactName;
    private final ImageView contactPhoto;
    private final TextView followButton;

    public static SocialFriendCard newInstance(BaseActivity activity, ViewGroup parent) {
        View view = activity.getLayoutInflater().inflate(R.layout.card_contact, parent, false);
        return new SocialFriendCard(view);
    }

    public SocialFriendCard(View itemView) {
        super(itemView);
        contactName = (TextView) itemView.findViewById(R.id.contact_name);
        contactPhoto = (ImageView) itemView.findViewById(R.id.contact_photo);
        followButton = (TextView) itemView.findViewById(R.id.follow_button);
    }

    private void updateActionButton(FriendsStore friendsStore, SocialFriend friend) {
        if (friend.contact != null && friendsStore.isFollowing(friend.contact.mobileNo)) {
            followButton.setText(R.string.ui_following);
            followButton.setSelected(true);
        } else {
            followButton.setText(R.string.ui_follow);
            followButton.setSelected(false);
        }
    }

    public void populate(Context context, final SocialFriend friend) {
        contactName.setText(friend.getName());

        int size = contactPhoto.getLayoutParams().height;
        contactPhoto.setImageDrawable(friend.getDrawable(context, size));
        contactPhoto.setBackgroundColor(Color.TRANSPARENT);
        if (followButton != null) {
            followButton.setVisibility(View.INVISIBLE);
        }
    }

    public void populate(final BaseActivity activity, final FriendsStore friendsStore,
                         final SocialFriendData friendData) {
        populate(activity, friendData.friend);

        followButton.setVisibility(friendData.cardType == FriendCardType.FOLLOW ? View.VISIBLE : View.GONE);
        updateActionButton(friendsStore, friendData.friend);
        followButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (friendData.friend.contact != null) {
                    friendsStore.setFollowing(friendData.friend.contact.mobileNo, friendData.friend.contact.contactId, !followButton.isSelected());
                    updateActionButton(friendsStore, friendData.friend);
                }
            }
        });
        contactName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(activity, UserProfileActivity.class);
                intent.putExtra(UserProfileActivity.PROFILE_ID, friendData.friend.contact.mobileNo);
                activity.startActivity(intent);
            }
        });
        //contactName.setTextColor(Color.parseColor("#09a0f6"));
    }
}

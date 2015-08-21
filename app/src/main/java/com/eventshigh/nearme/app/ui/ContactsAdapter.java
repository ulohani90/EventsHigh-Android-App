package com.eventshigh.nearme.app.ui;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.data.FriendsStore;
import com.eventshigh.nearme.app.data.SocialFriend;
import com.eventshigh.nearme.app.data.UserContact;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ContactsAdapter extends RecyclerView.Adapter<ViewHolder> {
    public enum FriendCardType {
        PLAIN,
        FOLLOW
    }

    private final BaseActivity activity;
    private final FriendsStore friendsStore;
    private List<Data> dataToShow = new ArrayList<>();

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

        for (UserContact contact : contacts) {
            SocialFriend friend = new SocialFriend(contact);
            dataToShow.add(new SocialFriendData(friend, cardType));
        }

        if (cardType == FriendCardType.FOLLOW && !contacts.isEmpty()) {
            dataToShow.add(new EhInviteData());
        }

        notifyDataSetChanged();
    }

    public void setFriends(Collection<SocialFriend> friends) {
        dataToShow.clear();

        for (SocialFriend friend : friends) {
            dataToShow.add(new SocialFriendData(friend, FriendCardType.PLAIN));
        }
        notifyDataSetChanged();
    }

    private enum DataType {
        SOCIAL_FRIEND(0),
        EH_INVITE(1);

        public final int typeId;
        DataType (int typeId) {
            this.typeId = typeId;
        }

        public static ViewHolder onCreateViewHolder(BaseActivity activity, ViewGroup parent, int typeId) {
            if (typeId == SOCIAL_FRIEND.typeId) {
                return SocialFriendCard.newInstance(activity, parent);
            }

            if (typeId == EH_INVITE.typeId) {
                View view = activity.getLayoutInflater().inflate(R.layout.card_share_app, parent, false);
                return new ViewHolder(view) {};
            }

            throw new IllegalArgumentException("invalid typeid");
        }
    }

    private interface Data {
        DataType getType();
        void onBindViewHolder(ViewHolder card, int position);
    }

    private class SocialFriendData implements Data {
        private final SocialFriend friend;
        private final FriendCardType cardType;

        private SocialFriendData(SocialFriend friend, FriendCardType cardType) {
            this.friend = friend;
            this.cardType = cardType;
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

    public static class SocialFriendCard extends ViewHolder {
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
            if (friend.contact != null && friendsStore.isFollowing(friend.contact.contactId)) {
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

            if (followButton != null) {
                followButton.setVisibility(View.INVISIBLE);
            }
        }

        public void populate(BaseActivity activity, final FriendsStore friendsStore,
                             final SocialFriendData friendData) {
            populate(activity, friendData.friend);

            followButton.setVisibility(friendData.cardType == FriendCardType.FOLLOW ? View.VISIBLE : View.GONE);
            updateActionButton(friendsStore, friendData.friend);
            followButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (friendData.friend.contact != null) {
                        friendsStore.setFollowing(friendData.friend.contact.contactId, !followButton.isSelected());
                        updateActionButton(friendsStore, friendData.friend);
                    }
                }
            });
        }
    }

    private abstract class InviteData implements Data {
        @Override
        public void onBindViewHolder(ViewHolder card, int position) {
            card.itemView.findViewById(R.id.share_app).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.shareApp();
                }
            });
        }
    }

    private class EhInviteData extends InviteData {
        @Override
        public DataType getType() {
            return DataType.EH_INVITE;
        }
    }
}

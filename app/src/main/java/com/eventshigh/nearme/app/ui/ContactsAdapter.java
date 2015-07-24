package com.eventshigh.nearme.app.ui;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.FriendsStore;
import com.eventshigh.nearme.app.data.UserContact;
import com.eventshigh.nearme.app.data.SocialFriend;
import com.eventshigh.nearme.app.network.MyContactsRequest.MyContact;
import com.eventshigh.nearme.app.ui.EventsAdapter.EventCard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ContactsAdapter extends RecyclerView.Adapter<ViewHolder> {
    public enum FriendCardType {
        PLAIN,
        FOLLOW,
        SELECT
    }

    private final BaseActivity activity;
    private final FriendsStore friendsStore;
    private List<Data> dataToShow = new ArrayList<>();
    private Set<SocialFriend> selectedFriends = new HashSet<>();

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

    public void setMyContacts(Collection<MyContact> contacts, FriendCardType cardType) {
        dataToShow.clear();
        selectedFriends.clear();

        for (UserContact contact : contacts) {
            SocialFriend friend = new SocialFriend(contact);
            dataToShow.add(new SocialFriendData(friend, cardType));
            selectedFriends.add(friend);
        }

        if (cardType == FriendCardType.FOLLOW && !contacts.isEmpty()) {
            dataToShow.add(new EhInviteData());
        }
        if (cardType == FriendCardType.SELECT) {
            dataToShow.add(new EventInviteData());
        }

        notifyDataSetChanged();
    }

    public void setEventContacts(Event event, Collection<MyContact> contacts) {
        setMyContacts(contacts, FriendCardType.SELECT);
        dataToShow.add(0, new EventData(event));
        dataToShow.add(1, new InviteMessageData());
        notifyDataSetChanged();
    }

    public void setFriends(Collection<SocialFriend> friends) {
        dataToShow.clear();
        selectedFriends.clear();

        for (SocialFriend friend : friends) {
            dataToShow.add(new SocialFriendData(friend, FriendCardType.PLAIN));
        }
        notifyDataSetChanged();
    }

    public Collection<SocialFriend> getSelectedFriends() {
        return selectedFriends;
    }

    private enum DataType {
        SOCIAL_FRIEND(0),
        EH_INVITE(1),
        EVENT_INVITE(2),
        EVENT(3),
        INVITE_MESSAGE(4);

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

            if (typeId == EVENT_INVITE.typeId) {
                View view = activity.getLayoutInflater().inflate(R.layout.card_invite_friends, parent, false);
                return new ViewHolder(view) {};
            }

            if (typeId == EVENT.typeId) {
                return EventCard.newInstance(activity, parent, false);
            }

            if (typeId == INVITE_MESSAGE.typeId) {
                View view = activity.getLayoutInflater().inflate(R.layout.view_select_message, parent, false);
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
            ((SocialFriendCard) card).populate(activity, friendsStore, selectedFriends, this);
        }
    }

    public static class SocialFriendCard extends ViewHolder {
        private final TextView contactName;
        private final ImageView contactPhoto;
        private final TextView followButton;
        private final CheckBox selectContact;

        public static SocialFriendCard newInstance(BaseActivity activity, ViewGroup parent) {
            View view = activity.getLayoutInflater().inflate(R.layout.card_contact, parent, false);
            return new SocialFriendCard(view);
        }

        public SocialFriendCard(View itemView) {
            super(itemView);

            contactName = (TextView) itemView.findViewById(R.id.contact_name);
            contactPhoto = (ImageView) itemView.findViewById(R.id.contact_photo);
            followButton = (TextView) itemView.findViewById(R.id.follow_button);
            selectContact = (CheckBox) itemView.findViewById(R.id.select_contact);
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

        private void setSelected(boolean isSelected) {
            selectContact.setChecked(isSelected);
        }

        public void populate(BaseActivity activity, final FriendsStore friendsStore,
                Set<SocialFriend> selectedFriends, final SocialFriendData friendData) {
            contactName.setText(friendData.friend.getName());

            int size = contactPhoto.getLayoutParams().height;
            contactPhoto.setImageDrawable(friendData.friend.getDrawable(activity, size));

            selectContact.setVisibility(
                    friendData.cardType == FriendCardType.SELECT ? View.VISIBLE : View.GONE);
            if (friendData.cardType == FriendCardType.SELECT) {
                setSelected(selectedFriends.contains(friendData.friend));
                SelectionListener listener = new SelectionListener(selectedFriends, friendData.friend);
                itemView.setOnClickListener(listener);
                selectContact.setOnClickListener(listener);
            }

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

        private class SelectionListener implements OnClickListener {
            private final Set<SocialFriend> selectedFriends;
            private final SocialFriend friend;

            private SelectionListener(Set<SocialFriend> selectedFriends, SocialFriend friend) {
                this.selectedFriends = selectedFriends;
                this.friend = friend;
            }

            @Override
            public void onClick(View v) {
                if (selectedFriends.contains(friend)) {
                    selectedFriends.remove(friend);
                    setSelected(false);
                } else {
                    selectedFriends.add(friend);
                    setSelected(true);
                }
            }
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

    private class EventInviteData extends InviteData {
        @Override
        public DataType getType() {
            return DataType.EVENT_INVITE;
        }
    }

    private class EventData implements Data {
        private final Event event;

        private EventData(Event event) {
            this.event = event;
        }

        @Override
        public DataType getType() {
            return DataType.EVENT;
        }

        @Override
        public void onBindViewHolder(ViewHolder card, int position) {
            ((EventCard) card).bindEventView(event, activity);
        }
    }

    private static class InviteMessageData implements Data {
        @Override
        public DataType getType() {
            return DataType.INVITE_MESSAGE;
        }

        @Override
        public void onBindViewHolder(ViewHolder card, int position) {
            // do nothing.
        }
    }
}

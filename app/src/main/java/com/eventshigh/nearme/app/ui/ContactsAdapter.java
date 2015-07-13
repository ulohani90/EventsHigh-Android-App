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
import com.eventshigh.nearme.app.ui.EventsAdapter.EventCard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ContactsAdapter extends RecyclerView.Adapter<ViewHolder> {
    public enum ContactCardType {
        PLAIN,
        FOLLOW,
        SELECT
    }

    private final BaseActivity activity;
    private final FriendsStore friendsStore;
    private List<Data> dataToShow = new ArrayList<>();
    private Set<UserContact> selectedContacts = new HashSet<>();

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

    public void setMyContacts(List<UserContact> contacts, ContactCardType cardType) {
        dataToShow.clear();
        for (UserContact contact : contacts) {
            dataToShow.add(new ContactData(contact, cardType));
        }

        if (cardType == ContactCardType.FOLLOW && !contacts.isEmpty()) {
            dataToShow.add(new EhInviteData());
        }
        if (cardType == ContactCardType.SELECT) {
            dataToShow.add(new EventInviteData());
        }

        selectedContacts.clear();
        selectedContacts.addAll(contacts);
        notifyDataSetChanged();
    }

    public void setEventContacts(Event event, List<UserContact> contacts) {
        dataToShow.clear();
        setMyContacts(contacts, ContactCardType.SELECT);
        dataToShow.add(0, new EventData(event));
        dataToShow.add(1, new InviteMessageData());
        notifyDataSetChanged();
    }

    public Collection<UserContact> getSelectedContacts() {
        return selectedContacts;
    }

    private enum DataType {
        CONTACT(0),
        EH_INVITE(1),
        EVENT_INVITE(2),
        EVENT(3),
        INVITE_MESSAGE(4);

        public final int typeId;
        DataType (int typeId) {
            this.typeId = typeId;
        }

        public static ViewHolder onCreateViewHolder(BaseActivity activity, ViewGroup parent, int typeId) {
            if (typeId == CONTACT.typeId) {
                return ContactCard.newInstance(activity, parent);
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
                View view = activity.getLayoutInflater().inflate(R.layout.card_event_big, parent, false);
                return new EventCard(view, false);
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

    private class ContactData implements Data {
        private final UserContact contact;
        private final ContactCardType cardType;

        private ContactData(UserContact contact, ContactCardType cardType) {
            this.contact = contact;
            this.cardType = cardType;
        }

        @Override
        public DataType getType() {
            return DataType.CONTACT;
        }

        @Override
        public void onBindViewHolder(ViewHolder card, int position) {
            ((ContactCard) card).populate(activity, friendsStore, selectedContacts, this);
        }
    }

    public static class ContactCard extends ViewHolder {
        private final TextView contactName;
        private final ImageView contactPhoto;
        private final TextView followButton;
        private final CheckBox selectContact;

        public static ContactCard newInstance(BaseActivity activity, ViewGroup parent) {
            View view = activity.getLayoutInflater().inflate(R.layout.card_contact, parent, false);
            return new ContactCard(view);
        }

        public ContactCard(View itemView) {
            super(itemView);

            contactName = (TextView) itemView.findViewById(R.id.contact_name);
            contactPhoto = (ImageView) itemView.findViewById(R.id.contact_photo);
            followButton = (TextView) itemView.findViewById(R.id.follow_button);
            selectContact = (CheckBox) itemView.findViewById(R.id.select_contact);
        }

        private void updateActionButton(FriendsStore friendsStore, String contactId) {
            if (friendsStore.isFollowing(contactId)) {
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
                Set<UserContact> selectedContacts, final ContactData contactData) {
            contactName.setText(contactData.contact.name);

            int size = contactPhoto.getLayoutParams().height;
            contactPhoto.setImageDrawable(contactData.contact.getDrawable(activity, size));

            selectContact.setVisibility(
                contactData.cardType == ContactCardType.SELECT ? View.VISIBLE : View.GONE);
            if (contactData.cardType == ContactCardType.SELECT) {
                setSelected(selectedContacts.contains(contactData.contact));
                SelectionListener listener = new SelectionListener(selectedContacts, contactData.contact);
                itemView.setOnClickListener(listener);
                selectContact.setOnClickListener(listener);
            }

            followButton.setVisibility(contactData.cardType == ContactCardType.FOLLOW ? View.VISIBLE : View.GONE);
            followButton.setTag(contactData.contact.contactId);
            updateActionButton(friendsStore, contactData.contact.contactId);
            followButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    friendsStore.setFollowing(contactData.contact.contactId, !followButton.isSelected());
                    updateActionButton(friendsStore, contactData.contact.contactId);
                }
            });
        }

        private class SelectionListener implements OnClickListener {
            private final Set<UserContact> selectedContacts;
            private final UserContact contact;

            private SelectionListener(Set<UserContact> selectedContacts, UserContact contact) {
                this.selectedContacts = selectedContacts;
                this.contact = contact;
            }

            @Override
            public void onClick(View v) {
                if (selectedContacts.contains(contact)) {
                    selectedContacts.remove(contact);
                    setSelected(false);
                } else {
                    selectedContacts.add(contact);
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

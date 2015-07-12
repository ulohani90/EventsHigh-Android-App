package com.eventshigh.nearme.app.ui;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.data.FriendsStore;
import com.eventshigh.nearme.app.data.UserContact;

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

    public Collection<UserContact> getSelectedContacts() {
        return selectedContacts;
    }

    private enum DataType {
        CONTACT(0),
        EH_INVITE(1),
        EVENT_INVITE(2);

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
                return new InviteCard(view);
            }

            if (typeId == EVENT_INVITE.typeId) {
                View view = activity.getLayoutInflater().inflate(R.layout.card_share_app, parent, false);
                return new InviteCard(view);
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
        private final TextView action;

        public static ContactCard newInstance(BaseActivity activity, ViewGroup parent) {
            View view = activity.getLayoutInflater().inflate(R.layout.card_contact, parent, false);
            return new ContactCard(view);
        }

        public ContactCard(View itemView) {
            super(itemView);

            contactName = (TextView) itemView.findViewById(R.id.contact_name);
            contactPhoto = (ImageView) itemView.findViewById(R.id.contact_photo);
            action = (TextView) itemView.findViewById(R.id.action);
        }

        private void updateActionButton(FriendsStore friendsStore, String contactId) {
            if (friendsStore.isFollowing(contactId)) {
                action.setText(R.string.ui_following);
                action.setSelected(true);
            } else {
                action.setText(R.string.ui_follow);
                action.setSelected(false);
            }
        }

        private void setContactImage(UserContact contact, Context context, boolean isSelected) {
            int size = contactPhoto.getLayoutParams().height;
            if (isSelected) {
                contactPhoto.setImageDrawable(
                        TextDrawable.builder().buildRoundRect("✓", 0xff616161, size));
            } else {
                contactPhoto.setImageDrawable(contact.getDrawable(context, size));
            }
        }

        public void populate(final BaseActivity activity, final FriendsStore friendsStore,
                final Set<UserContact> selectedContacts, final ContactData contactData) {
            contactName.setText(contactData.contact.name);

            setContactImage(contactData.contact, activity,
                contactData.cardType == ContactCardType.SELECT &&
                        selectedContacts.contains(contactData.contact));
            if (contactData.cardType == ContactCardType.SELECT) {
                contactPhoto.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (selectedContacts.contains(contactData.contact)) {
                            selectedContacts.remove(contactData.contact);
                            setContactImage(contactData.contact, activity, false);
                        } else {
                            selectedContacts.add(contactData.contact);
                            setContactImage(contactData.contact, activity, true);
                        }
                    }
                });
            }

            action.setVisibility(contactData.cardType == ContactCardType.FOLLOW ? View.VISIBLE : View.GONE);
            action.setTag(contactData.contact.contactId);
            updateActionButton(friendsStore, contactData.contact.contactId);
            action.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    friendsStore.setFollowing(contactData.contact.contactId, !action.isSelected());
                    updateActionButton(friendsStore, contactData.contact.contactId);
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

    private class EventInviteData extends InviteData {
        @Override
        public DataType getType() {
            return DataType.EVENT_INVITE;
        }
    }

    private static class InviteCard extends ViewHolder {
        public InviteCard(View itemView) {
            super(itemView);
        }
    }
}

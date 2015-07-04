package com.eventshigh.nearme.app.ui;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.data.FriendsStore;
import com.eventshigh.nearme.app.data.UserContact;
import com.eventshigh.nearme.app.ui.ContactsAdapter.ContactView;

import java.util.List;

public class ContactsAdapter extends RecyclerView.Adapter<ContactView> {
    private final BaseActivity activity;
    private final FriendsStore friendsStore;
    private final boolean showInviteFriends;

    private List<UserContact> contacts;

    public ContactsAdapter(BaseActivity activity, boolean showInviteFriends) {
        this.activity = activity;
        this.showInviteFriends = showInviteFriends;
        this.friendsStore = new FriendsStore(activity);
    }

    @Override
    public void onBindViewHolder(ContactView holder, int position) {
        if (position < contacts.size()) {
            holder.populate(contacts.get(position));
        } else {
            holder.itemView.findViewById(R.id.share_app).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.shareApp();
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return contacts == null || contacts.isEmpty() ? 0 :
                contacts.size() + (showInviteFriends ? 1 : 0);
    }

    @Override
    public int getItemViewType(int position) {
        return position < contacts.size() ? 0 : 1;
    }

    @Override
    public ContactView onCreateViewHolder(ViewGroup parent, int type) {
        View view = activity.getLayoutInflater().inflate(
                type == 0 ? R.layout.card_contact : R.layout.card_share_app, parent, false);
        return new ContactView(view);
    }

    public void setMyContacts(List<UserContact> contacts) {
        this.contacts = contacts;
        notifyDataSetChanged();
    }

    public class ContactView extends RecyclerView.ViewHolder {
        private final TextView contactName;
        private final ImageView contactPhoto;
        private final TextView action;

        public ContactView(View itemView) {
            super(itemView);

            contactName = (TextView) itemView.findViewById(R.id.contact_name);
            contactPhoto = (ImageView) itemView.findViewById(R.id.contact_photo);
            action = (TextView) itemView.findViewById(R.id.action);
        }

        private void updateActionButton( String contactId) {
            if (friendsStore.isFollowing(contactId)) {
                action.setText(R.string.ui_following);
                action.setSelected(true);
            } else {
                action.setText(R.string.ui_follow);
                action.setSelected(false);
            }
        }

        public void populate(final UserContact contact) {
            contactName.setText(contact.name);
            contactPhoto.setImageDrawable(
                    contact.getDrawable(activity, contactPhoto.getLayoutParams().height));

            action.setVisibility(showInviteFriends ? View.VISIBLE : View.GONE);
            action.setTag(contact.contactId);
            updateActionButton(contact.contactId);
            action.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    friendsStore.setFollowing(contact.contactId, !action.isSelected());
                    updateActionButton(contact.contactId);
                }
            });
        }
    }
}

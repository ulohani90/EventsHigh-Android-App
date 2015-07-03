package com.eventshigh.nearme.app.ui;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.data.UserContact;
import com.eventshigh.nearme.app.network.MyContactsRequest.MyContacts;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ContactView> {
    private final BaseActivity activity;

    private MyContacts myContacts;

    public ContactsAdapter(BaseActivity activity) {
        this.activity = activity;
    }

    @Override
    public ContactView onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = activity.getLayoutInflater().inflate(R.layout.card_contact, parent, false);
        return new ContactView(view);
    }

    @Override
    public void onBindViewHolder(ContactView holder, int position) {
        holder.populate(myContacts.EHContacts.get(position), activity);
    }

    @Override
    public int getItemCount() {
        return myContacts == null ? 0 : myContacts.EHContacts.size();
    }

    public void setMyContacts(MyContacts myContacts) {
        this.myContacts = myContacts;
        notifyDataSetChanged();
    }

    public static class ContactView extends RecyclerView.ViewHolder {
        private final TextView contactName;
        private final ImageView contactPhoto;
        private final TextView action;

        public ContactView(View itemView) {
            super(itemView);

            contactName = (TextView) itemView.findViewById(R.id.contact_name);
            contactPhoto = (ImageView) itemView.findViewById(R.id.contact_photo);
            action = (TextView) itemView.findViewById(R.id.action);
        }

        public void populate(UserContact contact, BaseActivity activity) {
            contactName.setText(contact.name);
            contactPhoto.setImageDrawable(
                    contact.getDrawable(activity, contactPhoto.getLayoutParams().height));
            action.setText(R.string.ui_follow);
            action.setSelected(false);
        }
    }
}

package com.eventshigh.nearme.app.ui;

import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.data.UserContact;
import com.eventshigh.nearme.app.utils.ContactUtils;
import com.eventshigh.nearme.app.utils.ImageUtils;

import java.util.List;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ContactView> {
    private final BaseActivity activity;

    private List<UserContact> contacts;
    private List<String> contactPhonesOnEh;

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
        UserContact contact = contacts.get(position);
        holder.populate(contact, activity, contactPhonesOnEh);
    }

    @Override
    public int getItemCount() {
        return contacts == null ? 0 : contacts.size();
    }

    public void setContacts(List<UserContact> contacts) {
        this.contacts = contacts;
        notifyDataSetChanged();
    }

    public void setContactsOnEh(List<String> contactsOnEh) {
        contactPhonesOnEh = contactsOnEh;
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

        public void populate(UserContact contact, BaseActivity activity,
                @Nullable List<String> contactPhonesOnEh) {
            contactName.setText(contact.name);
            Bitmap bitmap = ContactUtils.getPhotoForPhone(activity, contact.mobileNo);
            if (bitmap != null) {
                contactPhoto.setImageBitmap(ImageUtils.getCircularBitmapFrom(bitmap));
            }

            if (contactPhonesOnEh == null) {
                action.setVisibility(View.GONE);
            } else {
                action.setVisibility(View.VISIBLE);
                if (contactPhonesOnEh.contains(contact.mobileNo)) {
                    action.setText(R.string.ui_follow);
                    action.setSelected(false);
                } else {
                    action.setText(R.string.social_invite);
                    action.setSelected(true);
                }
            }
        }
    }
}

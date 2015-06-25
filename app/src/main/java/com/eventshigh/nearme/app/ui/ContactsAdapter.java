package com.eventshigh.nearme.app.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.data.UserContact;
import com.eventshigh.nearme.app.utils.ContactUtils;

import java.util.List;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ContactView> {
    private final BaseContextActivity activity;

    private List<UserContact> contacts;

    public ContactsAdapter(BaseContextActivity activity) {
        this.activity = activity;
    }

    @Override
    public ContactView onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = activity.getLayoutInflater().inflate(R.layout.card_contact, parent, false);
        return new ContactView(view);
    }

    @Override
    public void onBindViewHolder(ContactView holder, int position) {
        holder.contactName.setText(contacts.get(position).name);
        byte[] bitmapData = ContactUtils.getPhotoForContactId(activity, contacts.get(position).contactId);
        if (bitmapData != null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.length);
            holder.contactPhoto.setImageBitmap(bitmap);
        }
    }

    @Override
    public int getItemCount() {
        return contacts == null ? 0 : contacts.size();
    }

    public void setContacts(List<UserContact> contacts) {
        this.contacts = contacts;
        notifyDataSetChanged();
    }

    class ContactView extends RecyclerView.ViewHolder {
        private final TextView contactName;
        private final ImageView contactPhoto;

        public ContactView(View itemView) {
            super(itemView);

            contactName = (TextView) itemView.findViewById(R.id.contact_name);
            contactPhoto = (ImageView) itemView.findViewById(R.id.contact_photo);
        }
    }
}

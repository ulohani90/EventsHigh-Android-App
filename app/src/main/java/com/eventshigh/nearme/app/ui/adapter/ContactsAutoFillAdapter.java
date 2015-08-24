package com.eventshigh.nearme.app.ui.adapter;

import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;

import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.data.SocialFriend;
import com.eventshigh.nearme.app.data.UserContact;
import com.eventshigh.nearme.app.utils.ContactUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class ContactsAutoFillAdapter extends BaseAdapter implements Filterable {
    private final BaseActivity activity;
    private List<UserContact> contacts;
    private NameFilter filter;

    public ContactsAutoFillAdapter(BaseActivity activity) {
        this.activity = activity;
    }

    @Override
    public int getCount() {
        return contacts.size();
    }

    @Override
    public Object getItem(int position) {
        return contacts.get(position);
    }

    @Override
    public long getItemId(int position) {
        return contacts.get(position).contactId.hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = SocialFriendCard.newInstance(activity, parent).itemView;
        }

        new SocialFriendCard(convertView).populate(activity, new SocialFriend(contacts.get(position)));
        return convertView;
    }

    @Override
    public Filter getFilter() {
        if (filter == null) {
            filter = new NameFilter();
        }
        return filter;
    }

    private class NameFilter extends Filter {
        private List<UserContact> allContacts;

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            if (allContacts == null) {
                allContacts = new LinkedList<>(ContactUtils.getContacts(activity, null, Phone.DISPLAY_NAME, false));

                String lastContactName = null;
                Iterator<UserContact> contactIterator = allContacts.iterator();
                while (contactIterator.hasNext()) {
                    UserContact contact = contactIterator.next();
                    if (contact.name.equalsIgnoreCase(lastContactName)) {
                        contactIterator.remove();
                    } else {
                        lastContactName = contact.name;
                    }
                }
            }

            String c = constraint == null ? "" : constraint.toString().toLowerCase();
            List<UserContact> contacts = new ArrayList<>(allContacts.size());
            if (c.isEmpty()) {
                contacts.addAll(allContacts);
            } else {
                for (UserContact contact : allContacts) {
                    if (contact.name.toLowerCase().contains(c)) {
                        contacts.add(contact);
                    }
                }
            }

            FilterResults results = new FilterResults();
            results.values = contacts;
            results.count = contacts.size();
            return results;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void publishResults(CharSequence constraint, FilterResults results) {
            if (results.count > 0) {
                contacts = (List<UserContact>) results.values;
                notifyDataSetChanged();
            }
        }
    }
}

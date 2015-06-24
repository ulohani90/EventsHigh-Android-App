package com.eventshigh.nearme.app.ui;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.eventshigh.nearme.app.activity.BaseContextActivity;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.Contact> {
    private final BaseContextActivity activity;

    public ContactsAdapter(BaseContextActivity activity) {
        this.activity = activity;
    }

    @Override
    public Contact onCreateViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(Contact holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }

    public class Contact extends RecyclerView.ViewHolder {
        public Contact(View itemView) {
            super(itemView);
        }
    }
}

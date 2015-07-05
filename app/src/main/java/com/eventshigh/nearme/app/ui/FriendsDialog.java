package com.eventshigh.nearme.app.ui;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.support.v7.app.AlertDialog;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.data.UserContact;
import com.eventshigh.nearme.app.view.AutofitRecyclerView;

import java.util.List;

public class FriendsDialog {

    public static void show(final BaseActivity activity,List<UserContact> contacts) {
        final AlertDialog alertDialog = new AlertDialog.Builder(activity)
                .setView(R.layout.view_grid)
                .setPositiveButton(android.R.string.ok, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing.
                    }
                })
                .setCancelable(true)
                .create();
        alertDialog.show();

        AutofitRecyclerView gridView = (AutofitRecyclerView) alertDialog.findViewById(R.id.grid);
        ContactsAdapter contactsAdapter = new ContactsAdapter(activity, false);
        contactsAdapter.setMyContacts(contacts);
        gridView.setAdapter(contactsAdapter);
    }
}

package com.eventshigh.nearme.app.ui;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.text.format.DateUtils;
import android.view.View;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.user.Preferences;
import com.eventshigh.nearme.app.user.UserContactsUploader;

public class AskForContactsDialog {
    public static void doNeedful(BaseActivity activity) {
        Preferences preferences = Preferences.getInstance(activity);
        if (preferences.shouldUploadContacts()) {
            new UserContactsUploader(activity).uploadContacts();
        } else if (preferences.getLastUploadContactsAsked() < System.currentTimeMillis() - DateUtils.WEEK_IN_MILLIS) {
            AskForContactsDialog.show(activity, preferences);
        }
    }

    public static void show(final BaseActivity activity, final Preferences preferences) {
        @SuppressLint("InflateParams")
        View view = activity.getLayoutInflater().inflate(R.layout.dialog_ask_for_contacts, null);
        new AlertDialog.Builder(activity)
                .setView(view)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        activity.reportActionToAnalytics("uploadContactsAccepted");
                        preferences.setShouldUploadContacts(true);
                        new UserContactsUploader(activity).uploadContacts();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        activity.reportActionToAnalytics("uploadContactsRejected");
                    }
                })
                .setCancelable(true)
                .show();
        preferences.setUploadContactsAsked();
    }
}

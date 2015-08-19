package com.eventshigh.nearme.app.ui;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.text.format.DateUtils;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.user.Preferences;
import com.eventshigh.nearme.app.user.UserContactsUploader;

public class AskForContactsDialog {
    public interface ContactsRequestCallback {
        void onContactsUploadAccepted();
        void onContactsUploadRejected();
    }

    public static void doNeedful(BaseActivity activity) {
        Preferences preferences = Preferences.getInstance(activity);
        if (preferences.canUploadContacts()) {
            new UserContactsUploader(activity).uploadContacts();
        } else if (preferences.getLastUploadContactsAsked() < System.currentTimeMillis() - DateUtils.WEEK_IN_MILLIS) {
            AskForContactsDialog.show(activity, preferences);
        }
    }

    public static void show(final BaseActivity activity, final Preferences preferences) {
        show(activity, preferences, new DummyContactsRequestCallback());
    }

    public static void show(final BaseActivity activity, final Preferences preferences,
            final ContactsRequestCallback callback) {
        new AlertDialog.Builder(activity)
                .setView(R.layout.dialog_ask_for_contacts)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        activity.reportActionToAnalytics("uploadContactsAccepted");
                        preferences.setCanUploadContacts(true);
                        new UserContactsUploader(activity).uploadContacts();
                        callback.onContactsUploadAccepted();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        activity.reportActionToAnalytics("uploadContactsRejected");
                        callback.onContactsUploadRejected();
                    }
                })
                .setCancelable(true)
                .show();
        preferences.setUploadContactsAsked();
    }

    public static class DummyContactsRequestCallback implements ContactsRequestCallback {
        public void onContactsUploadAccepted() {
            // do nothing.
        }

        public void onContactsUploadRejected() {
            // do nothing.
        }
    }
}

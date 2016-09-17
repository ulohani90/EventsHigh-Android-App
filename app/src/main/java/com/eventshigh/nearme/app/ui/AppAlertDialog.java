package com.eventshigh.nearme.app.ui;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.FbLoginFragment;
import com.eventshigh.nearme.app.user.Preferences;
import com.eventshigh.nearme.app.user.UserContactsUploader;

/**
 * Created by umesh on 25/08/16.
 */
public class AppAlertDialog {

    public static void show(String title, String message, final BaseActivity activity, final FbLoginFragment.OnStartGoogleLoginListener listener) {
        new AlertDialog.Builder(activity)
                .setTitle(title).setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        activity.reportActionToAnalytics("fbLoginError", "message");
                        listener.onStartGoogleLogin();
                        dialog.dismiss();
                    }
                }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        })
                .setCancelable(true)
                .show();

    }
}

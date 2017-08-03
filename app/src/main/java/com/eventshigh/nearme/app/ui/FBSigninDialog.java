package com.eventshigh.nearme.app.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.FBLoginActivity;

/**
 * Created by umesh on 21/07/16.
 */
public class FBSigninDialog {

    public static void show(final BaseActivity activity, int titleId, int messageId, final int requestCode) {
        activity.reportActionToAnalytics("emailSigninDialog");
        new AlertDialog.Builder(activity)
                .setIcon(R.drawable.ic_launcher)
                .setTitle(titleId)
                .setMessage(messageId)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        activity.reportActionToAnalytics("emailSigninAccepted");
                        Intent intent = new Intent(activity, FBLoginActivity.class);
                        dialog.dismiss();
                        activity.startActivityForResult(intent, requestCode);

                    }
                })
                .setCancelable(true)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        activity.reportActionToAnalytics("emailSigninRejected");
                        dialog.dismiss();
                    }
                })
                .show();
    }
}

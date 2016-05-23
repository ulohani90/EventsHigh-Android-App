package com.eventshigh.nearme.app.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.PhoneLoginActivity;

/**
 * Created by umesh on 23/05/16.
 */
public class CommonAlertDialog {
    public static void show(final BaseActivity activity, int titleId, int messageId) {

        new AlertDialog.Builder(activity)
                .setIcon(R.drawable.ic_launcher)
                .setTitle(titleId)
                .setMessage(messageId)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        activity.startActivity(new Intent(activity, PhoneLoginActivity.class));
                    }
                })
                .setCancelable(true)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        activity.reportActionToAnalytics("phoneVerificationRejected");
                    }
                })
                .show();
    }
}

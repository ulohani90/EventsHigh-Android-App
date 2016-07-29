package com.eventshigh.nearme.app.ui;

import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.support.v7.app.AlertDialog;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.PhoneLoginActivity;

public class PhoneVerificationDialog {
    public static void show(final BaseActivity activity, int titleId, int messageId) {
        activity.reportActionToAnalytics("phoneVerificationAsked");
        new AlertDialog.Builder(activity)
                .setIcon(R.drawable.ic_launcher)
                .setTitle(titleId)
                .setMessage(messageId)
                .setPositiveButton(android.R.string.ok, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        activity.reportActionToAnalytics("phoneVerificationAccepted");
                        activity.startActivity(new Intent(activity, PhoneLoginActivity.class));

                    }
                })
                .setCancelable(true)
                .setOnCancelListener(new OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        activity.reportActionToAnalytics("phoneVerificationRejected");
                    }
                })
                .show();
    }
}

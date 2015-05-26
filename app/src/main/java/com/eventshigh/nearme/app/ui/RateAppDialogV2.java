package com.eventshigh.nearme.app.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.View.OnClickListener;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.FeedbackActivity;

public class RateAppDialogV2 {
    private static final String PREF_SHOW_RATE_APP_DIALOG = "showRateAppDialog";

    public static void show(final BaseActivity activity) {
        final SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(activity);
        if (!sharedPreferences.getBoolean(PREF_SHOW_RATE_APP_DIALOG, true)) {
            return;
        }

        @SuppressLint("InflateParams")
        View view = activity.getLayoutInflater().inflate(R.layout.dialog_rate_app_v2, null);
        final AlertDialog alertDialog = new AlertDialog.Builder(activity)
                .setView(view)
                .setCancelable(true)
                .create();
        alertDialog.show();

        alertDialog.findViewById(R.id.rate_title).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.reportActionToAnalytics("rateAppRejected");
                alertDialog.dismiss();
                disableRateDialog(sharedPreferences);
            }
        });
        alertDialog.findViewById(R.id.rate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.reportActionToAnalytics("rateAppAccepted");
                alertDialog.dismiss();
                disableRateDialog(sharedPreferences);

                Uri uri = Uri.parse("market://details?id=" + activity.getPackageName());
                activity.startActivity(new Intent(Intent.ACTION_VIEW, uri));
            }
        });
        alertDialog.findViewById(R.id.feedback).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.reportActionToAnalytics("sendFeedback");
                alertDialog.dismiss();

                activity.startActivity(new Intent(activity, FeedbackActivity.class));
        };
        });
    }

    private static void disableRateDialog(SharedPreferences sharedPreferences) {
        sharedPreferences.edit().putBoolean(PREF_SHOW_RATE_APP_DIALOG, false).apply();
    }
}

package com.eventshigh.nearme.app.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.view.View;

import com.eventshigh.nearme.app.R;

public class DialogUtils {
    private static final String PREF_SHOW_RATE_APP_DIALOG = "showRateAppDialog";

    public static void showRateAppDialog(final Activity activity) {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(
                activity);
        sharedPreferences.edit().putBoolean(PREF_SHOW_RATE_APP_DIALOG, true)
                .apply();
        if (!sharedPreferences.getBoolean(PREF_SHOW_RATE_APP_DIALOG, true)) {
            return;
        }

        View view = activity.getLayoutInflater().inflate(R.layout.dialog_rate_app, null);
        new AlertDialog.Builder(activity)
                .setView(view)
                .setPositiveButton(R.string.rate_app_now, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Uri uri = Uri.parse("market://details?id=" + activity.getPackageName());
                        activity.startActivity(new Intent(Intent.ACTION_VIEW, uri));
                    }
                })
                .setNegativeButton(R.string.rate_app_no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        sharedPreferences.edit().putBoolean(PREF_SHOW_RATE_APP_DIALOG, false)
                                .apply();
                    }
                })
                .setNeutralButton(R.string.rate_app_later, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                        // TODO: may want to do some exponential back off
                    }
                })
                .setCancelable(false)
                .show();
    }
}

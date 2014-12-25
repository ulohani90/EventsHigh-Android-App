package com.eventshigh.nearme.app.activity;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import java.util.Random;

/**
 * A placeholder {@link android.app.Activity} which is responsible for launching
 * either {@link com.eventshigh.nearme.app.activity.MapsActivity} or
 * {@link com.eventshigh.nearme.app.activity.EventGridActivity} based on user preference.
 *
 * For now, this activity sets the preference 50%-50% for first time and then use this
 * preference in future.
 */
public class LaunchActivity extends Activity {
    private static final String PREF_DEFAULT_ACTIVITY_MAPS = "eh_pref_default_activity_MAPS";
    private static final Random RANDOM = new Random(System.currentTimeMillis());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check for Google Play Services.
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (status != ConnectionResult.SUCCESS) {
            Toast.makeText(this, GooglePlayServicesUtil.getErrorString(status), Toast.LENGTH_SHORT).show();
            GooglePlayServicesUtil.getErrorDialog(status, this, 0, new OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    finish();
                }
            }).show();
            return;
        }

        // Set default activity if needed and launch.
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (!preferences.contains(PREF_DEFAULT_ACTIVITY_MAPS)) {
            Editor editor = preferences.edit();
            editor.putBoolean(PREF_DEFAULT_ACTIVITY_MAPS, RANDOM.nextBoolean());
            editor.apply();
        }

        Class target = isMapsViewDefault(preferences) ? MapsActivity.class : EventGridActivity.class;
        startActivity(new Intent(this, target));
    }

    public static boolean isMapsViewDefault(Context context) {
        return isMapsViewDefault(PreferenceManager.getDefaultSharedPreferences(context));
    }

    private static boolean isMapsViewDefault(SharedPreferences preferences) {
        return preferences.getBoolean(PREF_DEFAULT_ACTIVITY_MAPS, false);
    }
}

package com.eventshigh.nearme.app.task;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.annotation.Nullable;

import com.eventshigh.nearme.app.activity.BaseActivity;

/**
 * An {@link android.os.AsyncTask} which is used to report the network time in GA. This class also
 * reports the user connection type as event label.
 */
public class ReportTimingTask extends AsyncTask<Long, Void, Void> {
    private final BaseActivity activity;
    private final String resourceType;

    public ReportTimingTask(BaseActivity activity, String resourceType) {
        this.activity = activity;
        this.resourceType = resourceType;
    }

    @Override
    protected Void doInBackground(Long... params) {
        long timeTaken = params[0];

        String network;
        if (timeTaken <= 0) {
            network = "cache";
        } else {
            ConnectivityManager connectivityManager =
                    (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            network = getNetworkName(networkInfo);
        }

        report(network, timeTaken);
        return null;
    }

    private void report(String nwType, long time) {
        activity.reportActionToAnalytics("fetch_" + resourceType, nwType, time);
    }

    private String getNetworkName(@Nullable NetworkInfo networkInfo) {
        if (networkInfo == null || !networkInfo.isConnected()) {
            return "unknown";
        }

        return isNetworkTypeMobile(networkInfo.getType()) ?
                networkInfo.getSubtypeName() : networkInfo.getTypeName();
    }

    /**
     * Checks if a given type uses the cellular data connection.
     *
     * @param networkType the type to check
     * @return a boolean - {@code true} if uses cellular network, else {@code false}
     */
    public static boolean isNetworkTypeMobile(int networkType) {
        switch (networkType) {
            case ConnectivityManager.TYPE_MOBILE:
            case ConnectivityManager.TYPE_MOBILE_MMS:
            case ConnectivityManager.TYPE_MOBILE_SUPL:
            case ConnectivityManager.TYPE_MOBILE_DUN:
            case ConnectivityManager.TYPE_MOBILE_HIPRI:
                return true;
            default:
                return false;
        }
    }
}

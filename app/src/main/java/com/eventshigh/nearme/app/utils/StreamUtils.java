package com.eventshigh.nearme.app.utils;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Helper methods for reading stream or asserts file.
 */
public class StreamUtils {
    private static final String LOG_TAG = StreamUtils.class.getSimpleName();

    public static String[] readAssetFile(Context context, String filename) throws IOException {
        InputStream is = context.getAssets().open(filename);
        try {
            return readStream(is);
        } finally {
            is.close();
        }
    }

    public static String[] readStream(InputStream is) throws IOException {
        ArrayList<String> lines = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        while ((line = reader.readLine()) != null) {
            lines.add(line);
        }
        return lines.toArray(new String[lines.size()]);
    }

    public static JSONObject fetchJSON(String url) throws IOException, JSONException {
        Log.d(LOG_TAG, "Fetching: " + url);

        HttpURLConnection urlConnection = (HttpURLConnection) new URL(url).openConnection();
        urlConnection.setRequestMethod("GET");
        urlConnection.connect();
        try {
            StringBuilder jsonBuffer = new StringBuilder();
            for (String jsonStr : readStream(urlConnection.getInputStream())) {
                jsonBuffer.append(jsonStr);
            }
            Log.d(LOG_TAG, "Fetching completed: " + url);

            return new JSONObject(jsonBuffer.toString());
        } finally {
            urlConnection.disconnect();
        }
    }
}

package com.eventshigh.nearme.app.utils;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Helper methods for reading stream or asserts file.
 */
public class StreamUtils {
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

}

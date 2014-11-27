package com.eventshigh.nearme.app.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.ImageView;

import java.net.HttpURLConnection;
import java.net.URL;

/**
* {@link android.os.AsyncTask} which can be used to download an image and update
 * {@link android.widget.ImageView} with downloaded image bitmap.
*/
public class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
    private final ImageView bmImage;

    public DownloadImageTask(ImageView bmImage) {
        this.bmImage = bmImage;
    }

    protected Bitmap doInBackground(String... urls) {
        Bitmap image = null;
        if (urls.length > 0 && urls[0] != null) {
            try {
                HttpURLConnection urlConnection = (HttpURLConnection) new URL(urls[0]).openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();
                image = BitmapFactory.decodeStream(urlConnection.getInputStream());
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
        }
        return image;
    }

    protected void onPostExecute(@Nullable Bitmap result) {
        if (result != null) {
            bmImage.setImageBitmap(result);
        }
    }
}

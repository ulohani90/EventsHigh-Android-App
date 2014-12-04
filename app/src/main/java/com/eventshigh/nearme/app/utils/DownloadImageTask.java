package com.eventshigh.nearme.app.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.ImageView;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
* {@link android.os.AsyncTask} which can be used to download an image and update
 * {@link android.widget.ImageView} with downloaded image bitmap.
*/
public class DownloadImageTask extends AsyncTask<Void, Void, Bitmap> {
    // Note that same ImageView could be reused in ListView. We keep the map
    // of ImageView to last URL it was asked to load so that after loading
    // is over, we can update the image only if it was not reused.
    private static final Map<ImageView, URL> IMAGE_VIEW_URL_MAP =
            Collections.synchronizedMap(new WeakHashMap<ImageView, URL>());

    private final ImageView imageView;
    private final URL url;

    public static void setImage(ImageView imageView, @Nullable String url, int placeHolderImageId) {
        try {
            DownloadImageTask task = null;
            if (url != null) {
                task = new DownloadImageTask(imageView, new URL(url));
            }
            if (placeHolderImageId > 0) {
                imageView.setImageResource(placeHolderImageId);
            }
            if (task != null) {
                task.execute();
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public DownloadImageTask(ImageView imageView, URL url) {
        this.imageView = imageView;
        this.url = url;
        synchronized (IMAGE_VIEW_URL_MAP) {
            IMAGE_VIEW_URL_MAP.put(imageView, url);
        }
    }

    protected Bitmap doInBackground(Void... params) {
        try {
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();
            return BitmapFactory.decodeStream(urlConnection.getInputStream());
        } catch (Exception e) {
            Log.e(DownloadImageTask.class.getSimpleName(),
                    "Failed to load image: " + url.toString(), e);
        }

        return null;
    }

    protected void onPostExecute(@Nullable Bitmap result) {
        synchronized (IMAGE_VIEW_URL_MAP) {
            URL url = IMAGE_VIEW_URL_MAP.get(imageView);
            if (url != null && url.equals(this.url)) {
                if (result != null) {
                    imageView.setImageBitmap(result);
                }
                IMAGE_VIEW_URL_MAP.remove(imageView);
            }
        }
    }
}

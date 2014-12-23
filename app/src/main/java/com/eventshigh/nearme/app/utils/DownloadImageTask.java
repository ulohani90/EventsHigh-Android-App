package com.eventshigh.nearme.app.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
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

    // In memory cache for recent bitmap. This cache can use upto 1/8th of available
    // memory.
    private static final LruCache<URL, Bitmap> bitmapCache =
            new LruCache<URL, Bitmap>((int) (Runtime.getRuntime().maxMemory() / (8* 1024))) {
                @Override
                protected int sizeOf(URL key, Bitmap bitmap) {
                    // The cache size will be measured in kilobytes rather than
                    // number of items.
                    return bitmap.getByteCount() / 1024;
                }
            };

    public static void setImage(ImageView imageView, @Nullable String urlStr, int placeHolderImageId,
                                int width, int height) {
        try {
            DownloadImageTask task = null;
            if (urlStr != null) {
                URL url = new URL(urlStr);
                Bitmap bitmap = bitmapCache.get(url);
                if (bitmap == null) {
                    task = new DownloadImageTask(imageView, url, width, height);
                } else {
                    imageView.setImageBitmap(bitmap);
                    return;
                }
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

    private final WeakReference<ImageView> imageViewReference;
    private final URL url;
    private final int width;
    private final int height;

    public DownloadImageTask(ImageView imageView, URL url, int width, int height) {
        imageViewReference = new WeakReference<>(imageView);
        this.url = url;
        this.width = width;
        this.height = height;

        synchronized (IMAGE_VIEW_URL_MAP) {
            IMAGE_VIEW_URL_MAP.put(imageView, url);
        }
    }

    protected Bitmap doInBackground(Void... params) {
        try {
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();
            InputStream is = urlConnection.getInputStream();
            try {
                // Read Image data.
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                int nRead;
                byte[] data = new byte[16384];
                while ((nRead = is.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }
                buffer.flush();
                byte[] imageData = buffer.toByteArray();

                // See Image Dimensions and set scaling.
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(imageData, 0, imageData.length, options);
                options.inSampleSize = calculateInSampleSize(options, width, height);

                // Load Bitmap.
                options.inJustDecodeBounds = false;
                Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length, options);
                bitmapCache.put(url, bitmap);
                return bitmap;
            } finally {
                is.close();
            }
        } catch (Exception e) {
            Log.e(DownloadImageTask.class.getSimpleName(),
                    "Failed to load image: " + url.toString(), e);
        }

        return null;
    }

    protected void onPostExecute(@Nullable Bitmap result) {
        ImageView imageView = imageViewReference.get();
        if (imageView == null) {
            return;
        }

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

    private static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Set default if needed.
        if (reqHeight <= 0) {
            reqHeight = 768;   // 0.4 * 1920
        }
        if (reqWidth <= 0) {
            reqWidth = 1080;
        }

        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}

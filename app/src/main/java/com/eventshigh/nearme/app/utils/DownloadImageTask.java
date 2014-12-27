package com.eventshigh.nearme.app.utils;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import com.eventshigh.nearme.app.R;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

/**
* {@link android.os.AsyncTask} which can be used to download an image and update
 * {@link android.widget.ImageView} with downloaded image bitmap.
 *
 * Use the
 * {@link #setImage(android.widget.ImageView, android.content.res.Resources, String, int, int, int)}
 * helper method to set the image in ImageView. This method is optimized to use LRU cache
 * to store bitmap.
*/
public class DownloadImageTask extends AsyncTask<Void, Void, Bitmap> {

    public static void setImage(ImageView imageView, Resources resources,
                                @Nullable String urlStr, int placeHolderImageId,
                                int width, int height) {
        // Construct the ImgSrc from parameters
        ImageSrc src = new ImageSrc(urlStr, placeHolderImageId);

        // Check if some other task is running to load image in ImageView. If yes,
        // check if the other task is loading the same image as requested by this method.
        // In case old task is running for different image, we cancel that task.
        DownloadImageTask oldTask = getDownloadImageTask(imageView);
        if (oldTask != null) {
            if (oldTask.src.equals(src)) {
                // Image loading task is already in progress with same image
                return;
            } else {
                // cancel the old task. new task with new image will be instantiated
                oldTask.cancel(true);
            }
        }

        // Try to fetch the bitmap from cache. If the bitmap is already cached, we will
        // set the bitmap from cache and we are done.
        Bitmap bitmap = bitmapCache.get(urlStr == null ? src : new ImageSrc(urlStr, -1));
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
            return;
        }

        // We need to start new background task to load the bitmap. We now check if we have
        // cached bitmap for placeholder image. If not, we set the eh_default as placeholder
        // image.
        Bitmap placeholderBitmap = bitmapCache.get(new ImageSrc(null, placeHolderImageId));
        if (placeholderBitmap == null) {
            placeholderBitmap = bitmapCache.get(new ImageSrc(null, R.drawable.eh_default));
            if (placeholderBitmap == null) {
                placeholderBitmap = BitmapFactory.decodeResource(resources, R.drawable.eh_default);
            }
        } else {
            src = new ImageSrc(urlStr, -1);
        }
        DownloadImageTask task = new DownloadImageTask(imageView, resources, src, width, height);
        imageView.setImageDrawable(new AsyncDrawable(resources, placeholderBitmap, task));
        task.execute();
    }

    // Class used to store the image src. Image source can have URL from the web
    // as source and a placeholder image resource in case there is no URL or if
    // it is temporary unavailable.
    private static class ImageSrc {
        private final @Nullable String imgUrl;
        private final int placeHolderImageId;

        ImageSrc(@Nullable String imgUrl, int placeHolderImageId) {
            this.imgUrl = imgUrl;
            this.placeHolderImageId = placeHolderImageId;
        }

        public int hashCode() {
            return toString().hashCode();
        }

        public boolean equals(Object other) {
            if (!(other instanceof ImageSrc)) {
                return false;
            }

            ImageSrc another = ((ImageSrc) other);
            if (placeHolderImageId != another.placeHolderImageId) {
                return false;
            }

            if (imgUrl == null) {
                return another.imgUrl == null;
            }

            return another.imgUrl != null && imgUrl.equals(another.imgUrl);
        }

        public String toString() {
            return getClass().getSimpleName() + " [ " + imgUrl + ", " + placeHolderImageId + " ]";
        }
    }

    // In memory cache for recent bitmap. This cache can use upto 16% of available
    // memory. This cache is used to store bitmap for images fetched from internet as
    // we as for images fetched from resources.
    private static final LruCache<ImageSrc, Bitmap> bitmapCache =
            new LruCache<ImageSrc, Bitmap>((int) (Runtime.getRuntime().maxMemory() / (6 * 1024))) {
                @Override
                protected int sizeOf(ImageSrc key, Bitmap bitmap) {
                    // The cache size will be measured in kilobytes rather than
                    // number of items.
                    return bitmap.getByteCount() / 1024;
                }
            };

    private static class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<DownloadImageTask> DownloadImageTaskReference;

        public AsyncDrawable(Resources res, Bitmap bitmap,
                             DownloadImageTask downloadImageTask) {
            super(res, bitmap);
            DownloadImageTaskReference = new WeakReference<>(downloadImageTask);
        }

        public DownloadImageTask getDownloadImageTask() {
            return DownloadImageTaskReference.get();
        }
    }

    private static DownloadImageTask getDownloadImageTask(@Nullable ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable != null && drawable instanceof AsyncDrawable) {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getDownloadImageTask();
            }
        }
        return null;
    }

    private final WeakReference<ImageView> imageViewReference;
    private final Resources resources;
    private final ImageSrc src;
    private final int width;
    private final int height;

    private DownloadImageTask(ImageView imageView, Resources resources,
                              ImageSrc src, int width, int height) {
        imageViewReference = new WeakReference<>(imageView);
        this.resources = resources;
        this.src = src;
        this.width = width;
        this.height = height;
    }

    protected Bitmap doInBackground(Void... params) {
        // Try to load the image from URL if its present.
        if (src.imgUrl != null) {
            try {
                HttpURLConnection urlConnection =
                        (HttpURLConnection) new URL(src.imgUrl).openConnection();
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
                    bitmapCache.put(new ImageSrc(src.imgUrl, -1), bitmap);
                    return bitmap;
                } finally {
                    is.close();
                }
            } catch (Exception e) {
                Log.d(DownloadImageTask.class.getSimpleName(),
                        "Failed to load image: " + src.imgUrl, e);
            }
        }

        // Try to load for placeholder.
        if (src.placeHolderImageId > 0) {
            // See Image Dimensions and set scaling.
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeResource(resources, src.placeHolderImageId, options);
            options.inSampleSize = calculateInSampleSize(options, width, height);

            // Load Bitmap.
            options.inJustDecodeBounds = false;
            Bitmap bitmap = BitmapFactory.decodeResource(resources, src.placeHolderImageId, options);
            bitmapCache.put(new ImageSrc(null, src.placeHolderImageId), bitmap);
            return bitmap;
        }

        return null;
    }

    protected void onPostExecute(@Nullable Bitmap result) {
        ImageView imageView = imageViewReference.get();
        if (isCancelled() || imageView == null || result == null ||
                getDownloadImageTask(imageView) != this) {
            return;
        }

        imageView.setImageBitmap(result);
    }

    private static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Set default if needed.
        if (reqWidth <= 0) {
            reqWidth = 1080;
        }
        if (reqHeight <= 0) {
            reqHeight = 768;   // 0.4 * 1920
        }

        int sampleSize = (int) Math.round(Math.sqrt(
                options.outWidth * options.outHeight / (reqWidth * reqHeight)));
        if (sampleSize < 1) {
            sampleSize = 1;
        }
        return sampleSize;
    }
}

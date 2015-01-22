package com.eventshigh.nearme.app.network;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.LruCache;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

/**
 * Volley Helper which provide the simple methods to manage VolleyRequestQueue
 * and submit requests in queue for parallel processing.
 */
public class VolleyHelper {

    private static VolleyHelper instance;
    public static synchronized VolleyHelper getInstance(Context context) {
        if (instance == null) {
            instance = new VolleyHelper(context);
        }
        return instance;
    }

    private final RequestQueue requestQueue;
    private final ImageLoader imageLoader;

    private VolleyHelper(Context context) {
        requestQueue = Volley.newRequestQueue(context.getApplicationContext());

        // In memory cache for recent bitmap. This cache can use upto 16% of available
        // memory. This cache is used to store bitmap for images fetched from internet as
        // we as for images fetched from resources.
        imageLoader = new ImageLoader(requestQueue, new ImageLoader.ImageCache() {
            private final LruCache<String, Bitmap> cache =
                    new LruCache<String, Bitmap>((int) (Runtime.getRuntime().maxMemory() / (6 * 1024))) {
                @Override
                protected int sizeOf(String key, Bitmap bitmap) {
                    // The cache size will be measured in kilobytes rather than
                    // number of items.
                    return bitmap.getByteCount() / 1024;
                }
            };

            @Override
            public Bitmap getBitmap(String url) {
                return cache.get(url);
            }

            @Override
            public void putBitmap(String url, Bitmap bitmap) {
                cache.put(url, bitmap);
            }
        });
    }

    public static RequestQueue getRequestQueue(Context context) {
        return getInstance(context).requestQueue;
    }

    public static <T> void addToRequestQueue(Context context, Request<T> req) {
        getRequestQueue(context).add(req);
    }

    public static ImageLoader getImageLoader(Context context) {
        return getInstance(context).imageLoader;
    }
}

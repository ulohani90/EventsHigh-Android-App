package com.eventshigh.nearme.app.utils;

import android.content.Context;
import android.graphics.Typeface;
import android.widget.TextView;

public class FontUtils {
    // private static Typeface fontQuicksand;
    private static Typeface fontQuicksandBold;

    public static boolean loadedFonts = false;

    private static synchronized void loadFonts(Context context) {
        if (!loadedFonts) {
            // fontQuicksand = Typeface.createFromAsset(getAssets(), "Quicksand-Regular.ttf");
            fontQuicksandBold = Typeface.createFromAsset(context.getAssets(), "Quicksand-Bold.ttf");
            loadedFonts = true;
        }
    }

    public static void setTypefaceQuicksandBold(final TextView textView) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                loadFonts(textView.getContext());
                textView.post(new Runnable() {
                    @Override
                    public void run() {
                        textView.setTypeface(fontQuicksandBold);
                    }
                });
            }
        }).start();
    }
}

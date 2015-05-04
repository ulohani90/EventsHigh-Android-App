package com.eventshigh.nearme.app.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.v7.graphics.Palette;
import android.util.AttributeSet;
import android.view.View;

import com.android.volley.toolbox.NetworkImageView;
import com.eventshigh.nearme.app.R;

public class PaletteImageView extends NetworkImageView {
    private int defaultColor;
    private View headerView;

    public PaletteImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        defaultColor = context.getResources().getColor(R.color.gray500);
    }

    @Override
    public void setImageBitmap(Bitmap bitmap) {
        super.setImageBitmap(bitmap);
        if (headerView != null && bitmap != null) {
            updateHeader(bitmap);
        }
    }

    private void updateHeader(final Bitmap bitmap) {
        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                return Palette.from(bitmap).generate().getVibrantColor(defaultColor);
            }

            @Override
            protected void onPostExecute(Integer color) {
                headerView.setBackgroundColor(color);
            }
        }.execute();
    }

    public void setHeaderView(View headerView) {
        this.headerView = headerView;
    }
}

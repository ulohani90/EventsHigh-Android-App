package com.eventshigh.nearme.app.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.utils.ImageUtils;

public class CircularImageView extends ImageView {
    private boolean useCircularImage = true;

    public CircularImageView(Context context) {
        super(context);
    }

    public CircularImageView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray typedArray = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.CircularImageView, 0, 0);
        useCircularImage = typedArray.getBoolean(R.styleable.CircularImageView_circular, true);
    }

    public CircularImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        if (useCircularImage) {
            bm = ImageUtils.getCircularBitmapFrom(bm);
        }
        super.setImageBitmap(bm);
    }
}

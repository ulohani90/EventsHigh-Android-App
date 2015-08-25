package com.eventshigh.nearme.app.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.animation.AlphaAnimation;

import com.android.volley.toolbox.NetworkImageView;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.utils.ImageUtils;

public class FadeInNetworkImageView extends NetworkImageView {
    private static final int ANIMATION_DURATION = 1500;
    private boolean useCircularImage = false;

    public FadeInNetworkImageView(Context context) {
        super(context);
    }

    public FadeInNetworkImageView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray typedArray = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.FadeInNetworkImageView, 0, 0);
        useCircularImage = typedArray.getBoolean(R.styleable.FadeInNetworkImageView_circular, false);
    }

    public FadeInNetworkImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        if (useCircularImage) {
            bm = ImageUtils.getCircularBitmapFrom(bm);
        }
        super.setImageBitmap(bm);

        AlphaAnimation fadeInAnimation = new AlphaAnimation(0f, 1f);
        fadeInAnimation.setDuration(ANIMATION_DURATION);
        startAnimation(fadeInAnimation);
    }
}

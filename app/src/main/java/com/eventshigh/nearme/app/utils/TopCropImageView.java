package com.eventshigh.nearme.app.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by chris on 7/27/16.
 */
public class TopCropImageView extends ImageView {

    public TopCropImageView(Context context) {
        super(context);
        setScaleType(ScaleType.MATRIX);
        init();
    }

    public TopCropImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setScaleType(ScaleType.MATRIX);
        init();
    }

    public TopCropImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setScaleType(ScaleType.MATRIX);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public TopCropImageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setScaleType(ScaleType.MATRIX);
        init();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override

    protected boolean setFrame(int l, int t, int r, int b) {
        Matrix matrix = getImageMatrix();
        float scale;
        int viewWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        int viewHeight = getHeight() - getPaddingTop() - getPaddingBottom();
        int drawableWidth = getDrawable().getIntrinsicWidth();
        int drawableHeight = getDrawable().getIntrinsicHeight();

        //Get the scale
        if (drawableWidth * viewHeight > drawableHeight * viewWidth) {
            scale = (float) viewHeight / (float) drawableHeight;
        } else {
            scale = (float) viewWidth / (float) drawableWidth;
        }

        //Define the rect to take image portion from
        RectF drawableRect = new RectF(0, drawableHeight - (viewHeight / scale), drawableWidth, drawableHeight);
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        matrix.setRectToRect(drawableRect, viewRect, Matrix.ScaleToFit.FILL);


        setImageMatrix(matrix);

        return super.setFrame(l, t, r, b);
    }

    private void init() {
        setScaleType(ScaleType.MATRIX);
    }


}
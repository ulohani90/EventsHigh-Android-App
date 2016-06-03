package com.eventshigh.nearme.app.ui.animation;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;

/**
 * Created by umesh on 31/05/16.
 */
public class ResizeAnimation extends Animation {
    final int startHeight;
    final int targetHeight;
    View view;

    public ResizeAnimation(View view, int targetWidth) {
        this.view = view;
        this.targetHeight = targetWidth;
        startHeight = view.getHeight();
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        int newHeight = (int) (startHeight + (targetHeight - startHeight) * interpolatedTime);
        view.getLayoutParams().height = newHeight;
        view.requestLayout();
    }

    @Override
    public void initialize(int width, int height, int parentWidth, int parentHeight) {
        super.initialize(width, height, parentWidth, parentHeight);
    }

    @Override
    public boolean willChangeBounds() {
        return true;
    }
}
package com.eventshigh.nearme.app.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.animation.AlphaAnimation;

import com.android.volley.toolbox.NetworkImageView;

public class FadeInNetworkImageView extends NetworkImageView {
  private static final int ANIMATION_DURATION = 300;

  public FadeInNetworkImageView(Context context) {
    super(context);
  }

  public FadeInNetworkImageView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public FadeInNetworkImageView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  @Override
  public void setImageBitmap(final Bitmap bm) {
    super.setImageBitmap(bm);
    AlphaAnimation fadeInAnimation = new AlphaAnimation(0f, 1f);
    fadeInAnimation.setDuration(ANIMATION_DURATION);
    startAnimation(fadeInAnimation);
  }
}
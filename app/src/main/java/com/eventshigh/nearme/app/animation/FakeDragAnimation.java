package com.eventshigh.nearme.app.animation;

import android.support.v4.view.ViewPager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Transformation;

/**
 * {@link android.support.v4.view.ViewPager} drag animation.
 */
public class FakeDragAnimation extends Animation {

  private final ViewPager viewPager;
  private final int dragAmount;

  private boolean dragging;
  private int currentDrag;

  public FakeDragAnimation(ViewPager viewPager, int dragAmount) {
    this.viewPager = viewPager;
    this.dragAmount = dragAmount;

    setInterpolator(new AccelerateDecelerateInterpolator());
  }

  @Override
  protected void applyTransformation(float interpolatedTime, Transformation t) {
    super.applyTransformation(interpolatedTime, t);

    if (interpolatedTime == 0.0 && !dragging) {
      dragging = true;
      viewPager.beginFakeDrag();
    }

    if (dragging) {
      int drag = (int) (dragAmount * interpolatedTime);
      viewPager.fakeDragBy(drag - currentDrag);
      currentDrag = drag;
    }

    if (interpolatedTime == 1.0 && dragging) {
      dragging = false;
      viewPager.endFakeDrag();
    }
  }
}

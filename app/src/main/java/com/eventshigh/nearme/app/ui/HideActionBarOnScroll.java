package com.eventshigh.nearme.app.ui;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnScrollListener;

import com.eventshigh.nearme.app.activity.BaseContextActivity;

/**
 * OnScrollListener which is used to hid the action bar of an activity.
 */
public class HideActionBarOnScroll extends OnScrollListener {
    private final BaseContextActivity activity;

    private int currentY;
    private boolean actionBarShown = true;

    public HideActionBarOnScroll(BaseContextActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
        super.onScrollStateChanged(recyclerView, newState);
    }

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);

        if (currentY * dy > 0) {
            currentY += dy;
        } else {
            currentY = dy;
        }

        if (currentY > 250 || currentY < -250) {
            boolean isDown = dy > 0;
            if (isDown && actionBarShown) {
                activity.hideActionBar();
                actionBarShown = false;
            }

            if (!isDown && !actionBarShown) {
                activity.showActionBar();
                actionBarShown = true;
            }
        }
    }
}

package com.eventshigh.nearme.app.view;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class SpacesItemDecoration extends RecyclerView.ItemDecoration {
    private int horizontalSpacing;
    private int verticalSpacing;

    public SpacesItemDecoration(int horizontalSpacing, int verticalSpacing) {
        this.horizontalSpacing = horizontalSpacing;
        this.verticalSpacing = verticalSpacing;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        outRect.left = horizontalSpacing;
        outRect.right = horizontalSpacing;
        outRect.bottom = verticalSpacing;
        outRect.top = verticalSpacing;
    }
}

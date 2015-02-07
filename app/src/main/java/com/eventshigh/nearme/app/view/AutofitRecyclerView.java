package com.eventshigh.nearme.app.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

public class AutofitRecyclerView extends RecyclerView {
    private GridLayoutManager gridLayoutManager;
    private int columnWidth;

    public AutofitRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (attrs != null) {
            int[] attrsArray = {
                android.R.attr.columnWidth
            };
            TypedArray array = context.obtainStyledAttributes(
                attrs, attrsArray);
            columnWidth = array.getDimensionPixelSize(0, -1);
            array.recycle();
        }

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        setHasFixedSize(true);

        // use a grid layout manager
        gridLayoutManager = new GridLayoutManager(context, 1);
        setLayoutManager(gridLayoutManager);
    }

    protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, heightSpec);
        if (columnWidth > 0) {
            int spanCount = Math.max(1, getMeasuredWidth() / columnWidth);
            gridLayoutManager.setSpanCount(spanCount);
        }
    }
}

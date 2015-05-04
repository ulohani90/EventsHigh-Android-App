package com.eventshigh.nearme.app.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.GridLayoutManager.SpanSizeLookup;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

import com.eventshigh.nearme.app.ui.EventsAdapter;

public class AutofitRecyclerView extends RecyclerView {
    private GridLayoutManager gridLayoutManager;
    private int columnWidth;
    private EventsAdapter eventsAdapter;
    private int horizontalSpacing = 0;
    private int verticalSpacing = 0;

    public AutofitRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (attrs != null) {
            columnWidth = getDimensionPixelSize(context, attrs, android.R.attr.columnWidth, -1);
            horizontalSpacing = getDimensionPixelSize(context, attrs,
                    android.R.attr.horizontalSpacing, -1);
            verticalSpacing = getDimensionPixelSize(context, attrs,
                    android.R.attr.verticalSpacing, -1);
        }

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        setHasFixedSize(true);

        // use a grid layout manager
        gridLayoutManager = new GridLayoutManager(context, 1);
        setLayoutManager(gridLayoutManager);
        addItemDecoration(spacesItemDecoration);
    }

    protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, heightSpec);
        if (columnWidth > 0) {
            int spanCount = Math.max(1, getMeasuredWidth() / columnWidth);
            gridLayoutManager.setSpanCount(spanCount);
        }
    }

    private int getDimensionPixelSize(Context context, AttributeSet attributeSet, int attr,
                                      int defaultValue) {
        int[] attrsArray = { attr };
        TypedArray array = context.obtainStyledAttributes(attributeSet, attrsArray);
        int value = array.getDimensionPixelSize(0, defaultValue);
        array.recycle();
        return value;
    }

    public int getSpanCount() {
        return gridLayoutManager.getSpanCount();
    }

    public void setEventsAdapter(EventsAdapter eventsAdapter) {
        this.eventsAdapter = eventsAdapter;

        gridLayoutManager.setSpanSizeLookup(mSpanSizeLookup);
        setAdapter(eventsAdapter);
    }

    private SpanSizeLookup mSpanSizeLookup = new SpanSizeLookup() {
        @Override
        public int getSpanSize(int position) {
            return (eventsAdapter != null && eventsAdapter.spanAllColumns(position)) ? getSpanCount() : 1;
        }
    };

    private RecyclerView.ItemDecoration spacesItemDecoration = new RecyclerView.ItemDecoration() {
        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            outRect.left = 0;
            outRect.right = getSpanCount() > 1 ? horizontalSpacing : 0;
            outRect.bottom = verticalSpacing;
            outRect.top = 0;
        }
    };
}

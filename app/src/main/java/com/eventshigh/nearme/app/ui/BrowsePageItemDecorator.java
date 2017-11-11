package com.eventshigh.nearme.app.ui;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.eventshigh.nearme.app.ui.adapter.DataType;

/**
 * Created by umesh on 09/11/17.
 */

public class BrowsePageItemDecorator extends RecyclerView.ItemDecoration {

    int sideSpace;
    int verticalSpace;
    int bottomTopSpace;

    public BrowsePageItemDecorator(int sideSpace, int verticalSpace, int bottomTopSpace) {
        this.sideSpace = sideSpace;
        this.verticalSpace = verticalSpace;
        this.bottomTopSpace = bottomTopSpace;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                               RecyclerView.State state) {
        if (parent.getChildAdapterPosition(view) == 0) {
            outRect.top = bottomTopSpace;
            outRect.bottom = 0;
            outRect.left = 0;
            outRect.right = 0;
        } else if (parent.getAdapter().getItemViewType(parent.getChildAdapterPosition(view)) == DataType.BROWSE_HEADER_CARD.typeId) {
            outRect.top = 0;
            outRect.bottom = 0;
            outRect.left = 0;
            outRect.right = 0;
        } else if (parent.getAdapter().getItemViewType(parent.getChildAdapterPosition(view)) == DataType.BROWSE_SPONSORED_EVENTS.typeId) {
            outRect.top = sideSpace;
            outRect.bottom = sideSpace;
            outRect.left = 0;
            outRect.right = 0;
        } else if (parent.getChildAdapterPosition(view) == parent.getAdapter().getItemCount() - 1) {
            outRect.top = sideSpace;
            outRect.bottom = bottomTopSpace;
            outRect.left = sideSpace;
            outRect.right = sideSpace;
        } else if (DataType.spanAllColumns(parent.getAdapter().getItemViewType(parent.getChildAdapterPosition(view)))) {
            outRect.top = sideSpace;
            outRect.bottom = 0;
            outRect.left = sideSpace;
            outRect.right = sideSpace;
        } else {
            if (parent.getChildAdapterPosition(view) % 2 == 0) {
                outRect.top = 0;
                outRect.bottom = 2;
                outRect.left = 0;
                outRect.right = 1;
            } else {
                outRect.top = 0;
                outRect.bottom = 2;
                outRect.left = 1;
                outRect.right = 0;
            }
        }

    }


}

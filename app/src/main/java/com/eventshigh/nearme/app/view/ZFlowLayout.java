package com.eventshigh.nearme.app.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.eventshigh.nearme.app.R;


/**
 * User: Romain Guy
 * <p/>
 * Using example: <?xml version="4.0" encoding="utf-8"?>
 * <com.example.android.layout.FlowLayout
 * xmlns:f="http://schemas.android.com/apk/res/org.apmem.android"
 * xmlns:android="http://schemas.android.com/apk/res/android"
 * f:horizontalSpacing="6dip" f:verticalSpacing="12dip"
 * android:layout_width="wrap_content" android:layout_height="wrap_content"
 * android:paddingLeft="6dip" android:paddingTop="6dip"
 * android:paddingRight="12dip"> <Button android:layout_width="wrap_content"
 * android:layout_height="wrap_content" f:layout_horizontalSpacing="32dip"
 * f:layout_breakLine="true" android:text="Cancel" />
 * <p/>
 * </com.example.android.layout.FlowLayout>
 */
public class ZFlowLayout extends ViewGroup {

    public static final int HORIZONTAL = 0;
    public static final int VERTICAL = 1;

    private int horizontalSpacing = 0;
    private int verticalSpacing = 0;
    @SuppressWarnings("unused")
    private int orientation = 0;

    private int line_height;


    public ZFlowLayout(Context context) {
        super(context);
        this.readStyleParameters(context, null);
    }

    public ZFlowLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.readStyleParameters(context, attributeSet);
    }

    public ZFlowLayout(Context context, AttributeSet attributeSet, int defStyle) {
        super(context, attributeSet, defStyle);
        this.readStyleParameters(context, attributeSet);
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        assert (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED);

        final int width = MeasureSpec.getSize(widthMeasureSpec)
                - getPaddingLeft() - getPaddingRight();
        int height = MeasureSpec.getSize(heightMeasureSpec) - getPaddingTop()
                - getPaddingBottom();
        final int count = getChildCount();
        int line_height = 0;

        int xpos = getPaddingLeft();
        int ypos = getPaddingTop();

        int childHeightMeasureSpec;
        if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.AT_MOST) {
            childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(height,
                    MeasureSpec.AT_MOST);
        } else {
            childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(0,
                    MeasureSpec.UNSPECIFIED);
        }

        for (int i = 0; i < count; i++) {

            final View child = getChildAt(i);

            if (child.getVisibility() != GONE) {

                // final LayoutParam lp = (LayoutParam)child.getLayoutParams();
                child.measure(
                        MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST),
                        childHeightMeasureSpec);

                final int childw = child.getMeasuredWidth();
                line_height = Math.max(line_height, child.getMeasuredHeight()
                        + verticalSpacing);

                if (xpos + childw > width) {
                    xpos = getPaddingLeft();
                    ypos += line_height;
                }

                xpos += childw + horizontalSpacing;

            }

        }
        this.line_height = line_height;

        if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            height = ypos + line_height;
        } else if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.AT_MOST) {

            if (ypos + line_height < height) {
                height = ypos + line_height;
            }

        }

        if (measureActualHeight)
            actualHeight = height;
        setMeasuredDimension(width, height);


    }

    int actualHeight;

    boolean measureActualHeight;

    public void setMeasureActualHeight(boolean measureActualHeight) {
        this.measureActualHeight = measureActualHeight;
    }

    public int getActualHeight() {
        return actualHeight;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        final int count = getChildCount();
        final int width = r - l;
        int xpos = getPaddingLeft();
        int ypos = getPaddingTop();

        for (int i = 0; i < count; i++) {

            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {

                final int childw = child.getMeasuredWidth();
                final int childh = child.getMeasuredHeight();
                if (xpos + childw > width) {
                    xpos = getPaddingLeft();
                    ypos += line_height;
                }
                child.layout(xpos, ypos, xpos + childw, ypos + childh);
                xpos += childw + horizontalSpacing;

            }

        }

    }

    @Override
    public LayoutParam generateLayoutParams(AttributeSet attributeSet) {
        return new LayoutParam(getContext(), attributeSet);
    }

    @Override
    protected LayoutParam generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParam(p);
    }

    private void readStyleParameters(Context context, AttributeSet attributeSet) {

        TypedArray a = context.obtainStyledAttributes(attributeSet,
                R.styleable.ZFlowLayout);
        try {
            horizontalSpacing = a.getDimensionPixelSize(
                    R.styleable.ZFlowLayout_horizontalSpacing, 0);
            verticalSpacing = a.getDimensionPixelSize(
                    R.styleable.ZFlowLayout_verticalSpacing, 0);
            orientation = a.getInteger(R.styleable.ZFlowLayout_orientation,
                    HORIZONTAL);
        } finally {
            a.recycle();
        }

    }

    public static class LayoutParam extends ViewGroup.LayoutParams {
        private static int NO_SPACING = -1;

        @SuppressWarnings("unused")
        private int x;
        @SuppressWarnings("unused")
        private int y;
        private int horizontalSpacing = NO_SPACING;
        private int verticalSpacing = NO_SPACING;
        @SuppressWarnings("unused")
        private boolean newLine = false;

        public LayoutParam(Context context, AttributeSet attributeSet) {
            super(context, attributeSet);
            this.readStyleParameters(context, attributeSet);
        }

        public LayoutParam(int width, int height) {
            super(width, height);
        }

        public LayoutParam(ViewGroup.LayoutParams layoutParams) {
            super(layoutParams);
        }

        public boolean horizontalSpacingSpecified() {
            return horizontalSpacing != NO_SPACING;
        }

        public boolean verticalSpacingSpecified() {
            return verticalSpacing != NO_SPACING;
        }

        public void setPosition(int x, int y) {
            this.x = x;
            this.y = y;
        }

        private void readStyleParameters(Context context,
                                         AttributeSet attributeSet) {

            TypedArray a = context.obtainStyledAttributes(attributeSet,
                    R.styleable.FlowLayout_LayoutParams);
            try {
                horizontalSpacing = a
                        .getDimensionPixelSize(
                                R.styleable.FlowLayout_LayoutParams_layout_horizontalSpacing,
                                NO_SPACING);
                verticalSpacing = a
                        .getDimensionPixelSize(
                                R.styleable.FlowLayout_LayoutParams_layout_verticalSpacing,
                                NO_SPACING);
                newLine = a.getBoolean(
                        R.styleable.FlowLayout_LayoutParams_layout_newLine,
                        false);
            } finally {
                a.recycle();
            }

        }

    }

}
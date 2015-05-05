package com.eventshigh.nearme.app.view;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;

/**
 * A custom {@link Drawable} which is used to draw slanted rectangular background.
 */
public class SlantedDrawable extends Drawable {
    private int color;
    private int offsetY;

    public void setColor(int color) {
        this.color = color;
    }

    public void setOffsetY(int offsetY) {
        this.offsetY = offsetY;
    }

    @Override
    public void draw(Canvas canvas) {
        int width = this.getBounds().width();
        int height = this.getBounds().height();

        Path path = new Path();
        Paint paint = new Paint();
        path.moveTo(0, 0);
        path.lineTo(width, offsetY);
        path.lineTo(width, height);
        path.lineTo(0, height);
        path.close();

        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawPath(path, paint);
    }

    @Override
    public void setAlpha(int alpha) {
        // not supported.
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        // not supported.
    }

    @Override
    public int getOpacity() {
        return 0;
    }
}

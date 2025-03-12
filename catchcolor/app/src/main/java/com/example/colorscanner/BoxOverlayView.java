package com.example.colorscanner;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

public class BoxOverlayView extends View {
    private Paint paint;
    private Rect boxRect;
    private static final float BOX_SIZE_PERCENTAGE = 0.25f; 

    public BoxOverlayView(Context context) {
        super(context);
        init();
    }

    public BoxOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        int smallestDimension = Math.min(w, h);
        int boxSize = (int) (smallestDimension * BOX_SIZE_PERCENTAGE);

        int left = (w - boxSize) / 2;
        int top = (h - boxSize) / 2;
        int right = left + boxSize;
        int bottom = top + boxSize;

        boxRect = new Rect(left, top, right, bottom);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (boxRect != null) {
            canvas.drawRect(boxRect, paint);
            int centerX = boxRect.centerX();
            int centerY = boxRect.centerY();
            int crosshairSize = boxRect.width() / 8;
            canvas.drawLine(centerX - crosshairSize, centerY, centerX + crosshairSize, centerY, paint);
            canvas.drawLine(centerX, centerY - crosshairSize, centerX, centerY + crosshairSize, paint);
        }
    }

    public Rect getBoxRect() {
        return boxRect;
    }
}

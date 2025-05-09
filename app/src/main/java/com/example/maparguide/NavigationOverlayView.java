package com.example.maparguide;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;

/**
 * Кастомний View для відображення анімованої стрілки та тексту навігації.
 */
public class NavigationOverlayView extends View {
    private Paint arrowPaint, textPaint, bgPaint;
    private Path arrowPath;
    private float scaleFactor = 1.0f;
    private float arrowAngle = 0f;
    private String instruction = "Поверніть праворуч через 50 м";

    public NavigationOverlayView(Context context) {
        super(context);
        init();
    }

    public NavigationOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        arrowPaint = new Paint();
        arrowPaint.setColor(Color.BLUE);
        arrowPaint.setStyle(Paint.Style.FILL);
        arrowPaint.setAntiAlias(true);
        arrowPaint.setShadowLayer(10, 0, 0, Color.BLACK);

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(50);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setAntiAlias(true);
        textPaint.setShadowLayer(5, 0, 0, Color.BLACK);

        bgPaint = new Paint();
        bgPaint.setColor(Color.parseColor("#80000000"));
        bgPaint.setStyle(Paint.Style.FILL);

        arrowPath = new Path();

        post(() -> {
            new Thread(() -> {
                while (true) {
                    scaleFactor = scaleFactor == 1.0f ? 1.1f : 1.0f;
                    postInvalidate();
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        });
    }

    public void setArrowAngle(float angle) {
        this.arrowAngle = angle;
        invalidate();
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawRect(0, 0, getWidth(), 150, bgPaint);

        float textWidth = textPaint.measureText(instruction);
        canvas.drawText(instruction, (getWidth() - textWidth) / 2, 100, textPaint);

        arrowPath.reset();
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        float arrowSize = 100 * scaleFactor;

        canvas.save();
        canvas.rotate(arrowAngle, centerX, centerY);
        arrowPath.moveTo(centerX - arrowSize, centerY - arrowSize / 2);
        arrowPath.lineTo(centerX, centerY - arrowSize / 2);
        arrowPath.lineTo(centerX, centerY - arrowSize);
        arrowPath.lineTo(centerX + arrowSize, centerY);
        arrowPath.lineTo(centerX, centerY + arrowSize);
        arrowPath.lineTo(centerX, centerY + arrowSize / 2);
        arrowPath.lineTo(centerX - arrowSize, centerY + arrowSize / 2);
        arrowPath.close();
        canvas.drawPath(arrowPath, arrowPaint);
        canvas.restore();
    }
}
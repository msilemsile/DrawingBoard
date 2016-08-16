package com.msile.android.views.drawingboard;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Build;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * 简易画板
 * Created by msile on 2016/8/14.
 */
public class DrawingBoard extends View {

    private Paint mDrawPaint;
    private Paint mClearPaint;
    private Path mDrawPath;
    private Path mClearPath;
    private int color = Color.RED;
    private float mDownX, mDownY;
    private int mode;
    private static final int MODE_DRAW = 0;
    private static final int MODE_CLEAR = -1;
    private Bitmap mTempBitmap;
    private Canvas mTempCanvas;

    public DrawingBoard(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DrawingBoard(Context context) {
        this(context, null);
    }

    private void init() {
        mDrawPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mDrawPaint.setColor(color);
        mDrawPaint.setStyle(Paint.Style.STROKE);
        mDrawPaint.setStrokeWidth(10);
        mDrawPath = new Path();

        mClearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mClearPaint.setStyle(Paint.Style.STROKE);
        mClearPaint.setColor(Color.WHITE);
        mClearPaint.setStrokeWidth(20);
        mClearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        mClearPath = new Path();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            setLayerType(LAYER_TYPE_SOFTWARE, null);
        }
    }

    public void setClearMode() {
        mode = MODE_CLEAR;
    }

    public void reset() {
        mode = MODE_DRAW;
    }

    public void clearScreen() {
        mTempCanvas.drawColor(Color.WHITE);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mTempBitmap == null) {
            mTempBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.RGB_565);
            mTempCanvas = new Canvas(mTempBitmap);
            mTempCanvas.drawColor(Color.WHITE);
        }
        canvas.drawBitmap(mTempBitmap, 0, 0, null);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = MotionEventCompat.getActionMasked(event);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mDownX = event.getX();
                mDownY = event.getY();
                if (mode == MODE_DRAW) {
                    mDrawPath.reset();
                    mDrawPath.moveTo(mDownX, mDownY);
                    mTempCanvas.drawPath(mDrawPath, mDrawPaint);
                } else if (mode == MODE_CLEAR) {
                    mClearPath.reset();
                    mClearPath.moveTo(mDownX, mDownY);
                    mTempCanvas.drawPath(mClearPath, mClearPaint);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                float moveX = event.getX();
                float moveY = event.getY();
                if (mode == MODE_DRAW) {
                    mDrawPath.lineTo(moveX, moveY);
                    mTempCanvas.drawPath(mDrawPath, mDrawPaint);
                } else if (mode == MODE_CLEAR) {
                    mClearPath.lineTo(moveX, moveY);
                    mTempCanvas.drawPath(mClearPath, mClearPaint);
                }
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                mDrawPath.reset();
                mClearPath.reset();
                break;
        }
        return true;
    }
}

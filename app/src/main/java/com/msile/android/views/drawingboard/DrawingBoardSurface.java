package com.msile.view.canvas;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * 简易画板
 * Created by msile on 2016/8/14.
 */
public class DrawingBoardSurface extends SurfaceView implements SurfaceHolder.Callback {

    private Paint mDrawPaint;                       //画笔
    private Paint mClearPaint;                      //橡皮
    private Path mDrawPath;                         //画笔路径
    private Path mClearPath;                        //橡皮路径
    private int mDrawRadius = 10;                   //画笔半径
    private int mClearRadius = 88;                  //橡皮半径
    private int mDrawColor = Color.RED;             //画笔颜色
    private int mBoardColor = Color.WHITE;          //画板颜色

    private int mDownX, mDownY;
    private int mMoveX, mMoveY;
    private int mLastDistance;

    private int mDrawOperate;                       //当前绘制操作
    public static final int OPERATE_DRAW = 0;       //画笔操作
    public static final int OPERATE_CLEAR = -1;     //橡皮操作

    private Bitmap mTempBitmap;
    private Canvas mTempCanvas;
    private Canvas mSurfaceCanvas;                  //画布

    private int mDrawShape;                         //绘制图形
    public static final int SHAPE_PATH = 0;         //路径
    public static final int SHAPE_CIRCLE = 1;       //圆环
    public static final int SHAPE_RECTANGLE = 2;    //矩形
    public static final int SHAPE_CIRCLE_POINT = 4; //圆点

    private SurfaceHolder holder;
    private boolean hasReady;

    public DrawingBoardSurface(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DrawingBoardSurface(Context context) {
        this(context, null);
    }

    private void init() {
        //初始化画笔
        mDrawPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mDrawPaint.setColor(mDrawColor);
        mDrawPaint.setStyle(Paint.Style.STROKE);
        mDrawPaint.setStrokeWidth(mDrawRadius);
        mDrawPath = new Path();

        //初始化橡皮
        mClearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mClearPaint.setStyle(Paint.Style.STROKE);
        mClearPaint.setStrokeWidth(mClearRadius);
        mClearPaint.setColor(mBoardColor);
        mClearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        mClearPath = new Path();

        holder = getHolder();
        holder.addCallback(this);
    }

    public void setDrawShape(int mDrawShape) {
        setPaintMode();
        this.mDrawShape = mDrawShape;
    }

    public void setDrawColor(int mDrawColor) {
        this.mDrawColor = mDrawColor;
        mDrawPaint.setColor(mDrawColor);
    }

    public void setBoardColor(int mBoardColor) {
        this.mBoardColor = mBoardColor;
        mClearPaint.setColor(mBoardColor);
    }

    //设置橡皮操作
    public void setClearMode() {
        mDrawOperate = OPERATE_CLEAR;
    }

    //设置画笔操作
    public void setPaintMode() {
        mDrawOperate = OPERATE_DRAW;
    }

    //清屏
    public void clearBoard() {
        if (!checkCanDraw()) {
            return;
        }
        mTempCanvas.drawColor(mBoardColor);
        refreshDraw();
        mDrawOperate = OPERATE_DRAW;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!checkCanDraw()) {
            return false;
        }
        int action = MotionEventCompat.getActionMasked(event);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mDownX = (int) event.getX();
                mDownY = (int) event.getY();
                if (mDrawOperate == OPERATE_DRAW) {
                    switch (mDrawShape) {
                        case SHAPE_PATH:
                            mDrawPath.reset();
                            mDrawPath.moveTo(mDownX, mDownY);
                            break;
                        case SHAPE_CIRCLE:
                            break;
                        case SHAPE_RECTANGLE:
                            break;
                        case SHAPE_CIRCLE_POINT:
                            mDrawPaint.setStyle(Paint.Style.FILL);
                            mTempCanvas.drawCircle(mDownX, mDownY, mDrawRadius / 2, mDrawPaint);
                            mDrawPaint.setStyle(Paint.Style.STROKE);
                            break;
                    }
                } else if (mDrawOperate == OPERATE_CLEAR) {
                    mClearPath.reset();
                    mClearPath.moveTo(mDownX, mDownY);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                int moveX = (int) event.getX();
                int moveY = (int) event.getY();
                int dx = Math.abs(moveX - mDownX);
                int dy = Math.abs(moveY - mDownY);
                int distance = (int) Math.sqrt(dx * dx + dy * dy);
                if (mDrawOperate == OPERATE_DRAW) {
                    switch (mDrawShape) {
                        case SHAPE_PATH:
                            mDrawPath.lineTo(moveX, moveY);
                            mTempCanvas.drawPath(mDrawPath, mDrawPaint);
                            break;
                        case SHAPE_CIRCLE:
                            if (mLastDistance > 0) {
                                mDrawPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                                mDrawPaint.setColor(mBoardColor);
                                mTempCanvas.drawCircle(mDownX, mDownY, mLastDistance / 2 + 1, mDrawPaint);
                                mDrawPaint.setColor(mDrawColor);
                                mDrawPaint.setStyle(Paint.Style.STROKE);
                            }
                            mTempCanvas.drawCircle(mDownX, mDownY, distance / 2, mDrawPaint);
                            break;
                        case SHAPE_RECTANGLE:
                            if (mMoveX > 0 && mMoveY > 0) {
                                mDrawPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                                mDrawPaint.setColor(mBoardColor);
                                mTempCanvas.drawRect(mDownX, mDownY, mMoveX, mMoveY, mDrawPaint);
                                mDrawPaint.setColor(mDrawColor);
                                mDrawPaint.setStyle(Paint.Style.STROKE);
                            }
                            mTempCanvas.drawRect(mDownX, mDownY, moveX, moveY, mDrawPaint);
                            break;
                    }
                } else if (mDrawOperate == OPERATE_CLEAR) {
                    mClearPath.lineTo(moveX, moveY);
                    mTempCanvas.drawPath(mClearPath, mClearPaint);
                }
                mLastDistance = distance;
                mMoveX = moveX;
                mMoveY = moveY;
                break;
            case MotionEvent.ACTION_UP:
                mLastDistance = 0;
                mMoveX = 0;
                mMoveY = 0;
                break;
        }
        refreshDraw();
        return true;
    }

    private void refreshDraw() {
        mSurfaceCanvas.drawBitmap(mTempBitmap, 0, 0, null);
        holder.unlockCanvasAndPost(mSurfaceCanvas);
    }

    private boolean checkCanDraw() {
        if (!hasReady) {
            return false;
        }
        mSurfaceCanvas = holder.lockCanvas();
        if (mSurfaceCanvas == null) {
            return false;
        }
        return true;
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        hasReady = true;
        if (mTempBitmap == null) {
            mTempBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.RGB_565);
            mTempCanvas = new Canvas(mTempBitmap);
            mTempCanvas.drawColor(mBoardColor);
        }
        mSurfaceCanvas = holder.lockCanvas();
        refreshDraw();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

}
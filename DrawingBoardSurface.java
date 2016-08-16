package com.msile.view.canvas;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.LinkedList;

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
    private int mMoveDrawOffset = 44;

    private int mDownX, mDownY;
    private int mMoveX, mMoveY;
    private int mMoveTempOffset;
    private int mLastDistance;

    private int mDrawOperate;                       //当前绘制操作
    public static final int OPERATE_DRAW = 0;       //画笔操作
    public static final int OPERATE_CLEAR = -1;     //橡皮操作
    public static final int OPERATE_BACK = 1;       //撤销操作

    private Bitmap mShapePic;
    private int mShapePicWidth;
    private int mShapePicHeight;
    private Bitmap mBottomBitmap;
    private Canvas mBottomCanvas;
    private Canvas mSurfaceCanvas;                  //画布
    private DrawStack mDownStack, mMoveStack, mUpStack;

    private int mDrawShape;                         //绘制图形
    public static final int SHAPE_PATH = 0;         //路径
    public static final int SHAPE_CIRCLE = 1;       //圆环
    public static final int SHAPE_RECTANGLE = 2;    //矩形
    public static final int SHAPE_CIRCLE_POINT = 4; //圆点
    public static final int SHAPE_PIC_CAST = 5;     //撒花
    public static final int SHAPE_PIC_INSERT = 6;   //插入图片
    public static final int SHAPE_PIC_PICK = 7;     //抠图
    public static final int SHAPE_PIC_MARK = 7;     //马赛克

    private LinkedList<DrawStack> mDrawStacks = new LinkedList<>();
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

        //透明
        setZOrderOnTop(true);
        holder.setFormat(PixelFormat.TRANSLUCENT);
    }

    public void setDrawShape(int mDrawShape) {
        setPaintMode();
        this.mDrawShape = mDrawShape;
    }

    public void setCastPic(Bitmap bitmap) {
        if (bitmap == null) {
            return;
        }
        if (mShapePic != null) {
            mShapePic.recycle();
        }
        this.mShapePic = bitmap;
        mShapePicWidth = mShapePic.getWidth();
        mShapePicHeight = mShapePic.getHeight();
        mDrawShape = SHAPE_PIC_CAST;
        mDrawOperate = OPERATE_DRAW;
    }

    public void setDrawColor(int mDrawColor) {
        this.mDrawColor = mDrawColor;
        mDrawPaint.setColor(mDrawColor);
    }

    public void setDrawRadius(int mDrawRadius) {
        this.mDrawRadius = mDrawRadius;
        mDrawPaint.setStrokeWidth(mDrawRadius);
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

    //设置撤销操作
    public void setBackMode() {
        mDrawOperate = OPERATE_BACK;
    }

    //清屏
    public void clearBoard() {
        if (!checkCanDraw()) {
            return;
        }
        mDrawStacks.clear();
        mBottomCanvas.drawColor(mBoardColor);
        refreshDraw();
        mDrawOperate = OPERATE_DRAW;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!checkCanDraw()) {
            return false;
        }
        DrawStack touchDrawStack = null;
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
                            touchDrawStack = new DrawStack(mDrawShape);
                            touchDrawStack.setDownPoints(mDownX, mDownY);
                            mDrawPaint.setStyle(Paint.Style.FILL);
                            mBottomCanvas.drawCircle(mDownX, mDownY, mDrawRadius / 2, mDrawPaint);
                            mDrawPaint.setStyle(Paint.Style.STROKE);
                            break;
                        case SHAPE_PIC_CAST:
                            if (mShapePic != null) {
                                mBottomCanvas.drawBitmap(mShapePic, mDownX - mShapePicWidth / 2, mDownY - mShapePicHeight / 2, null);
                            }
                            break;
                    }
                } else if (mDrawOperate == OPERATE_CLEAR) {
                    mClearPath.reset();
                    mClearPath.moveTo(mDownX, mDownY);
                    mClearPaint.setStyle(Paint.Style.FILL);
                    mBottomCanvas.drawCircle(mDownX, mDownY, mClearRadius / 2, mClearPaint);
                    mClearPaint.setStyle(Paint.Style.STROKE);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                int moveX = (int) event.getX();
                int moveY = (int) event.getY();
                int dx = Math.abs(moveX - mDownX);
                int dy = Math.abs(moveY - mDownY);
                int distance = (int) Math.sqrt(dx * dx + dy * dy);
                int mx = Math.abs(moveX - mMoveX);
                int my = Math.abs(moveY - mMoveY);
                int tempDistance = (int) Math.sqrt(mx * mx + my * my);
                if (mDrawOperate == OPERATE_DRAW) {
                    switch (mDrawShape) {
                        case SHAPE_PATH:
                            mDrawPath.lineTo(moveX, moveY);
                            mBottomCanvas.drawPath(mDrawPath, mDrawPaint);
                            break;
                        case SHAPE_CIRCLE:
                            if (mLastDistance > 0) {
                                mDrawPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                                mDrawPaint.setColor(mBoardColor);
                                mBottomCanvas.drawCircle(mDownX, mDownY, mLastDistance / 2 + 1, mDrawPaint);
                                mDrawPaint.setColor(mDrawColor);
                                mDrawPaint.setStyle(Paint.Style.STROKE);
                            }
                            mBottomCanvas.drawCircle(mDownX, mDownY, distance / 2, mDrawPaint);
                            break;
                        case SHAPE_RECTANGLE:
                            if (mMoveX > 0 && mMoveY > 0) {
                                mDrawPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                                mDrawPaint.setColor(mBoardColor);
                                mBottomCanvas.drawRect(mDownX, mDownY, mMoveX, mMoveY, mDrawPaint);
                                mDrawPaint.setColor(mDrawColor);
                                mDrawPaint.setStyle(Paint.Style.STROKE);
                            }
                            mBottomCanvas.drawRect(mDownX, mDownY, moveX, moveY, mDrawPaint);
                            break;
                        case SHAPE_CIRCLE_POINT:
                            mMoveTempOffset += tempDistance;
                            if (mMoveTempOffset >= mMoveDrawOffset) {
                                mMoveTempOffset = 0;
                                mDrawPaint.setStyle(Paint.Style.FILL);
                                mBottomCanvas.drawCircle(moveX, moveY, mDrawRadius / 2, mDrawPaint);
                                mDrawPaint.setStyle(Paint.Style.STROKE);
                            }
                            break;
                        case SHAPE_PIC_CAST:
                            if (mMoveTempOffset >= mMoveDrawOffset) {
                                mMoveTempOffset = 0;
                                if (mShapePic != null) {
                                    mBottomCanvas.drawBitmap(mShapePic, moveX - mShapePicWidth / 2, moveY - mShapePicHeight / 2, null);
                                }
                            }
                            mMoveTempOffset += tempDistance;
                            break;
                    }
                } else if (mDrawOperate == OPERATE_CLEAR) {
                    mClearPath.lineTo(moveX, moveY);
                    mBottomCanvas.drawPath(mClearPath, mClearPaint);
                }
                mLastDistance = distance;
                mMoveX = moveX;
                mMoveY = moveY;
                break;
            case MotionEvent.ACTION_UP:
                mLastDistance = 0;
                mMoveX = 0;
                mMoveY = 0;
                mMoveTempOffset = 0;
                break;
        }
        if (touchDrawStack != null) {
            mDrawStacks.add(touchDrawStack);
        }
        refreshDraw();
        return true;
    }

    private void refreshDraw() {
        int stackSize = mDrawStacks.size();
        for (int i = 0; i < stackSize; i++) {
            DrawStack drawStack = mDrawStacks.get(i);
            switch (drawStack.drawShape) {
                case SHAPE_PATH:
                    break;
                case SHAPE_CIRCLE:
                    break;
                case SHAPE_RECTANGLE:
                    break;
                case SHAPE_CIRCLE_POINT:
                    break;
                case SHAPE_PIC_CAST:
                    break;
            }
        }
        mSurfaceCanvas.drawBitmap(mBottomBitmap, 0, 0, null);
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
        if (mBottomBitmap == null) {
            mBottomBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.RGB_565);
            mBottomCanvas = new Canvas(mBottomBitmap);
            mBottomCanvas.drawColor(mBoardColor);
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


    /**
     * 绘制回退栈
     */
    static class DrawStack {

        public DrawStack(int drawShape) {
            this.drawShape = drawShape;
        }

        public void setDownPoints(int pointX, int pointY) {
            this.pointX = pointX;
            this.pointY = pointY;
        }

        public void addPathPoint(int pointX, int pointY) {
            points.put(pointX, pointY);
        }

        public int drawShape;
        public int pointX, pointY;
        public SparseIntArray points = new SparseIntArray();
    }

}
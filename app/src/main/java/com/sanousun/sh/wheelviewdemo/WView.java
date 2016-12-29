package com.sanousun.sh.wheelviewdemo;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import static android.view.View.MeasureSpec.getSize;

/**
 * Date: 2016/11/21
 * Time: 下午6:52
 * Desc: 仿iOS的滚轮控件
 */

public class WView extends View {

    private static final int ANI_TIME = 16;
    private static final int ANI_COUNT = 15;

    private Paint mOuterTxtPaint;//分割线外文字画笔
    private Paint mInnerTxtPaint;//分割线内文字画笔
    private Paint mLinePaint;    //分割线画笔

    private List<Object> mData = new ArrayList<>();
    private OnWheelChangeListener mOnWheelChangeListener;

    private double mRadius;

    private int mVisibleItemCount;
    private int mItemHeight;   //在view的高度为wrap_content的时候可以自定义
    private double mItemAngle = 0; //item对应的转盘角度

    private float mTopSepLineHeight;   //第一条分隔线高度
    private float mBottomSepLineHeight;//第二条分隔线高度

    private int mCurItemIndex = 0;
    private double mItemOffset = 0;//滑动时的偏移，-mItemHeight<mItemOffset<mItemHeight

    private boolean isAutoScroll;

    private GestureDetector mGestureDetector;

    public WView(Context context) {
        this(context, null);
    }

    public WView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        mOuterTxtPaint = new Paint();
        mInnerTxtPaint = new Paint();
        mLinePaint = new Paint();

        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.WView);
        mVisibleItemCount = ta.getInt(R.styleable.WView_picker_visible_item, 7);
        mItemHeight = ta.getDimensionPixelSize(R.styleable.WView_picker_item_height, dp2px(45));
        int itemTextSize = ta.getDimensionPixelSize(R.styleable.WView_picker_item_text_size, dp2px(16));
        int innerTxtColor = ta.getColor(R.styleable.WView_picker_inner_text_color, Color.parseColor("#373737"));
        int outerTxtColor = ta.getColor(R.styleable.WView_picker_outer_text_color, Color.parseColor("#AFAFAF"));
        int sepColor = ta.getColor(R.styleable.WView_picker_sep_line_color, Color.parseColor("#E7E7E7"));
        int sepWidth = ta.getDimensionPixelSize(R.styleable.WView_picker_sep_line_width, 1);
        ta.recycle();

        mInnerTxtPaint.setAntiAlias(true);
        mInnerTxtPaint.setTextAlign(Paint.Align.CENTER);
        mInnerTxtPaint.setColor(innerTxtColor);
        mInnerTxtPaint.setTextSize(itemTextSize);

        mOuterTxtPaint.setAntiAlias(true);
        mOuterTxtPaint.setTextAlign(Paint.Align.CENTER);
        mOuterTxtPaint.setColor(outerTxtColor);
        mOuterTxtPaint.setTextSize(itemTextSize - dp2px(1));

        mLinePaint.setColor(sepColor);
        mLinePaint.setStrokeWidth(sepWidth);

        mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {

            @Override
            public boolean onDown(MotionEvent e) {
                isAutoScroll = false;
                return super.onDown(e);
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                double touchY = e.getY() - getPaddingTop();
                if (touchY < 0 || touchY > 2 * mRadius) return false;
                double touchAngle = Math.acos((mRadius - touchY) / mRadius);
                int scrollY = (int) ((touchAngle - Math.PI / 2) * mRadius);
                Log.i("xyz", "scrollY:" + scrollY);
                if (Math.abs(scrollY) < mItemHeight / 2) {
                    return true;
                }
                if (scrollY > 0) {
                    scrollY -= mItemHeight / 2;
                } else {
                    scrollY += mItemHeight / 2;
                }
                Log.i("xyz", "itemHeight:" + mItemHeight + ";scrollY:" + scrollY);
                isAutoScroll = true;
                postDelayed(new AutoFlingRunnable(scrollY), ANI_TIME);
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                mItemOffset += distanceY;
                invalidate();
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                isAutoScroll = true;
                postDelayed(new AutoFlingRunnable((int) (-velocityY / 10)), ANI_TIME);
                return true;
            }
        });
    }

    public void setData(List<Object> data) {
        mData = data;
        mCurItemIndex = 0;
        notifyIndexChange();
        mItemOffset = 0;
        invalidate();
    }

    public void setOnWheelChangeListener(OnWheelChangeListener onWheelChangeListener) {
        mOnWheelChangeListener = onWheelChangeListener;
    }

    private void notifyIndexChange() {
        if (mOnWheelChangeListener != null
                && mData != null && mData.size() > 0
                && mCurItemIndex >= 0 && mCurItemIndex < mData.size()) {
            mOnWheelChangeListener.onWheelChange(mCurItemIndex, mData.get(mCurItemIndex));
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = getSize(heightMeasureSpec);
        int height;
        double diameter;
        //获取item高度对应的角度
        mItemAngle = Math.PI / mVisibleItemCount;
        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize;
            diameter = height - getPaddingTop() - getPaddingBottom();
            mItemHeight = (int) (diameter * Math.sin(mItemAngle / 2));
        } else {
            //获取滚轮的直径
            diameter = mItemHeight / Math.sin(mItemAngle / 2);
            height = (int) (getPaddingTop() + getPaddingBottom() + diameter);
        }
        mRadius = diameter / 2;
        mTopSepLineHeight = (float) (diameter / 2 + getPaddingTop() - mItemHeight / 2);
        mBottomSepLineHeight = (float) (diameter / 2 + getPaddingTop() + mItemHeight / 2);
        setMeasuredDimension(getSize(widthMeasureSpec), height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int top = getPaddingTop();
        int left = getPaddingLeft();
        int right = getMeasuredWidth() - getPaddingRight();
        int bottom = getMeasuredHeight() - getPaddingBottom();

        //画出第一条线
        canvas.drawLine(left, mTopSepLineHeight, right, mTopSepLineHeight, mLinePaint);
        //画出第二条线
        canvas.drawLine(left, mBottomSepLineHeight, right, mBottomSepLineHeight, mLinePaint);

        if (mData == null || mData.size() == 0) {
            return;
        }
        if (mItemOffset > 0) {
            if (mCurItemIndex >= mData.size() - 1) {
                mItemOffset = 0;
            } else {
                while (mItemOffset > mItemHeight) {
                    if (mCurItemIndex >= mData.size() - 1) {
                        mItemOffset = 0;
                        break;
                    } else {
                        mItemOffset -= mItemHeight;
                        mCurItemIndex++;
                        notifyIndexChange();
                    }
                }
            }
        } else if (mItemOffset < 0) {
            if (mCurItemIndex <= 0) {
                mItemOffset = 0;
            } else {
                while (mItemOffset < 0) {
                    if (mCurItemIndex <= 0) {
                        mItemOffset = 0;
                        break;
                    } else {
                        mItemOffset += mItemHeight;
                        mCurItemIndex--;
                        notifyIndexChange();
                    }
                }
            }
        }

        for (int i = 0; i <= mVisibleItemCount; i++) {
            canvas.save();
            double offsetAngle = -(mItemAngle * mItemOffset / mItemHeight);
            double angle0 = i * mItemAngle + offsetAngle;
            if (angle0 < 0) angle0 = 0;
            double angle1 = (i + 1) * mItemAngle + offsetAngle;
            if (angle1 > Math.PI) angle1 = Math.PI;
            double itemHeight = (Math.cos(angle0) - Math.cos(angle1)) * mRadius;
            float scaleY = (float) (itemHeight / mItemHeight);
            double itemLocationY = top + ((1 - Math.cos(angle0)) * mRadius + itemHeight / 2) * mItemHeight / itemHeight;
            if (itemHeight < dp2px(1)) continue;
            if (i == mVisibleItemCount / 2) {
                canvas.save();
                canvas.clipRect(left, 0, right, mTopSepLineHeight);
                canvas.scale(1.0f, scaleY);
                drawText(canvas, getText(i), mOuterTxtPaint, (int) (itemLocationY));
                canvas.restore();
                canvas.clipRect(left, mTopSepLineHeight, right, bottom);
                canvas.scale(1.0f, scaleY);
                drawText(canvas, getText(i), mInnerTxtPaint, (int) (itemLocationY));
            } else if (i == mVisibleItemCount / 2 + 1) {
                canvas.save();
                canvas.clipRect(left, 0, right, mBottomSepLineHeight);
                canvas.scale(1.0f, scaleY);
                drawText(canvas, getText(i), mInnerTxtPaint, (int) (itemLocationY));
                canvas.restore();
                canvas.clipRect(left, mBottomSepLineHeight, right, bottom);
                canvas.scale(1.0f, scaleY);
                drawText(canvas, getText(i), mOuterTxtPaint, (int) (itemLocationY));
            } else {
                canvas.scale(1.0f, scaleY);
                drawText(canvas, getText(i), mOuterTxtPaint, (int) (itemLocationY));
            }
            canvas.restore();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mGestureDetector.onTouchEvent(event)) {
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_UP ||
                event.getAction() == MotionEvent.ACTION_CANCEL) {
            if (mItemOffset != 0) {
                isAutoScroll = true;
                postDelayed(new AutoSelectRunnable(), ANI_TIME);
            }
        }
        return true;
    }

    private String getText(int pos) {
        int itemPos = pos + mCurItemIndex - mVisibleItemCount / 2;
        if (itemPos < 0 || itemPos > mData.size() - 1) {
            return "";
        }
        return mData.get(itemPos).toString();
    }

    private void drawText(Canvas canvas, String str, Paint paint, int height) {
        Paint.FontMetricsInt fm = paint.getFontMetricsInt();
        int baseline = height + (fm.descent - fm.ascent) / 2 - fm.descent;
        canvas.drawText(str, getMeasuredWidth() / 2, baseline, paint);
    }

    private int dp2px(float dpValue) {
        final float scale = getContext().getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    private class AutoSelectRunnable implements Runnable {

        @Override
        public void run() {
            if (!isAutoScroll) {
                return;
            }
            if (mItemOffset <= mItemHeight / ANI_COUNT) {
                mItemOffset = 0;
                invalidate();
                isAutoScroll = false;
            } else if (mItemOffset >= mItemHeight * (ANI_COUNT - 1) / ANI_COUNT) {
                mItemOffset = 0;
                mCurItemIndex++;
                notifyIndexChange();
                invalidate();
                isAutoScroll = false;
            } else if (mItemOffset <= mItemHeight / 2) {
                mItemOffset -= mItemHeight / ANI_COUNT;
                invalidate();
                postDelayed(this, ANI_TIME);
            } else {
                mItemOffset += mItemHeight / ANI_COUNT;
                invalidate();
                postDelayed(this, ANI_TIME);
            }
        }
    }

    private class AutoFlingRunnable implements Runnable {

        private int scrollBy;
        private int scrollTimes = ANI_COUNT;

        AutoFlingRunnable(int scrollBy) {
            this.scrollBy = scrollBy;
        }

        @Override
        public void run() {
            if (!isAutoScroll) {
                return;
            }
            scrollTimes--;
            if (scrollBy > 0) {
                if (mCurItemIndex >= mData.size() - 1) {
                    postDelayed(new AutoSelectAfterFlingRunnable(true), ANI_TIME);
                    return;
                }
            } else {
                if (mCurItemIndex <= 0) {
                    Log.e("xyz", "off:" + mItemOffset);
                    postDelayed(new AutoSelectAfterFlingRunnable(false), ANI_TIME);
                    return;
                }
            }
            if (scrollTimes > 0) {
                mItemOffset += scrollBy / ANI_COUNT;
                invalidate();
                postDelayed(this, ANI_TIME);
            } else if (scrollTimes == 0) {
                postDelayed(new AutoSelectAfterFlingRunnable(scrollBy > 0), ANI_TIME);
            }
        }
    }

    private class AutoSelectAfterFlingRunnable implements Runnable {

        private boolean forward;

        AutoSelectAfterFlingRunnable(boolean forward) {
            this.forward = forward;
        }

        @Override
        public void run() {
            if (!isAutoScroll) {
                return;
            }
            if (forward) {
                if (mCurItemIndex >= mData.size() - 1) {
                    mItemOffset = 0;
                    invalidate();
                    isAutoScroll = false;
                } else if (mItemOffset >= mItemHeight * (ANI_COUNT - 1) / ANI_COUNT) {
                    mItemOffset = 0;
                    mCurItemIndex++;
                    notifyIndexChange();
                    invalidate();
                    isAutoScroll = false;
                } else {
                    mItemOffset += mItemHeight / ANI_COUNT;
                    invalidate();
                    postDelayed(this, ANI_TIME);
                }
            } else {
                if (mItemOffset <= mItemHeight / ANI_COUNT) {
                    mItemOffset = 0;
                    invalidate();
                    isAutoScroll = false;
                } else {
                    mItemOffset -= mItemHeight / ANI_COUNT;
                    invalidate();
                    postDelayed(this, ANI_TIME);
                }
            }
        }
    }

    public interface OnWheelChangeListener {
        void onWheelChange(int index, Object object);
    }
}
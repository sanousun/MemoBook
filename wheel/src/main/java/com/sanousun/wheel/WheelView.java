package com.sanousun.wheel;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
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
import android.view.animation.DecelerateInterpolator;

import java.util.ArrayList;
import java.util.List;

/**
 * Date: 2016/11/21
 * Time: 下午6:52
 * Desc: 仿iOS的滚轮控件
 */

public class WheelView extends View {

    private static final int ANIMATOR_DURING = 300;

    /* 转盘的数据*/
    private List<Object> mData = new ArrayList<>();
    /* 转盘的监听器*/
    private OnWheelChangeListener mOnWheelChangeListener;

    /* 分割线外文字画笔 */
    private Paint mOuterTxtPaint;
    /* 分割线内文字画笔 */
    private Paint mInnerTxtPaint;
    /* 分割线画笔 */
    private Paint mLinePaint;

    private double mRadius;

    /* item的可见数量*/
    private int mVisibleItemCount;
    /* item的高度，view的高度为wrap_content的时候可以自定义*/
    private int mItemHeight;
    /* item的对应角度，180/mVisibleItemCount*/
    private double mItemAngle;

    /* 第一条分隔线高度*/
    private float mTopSepLineHeight;
    /* 第二条分隔线高度*/
    private float mBottomSepLineHeight;

    /* 当前的偏移量，0..mItemHeight*(mData.size()-1)*/
    private int mCurrentOffset;
    /* 当期的下标，0..mData.size()-1*/
    private int mCurrentIndex;

    private boolean isAutoScroll;
    private ValueAnimator mValueAnimator;

    private GestureDetector mGestureDetector;

    public WheelView(Context context) {
        this(context, null);
    }

    public WheelView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WheelView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        mOuterTxtPaint = new Paint();
        mInnerTxtPaint = new Paint();
        mLinePaint = new Paint();

        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.WheelView);
        mVisibleItemCount = ta.getInt(R.styleable.WheelView_wheel_visible_item, 7);
        mItemHeight = ta.getDimensionPixelSize(R.styleable.WheelView_wheel_item_height, dp2px(45));
        int itemTextSize = ta.getDimensionPixelSize(R.styleable.WheelView_wheel_item_text_size, dp2px(16));
        int innerTxtColor = ta.getColor(R.styleable.WheelView_wheel_inner_text_color, Color.parseColor("#373737"));
        int outerTxtColor = ta.getColor(R.styleable.WheelView_wheel_outer_text_color, Color.parseColor("#AFAFAF"));
        int sepColor = ta.getColor(R.styleable.WheelView_wheel_sep_line_color, Color.parseColor("#E7E7E7"));
        int sepWidth = ta.getDimensionPixelSize(R.styleable.WheelView_wheel_sep_line_width, 1);
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
                if (isAutoScroll && mValueAnimator != null) {
                    mValueAnimator.cancel();
                }
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
                Log.i("xyz", "itemHeight:" + mItemHeight + "; scrollY:" + scrollY);
                autoFling(scrollY);
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                mCurrentOffset += distanceY;
                invalidate();
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                autoFling((int) (-velocityY / 10));
                return true;
            }
        });
    }

    /* *************************** 数据操作 *************************** */

    /**
     * 设置数据源
     */
    public void setData(List<Object> data) {
        mData = data;
        mCurrentIndex = 0;
        mCurrentOffset = 0;
        notifyIndexChange();
        invalidate();
    }

    /**
     * 获取当前选中的数据
     */
    public Object getCurrentData() {
        if (mData != null && mData.size() != 0) {
            return mData.get(mCurrentIndex);
        }
        return null;
    }

    /**
     * 设置监听器
     */
    public void setOnWheelChangeListener(OnWheelChangeListener onWheelChangeListener) {
        mOnWheelChangeListener = onWheelChangeListener;
    }

    private void notifyIndexChange() {
        if (mOnWheelChangeListener != null
                && mData != null && mData.size() > 0) {
            mOnWheelChangeListener.onWheelChange(mCurrentIndex, mData.get(mCurrentIndex));
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int height;
        double diameter;
        //获取item高度对应的角度
        mItemAngle = Math.PI / mVisibleItemCount;
        //高度已知，通过高度获取mItemHeight
        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize;
            diameter = height - getPaddingTop() - getPaddingBottom();
            mItemHeight = (int) (diameter / 2 * Math.sin(mItemAngle / 2) * 2);
        }
        //高度未知，通过mItemHeight推导高度
        else {
            //获取滚轮的直径
            diameter = mItemHeight / 2 / Math.sin(mItemAngle / 2) * 2;
            height = (int) (getPaddingTop() + getPaddingBottom() + diameter);
        }
        mRadius = diameter / 2;
        //在这里不需要考虑paddingTop，绘制时会将画布整体移动
        mTopSepLineHeight = (float) (diameter / 2 - mItemHeight / 2);
        mBottomSepLineHeight = (float) (diameter / 2 + mItemHeight / 2);
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int top = getPaddingTop();
        int left = getPaddingLeft();
        int right = getMeasuredWidth() - getPaddingRight();
        int bottom = getMeasuredHeight() - getPaddingBottom();

        //移除paddingTop的影响
        canvas.translate(0, top);

        //画出第一条线
        canvas.drawLine(left, mTopSepLineHeight, right, mTopSepLineHeight, mLinePaint);
        //画出第二条线
        canvas.drawLine(left, mBottomSepLineHeight, right, mBottomSepLineHeight, mLinePaint);

        if (mData == null || mData.size() == 0) {
            mCurrentIndex = 0;
            mCurrentOffset = 0;
            return;
        }
        if (mCurrentOffset < 0) {
            mCurrentOffset = 0;
        } else if (mCurrentOffset > (mData.size() - 1) * mItemHeight) {
            mCurrentOffset = (mData.size() - 1) * mItemHeight;
        }
        //计算相对于item的偏移量
        int itemOffset = mCurrentOffset % mItemHeight;
        //计算偏移后的index
        int curIndex = mCurrentOffset / mItemHeight;
        //偏移后的index改变时通知监听器
        if (mCurrentIndex != curIndex) {
            mCurrentIndex = curIndex;
            notifyIndexChange();
        }

        // 开始绘制，可见 item 为奇数，需要绘制 +1 个才能保证上下可见
        for (int i = 0; i <= mVisibleItemCount; i++) {
            canvas.save();
            double offsetAngle = -(mItemAngle * (itemOffset * 1.0 / mItemHeight));
            //计算顶部的角度
            double angle0 = i * mItemAngle + offsetAngle;
            if (angle0 < 0) angle0 = 0;
            Log.i("angle0", "angle0: " + angle0);
            //计算底部的角度
            double angle1 = (i + 1) * mItemAngle + offsetAngle;
            if (angle1 > Math.PI) angle1 = Math.PI;
            Log.i("angle1", "angle1: " + angle1);
            //计算出该 item 的实际高度
            float itemHeight = (float) ((Math.cos(angle0) - Math.cos(angle1)) * mRadius);
            //计算出一个缩放值
            float scaleY = itemHeight / mItemHeight;
            //这里的高度是折叠前的
            int itemY = (int) ((1 - Math.cos(angle0)) * mRadius + itemHeight / 2);
            Log.i("itemY", "itemY: " + (itemY - mRadius));
            int itemLocationY = (int) (itemY / scaleY);
            Log.i("itemLocationY", "itemLocationY: " + itemLocationY);
            if (itemHeight < dp2px(1)) continue;
            //需要靠画布的折叠来实现效果
            canvas.save();
            if (i == mVisibleItemCount / 2) {
                canvas.clipRect(left, 0, right, mTopSepLineHeight);
                canvas.scale(1.0f, scaleY);
                drawText(canvas, getText(i), mOuterTxtPaint, itemLocationY);
                canvas.restore();
                canvas.clipRect(left, mTopSepLineHeight, right, bottom);
                canvas.scale(1.0f, scaleY);
                drawText(canvas, getText(i), mInnerTxtPaint, itemLocationY);
            } else if (i == mVisibleItemCount / 2 + 1) {
                canvas.clipRect(left, 0, right, mBottomSepLineHeight);
                canvas.scale(1.0f, scaleY);
                drawText(canvas, getText(i), mInnerTxtPaint, itemLocationY);
                canvas.restore();
                canvas.clipRect(left, mBottomSepLineHeight, right, bottom);
                canvas.scale(1.0f, scaleY);
                drawText(canvas, getText(i), mOuterTxtPaint, itemLocationY);
            } else {
                canvas.scale(1.0f, scaleY);
                drawText(canvas, getText(i), mOuterTxtPaint, itemLocationY);
            }
            canvas.restore();
        }
    }

    /**
     * 获取对应位置上的文字
     */
    private String getText(int pos) {
        int itemPos = pos + mCurrentIndex - mVisibleItemCount / 2;
        if (itemPos < 0 || itemPos > mData.size() - 1) {
            return "";
        }
        return mData.get(itemPos).toString();
    }

    /**
     * 将文字居中绘制的方法
     */
    private void drawText(Canvas canvas, String str, Paint paint, int height) {
        Paint.FontMetricsInt fm = paint.getFontMetricsInt();
        int baseline = height + (fm.descent - fm.ascent) / 2 - fm.descent;
        canvas.drawText(str, getMeasuredWidth() / 2, baseline, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mGestureDetector.onTouchEvent(event)) {
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_UP ||
                event.getAction() == MotionEvent.ACTION_CANCEL) {
            autoSelect();
        }
        return true;
    }

    private int dp2px(float dpValue) {
        final float scale = getContext().getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 当划到一半时，会自动划到合适的位置
     */
    private void autoSelect() {
        int itemOffset = mCurrentOffset % mItemHeight;
        //偏移量未达到 item 高度一半，回弹
        int from = mCurrentOffset;
        int to;
        if (itemOffset < mItemHeight / 2) {
            to = mCurrentOffset - itemOffset;
        } else {
            to = mCurrentOffset - itemOffset + mItemHeight;
        }
        if (to < 0) {
            to = 0;
        } else if (to > mItemHeight * (mData.size() - 1)) {
            to = mItemHeight * (mData.size() - 1);
        }
        animator(from, to);
    }

    /**
     * 快速划动的情况
     */
    private void autoFling(int scrollBy) {
        int after = mCurrentOffset + scrollBy;
        int itemOffset = after % mItemHeight;
        int from = mCurrentOffset;
        int to;
        if (itemOffset < mItemHeight / 2) {
            to = after - itemOffset;
        } else {
            to = after - itemOffset + mItemHeight;
        }
        if (to < 0) {
            to = 0;
        } else if (to > mItemHeight * (mData.size() - 1)) {
            to = mItemHeight * (mData.size() - 1);
        }
        animator(from, to);
    }

    private void animator(int from, int to) {
        if (from == to) return;
        if (mValueAnimator == null) {
            mValueAnimator = ValueAnimator.ofInt(from, to);
            mValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    int offset = (int) valueAnimator.getAnimatedValue();
                    if (mCurrentOffset != offset) {
                        mCurrentOffset = offset;
                        invalidate();
                    }
                }
            });
            mValueAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    isAutoScroll = true;
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    isAutoScroll = false;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    isAutoScroll = false;
                }
            });
            mValueAnimator.setDuration(ANIMATOR_DURING);
            mValueAnimator.setInterpolator(new DecelerateInterpolator());
        } else {
            mValueAnimator.setIntValues(from, to);
        }
        mValueAnimator.start();
    }

    public interface OnWheelChangeListener {
        void onWheelChange(int index, Object object);
    }
}
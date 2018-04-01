package com.sanousun.wheel;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v4.widget.EdgeEffectCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.widget.EdgeEffect;
import android.widget.OverScroller;

import java.util.ArrayList;
import java.util.List;

/**
 * @author dashu
 * @date 2016/11/21
 * 仿 iOS 的滚轮控件
 * 通过 OverScroller 实现滚动
 * 停止滚动监听实现有点困难，想想看有什么好的解决方式
 */

public class ScrollWheelView extends View {

    private static final String TAG = ScrollWheelView.class.getSimpleName();

    private static final int INVALID_POINTER = -1;

    /**
     * 转盘的数据
     */
    private List<WheelBean> mData = new ArrayList<>();
    /**
     * 转盘的监听器
     */
    private OnWheelChangeListener mOnWheelChangeListener;

    /**
     * 分割线外文字画笔
     */
    private Paint mOuterTxtPaint;
    /**
     * 分割线内文字画笔
     */
    private Paint mInnerTxtPaint;
    /**
     * 分割线画笔
     */
    private Paint mLinePaint;

    private double mRadius;

    /**
     * item的可见数量
     */
    private int mVisibleItemCount;
    /**
     * item的高度，view的高度为wrap_content的时候可以自定义
     */
    private int mItemHeight;
    /**
     * item的对应角度，180/mVisibleItemCount
     */
    private double mItemAngle;

    /**
     * 第一条分隔线高度
     */
    private float mTopSepLineHeight;
    /**
     * 第二条分隔线高度
     */
    private float mBottomSepLineHeight;

    /**
     * 当前的偏移量，0..mItemHeight*(mData.size()-1)
     */
    private int mCurrentOffset;
    /**
     * 当期的下标，0..mData.size()-1
     */
    private int mCurrentIndex;

    private OverScroller mScroller;
    private EdgeEffect mEdgeGlowTop;
    private EdgeEffect mEdgeGlowBottom;
    private VelocityTracker mVelocityTracker;
    private int mTouchSlop;
    private int mMinimumVelocity;
    private int mMaximumVelocity;
    private int mOverScrollDistance;
    private int mOverFlingDistance;

    private int mActivePointerId = INVALID_POINTER;
    /**
     * 只考虑竖向的滑动
     */
    private int mLastMotionY;
    private boolean mIsBeingDragged = false;

    public ScrollWheelView(Context context) {
        this(context, null);
    }

    public ScrollWheelView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScrollWheelView(Context context, AttributeSet attrs, int defStyleAttr) {
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

        mScroller = new OverScroller(getContext());
        final ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        mOverScrollDistance = configuration.getScaledOverscrollDistance();
        mOverFlingDistance = configuration.getScaledOverflingDistance();
    }

    /* *************************** 数据操作 *************************** */

    /**
     * 设置数据源
     */
    public void setData(List<WheelBean> data) {
        mData = data;
        mCurrentIndex = 0;
        mCurrentOffset = 0;
        notifyIndexChange();
        invalidate();
    }

    /**
     * 获取当前选中的数据
     */
    public WheelBean getCurrentData() {
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
        } else if (mCurrentOffset > getTotalOffset()) {
            mCurrentOffset = getTotalOffset();
        }
        //计算相对于item的偏移量
        int itemOffset = mCurrentOffset % mItemHeight;
        //计算偏移后的index
        int curIndex = mCurrentOffset / mItemHeight;
        //偏移后的index改变时通知监听器
        if (mCurrentIndex != curIndex) {
            mCurrentIndex = curIndex;
        }

        // 开始绘制，可见 item 为奇数，需要绘制 +1 个才能保证上下可见
        for (int i = 0; i <= mVisibleItemCount; i++) {
            canvas.save();
            double offsetAngle = -(mItemAngle * (itemOffset * 1.0 / mItemHeight));
            // 计算顶部的角度
            double angle0 = i * mItemAngle + offsetAngle;
            if (angle0 < 0) {
                angle0 = 0;
            }
            // 计算底部的角度
            double angle1 = (i + 1) * mItemAngle + offsetAngle;
            if (angle1 > Math.PI) {
                angle1 = Math.PI;
            }
            // 计算出该 item 的实际高度
            float itemHeight = (float) ((Math.cos(angle0) - Math.cos(angle1)) * mRadius);
            // 计算出一个缩放值
            float scaleY = itemHeight / mItemHeight;
            // 这里的高度是折叠前的
            int itemY = (int) ((1 - Math.cos(angle0)) * mRadius + itemHeight / 2);
            int itemLocationY = (int) (itemY / scaleY);
            if (itemHeight < dp2px(1)) {
                continue;
            }
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
        canvas.translate(0, -top);
        if (mEdgeGlowTop != null) {
            if (!mEdgeGlowTop.isFinished()) {
                final int restoreCount = canvas.save();
                final int width;
                final int height;
                width = getWidth();
                height = getHeight();
                canvas.translate(0, 0);
                mEdgeGlowTop.setSize(width, height);
                if (mEdgeGlowTop.draw(canvas)) {
                    postInvalidateOnAnimation();
                }
                canvas.restoreToCount(restoreCount);
            }
            if (!mEdgeGlowBottom.isFinished()) {
                final int restoreCount = canvas.save();
                final int width;
                final int height;
                width = getWidth();
                height = getHeight();
                canvas.translate(-width, height);
                canvas.rotate(180, width, 0);
                mEdgeGlowBottom.setSize(width, height);
                if (mEdgeGlowBottom.draw(canvas)) {
                    postInvalidateOnAnimation();
                }
                canvas.restoreToCount(restoreCount);
            }
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
        return mData.get(itemPos).getShowText();
    }

    /**
     * 将文字居中绘制的方法
     */
    private void drawText(Canvas canvas, String str, Paint paint, int height) {
        Paint.FontMetricsInt fm = paint.getFontMetricsInt();
        int baseline = height + (fm.descent - fm.ascent) / 2 - fm.descent;
        canvas.drawText(str, getMeasuredWidth() / 2, baseline, paint);
    }

    /**
     * 参照ScrollView实现滑动逻辑
     */
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        initVelocityTrackerIfNotExists();
        int actionId = ev.getActionMasked();
        switch (actionId) {
            case MotionEvent.ACTION_DOWN:
                if ((mIsBeingDragged = !mScroller.isFinished())) {
                    final ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                }
                /*
                 * 如果是fling的时候，停止滑动.
                 */
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }
                mIsBeingDragged = false;
                mLastMotionY = (int) ev.getY();
                mActivePointerId = ev.getPointerId(0);
                break;
            case MotionEvent.ACTION_MOVE:
                final int activePointerIndex = ev.findPointerIndex(mActivePointerId);
                if (activePointerIndex == -1) {
                    Log.e(TAG, "Invalid pointerId=" + mActivePointerId + " in onTouchEvent");
                    break;
                }
                final int y = (int) ev.getY(activePointerIndex);
                int deltaY = mLastMotionY - y;
                if (!mIsBeingDragged && Math.abs(deltaY) > mTouchSlop) {
                    final ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                    mIsBeingDragged = true;
                    if (deltaY > 0) {
                        deltaY -= mTouchSlop;
                    } else {
                        deltaY += mTouchSlop;
                    }
                }
                if (mIsBeingDragged) {
                    mLastMotionY = y;
                    final int oldY = mCurrentOffset;
                    final int range = getTotalOffset();
                    final int overScrollMode = getOverScrollMode();
                    boolean canOverScroll = overScrollMode == OVER_SCROLL_ALWAYS ||
                            (overScrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && range > 0);
                    if (overScrollBy(0, deltaY,
                            0, oldY,
                            0, range,
                            0, mOverScrollDistance, true)) {
                        mVelocityTracker.clear();
                    }
                    if (canOverScroll) {
                        ensureGlows();
                        final int pulledToY = oldY + deltaY;
                        if (pulledToY < 0) {
                            EdgeEffectCompat.onPull(mEdgeGlowTop,
                                    (float) deltaY / getHeight(),
                                    ev.getX(activePointerIndex) / getWidth());
                            if (!mEdgeGlowBottom.isFinished()) {
                                mEdgeGlowBottom.onRelease();
                            }
                        } else if (pulledToY > range) {
                            EdgeEffectCompat.onPull(mEdgeGlowBottom,
                                    (float) deltaY / getHeight(),
                                    1.0f - ev.getX(activePointerIndex) / getWidth());
                            if (!mEdgeGlowTop.isFinished()) {
                                mEdgeGlowTop.onRelease();
                            }
                        }
                        if (mEdgeGlowTop != null && mEdgeGlowBottom != null
                                && (!mEdgeGlowTop.isFinished() || !mEdgeGlowBottom.isFinished())) {
                            postInvalidateOnAnimation();
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (mIsBeingDragged) {
                    final VelocityTracker velocityTracker = mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    int initialVelocity = (int) velocityTracker.getYVelocity(mActivePointerId);
                    if (Math.abs(initialVelocity) > mMinimumVelocity) {
                        fling(-initialVelocity);
                    } else if (mScroller.springBack(0, mCurrentOffset,
                            0, 0, 0, getTotalOffset())) {
                        postInvalidateOnAnimation();
                    }
                    mActivePointerId = INVALID_POINTER;
                    endDrag();
                }
                mLastMotionY = 0;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                final int index = ev.getActionIndex();
                mLastMotionY = (int) ev.getY(index);
                mActivePointerId = ev.getPointerId(index);
                break;
            case MotionEvent.ACTION_POINTER_UP:
                final int pointerIndex = ev.getActionIndex();
                final int pointerId = ev.getPointerId(pointerIndex);
                if (pointerId == mActivePointerId) {
                    // 如果是当前活动的手指抬起，重新选择一个新的手指代替.
                    // 源代码还有个 todo 是关于采用更智能的方案去寻找新的手指，而不是简单的采用最先的按下的那个
                    // 不过感觉应该也没有多余两个手指的使用场景了
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    mLastMotionY = (int) ev.getY(newPointerIndex);
                    mActivePointerId = ev.getPointerId(newPointerIndex);
                    if (mVelocityTracker != null) {
                        mVelocityTracker.clear();
                    }
                }
                mLastMotionY = (int) ev.getY(ev.findPointerIndex(mActivePointerId));
                break;
            default:
        }
        if (mVelocityTracker != null) {
            mVelocityTracker.addMovement(ev);
        }
        return true;
    }

    private void ensureGlows() {
        if (getOverScrollMode() != View.OVER_SCROLL_NEVER) {
            if (mEdgeGlowTop == null) {
                Context context = getContext();
                mEdgeGlowTop = new EdgeEffect(context);
                mEdgeGlowBottom = new EdgeEffect(context);
            }
        } else {
            mEdgeGlowTop = null;
            mEdgeGlowBottom = null;
        }
    }

    private void fling(int velocityY) {
        mScroller.fling(0, mCurrentOffset,
                0, velocityY,
                0, 0,
                Integer.MIN_VALUE, Integer.MAX_VALUE);
        postInvalidateOnAnimation();
    }

    /**
     * fling过程调用的方法，由view触发
     */
    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            int oldY = mCurrentOffset;
            int y = mScroller.getCurrY();
            if (oldY != y) {
                final int range = getTotalOffset();
                overScrollBy(0, y - oldY,
                        0, oldY,
                        0, range,
                        0, mOverFlingDistance, false);

                final int overScrollMode = getOverScrollMode();
                final boolean canOverScroll = overScrollMode == OVER_SCROLL_ALWAYS ||
                        (overScrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && range > 0);
                if (canOverScroll) {
                    ensureGlows();
                    if (y <= 0 && oldY > 0) {
                        mEdgeGlowTop.onAbsorb((int) mScroller.getCurrVelocity());
                    } else if (y >= range && oldY < range) {
                        mEdgeGlowBottom.onAbsorb((int) mScroller.getCurrVelocity());
                    }
                }
            }
            postInvalidateOnAnimation();
        }
    }

    @Override
    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        if (!mScroller.isFinished()) {
            scrollTo(0, scrollY);
            if (clampedY) {
                mScroller.springBack(0, mCurrentOffset, 0, 0, 0, getTotalOffset());
            }
        } else {
            scrollTo(scrollX, scrollY);
        }
    }

    @Override
    public void scrollBy(int x, int y) {
        scrollTo(0, mCurrentOffset + y);
    }

    @Override
    public void scrollTo(int x, int y) {
        mCurrentOffset = y;
        invalidate();
    }

    private void endDrag() {
        mIsBeingDragged = false;
        recycleVelocityTracker();
        if (mEdgeGlowTop != null) {
            mEdgeGlowTop.onRelease();
            mEdgeGlowBottom.onRelease();
        }
    }

    private void initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    private int getTotalOffset() {
        return (mData.size() - 1) * mItemHeight;
    }

    private int dp2px(float dpValue) {
        final float scale = getContext().getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }


    public interface OnWheelChangeListener {
        /**
         * 滚轮切换监听
         *
         * @param index     目标
         * @param wheelBean 目标数据
         */
        void onWheelChange(int index, WheelBean wheelBean);
    }
}
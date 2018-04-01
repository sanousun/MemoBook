package com.sanousun.wheel.sample;

import android.graphics.PointF;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.View;

/**
 * @author dashu
 * @date 2017/10/10
 * 模仿滚轮的LayoutManager
 */

public class WheelLayoutManager extends RecyclerView.LayoutManager
        implements RecyclerView.SmoothScroller.ScrollVectorProvider {

    private static final String TAG = WheelLayoutManager.class.getSimpleName();

    /**
     * 最小缩放比例
     */
    private static final float MIN_SCALE = 0.05f;
    /**
     * 中间状态的view需要放大
     * 开始放大的阀值
     */
    private static final float START_MAGNIFY = 0.90f;
    /**
     * 达到最大的阀值
     */
    private static final float END_MAGNIFY = 0.99f;
    /**
     * 最大的放大比例
     */
    private static final float MAX_SCALE = 1.2f;
    /**
     * 垂直偏移
     */
    private int mVerticalScrollOffset = 0;
    /**
     * 基础偏移量，1/4周长
     */
    private int mBaseVerticalScrollOffset = 0;
    /**
     * item的顶点位置
     */
    private int mTotalHeight = 0;
    /**
     * 保存所有的Item的上下左右的偏移量信息
     */
    private SparseArray<Rect> mAllItemFrames = new SparseArray<>();
    /**
     * 记录Item是否出现过屏幕且还没有回收。true表示出现过屏幕上，并且还没被回收
     */
    private SparseBooleanArray mHasAttachedItems = new SparseBooleanArray();

    private int mTargetPos = 0;

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.WRAP_CONTENT,
                RecyclerView.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        // 没有数据就不进行处理
        if (getItemCount() <= 0) {
            return;
        }
        // 跳过preLayout，preLayout主要用于支持动画
        if (state.isPreLayout()) {
            return;
        }
        // 进行布局
        int offsetY = 0;
        mTotalHeight = 0;
        // 考虑到曲面上的距离，所以基础偏移是1/4周长减去半个item的高度
        mBaseVerticalScrollOffset = (int) (Math.PI * getVerticalSpace() / 4);
        for (int i = 0; i < getItemCount(); i++) {
            View child = recycler.getViewForPosition(i);
            // 开始计算大小
            measureChildWithMargins(child, 0, 0);
            // 计算宽度
            int width = getDecoratedMeasuredWidth(child);
            // 计算高度
            int height = getDecoratedMeasuredHeight(child);
            mTotalHeight += height;
            Rect frame = mAllItemFrames.get(i);
            if (frame == null) {
                frame = new Rect();
            }
            frame.set(0, offsetY, width, offsetY + height);
            // 将当前的Item的Rect边界数据保存
            mAllItemFrames.put(i, frame);
            // 将竖直方向偏移量增大height
            offsetY += height;
        }
        // 如果所有子View的高度和没有填满RecyclerView的高度，则将高度设置为RecyclerView的高度
        mTotalHeight = Math.max(mTotalHeight, getVerticalCurveSpace());
        mVerticalScrollOffset = getOffsetForPosition(mTargetPos);
        mTargetPos = 0;
        recycleAndFillItems(recycler, state);
    }

    /**
     * 回收不需要的Item，并且将需要显示的Item从缓存中取出
     */
    private void recycleAndFillItems(RecyclerView.Recycler recycler, RecyclerView.State state) {
        // 将所有的子view临时移除并且回收
        detachAndScrapAttachedViews(recycler);
        for (int i = 0; i < getItemCount(); i++) {
            // 由于已经调用了detachAndScrapAttachedViews，因此需要将当前的Item设置为未出现过
            mHasAttachedItems.put(i, false);
        }
        // 跳过preLayout，preLayout主要用于支持动画
        if (state.isPreLayout()) {
            return;
        }

        // 当前scroll offset状态下的显示区域
        // 在滚轮曲面的显示区域
        Rect displayFrame = new Rect(0, mVerticalScrollOffset,
                getHorizontalSpace(), mVerticalScrollOffset + getVerticalCurveSpace());

        // 将滑出屏幕的Items回收到Recycle缓存中
//        Rect childFrame = new Rect();
//        Log.i(TAG, getChildCount() + "");
//        for (int i = 0; i < getChildCount(); i++) {
//            View child = getChildAt(i);
//            getDecoratedBoundsWithMargins(child, childFrame);
//            // 如果Item没有在显示区域，就说明需要回收
//            if (!Rect.intersects(displayFrame, childFrame)) {
//                // 回收掉滑出屏幕的View
//                removeAndRecycleView(child, recycler);
//            }
//        }

        // 重新显示需要出现在屏幕的子View
        for (int i = 0; i < getItemCount(); i++) {
            if (Rect.intersects(displayFrame, mAllItemFrames.get(i))) {
                View scrap = recycler.getViewForPosition(i);
                measureChildWithMargins(scrap, 0, 0);
                addView(scrap);
                Rect frame = mAllItemFrames.get(i);
                // 将转换后在滚轮曲面上的布局展示出来
                setScrapView(scrap, displayFrame, frame);
            }
        }
    }

    private void setScrapView(View scrap, Rect displayFrame, Rect frame) {
        double displayMidPoint = (displayFrame.top + displayFrame.bottom) / 2.0 - mVerticalScrollOffset;
        double frameMidPoint = (frame.top + frame.bottom) / 2.0 - mVerticalScrollOffset;
        double disY = displayMidPoint - frameMidPoint;
        double frameHeight = frame.bottom - frame.top;
        double r = getVerticalSpace() / 2.0;
        double arc = disY / r;
        double verticalPoint = (1 - Math.sin(arc)) * r;
        // 布局位置，然后进行缩放
        layoutDecorated(scrap,
                frame.left,
                (int) (verticalPoint - frameHeight / 2),
                frame.right,
                (int) (verticalPoint + frameHeight / 2)
        );
        float scaleRate = (float) Math.cos(arc);
        if (scaleRate < MIN_SCALE) {
            scaleRate = MIN_SCALE;
        }
        float scaleX, scaleY;
        float alpha;
        if (scaleRate >= END_MAGNIFY) {
            scaleX = scaleY = MAX_SCALE;
        } else if (scaleRate >= START_MAGNIFY) {
            scaleX = scaleY = 1 + (scaleRate - START_MAGNIFY) / (END_MAGNIFY - START_MAGNIFY) * (MAX_SCALE - 1);
        } else {
            scaleX = 1.0f;
            scaleY = scaleRate;
        }
        if (scaleRate >= END_MAGNIFY) {
            alpha = 1f;
        } else {
            alpha = 1.0f - (END_MAGNIFY - scaleRate) / END_MAGNIFY;
        }
        scrap.setScaleX(scaleX);
        scrap.setScaleY(scaleY);
        scrap.setAlpha(alpha);
    }

    private int getVerticalSpace() {
        return getHeight() - getPaddingBottom() - getPaddingTop();
    }

    private int getVerticalCurveSpace() {
        int height = getHeight() - getPaddingBottom() - getPaddingTop();
        return (int) (height * Math.PI / 2);
    }

    private int getHorizontalSpace() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    /**
     * 禁止水平滑动
     */
    @Override
    public boolean canScrollHorizontally() {
        return false;
    }

    /**
     * 允许竖直滑动
     */
    @Override
    public boolean canScrollVertically() {
        return true;
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        return super.scrollHorizontallyBy(dx, recycler, state);
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (mAllItemFrames.size() == 0) {
            return 0;
        }
        // 实际要滑动的距离
        int travel = dy;
        // 如果滑动到最顶部
        if (mVerticalScrollOffset + dy < getOffsetForPosition(0)) {
            travel = getOffsetForPosition(0) - mVerticalScrollOffset;
        }
        // 如果滑动到最底部
        else if (mVerticalScrollOffset + dy > getOffsetForPosition(mAllItemFrames.size() - 1)) {
            travel = getOffsetForPosition(mAllItemFrames.size() - 1) - mVerticalScrollOffset;
        }
        // 将竖直方向的偏移量+travel
        mVerticalScrollOffset += travel;
//         平移容器内的item
//        offsetChildrenVertical(-travel);
        recycleAndFillItems(recycler, state);
        return travel;
    }

    @Override
    public void scrollToPosition(int position) {
        mTargetPos = position;
        requestLayout();
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView,
                                       RecyclerView.State state, int position) {

    }

    @Override
    public PointF computeScrollVectorForPosition(int targetPosition) {
        if (getChildCount() == 0) {
            return null;
        }
        final int currentPos = getPositionForOffset(0);
        final int direction = targetPosition < currentPos ? -1 : 1;
        return new PointF(0, direction);
    }

    private int getPositionForOffset(int offset) {
        int middle = mVerticalScrollOffset + mBaseVerticalScrollOffset + offset;
        if (middle < 0) {
            return 0;
        }
        for (int i = 0; i < mAllItemFrames.size(); i++) {
            Rect rect = mAllItemFrames.get(i);
            if (rect.top <= middle && rect.bottom >= middle) {
                return i;
            }
        }
        return mAllItemFrames.size() - 1;
    }

    private int getOffsetForPosition(int position) {
        Rect rect = mAllItemFrames.get(position);
        return rect.top + (rect.bottom - rect.top) / 2 - mBaseVerticalScrollOffset;
    }
}
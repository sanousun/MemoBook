package com.sanousun.wheel.sample;

import android.graphics.PointF;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.View;

/**
 * Created by dashu on 2017/10/10.
 * 模仿滚轮的LayoutManager
 */

public class WheelLayoutManager extends RecyclerView.LayoutManager
        implements RecyclerView.SmoothScroller.ScrollVectorProvider {

    // 垂直偏移
    private int verticalScrollOffset = 0;
    // 基础偏移量，因为第一个item并不是从最顶上开始的
    private int baseVerticalScrollOffset = 0;
    // item的顶点位置
    private int totalHeight = 0;
    // 保存所有的Item的上下左右的偏移量信息
    private SparseArray<Rect> allItemFrames = new SparseArray<>();
    // 记录Item是否出现过屏幕且还没有回收。true表示出现过屏幕上，并且还没被回收
    private SparseBooleanArray hasAttachedItems = new SparseBooleanArray();

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
        // 将所有的子view临时移除并且回收
        detachAndScrapAttachedViews(recycler);
        // 进行布局
        int offsetY = 0;
        totalHeight = 0;
        // 考虑到曲面上的距离，所以基础偏移是1/4周长减去半个item的高度
        baseVerticalScrollOffset = (int) (Math.PI * getVerticalSpace() / 4);
        for (int i = 0; i < getItemCount(); i++) {
            View child = recycler.getViewForPosition(i);
            addView(child);
            //开始计算大小
            measureChildWithMargins(child, 0, 0);
            //计算宽度
            int width = getDecoratedMeasuredWidth(child);
            //计算高度
            int height = getDecoratedMeasuredHeight(child);
            totalHeight += height;
            if (i == 0) {
                baseVerticalScrollOffset -= height / 2;
            }
            Rect frame = allItemFrames.get(i);
            if (frame == null) {
                frame = new Rect();
            }
            frame.set(0, offsetY, width, offsetY + height);
            // 将当前的Item的Rect边界数据保存
            allItemFrames.put(i, frame);
            // 由于已经调用了detachAndScrapAttachedViews，因此需要将当前的Item设置为未出现过
            hasAttachedItems.put(i, false);
            // 将竖直方向偏移量增大height
            offsetY += height;
        }
        // 如果所有子View的高度和没有填满RecyclerView的高度，则将高度设置为RecyclerView的高度
        totalHeight = Math.max(totalHeight, getVerticalCurveSpace());
        verticalScrollOffset = -baseVerticalScrollOffset;
        recycleAndFillItems(recycler, state);
    }

    /**
     * 回收不需要的Item，并且将需要显示的Item从缓存中取出
     */
    private void recycleAndFillItems(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (state.isPreLayout()) { // 跳过preLayout，preLayout主要用于支持动画
            return;
        }

        // 当前scroll offset状态下的显示区域
        // 在滚轮曲面的显示区域
        Rect displayFrame = new Rect(0, verticalScrollOffset,
                getHorizontalSpace(), verticalScrollOffset + getVerticalCurveSpace());

        // 将滑出屏幕的Items回收到Recycle缓存中
        Rect childFrame = new Rect();
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            childFrame.left = getDecoratedLeft(child);
            childFrame.top = getDecoratedTop(child);
            childFrame.right = getDecoratedRight(child);
            childFrame.bottom = getDecoratedBottom(child);
            //如果Item没有在显示区域，就说明需要回收
            if (!Rect.intersects(displayFrame, childFrame)) {
                //回收掉滑出屏幕的View
                removeAndRecycleView(child, recycler);
            }
        }

        // 重新显示需要出现在屏幕的子View
        for (int i = 0; i < getItemCount(); i++) {
            if (Rect.intersects(displayFrame, allItemFrames.get(i))) {
                View scrap = recycler.getViewForPosition(i);
                measureChildWithMargins(scrap, 0, 0);
                addView(scrap);
                Rect frame = allItemFrames.get(i);
                // 将转换后在滚轮曲面上的布局展示出来
                setScrapView(scrap, displayFrame, frame);
            }
        }
    }

    private void setScrapView(View scrap, Rect displayFrame, Rect frame) {
        double displayMidPoint = (displayFrame.top + displayFrame.bottom) / 2.0 - verticalScrollOffset;
        double frameMidPoint = (frame.top + frame.bottom) / 2.0 - verticalScrollOffset;
        double disY = displayMidPoint - frameMidPoint;
        double frameHeight = frame.bottom - frame.top;
        double r = getVerticalSpace() / 2.0;
        double arc = disY / r;
        double verticalPoint = (1 - Math.sin(arc)) * r;
        layoutDecorated(scrap,
                frame.left,
                (int) (verticalPoint - frameHeight / 2),
                frame.right,
                (int) (verticalPoint + frameHeight / 2)
        );
        float scaleRate = (float) Math.cos(arc);
        if (scaleRate < 0.05f) {
            scaleRate = 0.05f;
        }
        scrap.setScaleY(scaleRate);
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
        //先detach掉所有的子View
        detachAndScrapAttachedViews(recycler);
        //实际要滑动的距离
        int travel = dy;
        //如果滑动到最顶部
        if (verticalScrollOffset + dy < -baseVerticalScrollOffset) {
            travel = -baseVerticalScrollOffset - verticalScrollOffset;
        } else if (verticalScrollOffset + dy > totalHeight - getVerticalCurveSpace() + baseVerticalScrollOffset) {//如果滑动到最底部
            travel = totalHeight - getVerticalCurveSpace() + baseVerticalScrollOffset - verticalScrollOffset;
        }
        //将竖直方向的偏移量+travel
        verticalScrollOffset += travel;
        // 平移容器内的item
        offsetChildrenVertical(-travel);
        recycleAndFillItems(recycler, state);
        Log.d("--->", " childView count:" + getChildCount());
        return travel;
    }

    @Override
    public PointF computeScrollVectorForPosition(int targetPosition) {
        if (getChildCount() == 0) {
            return null;
        }
        final int firstChildPos = getPosition(getChildAt(0));
        final int direction = targetPosition < firstChildPos ? -1 : 1;
        return new PointF(0, direction);
    }
}

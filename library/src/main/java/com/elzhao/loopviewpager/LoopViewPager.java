package com.elzhao.loopviewpager;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.Scroller;

import com.elzhao.loopviewpager.log.Lg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class LoopViewPager extends ViewGroup {

    private static final int COUNT_VISIBLE = 3;

    private static final float FRICTION = 10.0f;
    private static final float MAX_SPEED = 6.0f;

    private LoopPagerAdapter mAdapter;
    private AdapterDataSetObserver mDataSetObserver;
    private boolean mDataChanged = false;
    private List<ViewHolder> mCacheViewList = new ArrayList<>();
    private List<ViewHolder> mShowViewList = new ArrayList<>();
    private float mLastMotionX;

    private List<OnPageChangeListener> mPageChangeListeners = new ArrayList<>();
    /**
     * Determines speed during touch scrolling
     */
    private VelocityTracker mVelocityTracker;
    private Scroller mScroller;
    private PageTransformer mPageTransformer;
    private boolean mIsBeingDragged;
    private int mCurItem;// Index of currently displayed page.

    private static final Interpolator sInterpolator = new Interpolator() {
        public float getInterpolation(float t) {
            t -= 1.0f;
            return t * t * t * t * t + 1.0f;
        }
    };

    public LoopViewPager(Context context) {
        this(context, null);
    }

    public LoopViewPager(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LoopViewPager(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mScroller = new Scroller(context, sInterpolator);
    }

    public void setAdapter(LoopPagerAdapter adapter) {
        Lg.i();
        LoopPagerAdapter old = mAdapter;
        if (old != null) {
            old.unregisterDataSetObserver(mDataSetObserver);
        }

        mAdapter = adapter;
        if (adapter != null) {
            if (mDataSetObserver == null) {
                mDataSetObserver = new AdapterDataSetObserver();
            }
            adapter.registerDataSetObserver(mDataSetObserver);
        }
        mDataChanged = true;

        resetPagerState();
    }

    private void resetPagerState() {
        mCacheViewList.clear();
        mShowViewList.clear();
        removeAllViews();

        if (mAdapter.getCount() > 0) {
            firstAddChildViews();
        }
        scrollTo(0, 0);
        requestLayout();
        mCurItem = 0;
    }

    private void firstAddChildViews() {
        if (mDataChanged) {
            int width = getWidth();
            if (width > 0) {
                mDataChanged = false;
                updateShowList();
                addChildViews();
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Lg.i();
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mAdapter == null || getChildCount() == 0) {
            return;
        }
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(widthMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
        int height = MeasureSpec.getSize(heightMeasureSpec) - getPaddingTop() - getPaddingBottom();
        int itemMaxWidth = width / COUNT_VISIBLE;
        setMeasuredDimension(width, height);

        for (int i = 0; i < getChildCount(); ++i) {
            View view = getChildAt(i);
            measureChild(view, MeasureSpec.makeMeasureSpec(itemMaxWidth, widthMode), MeasureSpec.makeMeasureSpec(heightMode, height));
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        Lg.i();
        if (mAdapter == null || mAdapter.getCount() <= 0) {
            return;
        }
        firstAddChildViews();
        layoutChildViews();
        onPagedScrolled();
    }

    private void addChildViews() {
        removeAllViews();
        updateViewHolderState();
        for (ViewHolder vh:mShowViewList) {
            addView(vh.itemView);
        }
    }

    private void layoutChildViews() {
        for (ViewHolder vh:mShowViewList) {
            layoutChild(vh);
        }
    }

    private void layoutChild(ViewHolder vh) {
        int itemWidth = getWidth() / COUNT_VISIBLE;
        int itemHeight = getHeight();
        int left = (itemWidth - vh.itemView.getMeasuredWidth()) / 2 + vh.position * itemWidth;
        int top = (itemHeight - vh.itemView.getMeasuredHeight()) / 2;
        int right = left + vh.itemView.getMeasuredWidth();
        int bottom = top + vh.itemView.getMeasuredHeight();
        vh.itemView.layout(left, top, right, bottom);
    }

    /**
     * 更新要显示的View列表。
     */
    private void updateShowList() {
        int firstPos = calculateFirstPosition();
        int lastPos = firstPos + COUNT_VISIBLE + 2;

        Collections.sort(mShowViewList, new Comparator<ViewHolder>() {
            @Override
            public int compare(ViewHolder lhs, ViewHolder rhs) {
                return lhs.position - rhs.position;
            }
        });
        ViewHolder tempHolder;
        while (mShowViewList.size() > 0) {
            tempHolder = mShowViewList.get(0);
            if (tempHolder.position < firstPos) {
                mCacheViewList.add(mShowViewList.remove(0));
            } else {
                break;
            }
        }
        while (mShowViewList.size() > 0) {
            tempHolder = mShowViewList.get(mShowViewList.size() - 1);
            if (tempHolder.position > lastPos) {
                mCacheViewList.add(mShowViewList.remove(mShowViewList.size() - 1));
            } else {
                break;
            }
        }
        if (mShowViewList.size() == 0) {
            for (int i = firstPos; i <= lastPos; i++) {
                mShowViewList.add(queryOrCreateViewHolder(i));
            }
        } else {
            int tempPos1 = mShowViewList.get(0).position;
            int tempPos2 = mShowViewList.get(mShowViewList.size() - 1).position;
            for (int i = firstPos; i < tempPos1; i++) {
                mShowViewList.add(queryOrCreateViewHolder(i));
            }
            for (int i = tempPos2 + 1; i <= lastPos; i++) {
                mShowViewList.add(queryOrCreateViewHolder(i));
            }
        }
        int centerX = getScrollX() + getWidth() / 2;
        for (ViewHolder vh:mShowViewList) {
            vh.dCenterX = centerX - (2 * vh.position + 1) * getWidth() / COUNT_VISIBLE / 2;
            vh.dCenterX = Math.abs(vh.dCenterX);
        }
    }

    /**
     * 根据滑动偏移量计算出第一个View的实际位置。
     * (这个位置需要经过转换才能得到实际Item位置)
     * {@link #calculateValidPosition(int)}
     *
     * @return
     *          第一个View的实际位置
     */
    private int calculateFirstPosition() {
        int scrollX = getScrollX();
        int itemWidth = getWidth() / COUNT_VISIBLE;
        int firstPos = scrollX / itemWidth - 1;
        if (scrollX < 0) {
            firstPos --;
        }
        return firstPos;
    }

    /**
     * 根据实际位置计算出Item的有效位置。
     * {@link #calculateFirstPosition()}
     * @param position
     *          实际位置
     * @return
     *          有效位置
     */
    private int calculateValidPosition(int position) {
        final int maxCount = mAdapter.getCount();
        while (position < 0 || position >= maxCount) {
            if (position < 0) {
                position += maxCount;
            } else {
                position -= maxCount;
            }
        }
        return position;
    }

    /**
     * 根据实际位置获取ViewHolder对象，先从mCacheViewList缓存列表中查询，
     * 如果未找到合适的就重新创建。
     * @param position
     *          实际位置
     * @return
     *          ViewHolder对象
     */
    private ViewHolder queryOrCreateViewHolder(int position) {
        int validPos = calculateValidPosition(position);
        int type = mAdapter.getItemViewType(validPos);
        ViewHolder viewHolder = null;
        for (ViewHolder vh:mCacheViewList) {
            if (type == vh.type) {
                viewHolder = vh;
            }
        }
        if (viewHolder == null) {
            viewHolder = mAdapter.onCreateViewHolder(this, validPos);
            viewHolder.type = mAdapter.getItemViewType(validPos);
        }
        mCacheViewList.remove(viewHolder);
        viewHolder.position = position;
        //noinspection unchecked
        mAdapter.onBindViewHolder(viewHolder, validPos);
        return viewHolder;
    }

    /**
     * 更新mShowViewList列表所有ViewHolder状态。
     * 主要更新ViewHolder.dCenterX值，并重新排序。
     */
    private void updateViewHolderState() {
        int centerX = getScrollX() + getWidth() / 2;
        for (ViewHolder vh:mShowViewList) {
            vh.dCenterX = centerX - (2 * vh.position + 1) * getWidth() / COUNT_VISIBLE / 2;
            vh.dCenterX = Math.abs(vh.dCenterX);
        }
        Collections.sort(mShowViewList, new Comparator<ViewHolder>() {
            @Override
            public int compare(ViewHolder lhs, ViewHolder rhs) {
                return rhs.dCenterX - lhs.dCenterX;
            }
        });
    }

    /**
     * 获取显示在中心View的位置。
     * @return
     *          显示在中心View的实际位置
     */
    private int getCenterPosition() {
        Collections.sort(mShowViewList, new Comparator<ViewHolder>() {
            @Override
            public int compare(ViewHolder lhs, ViewHolder rhs) {
                return rhs.dCenterX - lhs.dCenterX;
            }
        });
        int size = mShowViewList.size();
        return size == 0 ? -1 : mShowViewList.get(size - 1).position;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mAdapter == null || getChildCount() == 0) {
            return false;
        }
        int action = event.getAction();
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastMotionX = event.getX();
                break;
            case MotionEvent.ACTION_MOVE:
                touchMoved(event.getX());
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                touchEnded();
                break;
            default:
                break;
        }

        return true;
    }

    private void touchMoved(float x) {
        Lg.i();
        int dx = (int) (mLastMotionX - x);
        if (dx != 0) {
            int targetX = dx + getScrollX();
            mIsBeingDragged = true;
            scrollTo(targetX, 0);
        }
        mLastMotionX = x;
    }

    private void touchEnded() {
        int scrollX = getScrollX();
        int itemWidth = getWidth() / COUNT_VISIBLE;

        mVelocityTracker.computeCurrentVelocity(1000);
        float speed = mVelocityTracker.getXVelocity() / getWidth();
        if (speed > MAX_SPEED) {
            speed = MAX_SPEED;
        } else if (speed < -MAX_SPEED) {
            speed = -MAX_SPEED;
        }
        float delta = speed * speed / (2 * FRICTION);
        if (speed > 0) {
            delta = -delta;
        }
        float targetX = scrollX + delta * itemWidth;
        int nextPosition = CommonUtils.rounding(targetX / itemWidth);

        mVelocityTracker.clear();
        mVelocityTracker.recycle();
        mVelocityTracker = null;

        Lg.i("speed: " + speed + " - delta: " + delta + " - nextPosition: " + nextPosition);
        if (mIsBeingDragged) {
            setCurrentItemInternal(nextPosition, true);
        }
        mIsBeingDragged = false;
    }

    @Override
    public void scrollTo(int x, int y) {
        super.scrollTo(x, y);
        onPagedScrolled();
        boolean needRequestLayout = needRequestLayout();
        Lg.i("needRequestLayout: " + needRequestLayout + " - scrollX: " + getScrollX());
        if (needRequestLayout) {
            updateShowList();
            addChildViews();
            requestLayout();
        }
    }

    private void onPagedScrolled() {
        if (mPageTransformer != null) {
            final int scrollX = getScrollX();
            int itemWidth = getWidth() / COUNT_VISIBLE;
            for (ViewHolder vh:mShowViewList) {
                final View child = vh.itemView;
                final float transformPos = (float) (vh.position * itemWidth - scrollX) / getWidth();
                mPageTransformer.transformPage(child, transformPos);
            }
        }
    }

    /**
     * 根据中心位置View是否变化来判断是否要重新刷新布局。
     * @return
     *          返回是否要刷新布局
     */
    private boolean needRequestLayout() {
        int oldCenterPos = getCenterPosition();
        updateViewHolderState();
        int newCenterPos = getCenterPosition();
        return oldCenterPos != newCenterPos;
    }

    /**
     * Set the currently selected page.
     *
     * @param item Item index to select
     * @param smoothScroll True to smoothly scroll to the new item, false to transition immediately
     */
    public void setCurrentItem(int item, boolean smoothScroll) {
        int scrollX = getScrollX();
        int itemWidth = getWidth() / COUNT_VISIBLE;
        int firstVisiblePosition = scrollX / itemWidth;
        if (scrollX < 0) {
            firstVisiblePosition --;
        }
        int firstVisibleValidPosition = calculateValidPosition(firstVisiblePosition);
        int actualDestPosition = firstVisiblePosition + item - firstVisibleValidPosition;
        setCurrentItemInternal(actualDestPosition, smoothScroll);
    }

    private void setCurrentItemInternal(int position, boolean smoothScroll) {
        int itemWidth = getWidth() / COUNT_VISIBLE;
        int desScrollX = position * itemWidth;
        int dx = desScrollX - getScrollX();
        if (smoothScroll) {
            mScroller.startScroll(getScrollX(), 0, dx, 0, 300);
        } else {
            scrollTo(desScrollX, getScrollY());
        }
        invalidate();
        mCurItem = calculateValidPosition(position);
        notifyPageSelected(mCurItem);
    }

    @Override
    public void computeScroll() {
        Lg.i();
        if (!mScroller.computeScrollOffset()) {
            //计算currX，currY,并检测是否已完成“滚动”
            return;
        }
        int tempX = mScroller.getCurrX();
        scrollTo(tempX, 0);
        invalidate();
    }

    public int getCurrentItem() {
        return mCurItem;
    }

    public void setPageTransformer(PageTransformer transformer) {
        mPageTransformer = transformer;
    }

    private void dataSetChanged() {
        updateShowList();
        bindViewHolder();
        addChildViews();
        requestLayout();
    }

    private void bindViewHolder() {
        for (ViewHolder vh:mShowViewList) {
            int validPos = calculateValidPosition(vh.position);
            //noinspection unchecked
            mAdapter.onBindViewHolder(vh, validPos);
        }
    }

    private void notifyPageSelected(int page) {
        for (OnPageChangeListener listener:mPageChangeListeners) {
            listener.onPageSelected(page);
        }
    }

    public void addOnPageChangeListener(OnPageChangeListener listener) {
        if (!mPageChangeListeners.contains(listener)) {
            mPageChangeListeners.add(listener);
        }
    }

    public void removeOnPageChangeListener(OnPageChangeListener listener) {
        mPageChangeListeners.remove(listener);
    }

    public static class ViewHolder {
        private final View itemView;
        private int position;
        private int dCenterX;
        private int index;
        private int type;
        public ViewHolder(View itemView) {
            this.itemView = itemView;
        }

        public View getItemView() {
            return itemView;
        }
    }

    private class AdapterDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            dataSetChanged();
        }

        @Override
        public void onInvalidated() {
            dataSetChanged();
        }
    }

    /**
     * A PageTransformer is invoked whenever a visible/attached page is scrolled.
     * This offers an opportunity for the application to apply a custom transformation
     * to the page views using animation properties.
     *
     * <p>As property animation is only supported as of Android 3.0 and forward,
     * setting a PageTransformer on a ViewPager on earlier platform versions will
     * be ignored.</p>
     */
    public interface PageTransformer {
        /**
         * Apply a property transformation to the given page.
         *
         * @param page Apply the transformation to this page
         * @param position Position of page relative to the current front-and-center
         *                 position of the pager. 0 is front and center. 1 is one full
         *                 page position to the right, and -1 is one page position to the left.
         */
        void transformPage(View page, float position);
    }

    /**
     * Callback interface for responding to changing state of the selected page.
     */
    public interface OnPageChangeListener {

        /**
         * This method will be invoked when a new page becomes selected. Animation is not
         * necessarily complete.
         *
         * @param position Position index of the new selected page.
         */
        void onPageSelected(int position);

    }
}

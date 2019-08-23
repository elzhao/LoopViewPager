package com.elzhao.loopviewpager;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.Interpolator;
import android.widget.Scroller;

import com.elzhao.loopviewpager.log.Lg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class LoopViewPager extends ViewGroup {

    /**
     * Indicates that the pager is in an idle, settled state. The current page
     * is fully in view and no animation is in progress.
     */
    public static final int SCROLL_STATE_IDLE = 0;

    /**
     * Indicates that the pager is currently being dragged by the user.
     */
    public static final int SCROLL_STATE_DRAGGING = 1;

    /**
     * Indicates that the pager is in the process of settling to a final position.
     */
    public static final int SCROLL_STATE_SETTLING = 2;

    private static final float FRICTION = 10.0f;
    private static final float MAX_SPEED = 6.0f;
    private static final int DEFAULT_GUTTER_SIZE = 25;
    private static final boolean DEBUG = true;

    private LoopPagerAdapter mAdapter;
    private AdapterDataSetObserver mDataSetObserver;
    private boolean mDataChanged = false;
    private List<ViewHolder> mCacheViewList = new ArrayList<>();
    private List<ViewHolder> mShowViewList = new ArrayList<>();
    private float mLastMotionX;
    private int mWidth;
    private int mHeight;
    private int mChildMaxWidth;

    private List<OnPageChangeListener> mPageChangeListeners = new ArrayList<>();
    /**
     * Determines speed during touch scrolling
     */
    private VelocityTracker mVelocityTracker;
    private Scroller mScroller;
    private PageTransformer mPageTransformer;
    private boolean mIsBeingDragged;
    private int mCurItem;// Index of currently displayed page.
    private int mItemCountInPage;// Count of item show in one page.
    private boolean mScrollPending;
    private boolean mFirstLayout = true;
    private int mScrollState = SCROLL_STATE_IDLE;

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
            mFirstLayout = true;
        }
        mDataChanged = true;
        mScrollPending = false;
        mItemCountInPage = mAdapter == null ? 0 : mAdapter.getItemCountInPage();

        resetPagerState();
    }

    private void resetPagerState() {
        mCacheViewList.clear();
        mShowViewList.clear();
        removeAllViews();
        if (mAdapter != null && mAdapter.getCount() > 0) {
            firstAddChildViews();
        }
        scrollTo(0, 0);
        requestLayout();
        mCurItem = 0;
    }

    private void firstAddChildViews() {
        if (mDataChanged && !mFirstLayout) {
            mDataChanged = false;
            updateShowList();
            addChildViews();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mFirstLayout = true;
        if (DEBUG) Lg.i();
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (DEBUG) Lg.i("hasWindowFocus: " + hasWindowFocus);
        if (hasWindowFocus) {
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (DEBUG) Lg.i();
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mAdapter == null) {
            return;
        }
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(widthMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
        int height = MeasureSpec.getSize(heightMeasureSpec) - getPaddingTop() - getPaddingBottom();
        int itemWidth = width / mItemCountInPage;
        setMeasuredDimension(width, height);
        mWidth = width;
        mHeight = height;
        mChildMaxWidth = itemWidth;

        for (int i = 0; i < getChildCount(); ++i) {
            View view = getChildAt(i);
            measureChild(view, MeasureSpec.makeMeasureSpec(itemWidth, widthMode), MeasureSpec.makeMeasureSpec(height, heightMode));
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (DEBUG) Lg.i();
        if (mAdapter == null || mAdapter.getCount() <= 0) {
            return;
        }
        mFirstLayout = false;
        if (mScrollPending) {
            mScrollPending = false;
            setCurrentItem(mCurItem, false);
        }
        firstAddChildViews();
        layoutChildViews();
        onPagedScrolled();
    }

    private void addChildViews() {
        removeAllViews();
        updateViewHolderState();
        for (ViewHolder vh : mShowViewList) {
            addView(vh.itemView);
        }
    }

    private void layoutChildViews() {
        for (ViewHolder vh : mShowViewList) {
            layoutChild(vh);
        }
    }

    private void layoutChild(ViewHolder vh) {
        int left = (mChildMaxWidth - vh.itemView.getMeasuredWidth()) / 2 + vh.position * mChildMaxWidth;
        int top = (mHeight - vh.itemView.getMeasuredHeight()) / 2;
        int right = left + vh.itemView.getMeasuredWidth();
        int bottom = top + vh.itemView.getMeasuredHeight();
        vh.itemView.layout(left, top, right, bottom);
    }

    /**
     * 更新要显示的View列表。
     */
    private void updateShowList() {
        int firstPos = calculateFirstPosition();
        int lastPos = firstPos + mItemCountInPage + 2;

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
        updateViewHolderState();
    }

    /**
     * 根据滑动偏移量计算出第一个View的实际位置。
     * (这个位置需要经过转换才能得到实际Item位置)
     * {@link #calculateValidPosition(int)}
     *
     * @return 第一个View的实际位置
     */
    private int calculateFirstPosition() {
        int scrollX = getScrollX();
        int firstPos = scrollX / mChildMaxWidth - 1;
        if (scrollX < 0) {
            firstPos--;
        }
        return firstPos;
    }

    /**
     * 根据实际位置计算出Item的有效位置。
     * {@link #calculateFirstPosition()}
     *
     * @param position 实际位置
     * @return 有效位置
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
     *
     * @param position 实际位置
     * @return ViewHolder对象
     */
    private ViewHolder queryOrCreateViewHolder(int position) {
        int validPos = calculateValidPosition(position);
        int type = mAdapter.getItemViewType(validPos);
        ViewHolder viewHolder = null;
        for (ViewHolder vh : mCacheViewList) {
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
        int centerX = getScrollX() + mWidth / 2;
        for (ViewHolder vh : mShowViewList) {
            vh.dCenterX = centerX - mChildMaxWidth * (2 * vh.position + 1) / 2;
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
     *
     * @return 显示在中心View的实际位置
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
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (DEBUG) Lg.i("mScrollState: " + mScrollState);
                mLastMotionX = ev.getX();
                if (mScrollState == SCROLL_STATE_SETTLING) {
                    mIsBeingDragged = true;
                    mScroller.abortAnimation();
                    requestParentDisallowInterceptTouchEvent(true);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                final float dx = ev.getX() - mLastMotionX;
                if (Math.abs(dx) > DEFAULT_GUTTER_SIZE) {
                    mLastMotionX = ev.getX();
                    mIsBeingDragged = true;
                    requestParentDisallowInterceptTouchEvent(true);
                }
                break;
            default:
                break;
        }
        return mIsBeingDragged;
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
        int dx = (int) (mLastMotionX - x);
        mLastMotionX = x;
        if (dx == 0) {
            return;
        }
        if (!mIsBeingDragged) {
            mIsBeingDragged = true;
            requestParentDisallowInterceptTouchEvent(true);
        }
        int targetX = dx + getScrollX();
        setScrollState(SCROLL_STATE_DRAGGING);
        scrollTo(targetX, 0);
    }

    private void touchEnded() {
        int scrollX = getScrollX();
        mVelocityTracker.computeCurrentVelocity(1000);
        float speed = mVelocityTracker.getXVelocity() / mWidth;
        if (speed > MAX_SPEED) {
            speed = MAX_SPEED;
        } else if (speed < -MAX_SPEED) {
            speed = -MAX_SPEED;
        }
        float delta = speed * speed / (2 * FRICTION);
        if (speed > 0) {
            delta = -delta;
        }
        float targetX = scrollX + delta * mChildMaxWidth;
        int nextPosition = rounding(targetX / mChildMaxWidth);

        mVelocityTracker.clear();
        mVelocityTracker.recycle();
        mVelocityTracker = null;

        if (mIsBeingDragged) {
            setCurrentItemInternal(nextPosition, true);
        }
        mIsBeingDragged = false;
    }

    private static int rounding(float value) {
        float temp = value - (int) value;
        int result = (int) value;
        if (temp >= 0.5) {
            result++;
        } else if (temp <= -0.5) {
            result--;
        }
        return result;
    }

    @Override
    public void scrollTo(int x, int y) {
        super.scrollTo(x, y);
        onPagedScrolled();
        boolean needRequestLayout = needRequestLayout();
        if (needRequestLayout) {
            updateShowList();
            addChildViews();
            requestLayout();
        }
    }

    private void onPagedScrolled() {
        if (mPageTransformer != null) {
            final int scrollX = getScrollX();
            for (ViewHolder vh : mShowViewList) {
                final View child = vh.itemView;
                final float transformPos = (float) (vh.position * mChildMaxWidth - scrollX) / mWidth;
                mPageTransformer.transformPage(child, transformPos);
            }
        }
    }

    /**
     * 根据中心位置View是否变化来判断是否要重新刷新布局。
     *
     * @return 返回是否要刷新布局
     */
    private boolean needRequestLayout() {
        int oldCenterPos = getCenterPosition();
        updateViewHolderState();
        int newCenterPos = getCenterPosition();
        return oldCenterPos != newCenterPos;
    }

    /**
     * Set the currently selected page. If the ViewPager has already been through its first
     * layout with its current adapter there will be a smooth animated transition between
     * the current item and the specified item.
     *
     * @param item Item index to select
     */
    public void setCurrentItem(int item) {
        setCurrentItem(item, !mFirstLayout);
    }

    /**
     * Set the currently selected page.
     *
     * @param item         Item index to select
     * @param smoothScroll True to smoothly scroll to the new item, false to transition immediately
     */
    public void setCurrentItem(int item, boolean smoothScroll) {
        if (mAdapter == null || mAdapter.getCount() == 0) {
            return;
        }
        if (item < 0) {
            item = 0;
        } else if (item >= mAdapter.getCount()) {
            item = mAdapter.getCount() - 1;
        }
        mCurItem = item;
        if (mFirstLayout) {
            mScrollPending = true;
            return;
        }
        int scrollX = getScrollX();
        int firstVisiblePosition = scrollX / mChildMaxWidth;
        if (scrollX < 0) {
            firstVisiblePosition--;
        }
        int firstVisibleValidPosition = calculateValidPosition(firstVisiblePosition);
        int actualDestPosition = firstVisiblePosition + item - firstVisibleValidPosition;
        setCurrentItemInternal(actualDestPosition, smoothScroll);
    }

    private void setCurrentItemInternal(int position, boolean smoothScroll) {
        int desScrollX = position * mChildMaxWidth;
        int dx = desScrollX - getScrollX();
        mScroller.abortAnimation();
        if (smoothScroll) {
            setScrollState(SCROLL_STATE_SETTLING);
            mScroller.startScroll(getScrollX(), 0, dx, 0, 300);
        } else {
            scrollTo(desScrollX, getScrollY());
        }
        invalidate();
        mCurItem = calculateValidPosition(position);
        dispatchOnPageSelected(mCurItem);
    }

    private void setScrollState(int newState) {
        if (DEBUG) Lg.i("newState: " + newState + " - mScrollState: " + mScrollState);
        if (mScrollState == newState) {
            return;
        }
        mScrollState = newState;
    }

    private void requestParentDisallowInterceptTouchEvent(boolean disallowIntercept) {
        final ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(disallowIntercept);
        }
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            int tempX = mScroller.getCurrX();
            scrollTo(tempX, 0);
            invalidate();
            return;
        }
        //计算currX，currY,并检测是否已完成“滚动”
        if (!mIsBeingDragged) {
            setScrollState(SCROLL_STATE_IDLE);
        }
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
        for (ViewHolder vh : mShowViewList) {
            int validPos = calculateValidPosition(vh.position);
            //noinspection unchecked
            mAdapter.onBindViewHolder(vh, validPos);
        }
    }

    private void dispatchOnPageSelected(int page) {
        for (OnPageChangeListener listener : mPageChangeListeners) {
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
         * @param page     Apply the transformation to this page
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

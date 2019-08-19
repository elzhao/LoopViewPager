package com.elzhao.loopviewpager;

import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.view.ViewGroup;

public abstract class LoopPagerAdapter<VH extends LoopViewPager.ViewHolder> {

    private final DataSetObservable mDataSetObservable = new DataSetObservable();

    void registerDataSetObserver(DataSetObserver observer) {
        mDataSetObservable.registerObserver(observer);
    }

    void unregisterDataSetObserver(DataSetObserver observer) {
        mDataSetObservable.unregisterObserver(observer);
    }

    /**
     * CoverFlowView数据集发生改变，刷新，尽量保留状态且不影响滑动事件
     */
    public void notifyDataSetChanged() {
        mDataSetObservable.notifyChanged();
    }

    /**
     * CoverFlowView重绘刷新。重置CoverFlowView的所有状态。
     */
    public void notifyDataSetInvalidated() {
        mDataSetObservable.notifyInvalidated();
    }

    public abstract int getCount();

    public abstract VH onCreateViewHolder(ViewGroup parent, int position);

    public abstract void onBindViewHolder(VH vh, int position);

    public int getItemViewType(int position) {
        return 0;
    }
}

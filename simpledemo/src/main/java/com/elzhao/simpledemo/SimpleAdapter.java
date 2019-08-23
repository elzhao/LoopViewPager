package com.elzhao.simpledemo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.elzhao.loopviewpager.LoopPagerAdapter;
import com.elzhao.loopviewpager.LoopViewPager;
import com.elzhao.loopviewpager.log.Lg;

import java.util.List;

public class SimpleAdapter extends LoopPagerAdapter<SimpleAdapter.SimpleHolder> {

    private Context mContext;
    private List<String> mDataList;

    SimpleAdapter(Context context, List<String> dataList) {
        mContext = context;
        mDataList = dataList;
    }

    @Override
    public int getCount() {
        return mDataList == null ? 0 : mDataList.size();
    }

    @Override
    public SimpleAdapter.SimpleHolder onCreateViewHolder(ViewGroup parent, int position) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_view, parent, false);
        return new SimpleHolder(view);
    }

    @Override
    public void onBindViewHolder(SimpleAdapter.SimpleHolder viewHolder, final int position) {
        String content = mDataList.get(position);
        viewHolder.tvContent.setText(content);
        viewHolder.getItemView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Lg.i("position: " + position);
            }
        });
    }

    @Override
    public int getItemCountInPage() {
        return 3;
    }

    class SimpleHolder extends LoopViewPager.ViewHolder{

        TextView tvContent;

        SimpleHolder(View itemView) {
            super(itemView);
            tvContent = (TextView) itemView.findViewById(R.id.tv_content);
        }
    }
}

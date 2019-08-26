package com.elzhao.simpledemo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import com.elzhao.loopviewpager.LoopViewPager;
import com.elzhao.loopviewpager.ScaleTransfer;
import com.elzhao.loopviewpager.log.Lg;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements LoopViewPager.OnPageChangeListener, SimpleAdapter.OnItemClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private LoopViewPager mLoopViewPager;
    private SimpleAdapter mAdapter;
    private List<String> mDataList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Lg.setModuleName(TAG);
        for (int i = 0; i < 20; i ++) {
            mDataList.add(String.valueOf(i));
        }

        mLoopViewPager = (LoopViewPager) findViewById(R.id.loopViewPager);
        mAdapter = new SimpleAdapter(this, mDataList, this);
        mLoopViewPager.setAdapter(mAdapter);
        mLoopViewPager.addOnPageChangeListener(this);
        mLoopViewPager.setPageTransformer(new ScaleTransfer());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLoopViewPager.removeOnPageChangeListener(this);
    }

    @Override
    public void onPageSelected(int position, boolean fromUser) {
        Lg.i("position: " + position + " - fromUser: " + fromUser);
    }

    @Override
    public void onItemClick(View view, int position) {
        Lg.i("position: " + position);
        position --;
        if (position < 0) {
            position += mAdapter.getCount();
        }
        mLoopViewPager.setCurrentItem(position);
    }
}

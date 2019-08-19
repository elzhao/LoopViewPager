package com.elzhao.simpledemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.elzhao.loopviewpager.LoopViewPager;
import com.elzhao.loopviewpager.ScaleTransfer;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements LoopViewPager.OnPageChangeListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private LoopViewPager mLoopViewPager;
    private SimpleAdapter mAdapter;
    private List<String> mDataList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        for (int i = 0; i < 20; i ++) {
            mDataList.add(String.valueOf(i));
        }

        mLoopViewPager = findViewById(R.id.loopViewPager);
        mAdapter = new SimpleAdapter(this, mDataList);
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
    public void onPageSelected(int position) {
        Log.i(TAG, "onPageSelected position: " + position);
    }
}
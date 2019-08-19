package com.elzhao.loopviewpager;

import android.view.View;

public class ScaleTransfer implements LoopViewPager.PageTransformer {

    private static final float MIN_SCALE = 0.8f;
    private static final float MAX_SCALE = 1.2f;
    private static final float MIN_ALPHA = 0.2f;

    @Override
    public void transformPage(View page, float position) {
        final float base = 1 / 3f;
        if (position < -base || position > base + 1) {
            page.setScaleX(1);
            page.setScaleY(1);
            page.setTranslationX(0);
            page.setAlpha(1);
        } else if (position < base) {
            float scale = (position + base) / (2 * base);
            page.setScaleX(scale * (MAX_SCALE - MIN_SCALE) + MIN_SCALE);
            page.setScaleY(scale * (MAX_SCALE - MIN_SCALE) + MIN_SCALE);
            page.setAlpha(scale * (1 - MIN_ALPHA) + MIN_ALPHA);
        } else {
            float scale = (1 - position) / (2 * base);
            page.setScaleX(scale * (MAX_SCALE - MIN_SCALE) + MIN_SCALE);
            page.setScaleY(scale * (MAX_SCALE - MIN_SCALE) + MIN_SCALE);
            page.setAlpha(scale * (1 - MIN_ALPHA) + MIN_ALPHA);
        }
    }
}

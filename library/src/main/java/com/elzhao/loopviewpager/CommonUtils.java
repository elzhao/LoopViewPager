package com.elzhao.loopviewpager;

public class CommonUtils {

    public static int rounding(float value) {
        float temp = value - (int) value;
        int result = (int) value;
        if (temp >= 0.5) {
            result ++;
        } else if (temp <= -0.5) {
            result --;
        }
        return result;
    }

}

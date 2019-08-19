package com.elzhao.loopviewpager.log;

import android.util.Log;

/**
 * Log日志工具类, 测试时才打印日志
 * &lt;功能详细描述&gt;
 *
 * @author administrator
 * @version [版本号]
 * @see [相关类/方法]
 * @since [产品/模块版本]
 */
class LogUtil {
    private static final String PLUS = "--->";

    //打印i级别日志
    static void i(String module, String tag, String msg) {
        // if (isLogOut())
        Log.i(module, tag + PLUS + msg);
    }

    //打印d级别日志
    static void d(String module, String tag, String msg) {
        // if (isLogOut())
        Log.d(module, tag + PLUS + msg);
    }

    //打印w级别日志
    static void w(String module, String tag, String msg) {
        //if (isLogOut())
        Log.w(module, tag + PLUS + msg);
    }

    //打印e级别日志
    static void e(String module, String tag, String msg) {
        // if (isLogOut())
        Log.e(module, tag + PLUS + msg);
    }

    //打印v级别日志
    static void v(String module, String tag, String msg) {
        // if (isLogOut())
        Log.v(module, tag + PLUS + msg);
    }

    //是否打印日志
    private static boolean isLogOut() {
        return true;
    }
}

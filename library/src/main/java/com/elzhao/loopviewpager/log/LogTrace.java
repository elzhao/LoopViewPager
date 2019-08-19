package com.elzhao.loopviewpager.log;

/**
 * 日志工具类
 * &lt;功能详细描述&gt;
 *
 * @author administrator
 * @version [版本号]
 * @see [相关类/方法]
 * @since [产品/模块版本]
 */
public class LogTrace {

    private String moduleName = "";
    private String tag = "";

    /**
     * 根据模块类型创建
     *
     * @param module String
     */
    public LogTrace(String module) {
        moduleName = module;
    }

    /**
     * 根据模块标签创建
     *
     * @param module String
     * @param tag    String
     */
    public LogTrace(String module, String tag) {
        moduleName = module;
        this.tag = tag;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    //打印i级别方法信息
    public void i(String method) {
        LogUtil.i(moduleName, tag, method);
    }

    //打印d级别方法信息
    public void d(String method) {
        LogUtil.d(moduleName, tag, method);
    }

    //打印e级别方法信息
    public void w(String method) {
        LogUtil.w(moduleName, tag, method);
    }

    //打印e级别方法信息
    public void e(String method) {
        LogUtil.e(moduleName, tag, method);
    }

    //打印v级别方法信息
    public void v(String method) {
        LogUtil.v(moduleName, tag, method);
    }

    //打印i级别方法和内容信息
    public void i(String method, String msg) {
        LogUtil.i(moduleName, tag, method + "--->" + msg);
    }

    //打印d级别方法和内容信息
    public void d(String method, String msg) {
        LogUtil.d(moduleName, tag, method + "--->" + msg);
    }

    //打印w级别方法和内容信息
    public void w(String method, String msg) {
        LogUtil.w(moduleName, tag, method + "--->" + msg);
    }

    //打印e级别方法和内容信息
    public void e(String method, String msg) {
        LogUtil.e(moduleName, tag, method + "--->" + msg);
    }

    //打印v级别方法和内容信息
    public void v(String method, String msg) {
        LogUtil.v(moduleName, tag, method + "--->" + msg);
    }
}

package com.elzhao.loopviewpager.log;

import java.util.HashMap;
import java.util.Map;

public class Lg {

    //设置模块名称
    public static void setModuleName(String module) {
        Agent.sModule = module;
    }

    //打印i级别方法信息
    public static void i() {
        printMessage();
    }

    //打印d级别方法信息
    public static void d() {
        printMessage();
    }

    //打印w级别方法信息
    public static void w() {
        printMessage();
    }

    //打印e级别方法信息
    public static void e() {
        printMessage();
    }

    //打印v级别方法信息
    public static void v() {
        printMessage();
    }

    //打印i级别方法和内容信息
    public static void i(String msg) {
        printMessage(msg);
    }

    //打印d级别方法和内容信息
    public static void d(String msg) {
        printMessage(msg);
    }

    //打印w级别方法和内容信息
    public static void w(String msg) {
        printMessage(msg);
    }

    //打印e级别方法和内容信息
    public static void e(String msg) {
        printMessage(msg);
    }

    //打印v级别方法和内容信息
    public static void v(String msg) {
        printMessage(msg);
    }

    //通过代理打印方法信息
    private static void printMessage() {
        Agent.printMessage();
    }
    //通过代理打印方法和内容信息
    private static void printMessage(String msg) {
        Agent.printMessage(msg);
    }

    private static class Agent{
        private static final String DEFAULT = "UNKNOWN";
        private static final Map<String, LogTrace> LOGS = new HashMap<>();
        private static String sModule = DEFAULT;

        private StackTraceElement[] stackList = new Throwable().getStackTrace();

        //查询获取创建LogTrace对象
        private LogTrace queryOrCreateLogTrace() {
            String clsName = getClsName(5);
            LogTrace logTrace = LOGS.get(clsName);
            if (logTrace == null) {
                logTrace = new LogTrace(sModule, clsName);
                LOGS.put(clsName, logTrace);
            }
            return logTrace;
        }

        //获取方法栈类名
        private String getClsName(int index) {
            String clsName = stackList.length > index ? stackList[index].getClassName() : DEFAULT;
            return clsName.substring(clsName.lastIndexOf(".") + 1);
        }

        //获取方法栈方法名
        private String getMethodName(int index) {
            return stackList.length > index ? stackList[index].getMethodName() : DEFAULT;
        }

        //实际打印方法信息操作
        private static void printMessage() {
            Agent agent = new Agent();
            LogTrace logTrace = agent.queryOrCreateLogTrace();
            String method = agent.getMethodName(5);
            String level = agent.getMethodName(4);
            switch (level) {
                case "i":
                    logTrace.i(method);
                    break;
                case "d":
                    logTrace.d(method);
                    break;
                case "w":
                    logTrace.w(method);
                    break;
                case "e":
                    logTrace.e(method);
                    break;
                case "v":
                    logTrace.v(method);
                    break;
                default:
                    break;
            }
        }

        //实际打印方法和内容信息操作
        private static void printMessage(String msg) {
            Agent agent = new Agent();
            LogTrace logTrace = agent.queryOrCreateLogTrace();
            String method = agent.getMethodName(5);
            String level = agent.getMethodName(4);
            switch (level) {
                case "i":
                    logTrace.i(method, msg);
                    break;
                case "d":
                    logTrace.d(method, msg);
                    break;
                case "w":
                    logTrace.w(method, msg);
                    break;
                case "e":
                    logTrace.e(method, msg);
                    break;
                case "v":
                    logTrace.v(method, msg);
                    break;
                default:
                    break;
            }
        }
    }
}

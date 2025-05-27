package cn.zhangjh.zhiyue.utils;

import android.util.Log;

import androidx.annotation.NonNull;

import cn.zhangjh.zhiyue.BuildConfig;

public class LogUtil {

    public static void d(@NonNull String tag, @NonNull String message) {
        if(BuildConfig.DEBUG) {
            Log.d(tag, message);
        }
    }

    public static void e(@NonNull String tag, @NonNull String message) {
        if(BuildConfig.DEBUG) {
            Log.e(tag, message);
        }
    }

    public static void e(@NonNull String tag, @NonNull String message, @NonNull Throwable t) {
        if(BuildConfig.DEBUG) {
            Log.e(tag, message, t);
        }
    }
}

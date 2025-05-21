package cn.zhangjh.zhiyue.utils;

import android.app.Activity;
import android.view.Window;

import androidx.annotation.ColorInt;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * 系统UI工具类，用于处理状态栏、导航栏等系统UI元素
 */
public class SystemUIUtils {

    /**
     * 设置状态栏颜色
     * @param activity 活动
     * @param color 颜色值
     */
    public static void setStatusBarColor(Activity activity, @ColorInt int color) {
        Window window = activity.getWindow();

	    // Android 11及以上使用WindowInsetsControllerCompat
	    window.setStatusBarColor(color);
    }

    /**
     * 设置导航栏颜色
     * @param activity 活动
     * @param color 颜色值
     */
    public static void setNavigationBarColor(Activity activity, @ColorInt int color) {
        Window window = activity.getWindow();

	    // Android 11及以上使用WindowInsetsControllerCompat
	    window.setNavigationBarColor(color);
    }

    /**
     * 设置状态栏文字颜色（深色/浅色）
     * @param activity 活动
     * @param isDark 是否使用深色文字
     */
    public static void setStatusBarTextColor(Activity activity, boolean isDark) {
        Window window = activity.getWindow();

	    // Android 11及以上使用WindowInsetsControllerCompat
	    WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
	    controller.setAppearanceLightStatusBars(isDark);
    }

    /**
     * 设置导航栏按钮颜色（深色/浅色）
     * @param activity 活动
     * @param isDark 是否使用深色按钮
     */
    public static void setNavigationBarButtonColor(Activity activity, boolean isDark) {
        Window window = activity.getWindow();

	    // Android 11及以上使用WindowInsetsControllerCompat
	    WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
	    controller.setAppearanceLightNavigationBars(isDark);
    }

    /**
     * 处理刘海屏显示
     * @param activity 活动
     */
    public static void handleDisplayCutout(Activity activity) {
        Window window = activity.getWindow();

	    // Android 11及以上使用WindowInsetsControllerCompat
	    WindowCompat.setDecorFitsSystemWindows(window, false);
    }

    /**
     * 设置全屏沉浸式体验
     * @param activity 活动
     * @param isImmersive 是否启用沉浸式
     */
    public static void setImmersiveMode(Activity activity, boolean isImmersive) {
        Window window = activity.getWindow();

	    // Android 11及以上使用WindowInsetsControllerCompat
	    WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
	    if (isImmersive) {
	        // 隐藏系统栏并使用沉浸式手势
	        controller.hide(WindowInsetsCompat.Type.systemBars());
	        controller.setSystemBarsBehavior(
	                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
	    } else {
	        // 显示系统栏
	        controller.show(WindowInsetsCompat.Type.systemBars());
	    }
    }
}
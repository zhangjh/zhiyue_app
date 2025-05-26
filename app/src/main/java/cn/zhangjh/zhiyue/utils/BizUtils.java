package cn.zhangjh.zhiyue.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class BizUtils {
	public static void saveCache(Context context, String namespace, String key, String value) {
		SharedPreferences prefs = context.getSharedPreferences(namespace, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(key, value);
		editor.apply();
	}

	public static String getCache(Context context, String namespace, String key) {
		SharedPreferences prefs = context.getSharedPreferences(namespace, Context.MODE_PRIVATE);
		return prefs.getString(key, "");
	}
}

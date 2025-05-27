package cn.zhangjh.zhiyue.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

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

	public static String readAssetFile(Context context, String assetPath) {
		try (InputStream is = context.getAssets().open(assetPath)) {
			int size = is.available();
			byte[] buffer = new byte[size];
			is.read(buffer);
			return new String(buffer, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException("Error reading asset file: " + assetPath, e);
		}
	}

	public static String escapeJson(String json) {
		return json.replace("\\", "\\\\")
				.replace("'", "\\'")
				.replace("\"", "\\\"")
				.replace("\n", "\\n")
				.replace("\r", "\\r")
				.replace("\t", "\\t");
	}
}

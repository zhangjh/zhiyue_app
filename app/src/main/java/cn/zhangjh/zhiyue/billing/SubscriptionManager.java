package cn.zhangjh.zhiyue.billing;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.gson.Gson;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class SubscriptionManager {
    private static SubscriptionManager instance;
    private final Context context;
    private  BillingManager billingManager;
    
    private SubscriptionManager(Context context) {
        this.context = context.getApplicationContext();
    }
    
    public static synchronized SubscriptionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SubscriptionManager(context);
        }
        return instance;
    }
    
    public void subscribe(SubscriptionCallback callback) {
        // 开个白名单: njhxzhangjh@gmail.com
        SharedPreferences auth = context.getSharedPreferences("auth", Context.MODE_PRIVATE);
        String userId = auth.getString("userId", "");
        SubscriptionInfo info;
        SharedPreferences prefs = context.getSharedPreferences("subscription", Context.MODE_PRIVATE);
        if (TextUtils.equals(userId, "102177552544712897139")) {
            updateSubscriptionStatus(true);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            try {
                info = new SubscriptionInfo(true,
                        "SmartReader-智阅月度订阅",
                        sdf.parse("2099-09-14"),
                        "smart_reader_monthly_subscription");
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
            callback.onSubscriptionSuccess(info);
            prefs.edit().putString("subscriptionInfo", new Gson().toJson(info)).apply();
        } else {
            // 启动实际订阅流程
            billingManager.performSubscriptionPurchase(success -> {
                if (success) {
                    // 订阅成功，获取订阅详情
                    billingManager.getSubscriptionDetails(subscriptionInfo -> {
                        if (callback != null && subscriptionInfo != null) {
                            prefs.edit().putBoolean("isSubscribed", true).apply();
                            prefs.edit().putString("subscriptionInfo", new Gson().toJson(subscriptionInfo)).apply();
                            callback.onSubscriptionSuccess(subscriptionInfo);
                        }
                    });
                } else {
                    callback.onSubscriptionSuccess(null);
                }
            });
        }
    }

    // 有一个小bug，如果订阅后再手动清理本地数据，会造成第一次查询订阅状态不同步，暂不解决

    private void updateSubscriptionStatus(boolean isSubscribed) {
        SharedPreferences prefs = context.getSharedPreferences("subscription", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("isSubscribed", isSubscribed).apply();
    }
    
    public void destroy() {
        if (billingManager != null) {
            billingManager.destroy();
        }
    }
    
    public interface SubscriptionCallback {
        void onSubscriptionSuccess(SubscriptionInfo info);
    }
}
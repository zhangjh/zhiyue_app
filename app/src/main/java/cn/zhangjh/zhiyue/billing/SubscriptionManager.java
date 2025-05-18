package cn.zhangjh.zhiyue.billing;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

public class SubscriptionManager {
    private static SubscriptionManager instance;
    private final Context context;
    private final BillingManager billingManager;
    
    private SubscriptionManager(Context context) {
        this.context = context.getApplicationContext();
        this.billingManager = new BillingManager((Activity) context, new BillingManager.BillingCallback() {
            @Override
            public void onBillingSetupFinished() {
                billingManager.querySubscriptionStatus();
            }

            @Override
            public void onSubscriptionStatusChecked(boolean isSubscribed) {
                updateSubscriptionStatus(isSubscribed);
            }

            @Override
            public void onPurchaseSuccess() {
                billingManager.querySubscriptionStatus();
                Toast.makeText(context, "订阅成功", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPurchaseFailure(int responseCode, String message) {
                Toast.makeText(context, "订阅失败: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    public static synchronized SubscriptionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SubscriptionManager(context);
        }
        return instance;
    }
    
    public void subscribe(SubscriptionCallback callback) {
        // 检查是否已经订阅
        Log.d("subscription", "isSubscription: " + isSubscribed());
        if (isSubscribed()) {
            Toast.makeText(context, "您已经订阅了此服务", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 启动实际订阅流程
        billingManager.purchaseSubscription(success -> {
            if (success) {
                // 订阅成功，获取订阅详情
                billingManager.getSubscriptionDetails(subscriptionInfo -> {
                    if (callback != null && subscriptionInfo != null) {
                        callback.onSubscriptionSuccess(subscriptionInfo);
                    }
                });
            }
        });
    }
    
    public boolean isSubscribed() {
        SharedPreferences prefs = context.getSharedPreferences("subscription", Context.MODE_PRIVATE);
        return prefs.getBoolean("isSubscribed", false);
    }
    
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
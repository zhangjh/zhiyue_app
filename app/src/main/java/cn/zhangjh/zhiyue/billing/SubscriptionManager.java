package cn.zhangjh.zhiyue.billing;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.util.Date;

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
    
    public void mockSubscribe(SubscriptionCallback callback) {
        // 模拟订阅过程
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // 保存订阅状态
            updateSubscriptionStatus(true);
            
            // 模拟订阅详情
            Date expireDate = new Date(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000);
            
            SubscriptionInfo mockInfo = new SubscriptionInfo(
                    true,
                    "包月服务（测试）",
                    expireDate,
                    "smart_reader_monthly_subscription"
            );
            
            // 回调通知
            if (callback != null) {
                callback.onSubscriptionSuccess(mockInfo);
            }
        }, 1500);
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
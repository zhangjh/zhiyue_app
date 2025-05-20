package cn.zhangjh.zhiyue.billing;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

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
        // 开个白名单: njhxzhangjh@gmail.com
        SharedPreferences auth = context.getSharedPreferences("auth", Context.MODE_PRIVATE);
        String userId = auth.getString("userId", "");
        if (TextUtils.equals(userId, "102177552544712900000")) {
            SharedPreferences prefs = context.getSharedPreferences("subscription", Context.MODE_PRIVATE);
            prefs.edit().putBoolean("isSubscribed", true).apply();

            updateSubscriptionStatus(true);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

            SubscriptionInfo info;
            try {
                info = new SubscriptionInfo(true,
                        "SmartReader-智阅月度订阅",
                        sdf.parse("2099-09-14"),
                        "smart_reader_monthly_subscription");
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
            callback.onSubscriptionSuccess(info);
        } else {
            billingManager.performSubscriptionPurchase(success -> {
                if (success) {
                    // 订阅成功，获取订阅详情
                    billingManager.getSubscriptionDetails(subscriptionInfo -> {
                        if (callback != null && subscriptionInfo != null) {
                            SharedPreferences prefs = context.getSharedPreferences("subscription", Context.MODE_PRIVATE);
                            prefs.edit().putBoolean("isSubscribed", true).apply();
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
    public boolean isSubscribed() {
        SharedPreferences prefs = context.getSharedPreferences("subscription", Context.MODE_PRIVATE);
        boolean isSubscribed = prefs.getBoolean("isSubscribed", false);
        if(!isSubscribed) {
            billingManager.querySubscriptionStatus();
        }
        return isSubscribed;
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
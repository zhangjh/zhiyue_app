package cn.zhangjh.zhiyue.billing;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.widget.Toast;

import com.google.gson.Gson;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.function.Function;

import cn.zhangjh.zhiyue.R;
import cn.zhangjh.zhiyue.utils.LogUtil;

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
                Toast.makeText(context, context.getString(R.string.subscription_success), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPurchaseFailure(int responseCode, String message) {
                Toast.makeText(context, context.getString(R.string.subscription_failed) + message, Toast.LENGTH_SHORT).show();
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
        LogUtil.d("subscription", "isSubscription: " + isSubscribed());
        if (isSubscribed()) {
            Toast.makeText(context, context.getString(R.string.subscription_already), Toast.LENGTH_SHORT).show();
            return;
        }
        
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
                        context.getString(R.string.monthly_subscription),
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
                    querySubscriptionInfo(subscriptionInfo -> {
                        if(subscriptionInfo != null) {
                            prefs.edit().putBoolean("isSubscribed", true).apply();
                            prefs.edit().putString("subscriptionInfo", new Gson().toJson(subscriptionInfo)).apply();
                            callback.onSubscriptionSuccess(subscriptionInfo);
                        }
                        return null;
                    });
                } else {
                    callback.onSubscriptionSuccess(null);
                }
            });
        }
    }

    public boolean isSubscribed() {
        SharedPreferences prefs = context.getSharedPreferences("subscription", Context.MODE_PRIVATE);
        boolean isSubscribed = prefs.getBoolean("isSubscribed", false);
        if(!isSubscribed) {
            billingManager.querySubscriptionStatus();
        }
        return prefs.getBoolean("isSubscribed", false);
    }

    // 先查缓存，如果没有则查询后保存（暂未考虑过期失效缓存场景）
    public void querySubscriptionInfo(Function<SubscriptionInfo, Void> cb) {
        billingManager.getSubscriptionDetails(cb::apply);
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
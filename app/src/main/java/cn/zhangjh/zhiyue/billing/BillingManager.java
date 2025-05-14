package cn.zhangjh.zhiyue.billing;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryPurchasesParams;
import com.android.billingclient.api.SkuDetailsParams;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cn.zhangjh.zhiyue.model.SubscriptionInfo;

public class BillingManager implements PurchasesUpdatedListener {
    private static final String TAG = "BillingManager";
    private final BillingClient billingClient;
    private final Activity activity;
    private BillingCallback billingCallback;
    
    // 定义订阅产品ID
    public static final String SUBSCRIPTION_MONTHLY = "smart_reader_monthly_subscription";

    public interface BillingCallback {
        void onPurchaseSuccess();
        void onPurchaseFailure(int errorCode, String message);
        void onSubscriptionStatusChecked(boolean isSubscribed);
    }

    public BillingManager(Activity activity) {
        this.activity = activity;
        billingClient = BillingClient.newBuilder(activity)
                .setListener(this)
                .enablePendingPurchases()
                .build();
        
        connectToGooglePlay();
    }

    private void connectToGooglePlay() {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Google Play Billing连接成功");
                    // 连接成功后查询订阅状态
                    querySubscriptionStatus();
                } else {
                    Log.e(TAG, "Google Play Billing连接失败: " + billingResult.getDebugMessage());
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                Log.d(TAG, "Google Play Billing服务断开连接");
                connectToGooglePlay(); // 重试连接
            }
        });
    }

    public void setBillingCallback(BillingCallback callback) {
        this.billingCallback = callback;
    }

    // 查询用户订阅状态
    public void querySubscriptionStatus() {
        if (!billingClient.isReady()) {
            Log.e(TAG, "BillingClient 未准备好");
            if (billingCallback != null) {
                billingCallback.onSubscriptionStatusChecked(false);
            }
            return;
        }

        QueryPurchasesParams params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build();

        billingClient.queryPurchasesAsync(params, (billingResult, purchases) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                boolean isSubscribed = false;
                for (Purchase purchase : purchases) {
                    if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED
                            && !purchase.isAcknowledged()) {
                        acknowledgePurchase(purchase);
                    }
                    
                    // 检查是否有有效的月度订阅
                    if (purchase.getProducts().contains(SUBSCRIPTION_MONTHLY) 
                            && purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                        isSubscribed = true;
                    }
                }
                
                if (billingCallback != null) {
                    billingCallback.onSubscriptionStatusChecked(isSubscribed);
                }
            } else {
                Log.e(TAG, "查询订阅状态失败: " + billingResult.getDebugMessage());
                if (billingCallback != null) {
                    billingCallback.onSubscriptionStatusChecked(false);
                }
            }
        });
    }
    
    // 订阅包月服务
    public void subscribeMonthly() {
        List<String> skuList = new ArrayList<>();
        skuList.add(SUBSCRIPTION_MONTHLY);
        
        SkuDetailsParams params = SkuDetailsParams.newBuilder()
                .setSkusList(skuList)
                .setType(BillingClient.SkuType.SUBS)
                .build();

        billingClient.querySkuDetailsAsync(params, (billingResult, skuDetailsList) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && skuDetailsList != null) {
                if (!skuDetailsList.isEmpty()) {
                    BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                            .setSkuDetails(skuDetailsList.get(0))
                            .build();
                    BillingResult billingFlow = billingClient.launchBillingFlow(activity, flowParams);
                    if(billingFlow.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        Toast.makeText(activity.getApplicationContext(), "订阅成功", Toast.LENGTH_SHORT).show();
                        // 购买成功，记录后端数据

                    } else {
                        Toast.makeText(activity.getApplicationContext(), "订阅失败", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    if (billingCallback != null) {
                        billingCallback.onPurchaseFailure(billingResult.getResponseCode(), "未找到订阅商品");
                    }
                }
            } else {
                if (billingCallback != null) {
                    billingCallback.onPurchaseFailure(
                            billingResult.getResponseCode(),
                            "获取订阅信息失败: " + billingResult.getDebugMessage()
                    );
                }
            }
        });
    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }
        } else {
            if (billingCallback != null) {
                billingCallback.onPurchaseFailure(
                        billingResult.getResponseCode(),
                        "购买失败: " + billingResult.getDebugMessage()
                );
            }
        }
    }

    private void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            // 确认购买
            if (!purchase.isAcknowledged()) {
                acknowledgePurchase(purchase);
            }
            
            if (billingCallback != null) {
                billingCallback.onPurchaseSuccess();
            }
        }
    }
    
    private void acknowledgePurchase(Purchase purchase) {
        AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken())
                .build();
                
        billingClient.acknowledgePurchase(params, billingResult -> {
            if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                Log.e(TAG, "确认购买失败: " + billingResult.getDebugMessage());
            }
        });
    }

    public void destroy() {
        if (billingClient != null) {
            billingClient.endConnection();
        }
    }

    public void getSubscriptionDetails(final SubscriptionDetailsCallback callback) {
        if (!billingClient.isReady()) {
            Log.e(TAG, "BillingClient 未准备好");
            if (callback != null) {
                callback.onSubscriptionDetailsReceived(null);
            }
            return;
        }
    
        QueryPurchasesParams params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build();
    
        billingClient.queryPurchasesAsync(params, (billingResult, purchases) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                for (Purchase purchase : purchases) {
                    if (purchase.getProducts().contains(SUBSCRIPTION_MONTHLY) 
                            && purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                        // 从购买信息中提取订阅详情
                        long purchaseTime = purchase.getPurchaseTime();
                        // 假设订阅周期为30天
                        long expireTime = purchaseTime + 30L * 24 * 60 * 60 * 1000;
                        
                        SubscriptionInfo info = new SubscriptionInfo(
                                true,
                                "包月服务",
                                new Date(expireTime),
                                SUBSCRIPTION_MONTHLY
                        );
                        
                        if (callback != null) {
                            callback.onSubscriptionDetailsReceived(info);
                        }
                        return;
                    }
                }
                
                // 没有找到有效订阅
                if (callback != null) {
                    callback.onSubscriptionDetailsReceived(null);
                }
            } else {
                Log.e(TAG, "查询订阅详情失败: " + billingResult.getDebugMessage());
                if (callback != null) {
                    callback.onSubscriptionDetailsReceived(null);
                }
            }
        });
    }

    public interface SubscriptionDetailsCallback {
        void onSubscriptionDetailsReceived(SubscriptionInfo subscriptionInfo);
    }
}
package cn.zhangjh.zhiyue.billing;

import android.app.Activity;
import android.util.Log;

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

public class BillingManager implements PurchasesUpdatedListener {
    private static final String TAG = "BillingManager";
    private static final String SUBSCRIPTION_MONTHLY = "smart_reader_monthly_subscription";
    
    private final BillingClient billingClient;
    private final Activity activity;
    private final BillingCallback billingCallback;
    
    public BillingManager(Activity activity, BillingCallback callback) {
        this.activity = activity;
        this.billingCallback = callback;

        billingClient = BillingClient.newBuilder(activity)
                .setListener(this)
                .enablePendingPurchases()
                .build();

        connectToPlayBillingService();
    }
    
    private void connectToPlayBillingService() {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Google Play Billing 服务已连接");
                    if (billingCallback != null) {
                        billingCallback.onBillingSetupFinished();
                    }
                } else {
                    Log.e(TAG, "Google Play Billing 服务连接失败: " + billingResult.getDebugMessage());
                }
            }
            
            @Override
            public void onBillingServiceDisconnected() {
                Log.d(TAG, "Google Play Billing 服务已断开连接");
                // 可以在这里实现重连逻辑
                billingClient.startConnection(this);
            }
        });
    }
    
    // 查询订阅状态
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
            boolean isSubscribed = false;
            
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                for (Purchase purchase : purchases) {
                    // 确认未确认的购买
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
            Log.d(TAG, "billingResult code:" + billingResult.getResponseCode());
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && skuDetailsList != null) {
                if (!skuDetailsList.isEmpty()) {
                    BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                            .setSkuDetails(skuDetailsList.get(0))
                            .build();
                    billingClient.launchBillingFlow(activity, flowParams);
                } else {
                    if (billingCallback != null) {
                        billingCallback.onPurchaseFailure(
                                billingResult.getResponseCode(),
                                "未找到订阅商品"
                        );
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

    public interface BillingCallback {
        void onBillingSetupFinished();
        void onSubscriptionStatusChecked(boolean isSubscribed);
        void onPurchaseSuccess();
        void onPurchaseFailure(int responseCode, String message);
    }

    public interface SubscriptionDetailsCallback {
        void onSubscriptionDetailsReceived(SubscriptionInfo subscriptionInfo);
    }
}
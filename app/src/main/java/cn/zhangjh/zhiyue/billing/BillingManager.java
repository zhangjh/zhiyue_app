package cn.zhangjh.zhiyue.billing;

import android.app.Activity;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.google.android.gms.common.util.CollectionUtils;
import com.google.gson.Gson;

import java.util.Date;
import java.util.List;

import cn.zhangjh.zhiyue.R;
import cn.zhangjh.zhiyue.utils.LogUtil;

public class BillingManager implements PurchasesUpdatedListener {
    private static final String TAG = "BillingManager";
    private static final String SUBSCRIPTION_MONTHLY = "smart_reader_monthly_subscription";
    
    private final BillingClient billingClient;
    private final Activity activity;
    private final BillingCallback billingCallback;
    private PurchaseCallback currentPurchaseCallback;

    private boolean isConnecting = false;
    
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
        if (billingClient.isReady() || isConnecting) {
            LogUtil.d(TAG, "BillingClient 已连接或正在连接，无需重复连接");
            return;
        }
        isConnecting = true;
        LogUtil.d(TAG, "开始连接 Google Play Billing 服务");
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                isConnecting = false;
                LogUtil.d(TAG, "连接结果: " + billingResult.getResponseCode() + ", 消息: " + billingResult.getDebugMessage());
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    LogUtil.d(TAG, "Google Play Billing 服务已连接成功");
                    if (billingCallback != null) {
                        billingCallback.onBillingSetupFinished();
                    }
                } else {
                    LogUtil.e(TAG, "Google Play Billing 连接失败: " + billingResult.getDebugMessage());
                }
            }
            
            @Override
            public void onBillingServiceDisconnected() {
                isConnecting = false;
                LogUtil.d(TAG, "Google Play Billing 服务已断开连接，尝试重新连接");
                connectToPlayBillingService();
            }
        });
    }

    private void launch() {
        if(!billingClient.isReady()) {
            LogUtil.e(TAG, "billingClient isn't ready");
            if(currentPurchaseCallback != null) {
                currentPurchaseCallback.onPurchaseComplete(false);
            }
            return;
        }
        
        billingClient.queryProductDetailsAsync(this.queryItems(),
                (billingResult, skuDetailsList) -> {
            LogUtil.d(TAG, "queryProduct result: " + new Gson().toJson(billingResult));
            
            if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                LogUtil.e(TAG, "查询商品失败: " + billingResult.getDebugMessage());
                activity.runOnUiThread(() -> Toast.makeText(activity, 
                    activity.getString(R.string.query_product_failed) + billingResult.getDebugMessage(), Toast.LENGTH_SHORT).show());
                if(currentPurchaseCallback != null) {
                    currentPurchaseCallback.onPurchaseComplete(false);
                }
                return;
            }
            
            if(CollectionUtils.isEmpty(skuDetailsList)) {
                LogUtil.e(TAG, "商品列表为空，商品ID可能不正确或未发布");
                activity.runOnUiThread(() -> Toast.makeText(activity, activity.getString(R.string.query_product_empty), Toast.LENGTH_SHORT).show());
                if(currentPurchaseCallback != null) {
                    currentPurchaseCallback.onPurchaseComplete(false);
                }
                return;
            }
            for (ProductDetails details : skuDetailsList) {
                ProductDetails.SubscriptionOfferDetails offerDetails = details.getSubscriptionOfferDetails().get(0);
                String offerToken = offerDetails.getOfferToken();

                BillingFlowParams.ProductDetailsParams params = BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(details)
                    .setOfferToken(offerToken)
                    .build();
                List<BillingFlowParams.ProductDetailsParams> detailsParams = List.of(params);
                BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(detailsParams)
                        .build();
                BillingResult launchResult = billingClient.launchBillingFlow(activity, flowParams);
                LogUtil.d(TAG, "launchBillingFlow: " + new Gson().toJson(launchResult));
                if (launchResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                    activity.runOnUiThread(() -> Toast.makeText(activity, activity.getString(R.string.launch_billing_failed), Toast.LENGTH_SHORT).show());
                    if(currentPurchaseCallback != null) {
                        currentPurchaseCallback.onPurchaseComplete(false);
                    }
                    return;
                }
                if(currentPurchaseCallback != null) {
                    currentPurchaseCallback.onPurchaseComplete(true);
                }
                LogUtil.d(TAG, "购买成功");
            }
        });
    }

    private QueryProductDetailsParams queryItems() {
        return QueryProductDetailsParams.newBuilder()
                .setProductList(List.of(
                    QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(SUBSCRIPTION_MONTHLY)
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build()
                ))
                .build();
    }
    
    // 查询订阅状态
    public void querySubscriptionStatus() {
        if (!billingClient.isReady()) {
            LogUtil.e(TAG, "BillingClient 未准备好");
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
                LogUtil.e(TAG, "查询订阅状态失败: " + billingResult.getDebugMessage());
                if (billingCallback != null) {
                    billingCallback.onSubscriptionStatusChecked(false);
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
                        activity.getString(R.string.billing_failed) + billingResult.getDebugMessage()
                );
            }
        }
    }
    
    public void performSubscriptionPurchase(PurchaseCallback callback) {
        LogUtil.d(TAG, "performSubscriptionPurchase called");
        LogUtil.d(TAG, "BillingClient ready state: " + billingClient.isReady());
        LogUtil.d(TAG, "Connection state: " + billingClient.getConnectionState());
        this.currentPurchaseCallback = callback;
        
        if (!billingClient.isReady()) {
            LogUtil.e(TAG, "BillingClient 未准备好，当前状态: " + billingClient.getConnectionState());
            if (callback != null) {
                callback.onPurchaseComplete(false);
            }
            return;
        }
        launch();
    }

    private void handlePurchase(Purchase purchase) {
        // 在这里处理订阅成功后事件
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            // 确认购买
            if (!purchase.isAcknowledged()) {
                acknowledgePurchase(purchase);
            }
            
            if (billingCallback != null) {
                billingCallback.onPurchaseSuccess();
            }
            
            if (currentPurchaseCallback != null) {
                currentPurchaseCallback.onPurchaseComplete(true);
                currentPurchaseCallback = null;
            }
        }
    }
    
    private void acknowledgePurchase(Purchase purchase) {
        AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken())
                .build();
                
        billingClient.acknowledgePurchase(params, billingResult -> {
            if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                LogUtil.e(TAG, "确认购买失败: " + billingResult.getDebugMessage());
            }
        });
    }

    public void destroy() {
        if (billingClient != null) {
            if (billingClient.isReady()) {
                billingClient.endConnection();
            }
        }
    }

    public void getSubscriptionDetails(final SubscriptionDetailsCallback callback) {
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
                                activity.getString(R.string.monthly_subscription),
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
            } else {
                LogUtil.e(TAG, "查询订阅详情失败: " + billingResult.getDebugMessage());
            }

            if (callback != null) {
                callback.onSubscriptionDetailsReceived(null);
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

    public interface PurchaseCallback {
        void onPurchaseComplete(boolean success);
    }
}
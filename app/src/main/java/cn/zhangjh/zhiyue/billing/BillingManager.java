package cn.zhangjh.zhiyue.billing;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;

import java.util.ArrayList;
import java.util.List;

public class BillingManager implements PurchasesUpdatedListener {
    private static final String TAG = "BillingManager";
    private final BillingClient billingClient;
    private final Activity activity;
    private BillingCallback billingCallback;

    public interface BillingCallback {
        void onPurchaseSuccess();
        void onPurchaseFailure(int errorCode, String message);
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

    public void purchaseBook(String productId) {
        List<String> skuList = new ArrayList<>();
        skuList.add(productId);
        
        SkuDetailsParams params = SkuDetailsParams.newBuilder()
                .setSkusList(skuList)
                .setType(BillingClient.SkuType.INAPP)
                .build();

        billingClient.querySkuDetailsAsync(params, (billingResult, skuDetailsList) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && skuDetailsList != null) {
                for (SkuDetails skuDetails : skuDetailsList) {
                    BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                            .setSkuDetails(skuDetails)
                            .build();
                    billingClient.launchBillingFlow(activity, flowParams);
                    break;
                }
            } else {
                if (billingCallback != null) {
                    billingCallback.onPurchaseFailure(
                            billingResult.getResponseCode(),
                            "获取商品信息失败: " + billingResult.getDebugMessage()
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
            if (billingCallback != null) {
                billingCallback.onPurchaseSuccess();
            }
        }
    }

    public void destroy() {
        if (billingClient != null) {
            billingClient.endConnection();
        }
    }
}
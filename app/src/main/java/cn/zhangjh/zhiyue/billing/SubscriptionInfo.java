package cn.zhangjh.zhiyue.billing;

import java.util.Date;

public class SubscriptionInfo {
    private final boolean isActive;
    private final String subscriptionName;
    private final Date expireDate;
    private final String productId;

    public SubscriptionInfo(boolean isActive, String subscriptionName, Date expireDate, String productId) {
        this.isActive = isActive;
        this.subscriptionName = subscriptionName;
        this.expireDate = expireDate;
        this.productId = productId;
    }

    public boolean isActive() {
        return isActive;
    }

    public String getSubscriptionName() {
        return subscriptionName;
    }

    public Date getExpireDate() {
        return expireDate;
    }

    public String getProductId() {
        return productId;
    }
}
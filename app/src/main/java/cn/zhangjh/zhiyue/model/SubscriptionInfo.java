package cn.zhangjh.zhiyue.model;

import java.util.Date;

public class SubscriptionInfo {
    private boolean isSubscribed;
    private String subscriptionType;
    private Date expireDate;
    private String productId;

    public SubscriptionInfo(boolean isSubscribed, String subscriptionType, Date expireDate, String productId) {
        this.isSubscribed = isSubscribed;
        this.subscriptionType = subscriptionType;
        this.expireDate = expireDate;
        this.productId = productId;
    }

    public boolean isSubscribed() {
        return isSubscribed;
    }

    public void setSubscribed(boolean subscribed) {
        isSubscribed = subscribed;
    }

    public String getSubscriptionType() {
        return subscriptionType;
    }

    public void setSubscriptionType(String subscriptionType) {
        this.subscriptionType = subscriptionType;
    }

    public Date getExpireDate() {
        return expireDate;
    }

    public void setExpireDate(Date expireDate) {
        this.expireDate = expireDate;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }
}
package com.alibaba.arthas.tunnel.server;

public class AppInfo {
    private String applicationVersion;
    private String sellerName;
    private String shopName;
    private String shopId;

    public AppInfo() {
    }

    public AppInfo(String applicationVersion, String sellerName, String shopName, String shopId) {
        this.applicationVersion = applicationVersion;
        this.sellerName = sellerName;
        this.shopName = shopName;
        this.shopId = shopId;
    }

    public String getApplicationVersion() {
        return applicationVersion;
    }

    public void setApplicationVersion(String applicationVersion) {
        this.applicationVersion = applicationVersion;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    public String getShopId() {
        return shopId;
    }

    public void setShopId(String shopId) {
        this.shopId = shopId;
    }
}

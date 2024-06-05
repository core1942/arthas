package com.alibaba.arthas.tunnel.server;

import com.alibaba.arthas.tunnel.common.URIConstans;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class AppInfo {
    private String appName;
    private Integer sellerId;
    private String sellerName;
    private Integer storeId;
    private String storeName;
    private Integer appType;
    private String appVersion;
    private String macAddr;
    private String localIp;
    private LocalDateTime connectTime;
    private LocalDateTime expireTime;

    public AppInfo(Map<String, List<String>> parameters) {
        String appName = getParam(parameters, URIConstans.APP_NAME);
        this.appName = StringUtils.isBlank(appName) ? "未知" : appName;
        String sellerdIdStr = getParam(parameters, "sellerId");
        this.sellerId = sellerdIdStr == null ? 0 : Integer.parseInt(sellerdIdStr);
        String sellerNameStr = getParam(parameters, "sellerName");
        this.sellerName = StringUtils.isBlank(sellerNameStr) ? "未绑定" : sellerNameStr;
        String storeIdStr = getParam(parameters, "storeId");
        this.storeId = storeIdStr == null ? 0 : Integer.parseInt(storeIdStr);
        String storeNameStr = getParam(parameters, "storeName");
        this.storeName = StringUtils.isBlank(storeNameStr) ? "未绑定" : storeNameStr;
        String appTypeStr = getParam(parameters, "appType");
        this.appType = appTypeStr == null ? 1 : Integer.parseInt(appTypeStr);
        this.macAddr = getParam(parameters, "macAddr");
        this.appVersion = getParam(parameters, "appVersion");
        this.localIp = getParam(parameters, "localIp");
        this.connectTime = LocalDateTime.now();
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public Integer getSellerId() {
        return sellerId;
    }

    public void setSellerId(Integer sellerId) {
        this.sellerId = sellerId;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public Integer getStoreId() {
        return storeId;
    }

    public void setStoreId(Integer storeId) {
        this.storeId = storeId;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public String getLocalIp() {
        return localIp;
    }

    public void setLocalIp(String localIp) {
        this.localIp = localIp;
    }

    public String getMacAddr() {
        return macAddr;
    }

    public void setMacAddr(String macAddr) {
        this.macAddr = macAddr;
    }

    public Integer getAppType() {
        return appType;
    }

    public void setAppType(Integer appType) {
        this.appType = appType;
    }

    public LocalDateTime getConnectTime() {
        return connectTime;
    }

    public void setConnectTime(LocalDateTime connectTime) {
        this.connectTime = connectTime;
    }

    public LocalDateTime getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
    }

    private <T> String getParam(Map<String, List<String>> parameters, String appName) {
        List<String> appNameList = parameters.get(appName);
        if (appNameList != null && !appNameList.isEmpty()) {
            return appNameList.get(0);
        }
        return null;
    }
}

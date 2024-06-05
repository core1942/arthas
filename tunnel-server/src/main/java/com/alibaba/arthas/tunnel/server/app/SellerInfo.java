package com.alibaba.arthas.tunnel.server.app;

/**
 * create by WG on 2023/11/22 19:23
 *
 * @author WangGang
 */
public class SellerInfo {
    private String sellerName;
    private String sellerId;
    private Integer storeNum;

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public Integer getStoreNum() {
        return storeNum;
    }

    public void setStoreNum(Integer storeNum) {
        this.storeNum = storeNum;
    }
}

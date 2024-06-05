package com.alibaba.arthas.tunnel.server.app;

/**
 * create by WG on 2023/11/22 19:23
 *
 * @author WangGang
 */
public class StoreInfo {
    private Integer sellerId;
    private String storeName;
    private String storeId;
    private Integer agentNum;

    public Integer getSellerId() {
        return sellerId;
    }

    public void setSellerId(Integer sellerId) {
        this.sellerId = sellerId;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public Integer getAgentNum() {
        return agentNum;
    }

    public void setAgentNum(Integer agentNum) {
        this.agentNum = agentNum;
    }
}

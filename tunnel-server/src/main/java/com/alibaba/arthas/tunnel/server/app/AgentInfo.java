package com.alibaba.arthas.tunnel.server.app;

/**
 * create by WG on 2023/11/22 19:23
 *
 * @author WangGang
 */
public class AgentInfo {
    private String clientConnectHost;
    private Integer clientConnectTunnelPort;
    private String host;
    private Integer port;
    private Integer sellerId;
    private Integer storeId;
    private String agentId;
    private String name;
    private String ip;
    private String macAddr;
    private String version;
    private Integer type;

    private String connectTimeAgo;
    private String connectTime;
    private boolean expire;
    private String expireTimeAgo;
    private String expireTime;

    public String getClientConnectHost() {
        return clientConnectHost;
    }

    public void setClientConnectHost(String clientConnectHost) {
        this.clientConnectHost = clientConnectHost;
    }

    public Integer getClientConnectTunnelPort() {
        return clientConnectTunnelPort;
    }

    public void setClientConnectTunnelPort(Integer clientConnectTunnelPort) {
        this.clientConnectTunnelPort = clientConnectTunnelPort;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Integer getSellerId() {
        return sellerId;
    }

    public void setSellerId(Integer sellerId) {
        this.sellerId = sellerId;
    }

    public Integer getStoreId() {
        return storeId;
    }

    public void setStoreId(Integer storeId) {
        this.storeId = storeId;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public boolean isExpire() {
        return expire;
    }

    public void setExpire(boolean expire) {
        this.expire = expire;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getConnectTimeAgo() {
        return connectTimeAgo;
    }

    public void setConnectTimeAgo(String connectTimeAgo) {
        this.connectTimeAgo = connectTimeAgo;
    }

    public String getConnectTime() {
        return connectTime;
    }

    public void setConnectTime(String connectTime) {
        this.connectTime = connectTime;
    }

    public String getExpireTimeAgo() {
        return expireTimeAgo;
    }

    public void setExpireTimeAgo(String expireTimeAgo) {
        this.expireTimeAgo = expireTimeAgo;
    }

    public String getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(String expireTime) {
        this.expireTime = expireTime;
    }

    public String getMacAddr() {
        return macAddr;
    }

    public void setMacAddr(String macAddr) {
        this.macAddr = macAddr;
    }
}

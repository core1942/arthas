package com.alibaba.arthas.tunnel.server;

import java.time.LocalDateTime;

/**
 * @author hengyunabc 2020-10-30
 *
 */
public class AgentClusterInfo {
    /**
     * agent本身以哪个ip连接到 tunnel server
     */
    private String agentId;
    private String host;
    private int port;
    private String arthasVersion;

    /**
     * agent 连接到的 tunnel server 的ip 和 port
     */
    private String clientConnectHost;
    private int clientConnectTunnelPort;

    private AppInfo appInfo;

    public AgentClusterInfo(String agentId, AgentInfo agentInfo, String clientConnectHost, int clientConnectTunnelPort) {
        this.agentId = agentId;
        this.host = agentInfo.getHost();
        this.port = agentInfo.getPort();
        this.arthasVersion = agentInfo.getArthasVersion();
        this.clientConnectHost = clientConnectHost;
        this.clientConnectTunnelPort = clientConnectTunnelPort;
        this.appInfo = agentInfo.getAppInfo();
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getArthasVersion() {
        return arthasVersion;
    }

    public void setArthasVersion(String arthasVersion) {
        this.arthasVersion = arthasVersion;
    }

    public String getClientConnectHost() {
        return clientConnectHost;
    }

    public void setClientConnectHost(String clientConnectHost) {
        this.clientConnectHost = clientConnectHost;
    }

    public int getClientConnectTunnelPort() {
        return clientConnectTunnelPort;
    }

    public void setClientConnectTunnelPort(int clientConnectTunnelPort) {
        this.clientConnectTunnelPort = clientConnectTunnelPort;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public AppInfo getAppInfo() {
        return appInfo;
    }

    public void setAppInfo(AppInfo appInfo) {
        this.appInfo = appInfo;
    }
}

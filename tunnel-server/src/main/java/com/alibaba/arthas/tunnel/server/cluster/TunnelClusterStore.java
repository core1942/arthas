package com.alibaba.arthas.tunnel.server.cluster;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.alibaba.arthas.tunnel.server.AgentClusterInfo;
import com.alibaba.arthas.tunnel.server.app.Apps;

/**
 * 保存agentId连接到哪个具体的 tunnel server，集群部署时使用
 * 
 * @author hengyunabc 2020-10-27
 *
 */
public interface TunnelClusterStore {
    public void addAgent(String agentId, AgentClusterInfo info, long expire, TimeUnit timeUnit);

    public AgentClusterInfo findAgent(String agentId);

    public void removeAgent(String agentId);

    public List<Apps> allAgentIds();

    public Map<String, AgentClusterInfo> agentInfo(String sellerName);
}

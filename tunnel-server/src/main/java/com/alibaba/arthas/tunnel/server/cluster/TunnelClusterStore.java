package com.alibaba.arthas.tunnel.server.cluster;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.alibaba.arthas.tunnel.server.AgentClusterInfo;
import com.alibaba.arthas.tunnel.server.app.AgentInfo;
import com.alibaba.arthas.tunnel.server.app.SellerInfo;
import com.alibaba.arthas.tunnel.server.app.StoreInfo;

/**
 * 保存agentId连接到哪个具体的 tunnel server，集群部署时使用
 * 
 * @author hengyunabc 2020-10-27
 *
 */
public interface TunnelClusterStore {
    void addAgent(String agentId, AgentClusterInfo info, long expire, TimeUnit timeUnit);

    AgentClusterInfo findAgent(Integer sellerId, Integer storeId, String agentId);

    void removeAgent(Integer sellerId, Integer storeId, String agentId);

    List<SellerInfo> sellerInfo();

    List<StoreInfo> storeInfo(Integer sellerId);

    List<AgentInfo> agentInfo(Integer sellerId, Integer storeId);
}

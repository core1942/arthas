package com.alibaba.arthas.tunnel.server.cluster;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.alibaba.arthas.tunnel.server.AppInfo;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.arthas.tunnel.server.AgentClusterInfo;

/**
 * 
 * @author hengyunabc 2020-12-02
 *
 */
public class InMemoryClusterStore implements TunnelClusterStore {
    private final static Logger logger = LoggerFactory.getLogger(InMemoryClusterStore.class);
    private static final Map<String, Map<String, AgentClusterInfo>> CACHE = new ConcurrentHashMap<>();
    // private Cache cache;

    @Override
    public AgentClusterInfo findAgent(String agentId) {
        String sellerName = findSellerName(agentId);
        Map<String, AgentClusterInfo> stringAgentClusterInfoMap = CACHE.get(sellerName);
        if (stringAgentClusterInfoMap != null) {
            return stringAgentClusterInfoMap.get(agentId);
        }
        return null;
    }

    @Override
    public void removeAgent(String agentId) {
        String sellerName = findSellerName(agentId);
        Map<String, AgentClusterInfo> stringAgentClusterInfoMap = CACHE.get(sellerName);
        if (stringAgentClusterInfoMap != null) {
            stringAgentClusterInfoMap.remove(agentId);
            if (stringAgentClusterInfoMap.isEmpty()) {
                CACHE.computeIfAbsent(sellerName, CACHE::remove);
            }
        }
    }

    @Override
    public void addAgent(String agentId, AgentClusterInfo info, long timeout, TimeUnit timeUnit) {
        AppInfo appInfo = parseInfo(agentId);
        Map<String, AgentClusterInfo> stringAgentClusterInfoMap = CACHE.computeIfAbsent(appInfo.getSellerName(), s -> new ConcurrentHashMap<>());
        info.setShopName(appInfo.getShopName());
        info.setShopId(appInfo.getShopId());
        info.setApplicationVersion(appInfo.getApplicationVersion());
        stringAgentClusterInfoMap.put(agentId, info);
    }

    @Override
    public Set<String> allAgentIds() {
        return CACHE.keySet();
    }

    @Override
    public Map<String, AgentClusterInfo> agentInfo(String sellerName) {
        return CACHE.get(sellerName);
    }


    public static String findSellerName(String agentId) {
        String appName = findAppNameFromAgentId(agentId);
        try {
            String[] split = StringUtils.split(appName, ":-");
            return getSellerName(split);
        } catch (Exception e) {
            return appName;
        }
    }

    public static AppInfo parseInfo(String agentId) {
        String appName = findAppNameFromAgentId(agentId);
        String[] split = StringUtils.split(appName, ":-");
        String sellerName;
        String shopName;
        String shopId;
        String version;
        try {
            sellerName = getSellerName(split);
        } catch (Exception e) {
            sellerName = appName;
        }
        try {
            shopName = split[2];
        } catch (Exception e) {
            shopName = appName;
        }
        try {
            shopId = split[4];
        } catch (Exception e) {
            shopId = "";
        }
        try {
            version = split[5].replace("(", "").replace(")", "");
        } catch (Exception e) {
            version = "";
        }
        return new AppInfo(version,sellerName,shopName,shopId);
    }

    private static String getSellerName(String[] split) {
        String sellerName = split[1] + "-" + split[3];
        if (!"PRO".equals(split[0])) {
            sellerName = split[0] + ":" + sellerName;
        }
        return sellerName;
    }

    public static String findAppNameFromAgentId(String id) {
        int index = id.lastIndexOf('_');
        if (index < 0) {
            return null;
        }

        return id.substring(0, index);
    }

}

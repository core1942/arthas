package com.alibaba.arthas.tunnel.server.cluster;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.alibaba.arthas.tunnel.server.AppInfo;
import com.alibaba.arthas.tunnel.server.app.Apps;
import com.google.common.collect.Lists;
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
        CACHE.compute(sellerName, (s, stringAgentClusterInfoMap) -> {
            if (stringAgentClusterInfoMap != null) {
                stringAgentClusterInfoMap.remove(agentId);
                if (!stringAgentClusterInfoMap.isEmpty()) {
                    return stringAgentClusterInfoMap;
                }
            }
            return null;
        });
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
    public List<Apps> allAgentIds() {
        return CACHE.keySet().stream().map(s -> {
            Map<String, AgentClusterInfo> stringAgentClusterInfoMap = CACHE.get(s);
            Apps apps = new Apps();
            String[] split = StringUtils.split(s, "-", 2);
            apps.setSellerName(split[0]);
            if (split.length > 1) {
                apps.setSellerId(split[1]);
            }else {
                apps.setSellerId(StringUtils.EMPTY);
            }
            if (stringAgentClusterInfoMap != null) {
                apps.setStoreNum(stringAgentClusterInfoMap.size());
            } else {
                apps.setStoreNum(0);
            }
            return apps;
        }).collect(Collectors.toList());
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

    /**
     * java -jar arthas-boot.jar --tunnel-server "ws://127.0.0.1:7777/ws" --app-name "PRO:qmaiasstant-zxapp:215968-182501:(20230831211307)_XdfertSDfdgwer"
     */
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

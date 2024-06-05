package com.alibaba.arthas.tunnel.server.cluster;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.alibaba.arthas.tunnel.server.Container;
import com.alibaba.arthas.tunnel.server.app.AgentInfo;
import com.alibaba.arthas.tunnel.server.app.SellerInfo;
import com.alibaba.arthas.tunnel.server.AgentClusterInfo;
import com.alibaba.arthas.tunnel.server.app.StoreInfo;
import org.ocpsoft.prettytime.PrettyTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author hengyunabc 2020-12-02
 */
public class InMemoryClusterStore implements TunnelClusterStore {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final PrettyTime P = new PrettyTime(new Locale("ch"));

    private static final Logger log = LoggerFactory.getLogger(InMemoryClusterStore.class);
    //                       sellerId  -> storeId   -> agentId -> AgentClusterInfo
    private static final Map<Integer, Map<Integer, Map<String, AgentClusterInfo>>> CACHE = new ConcurrentHashMap<>();

    private static final Map<Integer, Map<Integer, Map<String, AgentClusterInfo>>> EXPIRE_CACHE = new ConcurrentHashMap<>();

    static {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    LocalDateTime now = LocalDateTime.now();
                    EXPIRE_CACHE.keySet().forEach(sellerId -> EXPIRE_CACHE.compute(sellerId, (s, storeMap) -> {
                                if (storeMap != null) {
                                    storeMap.keySet().forEach(storeId -> storeMap.compute(storeId, (s1, agentMap) -> {
                                                if (agentMap != null) {
                                                    agentMap.keySet().forEach(agentId -> agentMap.compute(agentId, (s2, agentClusterInfo) -> {
                                                        if (agentClusterInfo != null) {
                                                            if (agentClusterInfo.getAppInfo().getExpireTime() == null || Duration.between(agentClusterInfo.getAppInfo().getExpireTime(), now).toDays() >= 2) {
                                                                return null;
                                                            }
                                                        }
                                                        return agentClusterInfo;
                                                    }));
                                                }
                                                return agentMap;
                                            }
                                    ));
                                }
                                return storeMap;
                            }
                    ));
                } catch (Exception e) {
                    log.error("InMemoryClusterStore expire error.", e);
                }
            }
        }, 0, 1000 * 60 * 30);
    }

    @Override
    public AgentClusterInfo findAgent(Integer sellerId, Integer storeId, String agentId) {
        Map<Integer, Map<String, AgentClusterInfo>> storeMap = CACHE.get(sellerId);
        if (storeMap != null) {
            Map<String, AgentClusterInfo> agentMap = storeMap.get(storeId);
            if (agentMap != null) {
                return agentMap.get(agentId);
            }
        }
        return null;
    }

    @Override
    public void removeAgent(Integer sellerId, Integer storeId, String agentId) {
        AgentClusterInfo agentClusterInfo = removeFromCache(CACHE, sellerId, storeId, agentId);
        if (agentClusterInfo != null) {
            agentClusterInfo.getAppInfo().setExpireTime(LocalDateTime.now());
            addToCache(EXPIRE_CACHE, agentClusterInfo);
        }
    }

    @Override
    public void addAgent(String agentId, AgentClusterInfo info, long timeout, TimeUnit timeUnit) {
        addToCache(CACHE, info);
        removeFromCache(EXPIRE_CACHE, info.getAppInfo().getSellerId(), info.getAppInfo().getStoreId(), agentId);
    }

    @Override
    public List<SellerInfo> sellerInfo() {
        return CACHE.values().stream().map(storeMap -> {
            SellerInfo sellerInfo = new SellerInfo();
            Map<String, AgentClusterInfo> agentMap = storeMap.values().stream().findFirst().get();
            AgentClusterInfo agentClusterInfo = agentMap.values().stream().findFirst().get();
            sellerInfo.setStoreNum(storeMap.size());
            sellerInfo.setSellerId(agentClusterInfo.getAppInfo().getSellerId().toString());
            sellerInfo.setSellerName(agentClusterInfo.getAppInfo().getSellerName());
            return sellerInfo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<StoreInfo> storeInfo(Integer sellerId) {
        return CACHE.get(sellerId).values().stream().map(s -> {
            AgentClusterInfo agentClusterInfo = s.values().stream().findFirst().get();
            StoreInfo storeInfo = new StoreInfo();
            storeInfo.setSellerId(agentClusterInfo.getAppInfo().getSellerId());
            storeInfo.setAgentNum(s.size());
            storeInfo.setStoreId(agentClusterInfo.getAppInfo().getStoreId().toString());
            storeInfo.setStoreName(agentClusterInfo.getAppInfo().getStoreName());
            return storeInfo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<AgentInfo> agentInfo(Integer sellerId, Integer storeId) {
        Map<String, AgentClusterInfo> infoMap = CACHE.getOrDefault(sellerId, Collections.emptyMap()).getOrDefault(storeId, Collections.emptyMap());
        Map<String, AgentClusterInfo> expire = EXPIRE_CACHE.getOrDefault(sellerId, Collections.emptyMap()).getOrDefault(storeId, Collections.emptyMap());
        List<AgentInfo> ok = infoMap.values().stream().map(InMemoryClusterStore::mapAgentInfo).sorted(Comparator.comparing(AgentInfo::getType)).collect(Collectors.toList());
        List<AgentInfo> expireList = expire.values().stream().map(InMemoryClusterStore::mapAgentInfo)
                .peek(agentInfo -> agentInfo.setExpire(true))
                .sorted(Comparator.comparing(AgentInfo::getType))
                .collect(Collectors.toList());
        ok.addAll(expireList);
        return ok;

    }

    private AgentClusterInfo removeFromCache(Map<Integer, Map<Integer, Map<String, AgentClusterInfo>>> cache, Integer sellerId, Integer storeId, String agentId) {
        Container<AgentClusterInfo> remove = new Container<>(null);
        cache.compute(sellerId, (s, storeMap) -> {
            if (storeMap != null) {
                Map<String, AgentClusterInfo> compute = storeMap.compute(storeId, (s1, agentMap) -> {
                    if (agentMap != null) {
                        AgentClusterInfo rm = agentMap.remove(agentId);
                        remove.set(rm);
                    }
                    return agentMap;
                });
                if (compute == null || compute.isEmpty()) {
                    return null;
                }
            }
            return storeMap;
        });
        return remove.get();
    }

    private void addToCache(Map<Integer, Map<Integer, Map<String, AgentClusterInfo>>> cache, AgentClusterInfo info) {
        Map<Integer, Map<String, AgentClusterInfo>> storeMap = cache.computeIfAbsent(info.getAppInfo().getSellerId(), s -> new ConcurrentHashMap<>());
        Map<String, AgentClusterInfo> agentMap = storeMap.computeIfAbsent(info.getAppInfo().getStoreId(), s -> new ConcurrentHashMap<>());
        agentMap.put(info.getAgentId(), info);
    }

    private static AgentInfo mapAgentInfo(AgentClusterInfo agentClusterInfo) {
        AgentInfo agentInfo = new AgentInfo();
        agentInfo.setClientConnectHost(agentClusterInfo.getClientConnectHost());
        agentInfo.setClientConnectTunnelPort(agentClusterInfo.getClientConnectTunnelPort());
        agentInfo.setHost(agentClusterInfo.getHost());
        agentInfo.setPort(agentClusterInfo.getPort());
        agentInfo.setSellerId(agentClusterInfo.getAppInfo().getSellerId());
        agentInfo.setStoreId(agentClusterInfo.getAppInfo().getStoreId());
        agentInfo.setAgentId(agentClusterInfo.getAgentId());
        agentInfo.setName(agentClusterInfo.getAppInfo().getAppName());
        agentInfo.setIp(agentClusterInfo.getHost() + "/" + agentClusterInfo.getAppInfo().getLocalIp());
        agentInfo.setMacAddr(agentClusterInfo.getAppInfo().getMacAddr());
        agentInfo.setVersion(agentClusterInfo.getAppInfo().getAppVersion());
        agentInfo.setType(agentClusterInfo.getAppInfo().getAppType());
        agentInfo.setConnectTime(DATE_TIME_FORMAT.format(agentClusterInfo.getAppInfo().getConnectTime()));
        agentInfo.setConnectTimeAgo(P.format(agentClusterInfo.getAppInfo().getConnectTime()).replace(" ", ""));
        if (agentClusterInfo.getAppInfo().getExpireTime() != null) {
            agentInfo.setExpireTime(DATE_TIME_FORMAT.format(agentClusterInfo.getAppInfo().getExpireTime()));
            agentInfo.setExpireTimeAgo("[离线] " + P.format(agentClusterInfo.getAppInfo().getExpireTime()).replace(" ", ""));
        }
        return agentInfo;
    }
}

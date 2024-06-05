package com.alibaba.arthas.tunnel.server.app.web;

import java.util.*;

import javax.servlet.http.HttpServletRequest;

import com.alibaba.arthas.tunnel.server.app.AgentInfo;
import com.alibaba.arthas.tunnel.server.app.SellerInfo;
import com.alibaba.arthas.tunnel.server.app.StoreInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.arthas.tunnel.server.AgentClusterInfo;
import com.alibaba.arthas.tunnel.server.app.configuration.ArthasProperties;
import com.alibaba.arthas.tunnel.server.cluster.TunnelClusterStore;

/**
 * 
 * @author hengyunabc 2020-11-03
 *
 */
@Controller
public class DetailAPIController {

    @Autowired
    private ArthasProperties arthasProperties;

    @Autowired(required = false)
    private TunnelClusterStore tunnelClusterStore;

    @RequestMapping("/api/tunnelSellers")
    @ResponseBody
    public List<SellerInfo> tunnelSellers(HttpServletRequest request, Model model) {
        if (!arthasProperties.isEnableDetailPages()) {
            throw new IllegalAccessError("not allow");
        }
        if (tunnelClusterStore != null) {
            return tunnelClusterStore.sellerInfo();
        }
        return Collections.emptyList();
    }

    @RequestMapping("/api/tunnelStores")
    @ResponseBody
    public List<StoreInfo> tunnelSores(@RequestParam Integer sellerId, HttpServletRequest request, Model model) {
        if (!arthasProperties.isEnableDetailPages()) {
            throw new IllegalAccessError("not allow");
        }
        if (tunnelClusterStore != null) {
            return tunnelClusterStore.storeInfo(sellerId);
        }
        return Collections.emptyList();
    }

    @RequestMapping("/api/tunnelAgents")
    @ResponseBody
    public List<AgentInfo> tunnelAgents(@RequestParam Integer sellerId, @RequestParam Integer storeId, HttpServletRequest request, Model model) {
        if (!arthasProperties.isEnableDetailPages()) {
            throw new IllegalAccessError("not allow");
        }
        if (tunnelClusterStore != null) {
            return tunnelClusterStore.agentInfo(sellerId, storeId);
        }
        return Collections.emptyList();
    }

}

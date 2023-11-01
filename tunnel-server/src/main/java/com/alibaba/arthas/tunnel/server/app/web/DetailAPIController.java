package com.alibaba.arthas.tunnel.server.app.web;

import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
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

    private final static Logger logger = LoggerFactory.getLogger(DetailAPIController.class);

    @Autowired
    ArthasProperties arthasProperties;

    @Autowired(required = false)
    private TunnelClusterStore tunnelClusterStore;

    public static void main(String[] args) {
        String str = "PRO:企迈正餐演示-企迈小中餐演示门店（正餐一体化）:215968-182500:(20230831211306)";
        String[] split = StringUtils.split(str, ":-");
        System.out.println(Arrays.toString(split));
    }

    @RequestMapping("/api/tunnelApps")
    @ResponseBody
    public Set<String> tunnelApps(HttpServletRequest request, Model model) {
        if (!arthasProperties.isEnableDetailPages()) {
            throw new IllegalAccessError("not allow");
        }
        if (tunnelClusterStore != null) {
            return tunnelClusterStore.allAgentIds();
        }
        return Collections.emptySet();
    }

    @RequestMapping("/api/tunnelAgentInfo")
    @ResponseBody
    public Map<String, AgentClusterInfo> tunnelAgentIds(@RequestParam(value = "app", required = true) String sellerName,
            HttpServletRequest request, Model model) {
        if (!arthasProperties.isEnableDetailPages()) {
            throw new IllegalAccessError("not allow");
        }

        if (tunnelClusterStore != null) {
            return tunnelClusterStore.agentInfo(sellerName);
        }

        return Collections.emptyMap();
    }

    /**
     * check if agentId exists
     * @param agentId
     * @return
     */
    @RequestMapping("/api/tunnelAgents")
    @ResponseBody
    public Map<String, Object> tunnelAgentIds(@RequestParam(value = "agentId", required = true) String agentId) {
        Map<String, Object> result = new HashMap<String, Object>();
        boolean success = false;
        try {
            AgentClusterInfo info = tunnelClusterStore.findAgent(agentId);
            if (info != null) {
                success = true;
            }
        } catch (Throwable e) {
            logger.error("try to find agentId error, id: {}", agentId, e);
        }
        result.put("success", success);
        return result;
    }


}

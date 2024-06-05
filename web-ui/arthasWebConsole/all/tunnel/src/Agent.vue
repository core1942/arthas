<script setup lang="ts">
import {onMounted, reactive} from 'vue';

const agentInfos: AgentInfo[] = reactive([])

function getUrlParam(name: string) {
    const urlparam = new URLSearchParams(window.location.search)
    return urlparam.get(name)
}

function tunnelWebConsoleLink(agentId: string, sellerId: number, storeId: number, tunnelPort: number, targetServer: string) {
    return `/?targetServer=${targetServer}&port=${tunnelPort}&agentId=${agentId}&sellerId=${sellerId}&storeId=${storeId}`;
}

function btnType(agentInfo: AgentInfo) {
    if (agentInfo.expire) {
        return 'btn-disabled'
    }
    if (agentInfo.type === 0) {
        return 'btn-outline btn-primary'
    }
    return 'btn-accent'
}

function realTime(agentInfo: AgentInfo) {
    if (agentInfo.expire) {
        return agentInfo.expireTime + "  (两天删除)";
    }
    return agentInfo.connectTime;
}

function timeAgo(agentInfo: AgentInfo) {
    if (agentInfo.expire) {
        return agentInfo.expireTimeAgo;
    }
    return agentInfo.connectTimeAgo;
}


const fetchMyApps = () => {
    const sellerId = getUrlParam("sellerId")
    if (!sellerId) {
        alert("请先选择品牌")
    }
    const storeId = getUrlParam("storeId")
    if (!storeId) {
        alert("请先选择门店")
    }
    let url = `/api/tunnelAgents?sellerId=${sellerId}&storeId=${storeId}`;
    fetch(url)
        .then((response) => response.json())
        .then((data: AgentInfo[]) => {
            data.length > 0 && data.forEach(agentInfo => agentInfos.push(agentInfo))
        })
        .catch((error) => console.error('api error ' + error))

}
onMounted(() => {
    fetchMyApps()
})
</script>

<template>
    <table class="table table-normal w-[100vw]">
        <thead>
        <tr>
            <th class="normal-case">名称</th>
            <th class="normal-case">版本</th>
            <th class="normal-case">类型</th>
            <th class="normal-case">IP</th>
            <th class="normal-case">mac地址</th>
            <th class="normal-case">上线/离线时间</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="agentInfo in agentInfos" :key="agentInfo" class="hover">
            <td>
                <a class="btn"
                   :class="btnType(agentInfo)"
                   :href="tunnelWebConsoleLink(agentInfo.agentId,agentInfo.sellerId,agentInfo.storeId,agentInfo.clientConnectTunnelPort, agentInfo.clientConnectHost)">{{
                        agentInfo.name
                    }}</a>
            </td>
            <td>
                {{ agentInfo.version }}
            </td>
            <td>
                {{ agentInfo.type === 0 ? '服务端' : '客户端' }}
            </td>
            <td>
                {{ agentInfo.ip }}
            </td>
            <td>
                {{ agentInfo.macAddr }}
            </td>
            <td>
                <div class="tooltip" :data-tip="realTime(agentInfo)">
                    {{ timeAgo(agentInfo) }}
                </div>
            </td>
        </tr>
        </tbody>
    </table>
</template>
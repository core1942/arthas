<script setup lang="ts">
import { onMounted, reactive } from 'vue';
type AgentId = string
const agentInfos: ([AgentId, AgentInfo])[] = reactive([])
function getUrlParam(name: string) {
  const urlparam = new URLSearchParams(window.location.search)
  return urlparam.get(name)
}
function tunnelWebConsoleLink(agentId: string, tunnelPort: number, targetServer: string) {
  return `/?targetServer=${targetServer}&port=${tunnelPort}&agentId=${agentId}`;
}

const fetchMyApps = () => {
  const appName = getUrlParam("app") ?? ""
  let url = `/api/tunnelAgentInfo?app=${appName}`
  fetch(url)
    .then((response) => response.json())
    .then((data: Record<AgentId, AgentInfo>) => {
      for (const key in data) {
        agentInfos.push(
          [key, data[key] as AgentInfo]
        )
      }

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
      <tr >
        <th class="normal-case">门店名称</th>
        <th class="normal-case">门店ID</th>
        <th class="normal-case">主机IP</th>
        <th class="normal-case">应用版本</th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="(agentInfoRecord) in agentInfos" :key="agentInfoRecord[0]" class="hover">
        <td>
          <a class="btn btn-primary btn-sm"
            :href="tunnelWebConsoleLink(agentInfoRecord[0], agentInfoRecord[1].clientConnectTunnelPort, agentInfoRecord[1].clientConnectHost)">{{
                agentInfoRecord[1].shopName
            }}</a>
        </td>
        <td>
          {{ agentInfoRecord[1].shopId }}
        </td>
        <td>
          {{ agentInfoRecord[1].host }}
        </td>
        <td>
          {{ agentInfoRecord[1].applicationVersion }}
        </td>
      </tr>
    </tbody>
  </table>
</template>
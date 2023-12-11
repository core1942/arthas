<script setup lang="ts">
import { onMounted, reactive } from 'vue';

const apps: AppInfo[] = reactive([])
function fetchMyApps() {
  fetch('/api/tunnelApps')
    .then((response) => response.json())
    .then((data: AppInfo[]) => {
      data.length > 0 && data.forEach(app => apps.push(app))
    })
    .catch((error) => {
      console.error('api error ' + error)
    })
}
function detailLink(appName: string) {
  return "agents.html?app=" + appName;
}
onMounted(() => {
  fetchMyApps()
})
</script>

<template>
  <table class="table w-[100vw] table-normal">
    <thead>
      <tr>
        <th class="normal-case">品牌名称</th>
        <th class="normal-case">品牌ID</th>
        <th class="normal-case">在线门店数</th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="app in apps" :key="app" class="hover">
        <td>
          <a class="btn btn-primary btn-sm normal-case" :href="detailLink(app.sellerName+'-'+app.sellerId)">{{ app.sellerName }}</a>
        </td>
        <td>
          {{ app.sellerId }}
        </td>
        <td>
          {{ app.storeNum }}
        </td>
      </tr>
    </tbody>
  </table>
</template>

<style scoped>

</style>
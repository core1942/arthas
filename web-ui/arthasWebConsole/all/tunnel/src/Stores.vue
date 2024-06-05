<script setup lang="ts">
import {onMounted, reactive} from 'vue';

const storeInfos: StoreInfo[] = reactive([])

function getUrlParam(name: string) {
    const urlparam = new URLSearchParams(window.location.search)
    return urlparam.get(name)
}

function fetchMyApps() {
    const sellerId = getUrlParam("sellerId");
    if (!sellerId) {
        alert("请先选择品牌！")
    }
    let url = `/api/tunnelStores?sellerId=${sellerId}`
    fetch(url)
        .then((response) => response.json())
        .then((data: StoreInfo[]) => {
            data.length > 0 && data.forEach(storeInfo => storeInfos.push(storeInfo));
        })
        .catch((error) => {
            console.error('api error ' + error)
        })
}

function detailLink(sellerId: number,storeId: number) {
    return "agents.html?sellerId=" + sellerId + "&storeId=" + storeId;
}

onMounted(() => {
    fetchMyApps()
})
</script>

<template>
    <table class="table w-[100vw] table-normal">
        <thead>
        <tr>
            <th class="normal-case">门店名称</th>
            <th class="normal-case">门店ID</th>
            <th class="normal-case">在线客户端数</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="storeInfo in storeInfos" :key="storeInfo" class="hover">
            <td>
                <a class="btn btn-outline btn-primary" :href="detailLink(storeInfo.sellerId,storeInfo.storeId)">{{
                        storeInfo.storeName
                    }}</a>
            </td>
            <td>
                {{ storeInfo.storeId }}
            </td>
            <td>
                {{ storeInfo.agentNum }}
            </td>
        </tr>
        </tbody>
    </table>
</template>

<style scoped>

</style>
<script setup lang="ts">
import {onMounted, reactive} from 'vue';

const sellerInfos: SellerInfo[] = reactive([])

function fetchMyApps() {
    fetch('/api/tunnelSellers')
        .then((response) => response.json())
        .then((data: SellerInfo[]) => {
            data.length > 0 && data.forEach(sellerInfo => sellerInfos.push(sellerInfo))
        })
        .catch((error) => {
            console.error('api error ' + error)
        })
}

function detailLink(sellerId: number) {
    return "stores.html?sellerId=" + sellerId;
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
        <tr v-for="sellerInfo in sellerInfos" :key="sellerInfo" class="hover">
            <td>
                <a class="btn btn-outline btn-primary" :href="detailLink(sellerInfo.sellerId)">{{
                        sellerInfo.sellerName
                    }}</a>
            </td>
            <td>
                {{ sellerInfo.sellerId }}
            </td>
            <td>
                {{ sellerInfo.storeNum }}
            </td>
        </tr>
        </tbody>
    </table>
</template>

<style scoped>

</style>
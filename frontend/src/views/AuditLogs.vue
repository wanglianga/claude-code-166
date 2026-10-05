<template>
  <div>
    <h2 class="page-title">操作审计日志</h2>
    <div class="card-soft">
      <el-input v-model="module" placeholder="按模块筛选，如 账单/巡诊任务/建床申请" style="width:300px" clearable @change="load" />
      <el-table :data="logs" v-loading="loading" size="small" class="mt-12">
        <el-table-column label="时间" width="165"><template #default="{row}">{{ row.occurredAt?.replace('T',' ').slice(0,19) }}</template></el-table-column>
        <el-table-column prop="module" label="模块" width="110" />
        <el-table-column prop="action" label="动作" width="140" />
        <el-table-column prop="bizNo" label="业务单号" width="170" />
        <el-table-column label="操作人" width="150">
          <template #default="{row}">{{ row.operatorName }} <span class="muted">({{ label('Role', row.operatorRole) }})</span></template>
        </el-table-column>
        <el-table-column prop="detail" label="内容" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP" width="120" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../api'
import { label } from '../labels'

const logs = ref([])
const loading = ref(true)
const module = ref('')
async function load() {
  loading.value = true
  logs.value = (await api.get('/trace/audit-logs' + (module.value ? '?module=' + encodeURIComponent(module.value) : ''))).data
  loading.value = false
}
onMounted(load)
</script>

<style scoped>
.mt-12 { margin-top: 12px; }
</style>

<template>
  <div class="page">
    <div class="page-title">风险事件</div>
    <el-card class="section-card">
      <div class="toolbar">
        <el-radio-group v-model="statusFilter" size="small" @change="load">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button value="OPEN">待处置</el-radio-button>
          <el-radio-button value="HANDLED">跟进中</el-radio-button>
          <el-radio-button value="CLOSED">已关闭</el-radio-button>
        </el-radio-group>
        <el-button @click="load">刷新</el-button>
      </div>
      <el-table :data="rows" stripe>
        <el-table-column label="类型" width="110">
          <template #default="{row}">
            <el-tag size="small" :type="eventType(row.eventType)">{{ EN[row.eventType] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="事件描述" min-width="320" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{row}">
            <el-tag size="small" :type="row.status==='OPEN'?'danger':row.status==='HANDLED'?'warning':'info'">
              {{ row.status==='OPEN'?'待处置':row.status==='HANDLED'?'跟进中':'已关闭' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="handleNote" label="处置记录" min-width="220" show-overflow-tooltip>
          <template #default="{row}">{{ row.handleNote || '—' }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="发生时间" width="170" />
        <el-table-column label="操作" width="180">
          <template #default="{row}">
            <el-button link type="primary" @click="$router.push(`/applications/${row.applicationId}`)">关联病床</el-button>
            <el-button v-if="['DOCTOR','ADMIN'].includes(role) && row.status!=='CLOSED'"
                       link type="success" @click="openHandle(row)">处置</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dlg" title="处置风险事件" width="520px">
      <el-alert type="error" :closable="false" style="margin-bottom:10px" :title="cur?.description" />
      <el-form label-width="92px">
        <el-form-item label="处置措施"><el-input v-model="hf.handleNote" type="textarea" :rows="4"
          placeholder="如：加密巡视频次、远程复核、联系家属、协调转诊、冷链药品封存等" /></el-form-item>
        <el-form-item label="关闭事件"><el-switch v-model="hf.close" active-text="处置完成并关闭" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg=false">取消</el-button>
        <el-button type="primary" @click="submit">提交处置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'
import { RISK_EVENT_NAMES as EN } from '../constants'

const role = JSON.parse(localStorage.getItem('user') || '{}').role
const rows = ref([])
const statusFilter = ref('')
const dlg = ref(false)
const cur = ref(null)
const hf = reactive({ handleNote: '', close: false })

function eventType(t) {
  return { DETERIORATION: 'danger', PRESSURE_ULCER: 'warning', OVERTIME: 'warning',
    COLD_CHAIN: 'danger', COMPLAINT: 'danger', OTHER: 'info' }[t]
}
async function load() { rows.value = await api.get('/risks', { params: { status: statusFilter.value } }) }
function openHandle(row) { cur.value = row; hf.handleNote = ''; hf.close = false; dlg.value = true }
async function submit() {
  if (!hf.handleNote) return ElMessage.warning('请填写处置措施')
  await api.post('/risks/handle', { eventId: cur.value.id, ...hf })
  ElMessage.success('处置已记录'); dlg.value = false; load()
}
onMounted(load)
</script>

<style scoped>.toolbar{display:flex;justify-content:space-between;margin-bottom:12px}</style>

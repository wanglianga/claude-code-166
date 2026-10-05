<template>
  <div class="page">
    <div class="page-title">收费质疑处理</div>
    <el-card>
      <el-table :data="rows" stripe>
        <el-table-column prop="id" label="#" width="60" />
        <el-table-column label="状态" width="90">
          <template #default="{row}"><el-tag size="small" :type="QT[row.status]">{{ QN[row.status] }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="content" label="家属质疑" min-width="260" show-overflow-tooltip />
        <el-table-column prop="reply" label="答复" min-width="260" show-overflow-tooltip>
          <template #default="{row}">{{ row.reply || '—' }}</template>
        </el-table-column>
        <el-table-column label="退费" width="90">
          <template #default="{row}">
            <span v-if="Number(row.refundAmount)>0" style="color:#f56c6c">{{ money(row.refundAmount) }}</span>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="提交时间" width="170" />
        <el-table-column label="操作" width="200">
          <template #default="{row}">
            <el-button link type="primary" @click="$router.push(`/applications/${row.applicationId}`)">还原服务经过</el-button>
            <el-button v-if="['OPEN','REPLIED'].includes(row.status)" link type="success" @click="openReply(row)">答复/退费</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dlg" title="答复收费质疑" width="560px">
      <el-alert type="info" :closable="false" style="margin-bottom:10px"
                title="请先在“还原服务经过”中核对巡诊记录、现场照片、医嘱与家属签字，再答复；确认多收的可退还个人自付部分。" />
      <div class="muted" style="margin-bottom:8px">家属质疑：{{ cur?.content }}</div>
      <el-form label-width="92px">
        <el-form-item label="答复内容"><el-input v-model="rf.reply" type="textarea" :rows="4" /></el-form-item>
        <el-form-item label="退还自付费"><el-input-number v-model="rf.refundAmount" :min="0" :precision="2" :step="1" style="width:160px" /> 元</el-form-item>
        <el-form-item label="标记解决"><el-switch v-model="rf.resolved" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg=false">取消</el-button>
        <el-button type="primary" @click="submit">提交答复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'
import { QN, QT, money } from '../constants'

const rows = ref([])
const dlg = ref(false)
const cur = ref(null)
const rf = reactive({ reply: '', refundAmount: 0, resolved: true })

async function load() { rows.value = await api.get('/bills/questions/all') }
function openReply(row) {
  cur.value = row
  rf.reply = ''; rf.refundAmount = 0; rf.resolved = true
  dlg.value = true
}
async function submit() {
  if (!rf.reply) return ElMessage.warning('请填写答复内容')
  await api.post('/bills/questions/reply', { questionId: cur.value.id, ...rf })
  ElMessage.success('答复已提交')
  dlg.value = false; load()
}
onMounted(load)
</script>

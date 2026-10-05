<template>
  <div>
    <h2 class="page-title">巡诊异常处理（住院/家属不在/缺货/超时/恶化/受阻）</h2>
    <div class="card-soft">
      <el-radio-group v-model="filter" size="default" class="mb12" @change="load">
        <el-radio-button label="">全部</el-radio-button>
        <el-radio-button label="REPORTED">已上报</el-radio-button>
        <el-radio-button label="CONFIRMED">已核实</el-radio-button>
        <el-radio-button label="RESOLVED">已处理</el-radio-button>
      </el-radio-group>
      <el-table :data="exceptions" v-loading="loading" size="small">
        <el-table-column label="类型" width="130">
          <template #default="{row}"><el-tag size="small" :type="row.type==='CONDITION_WORSENED'?'danger':'warning'">{{ label('ExceptionType', row.type) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="患者" width="100"><template #default="{row}">{{ row.patient.name }}</template></el-table-column>
        <el-table-column label="关联巡诊" width="180"><template #default="{row}">{{ row.visit.visitNo }}</template></el-table-column>
        <el-table-column prop="description" label="情况说明" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{row}"><el-tag size="small" :type="tagType('ExceptionStatus',row.status)">{{ label('ExceptionStatus',row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="处理结论" show-overflow-tooltip>
          <template #default="{row}">{{ row.handleConclusion || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{row}">
            <el-button v-if="row.status!=='RESOLVED' && canHandle" link type="primary" size="small" @click="open(row)">处理/改期改派</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="visible" title="处理巡诊异常" width="480px">
      <el-form label-width="92px">
        <el-form-item label="处理结论">
          <el-input v-model="form.conclusion" type="textarea" :rows="3" placeholder="核实情况与处置：改期/改派/暂停/联系家属等" />
        </el-form-item>
        <el-form-item label="改期到"><el-date-picker v-model="form.newTime" type="datetime" style="width:100%" /></el-form-item>
        <el-form-item label="或改派给">
          <el-select v-model="form.reassignNurseId" clearable placeholder="不改派则留空" style="width:100%">
            <el-option v-for="n in nurses" :key="n.id" :label="`${n.name}（${n.title}）`" :value="n.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible=false">取消</el-button>
        <el-button type="primary" @click="submit">提交处理并恢复任务</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import dayjs from '../util/dayjs-lite'
import api from '../api'
import { useAuthStore } from '../store/auth'
import { label, tagType } from '../labels'

const auth = useAuthStore()
const exceptions = ref([])
const loading = ref(true)
const filter = ref('REPORTED')
const nurses = ref([])
const visible = ref(false)
const form = reactive({})
let current = null
const canHandle = computed(() => ['ADMIN', 'FAMILY_DOCTOR'].includes(auth.role))

async function load() {
  loading.value = true
  exceptions.value = (await api.get('/visits/exceptions/list' + (filter.value ? '?status=' + filter.value : ''))).data
  loading.value = false
}
onMounted(async () => {
  await load()
  nurses.value = (await api.get('/users?role=NURSE')).data
})

function open(row) {
  current = row
  Object.assign(form, { conclusion: '', newTime: null, reassignNurseId: null, status: 'RESOLVED' })
  visible.value = true
}
async function submit() {
  if (!form.conclusion) return ElMessage.warning('请填写处理结论')
  const body = { conclusion: form.conclusion, status: 'RESOLVED' }
  if (form.newTime) body.newTime = dayjs(form.newTime)
  if (form.reassignNurseId) body.reassignNurseId = form.reassignNurseId
  await api.post(`/visits/exceptions/${current.id}/handle`, body)
  ElMessage.success('异常已处理，任务已恢复排班')
  visible.value = false
  load()
}
</script>

<style scoped>
.mb12 { margin-bottom: 12px; }
</style>

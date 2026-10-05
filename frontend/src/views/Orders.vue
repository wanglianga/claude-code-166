<template>
  <div>
    <div class="head-line">
      <h2 class="page-title" style="margin:0">医嘱与远程复核（调整处方 / 复诊 / 转诊）</h2>
      <el-radio-group v-model="filter" size="default" @change="load">
        <el-radio-button label="">全部</el-radio-button>
        <el-radio-button label="PROPOSED">待复核</el-radio-button>
        <el-radio-button label="REMOTE_REVIEWED">已复核</el-radio-button>
        <el-radio-button label="EXECUTED">已执行</el-radio-button>
        <el-radio-button label="VOID">已作废</el-radio-button>
      </el-radio-group>
    </div>
    <div class="card-soft">
      <el-table :data="orders" v-loading="loading" size="small">
        <el-table-column prop="orderNo" label="医嘱号" width="170" />
        <el-table-column label="患者" width="90"><template #default="{row}">{{ row.patient.name }}</template></el-table-column>
        <el-table-column label="类型" width="100">
          <template #default="{row}"><el-tag size="small" :type="row.orderType==='REFERRAL'?'danger':row.orderType==='PRESCRIPTION'?'warning':''">{{ label('OrderType',row.orderType) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="content" label="医嘱内容" show-overflow-tooltip />
        <el-table-column label="下达医生" width="100"><template #default="{row}">{{ row.doctor.name }}</template></el-table-column>
        <el-table-column label="复核" width="100"><template #default="{row}">{{ row.reviewer?.name || '-' }}</template></el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{row}"><el-tag size="small" :type="tagType('OrderStatus',row.status)">{{ label('OrderStatus',row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{row}">
            <el-button link type="info" size="small" @click="view(row)">查看</el-button>
            <el-button v-if="canReview(row)" link type="primary" size="small" @click="openReview(row)">远程复核</el-button>
            <el-button v-if="auth.isDoctor && row.status!=='EXECUTED' && row.status!=='VOID'" link type="success" size="small" @click="execute(row)">执行确认</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-drawer v-model="detailVisible" size="480px" :title="'医嘱 ' + (current?.orderNo||'')">
      <template v-if="current">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="患者">{{ current.patient.name }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ label('OrderType',current.orderType) }}</el-descriptions-item>
          <el-descriptions-item label="医嘱内容">{{ current.content }}</el-descriptions-item>
          <el-descriptions-item label="诊断依据">{{ current.diagnosisBasis || '-' }}</el-descriptions-item>
          <el-descriptions-item label="冷链要求">
            <el-tag v-if="current.coldChainRequired" size="small" type="primary">需冷链：{{ current.coldChainNote }}</el-tag>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="下达">{{ current.doctor.name }} · {{ current.createdAt?.replace('T',' ').slice(0,16) }}</el-descriptions-item>
          <el-descriptions-item label="远程复核">{{ current.reviewer ? current.reviewer.name+'：'+current.reviewComment : '待复核' }}</el-descriptions-item>
          <el-descriptions-item label="执行情况">{{ current.executionNote || '-' }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-drawer>

    <el-dialog v-model="reviewVisible" title="医生远程复核" width="460px">
      <el-input v-model="reviewComment" type="textarea" :rows="4" placeholder="复核意见（用药安全/转诊必要性等）" />
      <template #footer>
        <el-button type="danger" @click="doReview(false)">退回补充</el-button>
        <el-button type="primary" @click="doReview(true)">复核通过</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'
import { useAuthStore } from '../store/auth'
import { label, tagType } from '../labels'

const auth = useAuthStore()
const orders = ref([])
const loading = ref(true)
const filter = ref('PROPOSED')
const detailVisible = ref(false)
const current = ref(null)
const reviewVisible = ref(false)
const reviewComment = ref('')

async function load() {
  loading.value = true
  orders.value = (await api.get('/orders' + (filter.value ? '?status=' + filter.value : ''))).data
  loading.value = false
}
onMounted(load)

function canReview(row) {
  return auth.isDoctor && row.status === 'PROPOSED' && row.doctor.id !== auth.user.id
}
function view(row) { current.value = row; detailVisible.value = true }
function openReview(row) { current.value = row; reviewComment.value = ''; reviewVisible.value = true }
async function doReview(approved) {
  await api.post(`/orders/${current.value.id}/remote-review`, { approved, comment: reviewComment.value })
  ElMessage.success(approved ? '远程复核通过' : '已退回')
  reviewVisible.value = false
  load()
}
async function execute(row) {
  await api.post(`/orders/${row.id}/execute`, { note: '已按医嘱执行并告知家属' })
  ElMessage.success('已确认执行')
  load()
}
</script>

<style scoped>
.head-line { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
</style>

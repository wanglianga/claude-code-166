<template>
  <div class="page">
    <div class="page-title">统一账单与医保结算</div>

    <el-card class="section-card">
      <div class="toolbar">
        <el-radio-group v-if="role!=='FAMILY'" v-model="statusFilter" size="small" @change="load">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button v-for="(n,k) in BS" :key="k" :value="k">{{ n }}</el-radio-button>
        </el-radio-group>
        <div>
          <el-button v-if="['ADMIN','INSURANCE','DOCTOR'].includes(role)" type="primary" @click="genDlg=true">生成账单</el-button>
          <el-button @click="load">刷新</el-button>
        </div>
      </div>
      <el-alert type="success" :closable="false"
                title="一张账单汇总：护理收费 + 普通耗材 + 冷链药品，逐项拆分医保报销与个人自付；生成时自动进行重复收费、超频次、目录变化、目录外项目合规检查。" />
      <el-table :data="bills" stripe @row-click="open" style="cursor:pointer;margin-top:10px">
        <el-table-column prop="billNo" label="账单号" width="170" />
        <el-table-column prop="applicationId" label="病床申请#" width="100" />
        <el-table-column label="账期" width="180">
          <template #default="{row}">{{ row.periodStart }} ~ {{ row.periodEnd }}</template>
        </el-table-column>
        <el-table-column label="合计" width="100"><template #default="{row}">{{ money(row.totalAmount) }}</template></el-table-column>
        <el-table-column label="医保报销" width="110">
          <template #default="{row}"><span style="color:#67c23a">{{ money(row.insuranceAmount) }}</span></template>
        </el-table-column>
        <el-table-column label="个人自付" width="110">
          <template #default="{row}"><span style="color:#e6a23c">{{ money(row.selfPayAmount) }}</span></template>
        </el-table-column>
        <el-table-column label="冷链药费" width="100">
          <template #default="{row}">{{ money(row.coldChainAmount) }}</template>
        </el-table-column>
        <el-table-column label="合规" width="90">
          <template #default="{row}">
            <el-tag v-if="row.complianceIssues" type="warning" size="small">有提示</el-tag>
            <el-tag v-else type="success" size="small">通过</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{row}"><el-tag :type="BT[row.status]" size="small">{{ BS[row.status] }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{row}"><el-button link type="primary" @click.stop="open(row)">明细</el-button></template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 生成账单 -->
    <el-dialog v-model="genDlg" title="按账期生成统一账单" width="480px">
      <el-form label-width="100px">
        <el-form-item label="家庭病床">
          <el-select v-model="gf.applicationId" filterable placeholder="选择在建病床" style="width:100%">
            <el-option v-for="a in apps" :key="a.id"
                       :label="`#${a.id} ${a.patientName}（${a.bedNo||a.code}）`" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="账期开始"><el-date-picker v-model="gf.start" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
        <el-form-item label="账期结束"><el-date-picker v-model="gf.end" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
        <el-alert type="info" :closable="false" title="仅归集已完成巡诊中尚未进入账单的领用项目，避免重复收费。" />
      </el-form>
      <template #footer>
        <el-button @click="genDlg=false">取消</el-button>
        <el-button type="primary" @click="doGenerate">生成</el-button>
      </template>
    </el-dialog>

    <!-- 账单明细 -->
    <el-dialog v-model="detailDlg" title="账单明细" width="900px">
      <div v-if="cur">
        <el-descriptions :column="4" border size="small">
          <el-descriptions-item label="账单号">{{ cur.bill.billNo }}</el-descriptions-item>
          <el-descriptions-item label="状态"><el-tag :type="BT[cur.bill.status]" size="small">{{ BS[cur.bill.status] }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="合计">{{ money(cur.bill.totalAmount) }}</el-descriptions-item>
          <el-descriptions-item label="自付（含退费）">{{ money(cur.bill.selfPayAmount) }}</el-descriptions-item>
          <el-descriptions-item label="医保报销">{{ money(cur.bill.insuranceAmount) }}</el-descriptions-item>
          <el-descriptions-item label="冷链药费">{{ money(cur.bill.coldChainAmount) }}</el-descriptions-item>
          <el-descriptions-item label="服务时间范围" :span="2">{{ cur.bill.periodStart }} ~ {{ cur.bill.periodEnd }}</el-descriptions-item>
        </el-descriptions>
        <el-alert v-if="cur.bill.complianceIssues" type="warning" :closable="false" style="margin-top:8px" title="医保合规提示">
          <div style="white-space:pre-line">{{ cur.bill.complianceIssues }}</div>
        </el-alert>

        <el-table :data="cur.items" size="small" border style="margin-top:10px">
          <el-table-column label="确认" width="55" v-if="role==='FAMILY'">
            <template #default="{row}"><el-checkbox v-model="row.familyAck" /></template>
          </el-table-column>
          <el-table-column prop="itemName" label="项目" min-width="170" />
          <el-table-column label="类别" width="80"><template #default="{row}">{{ KN2[row.kind] }}</template></el-table-column>
          <el-table-column label="数量" width="70"><template #default="{row}">{{ row.quantity }}{{ row.unit }}</template></el-table-column>
          <el-table-column label="单价" width="75"><template #default="{row}">{{ money(row.unitPrice) }}</template></el-table-column>
          <el-table-column label="金额" width="80"><template #default="{row}">{{ money(row.amount) }}</template></el-table-column>
          <el-table-column label="医保/自付" width="150">
            <template #default="{row}">
              <div style="color:#67c23a">报{{ money(row.insuranceAmount) }}</div>
              <div style="color:#e6a23c">自付{{ money(row.selfPayAmount) }}</div>
            </template>
          </el-table-column>
          <el-table-column label="合规标记" min-width="180">
            <template #default="{row}">
              <el-tag v-if="row.complianceFlag" type="warning" size="small">{{ row.complianceFlag }}</el-tag>
              <el-tag v-else type="success" size="small">正常</el-tag>
              <el-button v-if="role==='FAMILY'" link type="danger" size="small" @click="ask(row)">质疑</el-button>
            </template>
          </el-table-column>
          <el-table-column label="服务时间" width="150"><template #default="{row}">{{ row.serviceTime }}</template></el-table-column>
        </el-table>

        <div class="qbox">
          <div v-for="q in cur.questions" :key="q.id" class="qitem">
            <el-tag size="small" :type="QT[q.status]">{{ QN[q.status] }}</el-tag>
            <b>家属质疑：</b>{{ q.content }}
            <div v-if="q.reply" class="reply"><b>卫生院/医保答复：</b>{{ q.reply }}
              <span v-if="Number(q.refundAmount)>0" style="color:#f56c6c">；已退自付费 {{ money(q.refundAmount) }}</span>
            </div>
          </div>
        </div>

        <div v-if="cur.bill.familyConfirmNote" class="muted">家属确认意见：{{ cur.bill.familyConfirmNote }}</div>
      </div>
      <template #footer>
        <template v-if="role==='FAMILY'">
          <el-button type="warning" plain @click="ask(null)">对账单提出质疑</el-button>
          <el-button type="primary" @click="confirm">逐项确认并确认账单</el-button>
        </template>
        <template v-if="['ADMIN','INSURANCE'].includes(role)">
          <el-button type="success" :disabled="cur?.bill.status!=='CONFIRMED'" @click="settle">医保结算</el-button>
        </template>
      </template>
    </el-dialog>

    <!-- 质疑 -->
    <el-dialog v-model="qDlg" title="对收费提出质疑" width="460px" append-to-body>
      <el-input v-model="qContent" type="textarea" :rows="4"
                placeholder="请说明疑问，例如：未使用过的耗材、重复收费、自费项目未提前告知等。卫生院可结合照片、医嘱、签字记录还原核对。" />
      <template #footer>
        <el-button @click="qDlg=false">取消</el-button>
        <el-button type="danger" @click="submitAsk">提交质疑</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'
import { BS, BT, KN2, QN, QT, money } from '../constants'

const role = JSON.parse(localStorage.getItem('user') || '{}').role
const bills = ref([])
const apps = ref([])
const statusFilter = ref('')
const genDlg = ref(false)
const detailDlg = ref(false)
const qDlg = ref(false)
const cur = ref(null)
const curItem = ref(null)
const qContent = ref('')
const gf = reactive({ applicationId: null, start: new Date().toISOString().slice(0, 10), end: new Date().toISOString().slice(0, 10) })

async function load() {
  bills.value = role === 'FAMILY' ? await api.get('/bills/mine') : await api.get('/bills', { params: { status: statusFilter.value } })
}
async function open(row) {
  cur.value = await api.get(`/bills/${row.id}`)
  detailDlg.value = true
}
async function doGenerate() {
  if (!gf.applicationId) return ElMessage.warning('请选择病床')
  const d = await api.post('/bills/generate', gf)
  ElMessage.success('账单已生成')
  genDlg.value = false
  load()
  cur.value = d; detailDlg.value = true
}
function ask(item) { curItem.value = item; qContent.value = ''; qDlg.value = true }
async function submitAsk() {
  if (!qContent.value) return ElMessage.warning('请填写质疑内容')
  await api.post('/bills/questions', {
    billId: cur.value.bill.id, billItemId: curItem.value?.id, content: qContent.value
  })
  ElMessage.success('已提交，卫生院将结合服务记录核对并答复')
  qDlg.value = false; detailDlg.value = false; load()
}
async function confirm() {
  const { value } = await ElMessageBox.prompt('确认意见（可选）', '确认账单', { inputValue: '已与护理记录和照片核对' })
  await api.post(`/bills/${cur.value.bill.id}/confirm`, {
    familyConfirmNote: value,
    ackItemIds: cur.value.items.filter(i => i.familyAck).map(i => i.id)
  })
  ElMessage.success('已确认，等待医保结算')
  detailDlg.value = false; load()
}
async function settle() {
  await ElMessageBox.confirm('确认进行医保结算？', '医保结算', { type: 'warning' })
  await api.post(`/bills/${cur.value.bill.id}/settle`)
  ElMessage.success('结算完成'); detailDlg.value = false; load()
}

onMounted(async () => {
  if (role !== 'FAMILY') apps.value = await api.get('/applications', { params: { status: 'ACTIVE' } })
  load()
})
</script>

<style scoped>
.toolbar { display:flex; justify-content:space-between; margin-bottom:12px; }
.qbox { margin-top:12px; }
.qitem { background:#fff8f0; border:1px solid #f5dab1; border-radius:6px; padding:8px 10px; margin-bottom:6px; font-size:13px; }
.reply { margin-top:4px; background:#f0f9eb; border-radius:4px; padding:6px 8px; }
</style>

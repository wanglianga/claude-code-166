<template>
  <div class="page">
    <div class="page-title">排班与上门路线</div>

    <el-card class="section-card">
      <div class="toolbar">
        <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" :clearable="false" @change="load" />
        <div>
          <el-button v-if="['DOCTOR','ADMIN'].includes(role)" type="primary" plain @click="reassignAll">重新自动排班</el-button>
          <el-button :icon="Refresh" @click="load">刷新</el-button>
        </div>
      </div>
      <el-alert type="info" :closable="false"
                title="排班综合考虑：8km 标准上门半径、护理与路程耗时、护士当日负荷上限、患者风险等级、家属期望频次；当资源冲突时会在“可解释安排”中说明取舍。" />
    </el-card>

    <el-row :gutter="14">
      <el-col v-for="n in board.nurses||[]" :key="n.nurse?.id||'x'" :span="12">
        <el-card class="section-card">
          <template #header>
            <div class="nurse-head">
              <div><el-icon><User /></el-icon> <b>{{ n.nurse?.name }}</b>
                <span class="muted">{{ n.nurse?.phone }}</span></div>
              <el-tag :type="n.overload?'danger':'success'">负荷 {{ n.loadRatio }}%（{{ n.totalMin }}/{{ n.capacityMin }}分）</el-tag>
            </div>
          </template>
          <el-progress :percentage="Math.min(n.loadRatio,100)" :status="n.overload?'exception':''" :show-text="false" style="margin-bottom:10px" />
          <div class="muted" style="margin-bottom:8px">当日往返约 {{ Math.round(n.roundTripKm*10)/10 }} km · {{ n.stops.length }} 个上门点</div>
          <el-timeline>
            <el-timeline-item v-for="s in n.stops" :key="s.planId" :timestamp="`第${s.routeOrder||'-'}站 · ${s.plannedTime||''} · ${s.distanceKm}km · ${s.durationMin}分`"
                              placement="top" :type="s.riskLevel==='HIGH'||s.riskLevel==='CRITICAL'?'danger':'primary'">
              <div class="stop">
                <div>
                  <el-link type="primary" @click="$router.push(`/applications/${s.applicationId}`)">
                    患者#{{ s.patientId }}
                  </el-link>
                  <el-tag v-if="s.riskLevel" size="small" :type="RT[s.riskLevel]" style="margin-left:6px">{{ RN[s.riskLevel] }}</el-tag>
                  <el-tag v-if="s.bedNo" size="small" effect="plain" style="margin-left:4px">{{ s.bedNo }}</el-tag>
                </div>
                <div class="muted">照护人：{{ s.caregiverName }} {{ s.caregiverPhone }}</div>
                <div class="explain">{{ s.scheduleNote }}</div>
                <div class="stop-actions">
                  <el-button v-if="role==='NURSE'" type="primary" size="small" @click="startVisit(s)">签到上门</el-button>
                  <el-button size="small" @click="viewVisit(s)">记录</el-button>
                  <el-button v-if="role==='NURSE'" type="warning" size="small" plain @click="openCancel(s)">异常/取消</el-button>
                  <el-button v-if="['DOCTOR','ADMIN'].includes(role)" size="small" plain @click="openReassign(s)">改派</el-button>
                </div>
              </div>
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
    </el-row>

    <el-card v-if="(board.suspended||[]).length" class="section-card" header="挂起 / 取消的上门（住院、家属不在、缺货等留痕）">
      <el-table :data="board.suspended" size="small">
        <el-table-column prop="planDate" label="日期" width="110"><template #default="{row}">{{ row.planDate }}</template></el-table-column>
        <el-table-column prop="bedNo" label="床号" width="90" />
        <el-table-column label="状态" width="90"><template #default="{row}">{{ row.status==='SUSPENDED'?'已挂起':'已终止' }}</template></el-table-column>
        <el-table-column prop="cancelReason" label="原因" min-width="260" />
      </el-table>
    </el-card>

    <!-- 巡诊执行弹窗 -->
    <el-dialog v-model="execDlg" title="上门巡诊记录" width="820px" :close-on-click-modal="false">
      <el-form :model="vf" label-width="110px">
        <el-divider content-position="left">生命体征</el-divider>
        <el-row :gutter="10">
          <el-col :span="8"><el-form-item label="体温(℃)"><el-input-number v-model="vf.temperature" :precision="1" :step="0.1" :min="34" :max="43" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="收缩压"><el-input-number v-model="vf.systolic" :min="60" :max="260" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="舒张压"><el-input-number v-model="vf.diastolic" :min="30" :max="160" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="心率"><el-input-number v-model="vf.heartRate" :min="30" :max="220" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="血糖(mmol/L)"><el-input-number v-model="vf.bloodGlucose" :precision="1" :step="0.1" :min="2" :max="40" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="血氧(%)"><el-input-number v-model="vf.spo2" :min="50" :max="100" style="width:100%" /></el-form-item></el-col>
        </el-row>

        <el-divider content-position="left">护理评估</el-divider>
        <el-form-item label="压疮情况"><el-input v-model="vf.pressureUlcer" type="textarea" :rows="2" placeholder="部位/分期/面积/渗液" /></el-form-item>
        <el-form-item label="换药部位"><el-input v-model="vf.dressingChange" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="导尿护理"><el-input v-model="vf.catheterCare" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="用药提醒"><el-input v-model="vf.medicationReminder" type="textarea" :rows="2" placeholder="服药情况、胰岛素/慢病用药提醒" /></el-form-item>
        <el-form-item label="护理记录"><el-input v-model="vf.nursingNote" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="异常发现">
          <el-input v-model="vf.abnormalNote" type="textarea" :rows="2" placeholder="病情恶化、感染征象等；填写后将自动生成风险事件并提示医生远程复核" />
        </el-form-item>

        <el-divider content-position="left">服务 / 耗材 / 冷链药品领用（自动出库并计入统一账单）</el-divider>
        <el-table :data="vf.itemRows" size="small" border>
          <el-table-column label="项目" min-width="200">
            <template #default="{row}">
              {{ row.name }}
              <el-tag size="small" :type="CT[row.category]" style="margin-left:4px">{{ CN[row.category] }}</el-tag>
              <el-tag v-if="row.coldChain" size="small" type="danger" style="margin-left:4px">冷链</el-tag>
              <el-tag v-if="!row.insuranceCovered" size="small" type="info" style="margin-left:4px">自费</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="库存" width="80"><template #default="{row}">{{ row.stockQty }}{{ row.unit }}</template></el-table-column>
          <el-table-column label="数量" width="120">
            <template #default="{row}"><el-input-number v-model="row.qty" :min="0" :max="99" :precision="0" size="small" style="width:100px" /></template>
          </el-table-column>
        </el-table>
        <el-form-item label="冷链交接温度" style="margin-top:10px">
          <el-input-number v-model="vf.coldChainTemp" :precision="1" :step="0.1" :min="-5" :max="30" style="width:160px" />
          <span class="muted">使用了冷链药品时必须填写，合规区间 2~8℃</span>
        </el-form-item>

        <el-divider content-position="left">现场照片</el-divider>
        <el-upload :show-file-list="false" :before-upload="uploadPhoto" accept="image/*" multiple>
          <el-button plain type="primary">上传换药/压疮/导尿/家属在场照片</el-button>
        </el-upload>
        <div v-if="photos.length" class="photos">
          <el-image v-for="(p,i) in photos" :key="i" :src="p.url" fit="cover" class="photo" />
        </div>

        <el-divider content-position="left">家属确认签字</el-divider>
        <el-form-item label="签字人姓名"><el-input v-model="vf.familySignName" placeholder="如：王梅" style="width:200px" /></el-form-item>
        <SignaturePad v-model="vf.familySignature" />
      </el-form>
      <template #footer>
        <el-button @click="execDlg=false">取消</el-button>
        <el-button type="warning" plain @click="abortDlg2=true">本次无法完成（家属不在/缺货等）</el-button>
        <el-button type="primary" @click="submitVisit">完成并提交（待医生复核）</el-button>
      </template>
    </el-dialog>

    <!-- 未完成原因弹窗 -->
    <el-dialog v-model="abortDlg2" title="上门未完成登记（不计费、不扣耗材）" width="520px">
      <el-form label-width="92px">
        <el-form-item label="原因">
          <el-select v-model="vf.abortType" style="width:100%">
            <el-option v-for="(n,k) in AN" :key="k" :label="n" :value="k" :disabled="k==='NONE'" />
          </el-select>
        </el-form-item>
        <el-form-item label="情况说明"><el-input v-model="vf.abortNote" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="abortDlg2=false">取消</el-button>
        <el-button type="warning" @click="submitAbort">提交未完成登记</el-button>
      </template>
    </el-dialog>

    <!-- 取消计划 -->
    <el-dialog v-model="cancelDlg" title="取消/终止该次上门" width="480px">
      <el-form label-width="92px">
        <el-form-item label="原因类型">
          <el-select v-model="cf.abortType" style="width:100%">
            <el-option v-for="(n,k) in AN" :key="k" :label="n" :value="k" :disabled="k==='NONE'" />
          </el-select>
        </el-form-item>
        <el-form-item label="说明"><el-input v-model="cf.note" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cancelDlg=false">取消</el-button>
        <el-button type="warning" @click="submitCancel">确认取消</el-button>
      </template>
    </el-dialog>

    <!-- 改派 -->
    <el-dialog v-model="reassignDlg" title="人工改派护士" width="420px">
      <el-form label-width="92px">
        <el-form-item label="改派给">
          <el-select v-model="rf.nurseId" style="width:100%">
            <el-option v-for="n in nurses" :key="n.id" :label="`${n.name}（${n.title}）`" :value="n.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="改派原因"><el-input v-model="rf.note" type="textarea" :rows="3" placeholder="如路线冲突、技能匹配、家属时间要求" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reassignDlg=false">取消</el-button>
        <el-button type="primary" @click="submitReassign">改派并重排路线</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh, User } from '@element-plus/icons-vue'
import api from '../api'
import { RN, RT, AN, CN, CT } from '../constants'
import SignaturePad from '../components/SignaturePad.vue'

const router = useRouter()
const role = JSON.parse(localStorage.getItem('user') || '{}').role
const date = ref(new Date().toISOString().slice(0, 10))
const board = ref({})
const catalog = ref([])
const nurses = ref([])

const execDlg = ref(false)
const abortDlg2 = ref(false)
const cancelDlg = ref(false)
const reassignDlg = ref(false)
const curStop = ref(null)
const curVisitId = ref(null)
const photos = ref([])

const emptyVf = () => ({ temperature: null, systolic: null, diastolic: null, heartRate: null,
  bloodGlucose: null, spo2: null, pressureUlcer: '', dressingChange: '', catheterCare: '',
  medicationReminder: '', nursingNote: '', abnormalNote: '', coldChainTemp: null,
  familySignName: '', familySignature: '', abortType: 'OTHER', abortNote: '', itemRows: [] })
const vf = reactive(emptyVf())
const cf = reactive({ abortType: 'FAMILY_ABSENT', note: '' })
const rf = reactive({ nurseId: null, note: '' })

async function load() {
  board.value = await api.get('/plans/board', { params: { date: date.value } })
}
async function startVisit(s) {
  curStop.value = s
  Object.assign(vf, emptyVf())
  photos.value = []
  const v = await api.post(`/visits/start/${s.planId}`)
  curVisitId.value = v.id
  catalog.value = await api.get('/catalog')
  vf.itemRows = catalog.value.map(c => ({ ...c, qty: 0 }))
  // 预填计划内护理项目数量为 1
  ;(s.services || '').split(',').filter(Boolean).forEach(code => {
    const row = vf.itemRows.find(r => r.code === code)
    if (row) row.qty = 1
  })
  execDlg.value = true
}
async function viewVisit(s) {
  router.push(`/applications/${s.applicationId}`)
}
async function uploadPhoto(file) {
  const fd = new FormData()
  fd.append('file', file)
  fd.append('category', '现场照片')
  const p = await api.post(`/visits/${curVisitId.value}/photos`, fd,
    { headers: { 'Content-Type': 'multipart/form-data' } })
  photos.value.push(p)
  ElMessage.success('照片已上传')
  return false
}
function usedItems() {
  return vf.itemRows.filter(r => r.qty > 0).map(r => ({ itemId: r.id, quantity: r.qty }))
}
function coldUsed() { return vf.itemRows.some(r => r.qty > 0 && r.coldChain) }

async function submitVisit() {
  if (!vf.familySignName || !vf.familySignature) return ElMessage.warning('请填写家属姓名并完成手写签字')
  if (coldUsed() && vf.coldChainTemp == null) return ElMessage.warning('使用了冷链药品，请记录交接温度')
  await api.post(`/visits/${curVisitId.value}/complete`, { ...vf, items: usedItems() })
  ElMessage.success('巡诊已提交，等待医生远程复核')
  execDlg.value = false
  load()
}
async function submitAbort() {
  await api.post(`/visits/${curVisitId.value}/complete`, {
    abortType: vf.abortType, abortNote: vf.abortNote
  })
  ElMessage.success('已登记未完成，本次不计费不扣耗材')
  abortDlg2.value = false; execDlg.value = false; load()
}
function openCancel(s) { curStop.value = s; cf.abortType = 'FAMILY_ABSENT'; cf.note = ''; cancelDlg.value = true }
async function submitCancel() {
  await api.post(`/plans/${curStop.value.planId}/cancel`, cf)
  ElMessage.success('该次上门已取消'); cancelDlg.value = false; load()
}
async function reassignAll() {
  const r = await api.post('/plans/assign', null, { params: { date: date.value } })
  ElMessage.success(r); load()
}
function openReassign(s) {
  curStop.value = s; rf.nurseId = null; rf.note = ''
  reassignDlg.value = true
}
async function submitReassign() {
  if (!rf.nurseId) return ElMessage.warning('请选择护士')
  await api.post(`/plans/${curStop.value.planId}/reassign`, rf)
  ElMessage.success('已改派并重排路线'); reassignDlg.value = false; load()
}

onMounted(async () => {
  nurses.value = await api.get('/users', { params: { role: 'NURSE' } })
  load()
})
</script>

<style scoped>
.toolbar { display:flex; justify-content:space-between; margin-bottom:12px; }
.nurse-head { display:flex; justify-content:space-between; align-items:center; }
.stop { background:#fafcff; border:1px solid #e6eef8; border-radius:8px; padding:8px 10px; }
.explain { font-size:12px; color:#4a6b8a; background:#f0f6fc; border-radius:4px; padding:4px 6px; margin:6px 0; }
.stop-actions { margin-top:6px; }
.photos { display:flex; gap:8px; margin-top:8px; flex-wrap:wrap; }
.photo { width:100px; height:75px; border-radius:6px; border:1px solid #eee; }
</style>

<template>
  <div>
    <div class="head-line">
      <h2 class="page-title" style="margin:0">巡诊计划与可解释排班</h2>
      <div>
        <el-select v-if="auth.isAdmin" v-model="genAppId" placeholder="选择已批准建床的申请生成计划" style="width:340px">
          <el-option v-for="a in approvedApps" :key="a.id"
                     :label="`${a.patient.name} · ${a.bedNo} · ${label('RiskLevel',a.riskLevel)} · ${(a.distanceMeters/1000).toFixed(1)}km`"
                     :value="a.id" />
        </el-select>
        <el-button v-if="auth.isAdmin" type="primary" :disabled="!genAppId" class="ml8"
                   :loading="generating" @click="generate">智能排班并生成4周计划</el-button>
      </div>
    </div>

    <el-row :gutter="14">
      <el-col :span="9">
        <div class="card-soft">
          <el-table :data="plans" size="small" highlight-current-row @current-change="selectPlan" height="640">
            <el-table-column label="计划/患者" min-width="180">
              <template #default="{row}">
                <div><b>{{ row.patient.name }}</b>
                  <el-tag size="small" :type="tagType('PlanStatus',row.status)" class="ml6">{{ label('PlanStatus',row.status) }}</el-tag>
                </div>
                <div class="muted">{{ row.planNo }}</div>
                <div class="muted">护士：{{ row.nurse?.name }} ｜ {{ row.timesPerWeek }}次/周</div>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>
      <el-col :span="15">
        <div class="card-soft" v-if="currentPlan">
          <div class="section-title">排班可解释说明</div>
          <div class="explain-box">{{ currentPlan.scheduleExplanation || '暂无' }}</div>
          <div class="section-title mt-16">计划内容</div>
          <div>{{ currentPlan.planContent }}</div>
          <div class="section-title mt-16">上门任务（{{ visits.length }}）</div>
          <el-table :data="visits" size="small" max-height="330">
            <el-table-column label="预约时间" width="155">
              <template #default="{row}">{{ row.scheduledAt?.replace('T',' ').slice(0,16) }}</template>
            </el-table-column>
            <el-table-column label="护士" width="80"><template #default="{row}">{{ row.nurse?.name }}</template></el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{row}"><el-tag size="small" :type="tagType('VisitStatus',row.status)">{{ label('VisitStatus',row.status) }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="arrangementNote" label="安排说明" show-overflow-tooltip />
            <el-table-column label="操作" width="130">
              <template #default="{row}">
                <el-button link type="primary" size="small" @click="openReschedule(row)">改期</el-button>
                <el-button v-if="auth.isAdmin" link type="warning" size="small" @click="openReassign(row)">改派</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <el-empty v-else description="选择左侧计划查看排班解释与任务" />
      </el-col>
    </el-row>

    <!-- 排班候选评分结果 -->
    <el-dialog v-model="resultVisible" title="智能排班结果（为什么是这位护士）" width="720px">
      <template v-if="genResult">
        <el-alert type="success" :closable="false" show-icon
                  :title="`责任护士：${genResult.selectedNurseName}，已生成 ${genResult.generatedVisits} 次上门任务`" class="mb12"/>
        <el-table :data="genResult.candidates" size="small" border>
          <el-table-column label="候选护士"><template #default="{row}">
            <b>{{ row.nurseName }}</b> <span class="muted">{{ row.title }}</span>
            <el-tag v-if="$index===0" size="small" type="success" class="ml6">选中</el-tag>
          </template></el-table-column>
          <el-table-column prop="score" label="综合得分" width="90" align="center" />
          <el-table-column label="技能匹配" width="90" align="center">
            <template #default="{row}">
              <el-tag size="small" :type="row.matchedSkills===row.requiredSkills?'success':'danger'">
                {{ row.matchedSkills }}/{{ row.requiredSkills }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="weeklyLoad" label="本周已排" width="90" align="center" />
          <el-table-column label="路线(km)" width="90" align="center">
            <template #default="{row}">{{ (row.avgRouteMeters/1000).toFixed(1) }}</template>
          </el-table-column>
          <el-table-column label="评分依据">
            <template #default="{row}"><div v-for="(r,i) in row.reasons" :key="i" class="muted">· {{ r }}</div></template>
          </el-table-column>
        </el-table>
        <el-divider>基层约束权衡</el-divider>
        <div v-for="(c,i) in genResult.constraints" :key="i" class="constraint">⚙ {{ c }}</div>
      </template>
    </el-dialog>

    <el-dialog v-model="rescheduleVisible" title="巡诊改期" width="380px">
      <el-date-picker v-model="newTime" type="datetime" placeholder="选择新的上门时间" style="width:100%" />
      <el-input v-model="changeReason" type="textarea" :rows="2" placeholder="改期原因（如家属不在家/道路受阻）" class="mt-12"/>
      <template #footer><el-button @click="rescheduleVisible=false">取消</el-button>
        <el-button type="primary" @click="doReschedule">确认改期</el-button></template>
    </el-dialog>

    <el-dialog v-model="reassignVisible" title="改派护士" width="420px">
      <el-select v-model="newNurseId" placeholder="选择护士" style="width:100%">
        <el-option v-for="n in nurses" :key="n.id"
                   :label="`${n.name}（${n.title}） 技能：${n.skills||'无'}`" :value="n.id" />
      </el-select>
      <el-input v-model="changeReason" type="textarea" :rows="2" placeholder="改派原因" class="mt-12"/>
      <template #footer><el-button @click="reassignVisible=false">取消</el-button>
        <el-button type="warning" @click="doReassign">确认改派</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import dayjs from '../util/dayjs-lite'
import api from '../api'
import { useAuthStore } from '../store/auth'
import { label, tagType } from '../labels'

const auth = useAuthStore()
const route = useRoute()
const plans = ref([])
const approvedApps = ref([])
const currentPlan = ref(null)
const visits = ref([])
const genAppId = ref(null)
const generating = ref(false)
const genResult = ref(null)
const resultVisible = ref(false)
const nurses = ref([])

async function load() {
  plans.value = (await api.get('/plans')).data
  approvedApps.value = (await api.get('/applications?status=APPROVED')).data
  if (route.query.applicationId) {
    const p = plans.value.find(p => String(p.application.id) === String(route.query.applicationId))
    if (p) selectPlan(p)
  } else if (plans.value.length) {
    selectPlan(plans.value[0])
  }
}
onMounted(async () => {
  await load()
  nurses.value = (await api.get('/users?role=NURSE')).data
})

async function selectPlan(plan) {
  if (!plan) return
  currentPlan.value = plan
  visits.value = (await api.get(`/visits?patientId=${plan.patient.id}`)).data
    .filter(v => v.plan.id === plan.id)
}

async function generate() {
  generating.value = true
  try {
    const res = (await api.post('/plans/generate', { applicationId: genAppId.value })).data
    genResult.value = res
    resultVisible.value = true
    ElMessage.success('计划已生成')
    await load()
    const p = plans.value.find(p => p.application.id === genAppId.value)
    if (p) selectPlan(p)
  } finally { generating.value = false }
}

const rescheduleVisible = ref(false)
const reassignVisible = ref(false)
const newTime = ref()
const newNurseId = ref(null)
const changeReason = ref('')
let targetVisit = null
function openReschedule(row) { targetVisit = row; newTime.value = new Date(row.scheduledAt); changeReason.value = ''; rescheduleVisible.value = true }
function openReassign(row) { targetVisit = row; newNurseId.value = null; changeReason.value = ''; reassignVisible.value = true }
async function doReschedule() {
  if (!newTime.value) return ElMessage.warning('请选择时间')
  await api.post(`/plans/visits/${targetVisit.id}/reschedule`,
    { newTime: dayjs(newTime.value), reason: changeReason.value })
  ElMessage.success('已改期')
  rescheduleVisible.value = false
  selectPlan(currentPlan.value)
}
async function doReassign() {
  if (!newNurseId.value) return ElMessage.warning('请选择护士')
  await api.post(`/plans/visits/${targetVisit.id}/reassign`,
    { nurseId: newNurseId.value, reason: changeReason.value })
  ElMessage.success('已改派')
  reassignVisible.value = false
  selectPlan(currentPlan.value)
}
</script>

<style scoped>
.head-line { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
.ml8 { margin-left: 8px; } .ml6 { margin-left: 6px; }
.mb12 { margin-bottom: 12px; }
.section-title { font-weight: 600; color: #303a45; margin-bottom: 6px; }
.constraint { background: #fdf6ec; border-left: 3px solid #e6a23c; padding: 6px 10px; margin-bottom: 6px; border-radius: 4px; font-size: 13px; }
</style>

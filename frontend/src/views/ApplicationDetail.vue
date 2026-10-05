<template>
  <div class="page">
    <el-page-header @back="$router.back()" class="page-title" style="margin-bottom:10px">
      <template #content>
        <span style="font-weight:700;font-size:17px">家庭病床详情</span>
        <el-tag style="margin-left:10px" :type="ST[d.application?.status]">{{ SN[d.application?.status] }}</el-tag>
        <el-tag v-if="d.application?.bedNo" type="success" effect="plain" style="margin-left:6px">床号 {{ d.application.bedNo }}</el-tag>
      </template>
    </el-page-header>

    <el-row :gutter="14">
      <el-col :span="8">
        <el-card class="section-card" header="患者与申请">
          <el-descriptions :column="1" size="small" border v-if="d.patient">
            <el-descriptions-item label="姓名">{{ d.patient.name }}</el-descriptions-item>
            <el-descriptions-item label="性别/年龄">{{ d.patient.gender }} {{ d.patient.age }}岁</el-descriptions-item>
            <el-descriptions-item label="行动能力">{{ d.patient.mobility }}</el-descriptions-item>
            <el-descriptions-item label="诊断">{{ d.patient.diagnosis }}</el-descriptions-item>
            <el-descriptions-item label="地址">{{ d.patient.address }}</el-descriptions-item>
            <el-descriptions-item label="医保">
              {{ d.patient.insuranceType }}
              <el-tag size="small" :type="d.application?.insuranceEligible?'success':'danger'" style="margin-left:6px">
                {{ d.application?.insuranceEligible===null ? '待核验' : d.application.insuranceEligible ? '资格有效' : '资格不符' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="照护人">
              {{ d.application?.caregiverName }}（{{ d.application?.caregiverRelation }}）{{ d.application?.caregiverPhone }}
            </el-descriptions-item>
            <el-descriptions-item label="申请原因">{{ d.application?.reason }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card class="section-card" header="医生评估" v-if="d.application?.assessDoctor">
          <el-descriptions :column="1" size="small" border>
            <el-descriptions-item label="评估医生">{{ d.assessDoctor?.name }}</el-descriptions-item>
            <el-descriptions-item label="距离">{{ d.application.distanceKm }} km</el-descriptions-item>
            <el-descriptions-item label="风险等级">
              <el-tag size="small" :type="RT[d.application.riskLevel]">{{ RN[d.application.riskLevel] }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="护理项目">{{ d.approvedServiceNames?.join('、') }}</el-descriptions-item>
            <el-descriptions-item label="建议频次">{{ d.application.approvedFrequency }}</el-descriptions-item>
            <el-descriptions-item label="评估意见">{{ d.application.assessOpinion }}</el-descriptions-item>
            <el-descriptions-item label="驳回原因" v-if="d.application.rejectReason">
              <span style="color:#f56c6c">{{ d.application.rejectReason }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card class="section-card" header="病床操作">
          <template v-if="['DOCTOR','ADMIN'].includes(role)">
            <el-button type="primary" plain :disabled="d.application?.status!=='ACTIVE'" @click="planDlg=true">生成巡诊计划</el-button>
            <el-button type="warning" plain :disabled="d.application?.status!=='ACTIVE'" @click="pause">暂停（住院/无人）</el-button>
            <el-button type="success" plain :disabled="d.application?.status!=='PAUSED'" @click="resume">恢复巡诊</el-button>
            <el-button type="danger" plain :disabled="!['ACTIVE','PAUSED'].includes(d.application?.status)" @click="discharge">撤床结案</el-button>
          </template>
          <el-alert v-if="d.application?.status==='PAUSED'" type="warning" :closable="false" style="margin-top:8px"
                    :title="`病床已暂停：${d.application.pauseReason||''}`" />
        </el-card>
      </el-col>

      <el-col :span="16">
        <el-card class="section-card" header="巡诊计划">
          <el-table :data="d.plans" size="small">
            <el-table-column prop="planDate" label="日期" width="110" />
            <el-table-column label="时间/顺序" width="100">
              <template #default="{row}">{{ row.plannedTime||'—' }} {{ row.routeOrder?`第${row.routeOrder}站`:'' }}</template>
            </el-table-column>
            <el-table-column label="护士" width="90">
              <template #default="{row}">{{ nurseName(row.nurseId) }}</template>
            </el-table-column>
            <el-table-column prop="durationMin" label="耗时" width="70">
              <template #default="{row}">{{ row.durationMin }}分</template>
            </el-table-column>
            <el-table-column label="状态" width="80">
              <template #default="{row}">
                <el-tag size="small" :type="planType(row.status)">{{ planName(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="cancelReason" label="异常/解释" min-width="180" show-overflow-tooltip />
            <el-table-column label="排班说明" min-width="220">
              <template #default="{row}">
                <el-popover v-if="row.scheduleNote" width="320" trigger="hover">
                  <template #reference><el-link type="primary" :underline="false">查看可解释安排</el-link></template>
                  <div style="max-width:300px">{{ row.scheduleNote }}</div>
                </el-popover>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <el-card class="section-card" header="巡诊记录（体征 · 护理 · 签字 · 照片）">
          <el-empty v-if="!d.visits?.length" description="暂无上门记录" :image-size="60" />
          <el-collapse v-model="activeVisits">
            <el-collapse-item v-for="b in d.visits" :key="b.visit.id" :name="b.visit.id">
              <template #title>
                <el-tag size="small" :type="b.visit.status==='COMPLETED'?'success':'info'" style="margin-right:8px">
                  {{ VS[b.visit.status] }}
                </el-tag>
                <span>巡诊 #{{ b.visit.id }}</span>
                <span class="muted" style="margin-left:10px">{{ b.visit.completedAt || b.visit.createdAt }}</span>
                <el-tag v-if="b.visit.overtimeMin>=20" type="danger" size="small" style="margin-left:8px">超时{{ b.visit.overtimeMin }}分</el-tag>
                <el-tag v-if="b.visit.reviewStatus==='PENDING'" type="warning" size="small" style="margin-left:6px">待复核</el-tag>
              </template>
              <el-descriptions :column="3" size="small" border>
                <el-descriptions-item label="体温">{{ b.visit.temperature ?? '—' }}℃</el-descriptions-item>
                <el-descriptions-item label="血压">{{ b.visit.systolic ?? '—' }}/{{ b.visit.diastolic ?? '—' }}</el-descriptions-item>
                <el-descriptions-item label="心率">{{ b.visit.heartRate ?? '—' }}</el-descriptions-item>
                <el-descriptions-item label="血糖">{{ b.visit.bloodGlucose ?? '—' }} mmol/L</el-descriptions-item>
                <el-descriptions-item label="血氧">{{ b.visit.spo2 ?? '—' }}%</el-descriptions-item>
                <el-descriptions-item label="实际耗时">{{ b.visit.actualDurationMin ?? '—' }} 分钟</el-descriptions-item>
                <el-descriptions-item label="压疮情况" :span="3">{{ b.visit.pressureUlcer || '—' }}</el-descriptions-item>
                <el-descriptions-item label="换药部位" :span="3">{{ b.visit.dressingChange || '—' }}</el-descriptions-item>
                <el-descriptions-item label="导尿护理" :span="3">{{ b.visit.catheterCare || '—' }}</el-descriptions-item>
                <el-descriptions-item label="用药提醒" :span="3">{{ b.visit.medicationReminder || '—' }}</el-descriptions-item>
                <el-descriptions-item label="护理记录" :span="3">{{ b.visit.nursingNote || '—' }}</el-descriptions-item>
                <el-descriptions-item label="⚠异常发现" :span="3">
                  <span :style="{color:b.visit.abnormalNote?'#f56c6c':'#909399'}">{{ b.visit.abnormalNote || '无' }}</span>
                </el-descriptions-item>
                <el-descriptions-item v-if="b.visit.abortType && b.visit.abortType!=='NONE'" label="未完成原因" :span="3">
                  <el-tag type="warning" size="small">{{ AN[b.visit.abortType] }}</el-tag> {{ b.visit.abortNote }}
                </el-descriptions-item>
                <el-descriptions-item label="冷链温度" :span="3">
                  <span v-if="b.visit.coldChainTemp!=null">
                    {{ b.visit.coldChainTemp }}℃
                    <el-tag size="small" :type="(b.visit.coldChainTemp>=2&&b.visit.coldChainTemp<=8)?'success':'danger'">
                      {{ (b.visit.coldChainTemp>=2&&b.visit.coldChainTemp<=8)?'温度合规':'冷链异常' }}
                    </el-tag>
                  </span>
                  <span v-else class="muted">本次未使用冷链药品</span>
                </el-descriptions-item>
              </el-descriptions>

              <div class="sub">领用项目（计入账单）</div>
              <el-table :data="b.items" size="small" border>
                <el-table-column prop="itemName" label="项目" min-width="160" />
                <el-table-column prop="kind" label="类别" width="90">
                  <template #default="{row}">{{ KN[row.kind] }}</template>
                </el-table-column>
                <el-table-column label="数量" width="80"><template #default="{row}">{{ row.quantity }}{{ row.unit }}</template></el-table-column>
                <el-table-column label="单价" width="80"><template #default="{row}">{{ money(row.unitPrice) }}</template></el-table-column>
                <el-table-column label="医保" width="90">
                  <template #default="{row}">
                    <el-tag size="small" :type="row.insuranceCovered?'success':'info'">
                      {{ row.insuranceCovered ? `报${row.reimburseRatio}%` : '自费' }}
                    </el-tag>
                  </template>
                </el-table-column>
              </el-table>

              <div class="sub">现场照片</div>
              <div v-if="b.photos.length" class="photos">
                <el-image v-for="ph in b.photos" :key="ph.id" :src="ph.url"
                          :preview-src-list="b.photos.map(x=>x.url)" :initial-index="b.photos.indexOf(ph)"
                          fit="cover" class="photo">
                  <template #placeholder><div class="ph-cap">{{ ph.category }}</div></template>
                </el-image>
              </div>
              <span v-else class="muted">无照片</span>

              <div class="sub">医生复核与医嘱</div>
              <div v-if="b.visit.reviewStatus && b.visit.reviewStatus!=='NONE'" class="timeline-explain">
                远程复核（{{ RV[b.visit.reviewStatus] }}）：{{ b.visit.reviewNote || '—' }}
              </div>
              <el-timeline v-if="b.orders?.length" style="margin-top:6px">
                <el-timeline-item v-for="o in b.orders" :key="o.id" :timestamp="o.targetDate||''" placement="top">
                  <el-tag size="small" type="primary" style="margin-right:6px">{{ ON[o.orderType] }}</el-tag>{{ o.content }}
                  <div v-if="o.targetOrg" class="muted">转诊机构：{{ o.targetOrg }}</div>
                </el-timeline-item>
              </el-timeline>

              <div class="sub">家属签字</div>
              <div v-if="b.visit.familySignature" class="sign-box">
                <el-image :src="b.visit.familySignature" style="width:120px;height:40px;background:#fff;border:1px solid #eee" fit="contain" />
                <span style="margin-left:10px">签字人：{{ b.visit.familySignName }} ｜ {{ b.visit.signedAt }}</span>
              </div>
              <span v-else class="muted">未完成服务或暂无签字</span>
            </el-collapse-item>
          </el-collapse>
        </el-card>

        <el-card class="section-card" header="争议还原 · 全过程时间线（申请/执行/照片/医嘱/签字/账单）">
          <el-timeline>
            <el-timeline-item v-for="(t,i) in d.timeline" :key="i"
                              :type="t.type==='VISIT'?'primary':'success'" :timestamp="t.time" placement="top">
              <b>{{ t.title }}</b>
              <div v-if="t.detail">{{ t.detail }}</div>
              <div v-if="t.operator" class="muted">操作人：{{ t.operator }}</div>
              <div v-if="t.type==='VISIT'">
                <span v-if="t.signed" class="flag-tag">家属签字：{{ t.signName }}</span>
                <span v-if="t.photos?.length" class="flag-tag">照片 {{ t.photos.length }} 张</span>
                <span v-if="t.items?.length" class="flag-tag">领用 {{ t.items.length }} 项</span>
                <span v-if="t.orders?.length" class="flag-tag">医嘱 {{ t.orders.length }} 条</span>
              </div>
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
    </el-row>

    <!-- 生成计划 -->
    <el-dialog v-model="planDlg" title="生成巡诊计划并自动排班" width="560px">
      <el-form label-width="110px">
        <el-form-item label="开始日期"><el-date-picker v-model="pf2.start" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
        <el-form-item label="结束日期"><el-date-picker v-model="pf2.end" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
        <el-form-item label="上门星期">
          <el-checkbox-group v-model="pf2.weekdays">
            <el-checkbox v-for="(w,i) in ['一','二','三','四','五','六','日']" :key="i" :value="i+1">周{{ w }}</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="护理项目">
          <el-select v-model="pf2.services" multiple style="width:100%" placeholder="不选则使用评估核定项目">
            <el-option v-for="p in approvedPairs()" :key="p.code" :label="p.name" :value="p.code" />
          </el-select>
        </el-form-item>
        <el-alert type="info" :closable="false"
                  title="系统将综合护士负荷、8km上门半径、患者风险等级与家属期望频次自动分配护士与路线，并给出可解释说明。" />
      </el-form>
      <template #footer>
        <el-button @click="planDlg=false">取消</el-button>
        <el-button type="primary" @click="genPlan">生成并排班</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'
import { SN, ST, RN, RT, VS, AN, ON, RV, KN, money } from '../constants'

const route = useRoute()
const role = JSON.parse(localStorage.getItem('user') || '{}').role
const d = ref({})
const activeVisits = ref([])
const planDlg = ref(false)
const pf2 = reactive({ start: new Date().toISOString().slice(0, 10), end: '', weekdays: [1, 3, 5], services: [] })

async function load() {
  d.value = await api.get(`/applications/${route.params.id}`)
  activeVisits.value = (d.value.visits || []).slice(0, 1).map(b => b.visit.id)
  const users = await api.get('/users', { params: { role: 'NURSE' } })
  nurseMap.value = Object.fromEntries(users.map(u => [u.id, u.name]))
}
function nurseName(id) { return nurseMap.value[id] || '未分配' }
const nurseMap = ref({})

// 核定护理项目 code/name 配对（评估通过后已固定）
const approvedPairs = () => {
  const codes = (d.value.application?.approvedServices || '').split(',').filter(Boolean)
  const names = d.value.approvedServiceNames || []
  return codes.map((c, i) => ({ code: c, name: names[i] || c }))
}

function planName(s) { return { ACTIVE: '待执行', SUSPENDED: '已挂起', FINISHED: '已终止/完成' }[s] || s }
function planType(s) { return { ACTIVE: 'success', SUSPENDED: 'warning', FINISHED: 'info' }[s] || '' }

async function genPlan() {
  if (!pf2.start || !pf2.end) return ElMessage.warning('请选择日期')
  const r = await api.post('/plans/generate', {
    applicationId: Number(route.params.id), startDate: pf2.start, endDate: pf2.end,
    weekdays: pf2.weekdays, services: pf2.services.length ? pf2.services : null
  })
  ElMessage.success(`已生成 ${r.created} 个计划；${(r.assignments||[]).join('；')}`)
  planDlg.value = false
  load()
}
async function pause() {
  const { value } = await ElMessageBox.prompt('暂停原因（如患者临时住院、家属不在家）', '暂停病床', { inputType: 'textarea' })
  await api.post(`/applications/${route.params.id}/pause`, { reason: value })
  ElMessage.success('已暂停，未来计划同步挂起'); load()
}
async function resume() {
  const { value } = await ElMessageBox.prompt('恢复说明', '恢复巡诊', { inputValue: '患者已出院回家，恢复正常巡诊' })
  await api.post(`/applications/${route.params.id}/resume`, { reason: value })
  ElMessage.success('已恢复'); load()
}
async function discharge() {
  const { value } = await ElMessageBox.prompt('撤床说明', '撤床结案', { inputType: 'textarea', inputValue: '患者病情稳定，结束家庭病床服务' })
  await api.post(`/applications/${route.params.id}/discharge`, { reason: value })
  ElMessage.success('已撤床结案'); load()
}

onMounted(load)
</script>

<style scoped>
:deep(.el-card__header) { font-weight:600; padding: 10px 14px; }
.sub { font-weight:600; font-size:13px; color:#1e4e7e; margin:12px 0 6px; }
.photos { display:flex; gap:8px; flex-wrap:wrap; }
.photo { width:120px; height:90px; border-radius:6px; border:1px solid #eee; }
.ph-cap { font-size:12px; color:#fff; display:flex; align-items:center; justify-content:center; height:100%; background:#8fa8c4; }
.sign-box { display:flex; align-items:center; background:#fafbfc; padding:8px; border:1px dashed #dcdfe6; border-radius:6px; }
</style>

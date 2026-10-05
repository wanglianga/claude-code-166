<template>
  <div>
    <div class="head-line">
      <h2 class="page-title" style="margin:0">家庭病床申请与医生评估</h2>
      <div>
        <el-select v-model="filter" placeholder="全部状态" clearable size="default" style="width:150px" @change="load">
          <el-option v-for="(v,k) in ENUMS.BedApplyStatus" :key="k" :label="v" :value="k" />
        </el-select>
        <el-button v-if="auth.isFamily" type="primary" :icon="Plus" class="ml8" @click="openSubmit">为老人提交建床申请</el-button>
      </div>
    </div>

    <div class="card-soft">
      <el-table :data="list" v-loading="loading" size="small">
        <el-table-column prop="applyNo" label="申请单号" width="190" />
        <el-table-column label="患者" width="100"><template #default="{row}">{{ row.patient?.name }}</template></el-table-column>
        <el-table-column label="疾病/行动" width="150">
          <template #default="{row}">{{ label('DiseaseCategory', row.patient?.diseaseCategory) }} · {{ row.patient?.mobilityLevel }}</template>
        </el-table-column>
        <el-table-column prop="applyReason" label="申请理由" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{row}"><el-tag size="small" :type="tagType('BedApplyStatus',row.status)">{{ label('BedApplyStatus',row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="风险" width="85">
          <template #default="{row}"><el-tag v-if="row.riskLevel" size="small" :type="tagType('RiskLevel',row.riskLevel)">{{ label('RiskLevel',row.riskLevel) }}</el-tag><span v-else class="muted">待评估</span></template>
        </el-table-column>
        <el-table-column label="距离" width="80">
          <template #default="{row}">{{ row.distanceMeters ? (row.distanceMeters/1000).toFixed(1)+'km' : '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{row}">
            <el-button link type="primary" size="small" @click="view(row)">详情</el-button>
            <el-button v-if="canAssess(row)" link type="warning" size="small" @click="openAssess(row)">评估</el-button>
            <el-button v-if="auth.isFamily && row.status==='SUBMITTED'" link type="info" size="small" @click="cancel(row)">撤销</el-button>
            <el-button v-if="row.status==='APPROVED'" link type="success" size="small" @click="$router.push('/plans?applicationId='+row.id)">巡诊安排</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 申请详情/评估结论 -->
    <el-drawer v-model="detailVisible" size="520px" :title="'申请单 ' + (current?.applyNo || '')">
      <template v-if="current">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="患者">{{ current.patient?.name }}（{{ current.patient?.gender }}/{{ current.patient?.age }}岁）</el-descriptions-item>
          <el-descriptions-item label="住址">{{ current.patient?.address }}</el-descriptions-item>
          <el-descriptions-item label="医保">{{ label('InsuranceType', current.patient?.insuranceType) }} / {{ current.patient?.insuranceNo }}</el-descriptions-item>
          <el-descriptions-item label="申请理由">{{ current.applyReason }}</el-descriptions-item>
          <el-descriptions-item label="期望项目">{{ current.expectedItems }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">护理需求</el-divider>
        <el-tag v-for="n in careNeeds" :key="n.id" class="ml6" type="success" effect="plain">
          {{ label('CareItemType', n.itemType) }} {{ n.timesPerWeek }}次/周
        </el-tag>

        <template v-if="current.doctor">
          <el-divider content-position="left">家庭医生评估（六要素）</el-divider>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="评估医生">{{ current.doctor?.name }} · {{ current.assessedAt?.replace('T',' ').slice(0,16) }}</el-descriptions-item>
            <el-descriptions-item label="①病情/项目"><b>{{ current.assessmentConclusion }}</b></el-descriptions-item>
            <el-descriptions-item label="②距离/路程">距卫生院 {{ current.distanceMeters }} 米，单程约 {{ current.travelMinutes }} 分钟</el-descriptions-item>
            <el-descriptions-item label="③照护人">{{ current.caregiverCondition }}</el-descriptions-item>
            <el-descriptions-item label="居住条件">{{ current.livingCondition }}</el-descriptions-item>
            <el-descriptions-item label="④医保资格">
              <el-tag size="small" :type="current.insuranceEligible?'success':'danger'">{{ current.insuranceEligible ? '符合' : '不符合' }}</el-tag>
              {{ current.insuranceNote }}
            </el-descriptions-item>
            <el-descriptions-item label="⑤护理项目">见上方标签</el-descriptions-item>
            <el-descriptions-item label="⑥风险等级">
              <el-tag size="small" :type="tagType('RiskLevel',current.riskLevel)">{{ label('RiskLevel',current.riskLevel) }}</el-tag>
              {{ current.riskNotes }}
            </el-descriptions-item>
          </el-descriptions>
          <div v-if="current.rejectReason" class="reject">驳回原因：{{ current.rejectReason }}</div>
          <el-button v-if="current.status==='APPROVED'" type="success" class="mt-12" @click="$router.push('/plans?applicationId='+current.id)">查看巡诊计划与排班解释</el-button>
          <el-button class="mt-12" @click="$router.push('/trace?applicationId='+current.id)">查看服务全链还原</el-button>
        </template>
      </template>
    </el-drawer>

    <!-- 家属提交申请 -->
    <el-dialog v-model="submitVisible" title="提交家庭病床申请" width="560px">
      <el-form :model="submitForm" label-width="100px">
        <el-form-item label="选择患者">
          <el-select v-model="submitForm.patientId" placeholder="请选择老人" style="width:100%">
            <el-option v-for="p in myPatients" :key="p.id"
                       :label="`${p.name}（${label('DiseaseCategory',p.diseaseCategory)}/${p.address}）`" :value="p.id" />
          </el-select>
          <div class="muted">如无档案，请先在“患者档案”中建立</div>
        </el-form-item>
        <el-form-item label="申请理由">
          <el-input v-model="submitForm.reason" type="textarea" :rows="3"
                    placeholder="如：术后行动不便/慢病长期卧床，说明病情与上门需要" />
        </el-form-item>
        <el-form-item label="期望护理">
          <el-input v-model="submitForm.expectedItems" placeholder="如：测血压、压疮护理、换药、胰岛素注射" />
        </el-form-item>
        <el-form-item label="护理需求">
          <el-checkbox-group v-model="submitForm.careItems">
            <el-checkbox v-for="(v,k) in ENUMS.CareItemType" :key="k" :value="k" border size="small" class="ml6">{{ v }}</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="submitVisible=false">取消</el-button>
        <el-button type="primary" @click="doSubmit">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 医生评估 -->
    <el-drawer v-model="assessVisible" size="600px" title="家庭医生评估">
      <template v-if="current">
        <el-alert type="info" :closable="false" show-icon class="mb12"
                  :title="`评估对象：${current.patient?.name}，住址 ${current.patient?.address}`" />
        <el-form :model="assessForm" label-width="120px">
          <el-form-item label="距卫生院(米)">
            <el-input-number v-model="assessForm.distanceMeters" :min="0" :step="100" />
            <span class="muted ml8">留空则按坐标自动测算（当前坐标：{{ current.patient?.latitude }},{{ current.patient?.longitude }}）</span>
          </el-form-item>
          <el-form-item label="照护人情况"><el-input v-model="assessForm.caregiverCondition" placeholder="是否长期在家、照护能力" /></el-form-item>
          <el-form-item label="居住条件"><el-input v-model="assessForm.livingCondition" placeholder="是否具备护理操作条件" /></el-form-item>
          <el-form-item label="医保资格">
            <el-switch v-model="assessForm.insuranceEligible" active-text="符合报销资格" inactive-text="不符合" />
            <el-input v-model="assessForm.insuranceNote" placeholder="参保类型/待遇备案核实说明" class="mt-12" />
          </el-form-item>
          <el-form-item label="风险等级">
            <el-radio-group v-model="assessForm.riskLevel">
              <el-radio-button v-for="(v,k) in ENUMS.RiskLevel" :key="k" :value="k">{{ v }}</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="风险说明"><el-input v-model="assessForm.riskNotes" type="textarea" :rows="2" /></el-form-item>
          <el-form-item label="护理项目与频次">
            <div v-for="(c,i) in assessForm.careItems" :key="i" class="need-row">
              <el-select v-model="c.itemType" style="width:180px">
                <el-option v-for="(v,k) in ENUMS.CareItemType" :key="k" :label="v" :value="k" />
              </el-select>
              <el-input-number v-model="c.timesPerWeek" :min="1" :max="7" /> 次/周
              <el-button link type="danger" @click="assessForm.careItems.splice(i,1)">删除</el-button>
            </div>
            <el-button size="small" @click="assessForm.careItems.push({itemType:'VITALS',timesPerWeek:1})">+ 添加护理项目</el-button>
          </el-form-item>
          <el-form-item label="评估结论"><el-input v-model="assessForm.conclusion" type="textarea" :rows="3" placeholder="是否同意建床、安排频次、特别说明" /></el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="showReject=true" :disabled="!current">驳回</el-button>
        <el-button type="primary" @click="doAssess(true)">批准建床</el-button>
      </template>
    </el-drawer>

    <el-dialog v-model="showReject" title="驳回申请" width="420px">
      <el-input v-model="assessForm.rejectReason" type="textarea" :rows="3" placeholder="驳回原因（将告知家属）" />
      <template #footer><el-button @click="showReject=false">取消</el-button>
        <el-button type="danger" @click="doAssess(false)">确认驳回</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'
import { useAuthStore } from '../store/auth'
import { ENUMS, label, tagType } from '../labels'

const auth = useAuthStore()
const list = ref([])
const myPatients = ref([])
const loading = ref(true)
const filter = ref('')
const detailVisible = ref(false)
const current = ref(null)
const careNeeds = ref([])

async function load() {
  loading.value = true
  const url = '/applications' + (filter.value ? '?status=' + filter.value : '')
  list.value = (await api.get(url)).data
  loading.value = false
}
onMounted(async () => {
  await load()
  if (auth.isFamily) myPatients.value = (await api.get('/patients')).data
})

function canAssess(row) {
  return (auth.isDoctor || auth.isAdmin) && row.status === 'SUBMITTED'
}

async function view(row) {
  current.value = row
  careNeeds.value = (await api.get(`/applications/${row.id}/care-needs`)).data
  detailVisible.value = true
}

async function cancel(row) {
  await ElMessageBox.confirm('确认撤销该申请？', '提示', { type: 'warning' })
  await api.post(`/applications/${row.id}/cancel`)
  ElMessage.success('已撤销')
  load()
}

// 家属提交
const submitVisible = ref(false)
const submitForm = reactive({ patientId: null, reason: '', expectedItems: '', careItems: [] })
function openSubmit() {
  Object.assign(submitForm, { patientId: null, reason: '', expectedItems: '', careItems: [] })
  submitVisible.value = true
}
async function doSubmit() {
  if (!submitForm.patientId) return ElMessage.warning('请选择患者')
  await api.post('/applications', submitForm)
  ElMessage.success('申请已提交，等待家庭医生评估')
  submitVisible.value = false
  load()
}

// 医生评估
const assessVisible = ref(false)
const showReject = ref(false)
const assessForm = reactive({})
function openAssess(row) {
  current.value = row
  Object.assign(assessForm, {
    distanceMeters: row.distanceMeters,
    caregiverCondition: '', livingCondition: '',
    insuranceEligible: true, insuranceNote: '',
    riskLevel: 'MEDIUM', riskNotes: '', conclusion: '',
    rejectReason: '',
    careItems: [{ itemType: 'VITALS', timesPerWeek: 2 }]
  })
  assessVisible.value = true
}
async function doAssess(approved) {
  if (approved && !assessForm.conclusion) return ElMessage.warning('请填写评估结论')
  if (!approved && !assessForm.rejectReason) return ElMessage.warning('请填写驳回原因')
  await api.post(`/applications/${current.value.id}/assess`, { approved, ...assessForm })
  ElMessage.success(approved ? '已批准建床，可生成巡诊计划' : '已驳回')
  assessVisible.value = false
  showReject.value = false
  load()
}
</script>

<style scoped>
.head-line { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
.ml8 { margin-left: 8px; } .ml6 { margin: 0 6px 6px 0; }
.mb12 { margin-bottom: 12px; }
.need-row { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.reject { color: #f56c6c; margin-top: 10px; }
</style>

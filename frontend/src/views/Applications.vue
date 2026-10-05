<template>
  <div class="page">
    <div class="page-title">家庭病床申请</div>

    <el-card class="section-card">
      <div class="toolbar">
        <el-radio-group v-if="role !== 'FAMILY'" v-model="statusFilter" size="small" @change="load">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button v-for="(n,k) in SN" :key="k" :value="k">{{ n }}</el-radio-button>
        </el-radio-group>
        <div>
          <el-button v-if="role==='FAMILY'" type="primary" :icon="Plus" @click="openSubmit">新建家庭病床申请</el-button>
          <el-button :icon="Refresh" @click="load">刷新</el-button>
        </div>
      </div>

      <el-table :data="list" stripe @row-click="go" highlight-current-row style="cursor:pointer">
        <el-table-column prop="code" label="申请编号" width="150" />
        <el-table-column prop="patientName" label="患者" width="90" />
        <el-table-column prop="diagnosis" label="主要诊断" min-width="200" show-overflow-tooltip />
        <el-table-column prop="address" label="居住地址/距离" min-width="200" show-overflow-tooltip>
          <template #default="{row}">
            {{ row.address }}
            <span v-if="row.distanceKm!=null" class="muted">（{{ row.distanceKm }}km）</span>
          </template>
        </el-table-column>
        <el-table-column label="风险" width="90">
          <template #default="{row}">
            <el-tag v-if="row.riskLevel" :type="RT[row.riskLevel]" size="small">{{ RN[row.riskLevel] }}</el-tag>
            <span v-else class="muted">未评估</span>
          </template>
        </el-table-column>
        <el-table-column prop="approvedFrequency" label="医嘱频次" width="120" />
        <el-table-column prop="bedNo" label="床号" width="90" />
        <el-table-column label="状态" width="100">
          <template #default="{row}">
            <el-tag :type="ST[row.status]" size="small">{{ SN[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{row}">
            <el-button link type="primary" @click.stop="go(row)">查看</el-button>
            <el-button v-if="role==='DOCTOR' && row.status==='SUBMITTED'"
                       link type="warning" @click.stop="openAssess(row)">评估</el-button>
            <el-button v-if="['ADMIN','DOCTOR'].includes(role) && row.status==='ASSESSED'"
                       link type="success" @click.stop="approve(row)">批准建床</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 家属提交申请 -->
    <el-dialog v-model="submitDlg" title="提交家庭病床申请" width="640px">
      <el-form :model="sf" label-width="104px">
        <el-form-item label="选择患者">
          <el-select v-model="sf.patientId" placeholder="已建档患者" style="width:100%">
            <el-option v-for="p in patients" :key="p.id"
                       :label="`${p.name}（${p.gender||''} ${p.age||''}岁 ${p.insuranceType||'医保未填'}）`" :value="p.id" />
          </el-select>
          <el-button link type="primary" @click="newPatientDlg=true">没有档案？点此建档</el-button>
        </el-form-item>
        <el-form-item label="主要照护人">
          <el-input v-model="sf.caregiverName" placeholder="姓名" style="width:150px" />
          <el-input v-model="sf.caregiverRelation" placeholder="与患者关系" style="width:130px;margin:0 8px" />
          <el-input v-model="sf.caregiverPhone" placeholder="联系电话" style="width:160px" />
        </el-form-item>
        <el-form-item label="申请原因">
          <el-input v-model="sf.reason" type="textarea" :rows="3"
                    placeholder="病情、行动能力、为何需要上门护理（如术后康复、卧床、慢病并发症）" />
        </el-form-item>
        <el-form-item label="需要护理项目">
          <el-select v-model="sf.requestedServices" multiple style="width:100%" placeholder="可多选">
            <el-option v-for="c in serviceCatalog" :key="c.code"
                       :label="`${c.name}（${money(c.price)}）`" :value="c.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="期望上门频次">
          <el-input v-model="sf.expectedFrequency" placeholder="如：每周3次" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="submitDlg=false">取消</el-button>
        <el-button type="primary" @click="doSubmit">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 快速建档 -->
    <el-dialog v-model="newPatientDlg" title="新建患者档案" width="600px">
      <el-form :model="pf" label-width="100px">
        <el-form-item label="姓名"><el-input v-model="pf.name" /></el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="pf.gender"><el-radio value="女">女</el-radio><el-radio value="男">男</el-radio></el-radio-group>
        </el-form-item>
        <el-form-item label="年龄"><el-input-number v-model="pf.age" :min="1" :max="120" /></el-form-item>
        <el-form-item label="身份证号"><el-input v-model="pf.idCard" /></el-form-item>
        <el-form-item label="联系电话"><el-input v-model="pf.phone" /></el-form-item>
        <el-form-item label="居住地址"><el-input v-model="pf.address" placeholder="含村、组及距卫生院距离" /></el-form-item>
        <el-form-item label="医保类型">
          <el-select v-model="pf.insuranceType" style="width:100%">
            <el-option v-for="t in ['城镇职工医保','城乡居民医保','异地医保','无医保']" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="医保号"><el-input v-model="pf.insuranceNo" /></el-form-item>
        <el-form-item label="主要诊断"><el-input v-model="pf.diagnosis" /></el-form-item>
        <el-form-item label="行动能力">
          <el-select v-model="pf.mobility" style="width:100%">
            <el-option v-for="t in ['可搀扶','轮椅','卧床']" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="newPatientDlg=false">取消</el-button>
        <el-button type="primary" @click="doCreatePatient">保存档案</el-button>
      </template>
    </el-dialog>

    <!-- 医生评估 -->
    <el-dialog v-model="assessDlg" title="家庭医生评估" width="680px">
      <el-form :model="af" label-width="118px">
        <el-alert type="info" :closable="false" style="margin-bottom:10px"
                  title="评估维度：病情与风险、居住距离、照护人条件、医保资格、需要的护理项目" />
        <el-form-item label="距卫生院(km)">
          <el-input-number v-model="af.distanceKm" :min="0" :max="60" :step="0.1" :precision="1" />
          <span v-if="af.distanceKm>8" class="warn-text">⚠ 超过 8km 标准上门半径，需在意见中说明安排</span>
        </el-form-item>
        <el-form-item label="医保资格核验">
          <el-switch v-model="af.insuranceEligible" active-text="资格有效" inactive-text="资格不符" />
        </el-form-item>
        <el-form-item label="风险等级">
          <el-radio-group v-model="af.riskLevel">
            <el-radio-button v-for="(n,k) in RN" :key="k" :value="k">{{ n }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="照护人">
          <el-input :model-value="`${cur?.caregiverName||''}（${cur?.caregiverRelation||''}） ${cur?.caregiverPhone||''}`" disabled />
        </el-form-item>
        <el-form-item label="核定护理项目">
          <el-select v-model="af.approvedServices" multiple style="width:100%">
            <el-option v-for="c in serviceCatalog" :key="c.code"
                       :label="`${c.code} ${c.name}（${c.frequencyLimit||'不限频次'}）`" :value="c.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="建议上门频次"><el-input v-model="af.approvedFrequency" placeholder="如：每周一、三、五" /></el-form-item>
        <el-form-item label="评估意见">
          <el-input v-model="af.assessOpinion" type="textarea" :rows="3"
                    placeholder="对病情、距离、照护、风险、医保的综合判断及可解释安排说明" />
        </el-form-item>
        <el-form-item label="驳回原因" v-if="!af.eligible">
          <el-input v-model="af.rejectReason" type="textarea" :rows="2" placeholder="不建床的原因" />
        </el-form-item>
        <el-form-item label="评估结论">
          <el-radio-group v-model="af.eligible">
            <el-radio :value="true">符合条件，建议建床</el-radio>
            <el-radio :value="false">不符合，驳回</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assessDlg=false">取消</el-button>
        <el-button type="primary" @click="doAssess">提交评估</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import api from '../api'
import { SN, ST, RN, RT, money } from '../constants'

const router = useRouter()
const role = JSON.parse(localStorage.getItem('user') || '{}').role
const list = ref([])
const patients = ref([])
const serviceCatalog = ref([])
const statusFilter = ref('')

const submitDlg = ref(false)
const newPatientDlg = ref(false)
const assessDlg = ref(false)
const cur = ref(null)
const sf = reactive({ patientId: null, caregiverName: '', caregiverRelation: '', caregiverPhone: '',
  reason: '', requestedServices: [], expectedFrequency: '' })
const pf = reactive({ name: '', gender: '女', age: 70, idCard: '', phone: '', address: '',
  insuranceType: '城乡居民医保', insuranceNo: '', diagnosis: '', mobility: '卧床' })
const af = reactive({ applicationId: null, distanceKm: 5, insuranceEligible: true, riskLevel: 'MEDIUM',
  approvedServices: [], approvedFrequency: '', assessOpinion: '', eligible: true, rejectReason: '' })

async function load() {
  list.value = role === 'FAMILY' ? await api.get('/applications/mine') : await api.get('/applications', { params: { status: statusFilter.value } })
}
async function loadAux() {
  patients.value = await api.get('/patients')
  serviceCatalog.value = (await api.get('/catalog', { params: { category: 'SERVICE' } }))
}
function go(row) { router.push(`/applications/${row.id}`) }

function openSubmit() {
  Object.assign(sf, { patientId: null, caregiverName: '', caregiverRelation: '', caregiverPhone: '',
    reason: '', requestedServices: [], expectedFrequency: '' })
  submitDlg.value = true
}
async function doCreatePatient() {
  if (!pf.name) return ElMessage.warning('请填写姓名')
  const p = await api.post('/patients', pf)
  ElMessage.success('档案已创建')
  newPatientDlg.value = false
  await loadAux()
  sf.patientId = p.id
}
async function doSubmit() {
  if (!sf.patientId) return ElMessage.warning('请选择患者')
  await api.post('/applications', sf)
  ElMessage.success('申请已提交，等待家庭医生评估')
  submitDlg.value = false
  load()
}
function openAssess(row) {
  cur.value = row
  Object.assign(af, { applicationId: row.id, distanceKm: row.distanceKm || 5, insuranceEligible: true,
    riskLevel: 'MEDIUM', approvedServices: [], approvedFrequency: '', assessOpinion: '', eligible: true, rejectReason: '' })
  assessDlg.value = true
}
async function doAssess() {
  await api.post('/applications/assess', af)
  ElMessage.success(af.eligible ? '评估已提交，等待审批建床' : '已驳回')
  assessDlg.value = false
  load()
}
async function approve(row) {
  await ElMessageBox.confirm(`确认审批通过并为患者建立家庭病床？`, '建床审批', { type: 'warning' })
  await api.post(`/applications/${row.id}/approve`)
  ElMessage.success('已建床，可生成巡诊计划')
  load()
}

onMounted(async () => { await loadAux(); load() })
</script>

<style scoped>
.toolbar { display:flex; justify-content:space-between; margin-bottom:12px; }
.warn-text { color:#e6a23c; font-size:13px; margin-left:10px; }
</style>

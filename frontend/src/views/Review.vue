<template>
  <div class="page">
    <div class="page-title">远程复核与医嘱</div>

    <el-card class="section-card" header="待远程复核的巡诊记录">
      <el-table :data="pending" stripe>
        <el-table-column prop="id" label="巡诊#" width="80" />
        <el-table-column label="患者" width="90"><template #default="{row}">患者#{{ row.patientId }}</template></el-table-column>
        <el-table-column label="生命体征" min-width="220">
          <template #default="{row}">
            T{{ row.temperature }}℃ 血压{{ row.systolic }}/{{ row.diastolic }} 心率{{ row.heartRate }}
            血糖{{ row.bloodGlucose }} 血氧{{ row.spo2 }}%
          </template>
        </el-table-column>
        <el-table-column prop="abnormalNote" label="异常发现" min-width="200" show-overflow-tooltip>
          <template #default="{row}"><span :style="{color:row.abnormalNote?'#f56c6c':'#909399'}">{{ row.abnormalNote || '无' }}</span></template>
        </el-table-column>
        <el-table-column label="超时" width="80">
          <template #default="{row}">
            <el-tag v-if="row.overtimeMin>=20" type="danger" size="small">{{ row.overtimeMin }}分</el-tag>
            <span v-else class="muted">正常</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{row}">
            <el-button link type="primary" @click="open(row)">复核/下医嘱</el-button>
            <el-button link @click="$router.push(`/applications/${row.applicationId}`)">病床</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dlg" title="医生远程复核" width="760px">
      <el-descriptions :column="2" border size="small" v-if="cur">
        <el-descriptions-item label="压疮">{{ cur.pressureUlcer || '—' }}</el-descriptions-item>
        <el-descriptions-item label="换药">{{ cur.dressingChange || '—' }}</el-descriptions-item>
        <el-descriptions-item label="导尿护理" :span="2">{{ cur.catheterCare || '—' }}</el-descriptions-item>
        <el-descriptions-item label="用药提醒" :span="2">{{ cur.medicationReminder || '—' }}</el-descriptions-item>
        <el-descriptions-item label="护理记录" :span="2">{{ cur.nursingNote || '—' }}</el-descriptions-item>
        <el-descriptions-item label="冷链温度" :span="2">
          {{ cur.coldChainTemp ?? '未使用' }}
          <el-tag v-if="cur.coldChainTemp!=null" size="small"
                  :type="(cur.coldChainTemp>=2&&cur.coldChainTemp<=8)?'success':'danger'">
            {{ (cur.coldChainTemp>=2&&cur.coldChainTemp<=8)?'合规':'异常' }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <el-form label-width="92px" style="margin-top:12px">
        <el-form-item label="复核意见">
          <el-input v-model="rf.reviewNote" type="textarea" :rows="2" placeholder="对护理处置、病情变化的复核结论" />
        </el-form-item>
        <el-form-item label="升级处理">
          <el-switch v-model="rf.escalated" active-text="病情危重，建议转诊/上级会诊" />
        </el-form-item>
      </el-form>

      <el-divider content-position="left">下达医嘱（调处方 / 复诊 / 转诊 / 护理方案）</el-divider>
      <el-form :model="of" inline>
        <el-form-item label="类型">
          <el-select v-model="of.orderType" style="width:140px">
            <el-option v-for="(n,k) in ON" :key="k" :label="n" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容" style="width:100%">
          <el-input v-model="of.content" type="textarea" :rows="2" placeholder="如：胰岛素调整为早12U晚10U；三天后复诊" />
        </el-form-item>
        <el-form-item label="目标日期"><el-date-picker v-model="of.targetDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="机构"><el-input v-model="of.targetOrg" placeholder="转诊机构，如县人民医院" /></el-form-item>
        <el-form-item><el-button type="primary" plain @click="addOrder">添加医嘱</el-button></el-form-item>
      </el-form>
      <el-table :data="orders" size="small" border>
        <el-table-column label="类型" width="110"><template #default="{row}">{{ ON[row.orderType] }}</template></el-table-column>
        <el-table-column prop="content" label="内容" min-width="260" />
        <el-table-column prop="targetDate" label="日期" width="110" />
        <el-table-column prop="targetOrg" label="机构" width="130" />
      </el-table>

      <template #footer>
        <el-button @click="dlg=false">关闭</el-button>
        <el-button type="primary" @click="submitReview">提交复核结论</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'
import { ON } from '../constants'

const pending = ref([])
const dlg = ref(false)
const cur = ref(null)
const rf = reactive({ reviewNote: '', escalated: false })
const of = reactive({ orderType: 'PRESCRIPTION_ADJUST', content: '', targetDate: null, targetOrg: '' })
const orders = ref([])

async function load() { pending.value = await api.get('/visits/pending-review') }
async function open(row) {
  cur.value = row
  rf.reviewNote = ''; rf.escalated = false
  orders.value = (await api.get(`/visits/${row.id}`)).orders || []
  dlg.value = true
}
async function addOrder() {
  if (!of.content) return ElMessage.warning('请填写医嘱内容')
  const o = await api.post('/orders', { visitId: cur.value.id, ...of })
  orders.value.push(o)
  ElMessage.success('医嘱已记录')
  of.content = ''; of.targetDate = null; of.targetOrg = ''
}
async function submitReview() {
  if (!rf.reviewNote) return ElMessage.warning('请填写复核意见')
  await api.post('/visits/review', { visitId: cur.value.id, ...rf })
  ElMessage.success('复核完成'); dlg.value = false; load()
}
onMounted(load)
</script>

<template>
  <div>
    <div class="head-line">
      <h2 class="page-title" style="margin:0">收费与医保目录（支持目录变更留版本）</h2>
      <div>
        <el-checkbox v-model="includeInactive" @change="load">显示历史失效版本</el-checkbox>
        <el-button type="primary" class="ml8" @click="openCreate">新增目录项</el-button>
      </div>
    </div>
    <div class="card-soft">
      <el-table :data="catalogs" size="small" v-loading="loading">
        <el-table-column prop="code" label="编码" width="85" />
        <el-table-column prop="name" label="名称" min-width="180" />
        <el-table-column label="分类" width="100"><template #default="{row}">{{ label('CatalogCategory',row.category) }}</template></el-table-column>
        <el-table-column label="单价" width="80" align="right"><template #default="{row}">¥{{ row.unitPrice }}</template></el-table-column>
        <el-table-column label="医保" width="90" align="center">
          <template #default="{row}"><el-tag size="small" :type="row.reimbursable?'success':'info'">{{ row.insuranceCategory }}</el-tag></template>
        </el-table-column>
        <el-table-column label="报销比例" width="90" align="center"><template #default="{row}">{{ row.reimbursable ? Math.round(row.reimbursementRate*100)+'%' : '-' }}</template></el-table-column>
        <el-table-column label="冷链" width="60" align="center">
          <template #default="{row}"><el-icon v-if="row.coldChain" color="#409eff"><Snowflake /></el-icon><span v-else>-</span></template>
        </el-table-column>
        <el-table-column prop="limitRule" label="限制规则" show-overflow-tooltip />
        <el-table-column label="生效区间" width="200">
          <template #default="{row}">{{ row.effectiveFrom }} ~ {{ row.effectiveTo || '至今' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="75">
          <template #default="{row}"><el-tag size="small" :type="row.active?'success':'info'">{{ row.active?'生效':'失效' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{row}">
            <el-button v-if="row.active" link type="warning" size="small" @click="openRevise(row)">目录变更</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="createVisible" title="新增收费/医保目录项" width="520px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="编码"><el-input v-model="form.code" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-row><el-col :span="12"><el-form-item label="分类"><el-select v-model="form.category" style="width:100%">
          <el-option v-for="(v,k) in ENUMS.CatalogCategory" :key="k" :label="v" :value="k" />
        </el-select></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="单位"><el-input v-model="form.unit" /></el-form-item></el-col></el-row>
        <el-row><el-col :span="12"><el-form-item label="单价"><el-input-number v-model="form.unitPrice" :step="0.5" :precision="2" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="每日限次"><el-input-number v-model="form.dailyLimit" :min="1" :max="20" /></el-form-item></el-col></el-row>
        <el-form-item label="医保可报"><el-switch v-model="form.reimbursable" /></el-form-item>
        <template v-if="form.reimbursable">
          <el-row><el-col :span="12"><el-form-item label="目录类别"><el-select v-model="form.insuranceCategory" style="width:100%">
            <el-option label="甲类" value="甲类" /><el-option label="乙类" value="乙类" /><el-option label="丙类" value="丙类" />
          </el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="报销比例"><el-slider v-model="ratePct" :min="0" :max="100" show-input :show-input-controls="false" /></el-form-item></el-col></el-row>
        </template>
        <el-form-item label="冷链药品"><el-switch v-model="form.coldChain" /></el-form-item>
        <el-form-item label="限制说明"><el-input v-model="form.limitRule" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="createVisible=false">取消</el-button>
        <el-button type="primary" @click="doCreate">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="reviseVisible" title="医保目录变更（旧版本自动失效）" width="480px">
      <el-alert type="warning" :closable="false" show-icon class="mb12"
                title="变更后新版本今日生效；已出账单按出账时版本快照结算，不会被追溯改变。" />
      <el-form label-width="100px">
        <el-form-item label="项目">{{ current?.name }}（{{ current?.code }}）</el-form-item>
        <el-form-item label="新单价"><el-input-number v-model="reviseForm.unitPrice" :step="0.5" :precision="2" /></el-form-item>
        <el-form-item label="医保可报"><el-switch v-model="reviseForm.reimbursable" /></el-form-item>
        <el-form-item v-if="reviseForm.reimbursable" label="新报销比例">
          <el-slider v-model="reviseRatePct" :min="0" :max="100" show-input :show-input-controls="false" />
        </el-form-item>
        <el-form-item label="变更说明"><el-input v-model="reviseForm.versionNote" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="reviseVisible=false">取消</el-button>
        <el-button type="warning" @click="doRevise">发新版本</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'
import { ENUMS, label } from '../labels'

const catalogs = ref([])
const loading = ref(true)
const includeInactive = ref(false)

async function load() {
  loading.value = true
  catalogs.value = (await api.get('/catalogs?includeInactive=' + includeInactive.value)).data
  loading.value = false
}
onMounted(load)

const createVisible = ref(false)
const form = reactive({})
const ratePct = ref(70)
function openCreate() {
  Object.assign(form, { code: '', name: '', category: 'NURSING', unit: '次', unitPrice: 10,
    dailyLimit: 1, reimbursable: true, insuranceCategory: '乙类', coldChain: false, limitRule: '' })
  ratePct.value = 70
  createVisible.value = true
}
async function doCreate() {
  form.reimbursementRate = form.reimbursable ? (ratePct.value / 100) : 0
  await api.post('/catalogs', form)
  ElMessage.success('目录项已新增')
  createVisible.value = false
  load()
}

const reviseVisible = ref(false)
const current = ref(null)
const reviseForm = reactive({})
const reviseRatePct = ref(70)
function openRevise(row) {
  current.value = row
  Object.assign(reviseForm, { unitPrice: Number(row.unitPrice), reimbursable: row.reimbursable, versionNote: '' })
  reviseRatePct.value = Math.round((Number(row.reimbursementRate) || 0) * 100)
  reviseVisible.value = true
}
async function doRevise() {
  await api.post(`/catalogs/${current.value.id}/revise`, {
    ...reviseForm,
    reimbursementRate: reviseForm.reimbursable ? (reviseRatePct.value / 100) : 0
  })
  ElMessage.success('新版本已生效，旧版本已失效')
  reviseVisible.value = false
  load()
}
</script>

<style scoped>
.head-line { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
.ml8 { margin-left: 8px; } .mb12 { margin-bottom: 12px; }
</style>

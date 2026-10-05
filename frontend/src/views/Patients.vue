<template>
  <div>
    <div class="head-line">
      <h2 class="page-title" style="margin:0">患者档案</h2>
      <el-button v-if="canEdit" type="primary" :icon="Plus" @click="openDialog()">新增患者/老人</el-button>
    </div>
    <div class="card-soft">
      <el-table :data="patients" v-loading="loading" size="small">
        <el-table-column prop="name" label="姓名" width="90" />
        <el-table-column label="性别/年龄" width="90"><template #default="{row}">{{ row.gender }} · {{ row.age }}</template></el-table-column>
        <el-table-column label="疾病类型" width="110">
          <template #default="{row}">{{ label('DiseaseCategory', row.diseaseCategory) }}</template>
        </el-table-column>
        <el-table-column label="行动能力" width="90"><template #default="{row}">{{ row.mobilityLevel }}</template></el-table-column>
        <el-table-column label="医保" width="130">
          <template #default="{row}">{{ label('InsuranceType', row.insuranceType) }}</template>
        </el-table-column>
        <el-table-column prop="address" label="住址" show-overflow-tooltip />
        <el-table-column prop="primaryCaregiver" label="主要照护人" width="150" />
        <el-table-column prop="primaryContactPhone" label="联系电话" width="125" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{row}">
            <el-button v-if="canEdit" link type="primary" size="small" @click="openDialog(row)">编辑</el-button>
            <el-button link type="primary" size="small" @click="goTrace(row)">服务链</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialog" :title="form.id ? '编辑患者' : '新增患者'" width="680px">
      <el-form :model="form" label-width="110px">
        <el-row :gutter="10">
          <el-col :span="8"><el-form-item label="姓名"><el-input v-model="form.name" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="性别"><el-select v-model="form.gender"><el-option label="女" value="女"/><el-option label="男" value="男"/></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="年龄"><el-input-number v-model="form.age" :min="1" :max="120" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="疾病类型"><el-select v-model="form.diseaseCategory" style="width:100%">
            <el-option v-for="(v,k) in ENUMS.DiseaseCategory" :key="k" :label="v" :value="k" />
          </el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="行动能力"><el-select v-model="form.mobilityLevel" style="width:100%">
            <el-option label="卧床" value="卧床"/><el-option label="轮椅" value="轮椅"/>
            <el-option label="可扶行" value="可扶行"/><el-option label="自理" value="自理"/>
          </el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="医保类型"><el-select v-model="form.insuranceType" style="width:100%">
            <el-option v-for="(v,k) in ENUMS.InsuranceType" :key="k" :label="v" :value="k" />
          </el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="医保号"><el-input v-model="form.insuranceNo" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="家庭住址"><el-input v-model="form.address" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="纬度"><el-input-number v-model="form.latitude" :step="0.0001" :precision="4" :controls="false" style="width:100%"/></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="经度"><el-input-number v-model="form.longitude" :step="0.0001" :precision="4" :controls="false" style="width:100%"/></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="主要照护人"><el-input v-model="form.primaryCaregiver" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="照护人电话"><el-input v-model="form.primaryContactPhone" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="紧急联系人"><el-input v-model="form.emergencyContact" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="紧急电话"><el-input v-model="form.emergencyPhone" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialog=false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '../api'
import { useAuthStore } from '../store/auth'
import { ENUMS, label } from '../labels'

const router = useRouter()
const auth = useAuthStore()
const patients = ref([])
const loading = ref(true)
const dialog = ref(false)
const form = reactive({})
const canEdit = computed(() => ['FAMILY', 'ADMIN'].includes(auth.role))

onMounted(async () => {
  patients.value = (await api.get('/patients')).data
  loading.value = false
})

function openDialog(row) {
  Object.keys(form).forEach(k => delete form[k])
  if (row) Object.assign(form, row)
  if (!form.gender) form.gender = '女'
  dialog.value = true
}

async function save() {
  if (!form.name) return ElMessage.warning('请填写姓名')
  if (form.id) await api.put('/patients/' + form.id, form)
  else await api.post('/patients', form)
  ElMessage.success('已保存')
  dialog.value = false
  patients.value = (await api.get('/patients')).data
}

function goTrace(row) {
  // 找到该患者最近的申请单后跳转（服务链按申请维度）
  router.push({ path: '/trace', query: { patientId: row.id, patientName: row.name } })
}
</script>

<style scoped>
.head-line { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
</style>

<template>
  <div class="page">
    <div class="page-title">患者档案</div>
    <el-card>
      <el-button type="primary" @click="openCreate" style="margin-bottom:12px">新建患者档案</el-button>
      <el-table :data="rows" stripe>
        <el-table-column prop="name" label="姓名" width="90" />
        <el-table-column label="性别/年龄" width="90"><template #default="{row}">{{ row.gender }} {{ row.age }}岁</template></el-table-column>
        <el-table-column prop="mobility" label="行动能力" width="90" />
        <el-table-column prop="insuranceType" label="医保类型" width="130" />
        <el-table-column prop="diagnosis" label="主要诊断" min-width="200" show-overflow-tooltip />
        <el-table-column prop="address" label="地址" min-width="220" show-overflow-tooltip />
        <el-table-column prop="phone" label="联系电话" width="130" />
        <el-table-column label="操作" width="90">
          <template #default="{row}"><el-button link type="primary" @click="openEdit(row)">编辑</el-button></template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dlg" :title="form.id?'编辑患者':'新建患者'" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="姓名"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="性别"><el-radio-group v-model="form.gender"><el-radio value="女">女</el-radio><el-radio value="男">男</el-radio></el-radio-group></el-form-item>
        <el-form-item label="年龄"><el-input-number v-model="form.age" :min="1" :max="120" /></el-form-item>
        <el-form-item label="身份证号"><el-input v-model="form.idCard" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="地址"><el-input v-model="form.address" placeholder="村组及距卫生院距离" /></el-form-item>
        <el-form-item label="经纬度"><el-input v-model.number="form.lng" placeholder="经度" style="width:130px" /> <el-input v-model.number="form.lat" placeholder="纬度" style="width:130px" /></el-form-item>
        <el-form-item label="医保类型">
          <el-select v-model="form.insuranceType" style="width:100%">
            <el-option v-for="t in ['城镇职工医保','城乡居民医保','异地医保','无医保']" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="医保号"><el-input v-model="form.insuranceNo" /></el-form-item>
        <el-form-item label="诊断"><el-input v-model="form.diagnosis" /></el-form-item>
        <el-form-item label="行动能力">
          <el-select v-model="form.mobility" style="width:100%">
            <el-option v-for="t in ['可搀扶','轮椅','卧床']" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg=false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'

const rows = ref([])
const dlg = ref(false)
const empty = () => ({ id: null, name: '', gender: '女', age: 70, idCard: '', phone: '',
  address: '', lng: null, lat: null, insuranceType: '城乡居民医保', insuranceNo: '',
  diagnosis: '', mobility: '卧床' })
const form = reactive(empty())

async function load() { rows.value = await api.get('/patients') }
function openCreate() { Object.assign(form, empty()); dlg.value = true }
function openEdit(r) { Object.assign(form, r); dlg.value = true }
async function save() {
  if (!form.name) return ElMessage.warning('请填写姓名')
  if (form.id) await api.put(`/patients/${form.id}`, form)
  else await api.post('/patients', form)
  ElMessage.success('已保存'); dlg.value = false; load()
}
onMounted(load)
</script>

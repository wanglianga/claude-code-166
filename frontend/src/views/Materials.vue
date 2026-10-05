<template>
  <div>
    <h2 class="page-title">耗材与冷链药品库存</h2>
    <el-alert v-if="shortage.length" type="error" :closable="false" show-icon class="mb12"
              :title="`缺货/低库存预警 ${shortage.length} 项：${shortage.map(s=>s.name+'('+s.stockQty+s.unit+')').join('、')}`" />
    <el-row :gutter="14">
      <el-col :span="15">
        <div class="card-soft">
          <div class="head-line">
            <span class="section-title">库存清单（护士上门领用即扣减并记账）</span>
            <el-button v-if="canEdit" type="primary" size="small" @click="openEdit()">新增耗材</el-button>
          </div>
          <el-table :data="materials" size="small">
            <el-table-column prop="code" label="编码" width="80" />
            <el-table-column label="名称/规格">
              <template #default="{row}">
                {{ row.name }} <span class="muted">{{ row.spec }}</span>
                <el-tag v-if="row.coldChain" size="small" type="primary" effect="plain">冷链2-8℃</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="分类" width="100"><template #default="{row}">{{ label('CatalogCategory',row.category) }}</template></el-table-column>
            <el-table-column label="库存" width="90" align="center">
              <template #default="{row}">
                <b :class="row.stockQty<=row.safetyStock?'danger-text':''">{{ row.stockQty }}{{ row.unit }}</b>
              </template>
            </el-table-column>
            <el-table-column prop="safetyStock" label="安全库存" width="90" align="center" />
            <el-table-column label="效期" width="110"><template #default="{row}">{{ row.nearestExpiry || '-' }}</template></el-table-column>
            <el-table-column label="操作" width="130">
              <template #default="{row}">
                <el-button v-if="canEdit" link type="primary" size="small" @click="openStockIn(row)">入库</el-button>
                <el-button v-if="canEdit" link type="info" size="small" @click="openEdit(row)">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>
      <el-col :span="9">
        <div class="card-soft">
          <div class="section-title">出入库流水（含冷链交接温度）</div>
          <el-table :data="txs" size="small" max-height="560">
            <el-table-column label="时间" width="145"><template #default="{row}">{{ row.occurredAt?.replace('T',' ').slice(0,16) }}</template></el-table-column>
            <el-table-column label="耗材"><template #default="{row}">{{ row.material.name }}</template></el-table-column>
            <el-table-column label="类型" width="70">
              <template #default="{row}"><el-tag size="small" :type="row.type==='OUT'?'warning':'success'">{{ label('MaterialTxType',row.type) }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="quantity" label="数量" width="55" align="center" />
            <el-table-column label="温度" width="65">
              <template #default="{row}">
                <span v-if="row.coldChainTemp!=null" :style="{color:(row.coldChainTemp<2||row.coldChainTemp>8)?'#f56c6c':'#1f6f5c'}">{{ row.coldChainTemp }}℃</span>
                <span v-else>-</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>
    </el-row>

    <el-dialog v-model="stockInVisible" :title="'入库：' + (current?.name||'')" width="420px">
      <el-form :model="inForm" label-width="100px">
        <el-form-item label="数量"><el-input-number v-model="inForm.quantity" :min="1" /></el-form-item>
        <el-form-item label="批号"><el-input v-model="inForm.batchNo" /></el-form-item>
        <el-form-item label="效期"><el-date-picker v-model="inForm.expiryDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
        <el-form-item v-if="current?.coldChain" label="入库温度℃"><el-input-number v-model="inForm.coldChainTemp" :step="0.1" :precision="1" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="inForm.note" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="stockInVisible=false">取消</el-button>
        <el-button type="primary" @click="doStockIn">确认入库</el-button></template>
    </el-dialog>

    <el-dialog v-model="editVisible" :title="form.id?'编辑耗材':'新增耗材'" width="520px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="编码"><el-input v-model="form.code" :disabled="!!form.id" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="规格"><el-input v-model="form.spec" /></el-form-item>
        <el-row><el-col :span="12"><el-form-item label="单位"><el-input v-model="form.unit" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="分类"><el-select v-model="form.category" style="width:100%">
          <el-option v-for="(v,k) in ENUMS.CatalogCategory" :key="k" :label="v" :value="k" />
        </el-select></el-form-item></el-col></el-row>
        <el-row><el-col :span="12"><el-form-item label="库存"><el-input-number v-model="form.stockQty" :min="0" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="安全库存"><el-input-number v-model="form.safetyStock" :min="0" /></el-form-item></el-col></el-row>
        <el-form-item label="采购单价"><el-input-number v-model="form.unitCost" :step="0.1" :precision="2" /></el-form-item>
        <el-form-item label="冷链药品"><el-switch v-model="form.coldChain" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="editVisible=false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'
import { useAuthStore } from '../store/auth'
import { ENUMS, label } from '../labels'

const auth = useAuthStore()
const materials = ref([])
const txs = ref([])
const canEdit = computed(() => ['ADMIN', 'FINANCE'].includes(auth.role))
const shortage = computed(() => materials.value.filter(m => m.stockQty <= m.safetyStock))

async function load() {
  materials.value = (await api.get('/materials')).data
  txs.value = (await api.get('/materials/transactions')).data
}
onMounted(load)

const stockInVisible = ref(false)
const current = ref(null)
const inForm = reactive({ quantity: 1, batchNo: '', expiryDate: null, coldChainTemp: 5, note: '' })
function openStockIn(row) {
  current.value = row
  Object.assign(inForm, { quantity: 1, batchNo: '', expiryDate: row.nearestExpiry, coldChainTemp: 5, note: '' })
  stockInVisible.value = true
}
async function doStockIn() {
  await api.post(`/materials/${current.value.id}/stock-in`, inForm)
  ElMessage.success('入库成功')
  stockInVisible.value = false
  load()
}

const editVisible = ref(false)
const form = reactive({})
function openEdit(row) {
  Object.keys(form).forEach(k => delete form[k])
  if (row) Object.assign(form, row)
  else Object.assign(form, { code: '', name: '', spec: '', unit: '个', category: 'CONSUMABLE', stockQty: 0, safetyStock: 5, unitCost: 0, coldChain: false })
  editVisible.value = true
}
async function save() {
  await api.post('/materials', form)
  ElMessage.success('已保存')
  editVisible.value = false
  load()
}
</script>

<style scoped>
.mb12 { margin-bottom: 12px; }
.head-line { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.section-title { font-weight: 600; }
.danger-text { color: #f56c6c; }
</style>

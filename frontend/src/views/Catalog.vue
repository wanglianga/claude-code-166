<template>
  <div class="page">
    <div class="page-title">医保收费目录与耗材库存</div>

    <el-card class="section-card">
      <div class="toolbar">
        <el-radio-group v-model="catFilter" size="small" @change="load">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button v-for="(n,k) in CN" :key="k" :value="k">{{ n }}</el-radio-button>
        </el-radio-group>
        <div>
          <el-button type="warning" plain @click="lowDlg=true">低库存/缺货预警（{{ low.length }}）</el-button>
          <el-button v-if="canEdit" type="primary" @click="openCreate">新增目录项</el-button>
        </div>
      </div>
      <el-table :data="rows" stripe>
        <el-table-column prop="code" label="编码" width="90" />
        <el-table-column prop="name" label="名称" min-width="180" />
        <el-table-column prop="spec" label="规格" width="90" />
        <el-table-column label="类别" width="100">
          <template #default="{row}">
            <el-tag size="small" :type="CT[row.category]">{{ CN[row.category] }}</el-tag>
            <el-tag v-if="row.coldChain" size="small" type="danger" effect="plain">❄冷链</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="单价" width="90"><template #default="{row}">{{ money(row.price) }}/{{ row.unit }}</template></el-table-column>
        <el-table-column label="医保" width="130">
          <template #default="{row}">
            <el-tag size="small" :type="row.insuranceCovered?'success':'info'">
              {{ row.insuranceCovered ? `报销${row.reimburseRatio}%` : '全自费' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="频次限制" width="90"><template #default="{row}">{{ row.frequencyLimit || '不限' }}</template></el-table-column>
        <el-table-column label="库存" width="150">
          <template #default="{row}">
            <el-tag size="small" :type="stockType(row)">{{ row.stockQty }}{{ row.unit }}</el-tag>
            <span class="muted"> / 警戒{{ row.safetyStock }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170">
          <template #default="{row}">
            <el-button v-if="canEdit" link type="primary" @click="openEdit(row)">调整目录</el-button>
            <el-button v-if="canStock && row.category!=='SERVICE'" link type="warning" @click="openStock(row)">入/盘库</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑目录 -->
    <el-dialog v-model="editDlg" :title="form.id?'调整目录项（历史账单保留快照）':'新增目录项'" width="600px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="编码"><el-input v-model="form.code" :disabled="!!form.id" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="规格"><el-input v-model="form.spec" /></el-form-item>
        <el-form-item label="类别">
          <el-select v-model="form.category" style="width:100%">
            <el-option v-for="(n,k) in CN" :key="k" :label="n" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="单价(元)"><el-input-number v-model="form.price" :min="0" :precision="2" :step="0.5" style="width:160px" /></el-form-item>
        <el-form-item label="单位"><el-input v-model="form.unit" placeholder="次/个/盒/支" style="width:120px" /></el-form-item>
        <el-form-item label="医保目录内"><el-switch v-model="form.insuranceCovered" active-text="可报销" inactive-text="自费" /></el-form-item>
        <el-form-item label="报销比例(%)" v-if="form.insuranceCovered">
          <el-input-number v-model="form.reimburseRatio" :min="0" :max="100" :step="5" style="width:160px" />
        </el-form-item>
        <el-form-item label="冷链药品"><el-switch v-model="form.coldChain" /></el-form-item>
        <el-form-item label="频次限制"><el-input v-model="form.frequencyLimit" placeholder="如 1/日、2/周、1/次；空为不限" style="width:200px" /></el-form-item>
        <el-form-item label="标准耗时(分)"><el-input-number v-model="form.durationMin" :min="5" :max="180" style="width:140px" /></el-form-item>
        <el-form-item label="初始库存" v-if="!form.id && form.category!=='SERVICE'">
          <el-input-number v-model="form.stockQty" :min="0" style="width:140px" />
        </el-form-item>
        <el-form-item label="安全库存"><el-input-number v-model="form.safetyStock" :min="0" style="width:140px" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDlg=false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 库存调整 -->
    <el-dialog v-model="stockDlg" title="耗材入库 / 盘库" width="440px">
      <div style="margin-bottom:10px">{{ cur?.name }}（当前 {{ cur?.stockQty }}{{ cur?.unit }}）</div>
      <el-form label-width="110px">
        <el-form-item label="数量(+入/-出)"><el-input-number v-model="sf.changeQty" :precision="0" :step="5" style="width:180px" /></el-form-item>
        <el-form-item label="原因/单号"><el-input v-model="sf.reason" placeholder="采购入库单号 / 盘亏原因" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stockDlg=false">取消</el-button>
        <el-button type="primary" @click="saveStock">确定</el-button>
      </template>
    </el-dialog>

    <!-- 低库存 -->
    <el-dialog v-model="lowDlg" title="低库存 / 缺货预警" width="560px">
      <el-table :data="low" size="small">
        <el-table-column prop="name" label="耗材/药品" min-width="180" />
        <el-table-column label="库存" width="110">
          <template #default="{row}"><el-tag size="small" :type="row.outOfStock?'danger':'warning'">{{ row.stockQty }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="safetyStock" label="安全库存" width="90" />
        <el-table-column label="冷链" width="70"><template #default="{row}">{{ row.coldChain?'是':'否' }}</template></el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'
import { CATEGORY_NAMES as CN, CATEGORY_TYPE as CT, money } from '../constants'

const role = JSON.parse(localStorage.getItem('user') || '{}').role
const canEdit = ['ADMIN', 'INSURANCE'].includes(role)
const canStock = ['ADMIN', 'NURSE'].includes(role)

const rows = ref([])
const low = ref([])
const catFilter = ref('')
const editDlg = ref(false)
const stockDlg = ref(false)
const lowDlg = ref(false)
const cur = ref(null)
const emptyForm = () => ({ id: null, code: '', name: '', spec: '', category: 'SERVICE', price: 10,
  unit: '次', insuranceCovered: true, reimburseRatio: 80, coldChain: false, frequencyLimit: '',
  durationMin: 30, stockQty: 0, safetyStock: 0 })
const form = reactive(emptyForm())
const sf = reactive({ changeQty: 10, reason: '' })

function stockType(r) {
  if (r.category === 'SERVICE') return 'info'
  if (r.stockQty === 0) return 'danger'
  if (r.stockQty <= r.safetyStock) return 'warning'
  return 'success'
}
async function load() {
  rows.value = await api.get('/catalog', { params: { category: catFilter.value } })
  low.value = await api.get('/catalog/low-stock')
}
function openCreate() { Object.assign(form, emptyForm()); editDlg.value = true }
function openEdit(r) { Object.assign(form, r); editDlg.value = true }
async function save() {
  if (!form.code || !form.name) return ElMessage.warning('编码与名称必填')
  if (form.id) await api.put(`/catalog/${form.id}`, form)
  else await api.post('/catalog', form)
  ElMessage.success('目录已保存（新服务按新标准，历史账单保留快照）')
  editDlg.value = false; load()
}
function openStock(r) { cur.value = r; sf.changeQty = 10; sf.reason = ''; stockDlg.value = true }
async function saveStock() {
  await api.post(`/catalog/${cur.value.id}/stock`, sf)
  ElMessage.success('库存已调整'); stockDlg.value = false; load()
}
onMounted(load)
</script>

<style scoped>.toolbar{display:flex;justify-content:space-between;margin-bottom:12px}</style>

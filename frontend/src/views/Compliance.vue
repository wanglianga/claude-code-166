<template>
  <div class="page">
    <div class="page-title">医保合规审核</div>
    <el-card class="section-card">
      <el-alert type="warning" :closable="false"
                title="系统按医保规则自动筛查：重复收费、超频次收费、目录外自费、医保目录/价格变化。命中条目保留服务当日价格快照，供医保经办复核。" />
      <div class="stat-grid" style="margin:12px 0">
        <div class="stat-card"><div class="v">{{ rows.length }}</div><div class="l">命中合规提示明细</div></div>
        <div class="stat-card"><div class="v">{{ dupCount }}</div><div class="l">涉及账单</div></div>
        <div class="stat-card"><div class="v">{{ money(flagAmount) }}</div><div class="l">命中明细金额合计</div></div>
        <div class="stat-card"><div class="v">{{ money(flagIns) }}</div><div class="l">其中医保支付金额</div></div>
      </div>
      <el-table :data="rows" stripe>
        <el-table-column prop="billNo" label="账单" width="170" />
        <el-table-column prop="applicationId" label="病床#" width="80" />
        <el-table-column prop="itemName" label="项目" min-width="150" />
        <el-table-column label="金额/医保" width="150">
          <template #default="{row}">{{ money(row.amount) }}（报{{ money(row.insuranceAmount) }}）</template>
        </el-table-column>
        <el-table-column prop="serviceTime" label="服务时间" width="170" />
        <el-table-column label="家属确认" width="90">
          <template #default="{row}">
            <el-tag size="small" :type="row.familyAck?'success':'info'">{{ row.familyAck?'已确认':'未确认' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="合规问题" min-width="240">
          <template #default="{row}"><span class="flag-tag">{{ row.flag }}</span></template>
        </el-table-column>
        <el-table-column label="账单状态" width="90">
          <template #default="{row}"><el-tag size="small" :type="BT[row.billStatus]">{{ BS[row.billStatus] }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="{row}"><el-button link type="primary" @click="$router.push(`/applications/${row.applicationId}`)">还原经过</el-button></template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '../api'
import { BS, BT, money } from '../constants'

const rows = ref([])
const dupCount = computed(() => new Set(rows.value.map(r => r.billId)).size)
const flagAmount = computed(() => rows.value.reduce((s, r) => s + Number(r.amount || 0), 0))
const flagIns = computed(() => rows.value.reduce((s, r) => s + Number(r.insuranceAmount || 0), 0))
onMounted(async () => { rows.value = await api.get('/bills/compliance/list') })
</script>

<template>
  <div class="page">
    <div class="page-title">卫生院管理看板</div>

    <el-row :gutter="14" v-if="d">
      <el-col :span="6">
        <el-card class="stat-card"><div class="v">{{ d.beds.active }}</div><div class="l">在建家庭病床</div></el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card"><div class="v">{{ d.beds.paused }}</div><div class="l">暂停（住院/无人等）</div></el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card"><div class="v" style="color:#e6a23c">{{ d.beds.submitted }}</div><div class="l">待评估申请</div></el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card"><div class="v" style="color:#f56c6c">{{ d.riskEvents.open }}</div><div class="l">未关闭风险事件</div></el-card>
      </el-col>
    </el-row>

    <el-row :gutter="14" style="margin-top:14px" v-if="d">
      <el-col :span="12">
        <el-card header="今日护士巡诊负荷（含路程耗时）">
          <div v-for="n in d.nurseLoadToday" :key="n.nurseName" class="load-row">
            <div class="load-label">
              <b>{{ n.nurseName }}</b>
              <el-tag size="small" :type="n.overload?'danger':'success'">{{ n.stops }}单 / {{ n.plannedMin }}分</el-tag>
              <span class="muted">往返 {{ Math.round(n.roundTripKm*10)/10 }}km</span>
            </div>
            <el-progress :percentage="n.loadRatio" :status="n.overload?'exception':''" style="flex:1" />
          </div>
          <el-empty v-if="!d.nurseLoadToday.length" description="今日无排班" :image-size="50" />
        </el-card>

        <el-card header="上门半径" style="margin-top:14px">
          <el-descriptions :column="3" size="small" border>
            <el-descriptions-item label="平均距离">{{ d.radius.averageKm }} km</el-descriptions-item>
            <el-descriptions-item label="最远">{{ d.radius.maxKm }} km</el-descriptions-item>
            <el-descriptions-item label="超8km半径床位">
              <el-tag size="small" :type="d.radius.overRadiusBeds?'warning':'success'">{{ d.radius.overRadiusBeds }} 张</el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card header="巡诊执行" style="margin-top:14px">
          <el-descriptions :column="2" size="small" border>
            <el-descriptions-item label="已完成">{{ d.visits.completed }}</el-descriptions-item>
            <el-descriptions-item label="未完成">{{ d.visits.aborted }}</el-descriptions-item>
            <el-descriptions-item label="上门超时">
              <el-tag size="small" :type="d.visits.overtime?'danger':'success'">{{ d.visits.overtime }} 次</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="待医生复核">{{ d.visits.pendingReview }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card header="耗材 / 冷链药品消耗（累计）">
          <el-table :data="d.consumption" size="small" max-height="260">
            <el-table-column prop="name" label="项目" min-width="150" />
            <el-table-column label="类别" width="90">
              <template #default="{row}">
                <el-tag size="small" :type="CT2[row.category]">{{ CN2[row.category] }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="已用量" width="90"><template #default="{row}">{{ row.usedQty }}{{ row.unit }}</template></el-table-column>
            <el-table-column label="结存" width="90">
              <template #default="{row}"><el-tag size="small" :type="row.lowStock?'danger':'success'">{{ row.stockQty }}</el-tag></template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!d.consumption.length" description="暂无消耗" :image-size="50" />
          <div class="muted" style="margin-top:6px">冷链药品费用合计：<b style="color:#f56c6c">{{ money(d.coldChainAmount) }}</b></div>
        </el-card>

        <el-card header="风险事件分布" style="margin-top:14px">
          <div class="risk-grid">
            <div v-for="(c,k) in d.riskEvents.byType" :key="k" class="risk-box" :class="{active:c>0}">
              <div class="rv">{{ c }}</div>
              <div class="rl">{{ EN2[k] }}</div>
            </div>
          </div>
        </el-card>

        <el-card header="结算概况" style="margin-top:14px">
          <el-descriptions :column="2" size="small" border>
            <el-descriptions-item label="账单数">{{ d.billing.count }}</el-descriptions-item>
            <el-descriptions-item label="已结算">{{ d.billing.settled }}</el-descriptions-item>
            <el-descriptions-item label="费用合计">{{ money(d.billing.totalAmount) }}</el-descriptions-item>
            <el-descriptions-item label="医保支付">{{ money(d.billing.insuranceAmount) }}</el-descriptions-item>
            <el-descriptions-item label="个人自付">{{ money(d.billing.selfPayAmount) }}</el-descriptions-item>
            <el-descriptions-item label="被质疑账单">{{ d.billing.questioned }}</el-descriptions-item>
            <el-descriptions-item label="合规命中明细" :span="2">
              <el-tag size="small" type="warning">{{ d.billing.complianceFlaggedItems }} 条待医保关注</el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../api'
import { CATEGORY_NAMES as CN2, CATEGORY_TYPE as CT2, RISK_EVENT_NAMES as EN2, money } from '../constants'

const d = ref(null)
onMounted(async () => { d.value = await api.get('/stats/dashboard') })
</script>

<style scoped>
.load-row { display:flex; align-items:center; gap:12px; margin-bottom:10px; }
.load-label { width:210px; display:flex; align-items:center; gap:6px; }
.risk-grid { display:grid; grid-template-columns:repeat(3,1fr); gap:10px; }
.risk-box { text-align:center; padding:10px; border-radius:8px; background:#f4f6f8; }
.risk-box.active { background:#fef0f0; }
.risk-box .rv { font-size:22px; font-weight:700; color:#1e4e7e; }
.risk-box.active .rv { color:#f56c6c; }
.risk-box .rl { font-size:12px; color:#909399; }
</style>

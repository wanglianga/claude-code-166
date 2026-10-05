<template>
  <el-container style="height:100%">
    <el-aside width="214px" class="aside">
      <div class="logo">🏥 家庭病床平台</div>
      <el-menu :default-active="route.path" router background-color="#1e4e7e"
               text-color="#cfe0f2" active-text-color="#ffd980">
        <el-menu-item v-for="m in menus" :key="m.path" :index="m.path">
          <el-icon><component :is="m.icon" /></el-icon>
          <span>{{ m.label }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="crumb">{{ currentLabel }}</div>
        <div class="userbox">
          <el-tag :type="roleTag" effect="dark" size="small">{{ roleName }}</el-tag>
          <span class="uname">{{ user.name }}</span>
          <el-button link type="primary" @click="logout">退出</el-button>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ROLE_NAMES } from '../constants'
import api from '../api'

const route = useRoute()
const router = useRouter()
const user = JSON.parse(localStorage.getItem('user') || '{}')
const roleName = ROLE_NAMES[user.role] || user.role

const ALL_MENUS = [
  { path: '/dashboard', label: '管理看板', icon: 'DataAnalysis', roles: ['ADMIN', 'DOCTOR'] },
  { path: '/applications', label: '病床申请与进展', icon: 'Document', roles: ['FAMILY', 'DOCTOR', 'ADMIN', 'NURSE'] },
  { path: '/board', label: '排班与上门路线', icon: 'Van', roles: ['NURSE', 'DOCTOR', 'ADMIN'] },
  { path: '/review', label: '远程复核与医嘱', icon: 'Monitor', roles: ['DOCTOR'] },
  { path: '/bills', label: '账单与结算', icon: 'Wallet', roles: ['FAMILY', 'ADMIN', 'INSURANCE', 'DOCTOR'] },
  { path: '/compliance', label: '医保合规审核', icon: 'Stamp', roles: ['INSURANCE', 'ADMIN'] },
  { path: '/questions', label: '收费质疑处理', icon: 'ChatDotRound', roles: ['ADMIN', 'INSURANCE', 'DOCTOR'] },
  { path: '/risks', label: '风险事件', icon: 'Warning', roles: ['NURSE', 'DOCTOR', 'ADMIN'] },
  { path: '/catalog', label: '目录与耗材库存', icon: 'Box', roles: ['ADMIN', 'INSURANCE', 'NURSE'] },
  { path: '/patients', label: '患者档案', icon: 'UserFilled', roles: ['ADMIN', 'DOCTOR'] }
]
const menus = computed(() => ALL_MENUS.filter(m => m.roles.includes(user.role)))
const currentLabel = computed(() => ALL_MENUS.find(m => route.path.startsWith(m.path))?.label || '')
const roleTag = computed(() => ({
  FAMILY: 'success', NURSE: 'warning', DOCTOR: 'primary', ADMIN: 'danger', INSURANCE: 'info'
}[user.role]))

async function logout() {
  try { await api.post('/auth/logout') } catch {}
  localStorage.clear()
  router.push('/login')
}
</script>

<style scoped>
.aside { background: #1e4e7e; }
.logo { color: #fff; font-weight: 700; font-size: 16px; padding: 18px 18px 12px; line-height: 1.3; }
:deep(.el-menu) { border-right: none; }
.header { background: #fff; display:flex; align-items:center; justify-content:space-between;
  box-shadow: 0 1px 4px rgba(0,0,0,.06); }
.crumb { font-weight: 600; color: #1e4e7e; }
.userbox { display:flex; align-items:center; gap:10px; }
.uname { font-size: 14px; }
.main { background: var(--bg); padding: 0; }
</style>

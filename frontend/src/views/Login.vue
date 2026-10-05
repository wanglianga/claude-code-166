<template>
  <div class="login-wrap">
    <div class="login-card">
      <h2>乡镇卫生院家庭病床平台</h2>
      <div class="login-sub">申请 · 巡诊 · 耗材 · 医保结算 一体化</div>
      <el-form :model="form" @submit.prevent>
        <el-form-item>
          <el-input v-model="form.username" size="large" placeholder="账号" prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" size="large" type="password" placeholder="密码"
                    prefix-icon="Lock" show-password @keyup.enter="submit" />
        </el-form-item>
        <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="submit">登 录</el-button>
      </el-form>
      <el-divider>测试账号（密码均为 123456）</el-divider>
      <div class="accts">
        <el-tag v-for="a in accts" :key="a.u" class="acct" :type="a.type"
                @click="quick(a.u)">{{ a.label }}</el-tag>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'
import { ElMessage } from 'element-plus'
import { homeByRole } from '../router'

const router = useRouter()
const form = reactive({ username: 'family', password: '123456' })
const loading = ref(false)

const accts = [
  { u: 'family', label: '家属 王梅', type: 'success' },
  { u: 'nurse1', label: '护士 李护英', type: 'warning' },
  { u: 'doctor', label: '家庭医生 陈济民', type: 'primary' },
  { u: 'admin', label: '管理 周院长', type: 'danger' },
  { u: 'yibao', label: '医保经办 张瑾', type: 'info' }
]
function quick(u) { form.username = u; form.password = '123456'; submit() }

async function submit() {
  if (!form.username || !form.password) return ElMessage.warning('请输入账号密码')
  loading.value = true
  try {
    const data = await api.post('/auth/login', form)
    localStorage.setItem('token', data.token)
    localStorage.setItem('user', JSON.stringify(data.user))
    router.push(homeByRole(data.user.role))
  } finally { loading.value = false }
}
</script>

<style scoped>
.accts { display: flex; flex-wrap: wrap; gap: 8px; }
.acct { cursor: pointer; }
</style>

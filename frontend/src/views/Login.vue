<template>
  <div class="login-wrap">
    <div class="login-card">
      <div class="brand">
        <el-icon :size="30" color="#1f6f5c"><HomeFilled /></el-icon>
        <div>
          <div class="b1">乡镇卫生院家庭病床平台</div>
          <div class="b2">申请 · 巡诊 · 耗材 · 医保结算 一体化</div>
        </div>
      </div>
      <el-form :model="form" @keyup.enter="doLogin">
        <el-form-item>
          <el-input v-model="form.username" size="large" placeholder="账号" prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" size="large" type="password" show-password
                    placeholder="密码" prefix-icon="Lock" />
        </el-form-item>
        <el-button type="primary" size="large" style="width:100%;background:#1f6f5c;border-color:#1f6f5c"
                   :loading="loading" @click="doLogin">登 录</el-button>
      </el-form>
      <el-divider>演示账号（密码均为 123456）</el-divider>
      <div class="accounts">
        <el-tag v-for="a in accounts" :key="a.u" class="acc" :type="a.type"
                @click="quick(a.u)">{{ a.label }}：{{ a.u }}</el-tag>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'
import { useAuthStore } from '../store/auth'

const router = useRouter()
const auth = useAuthStore()
const form = reactive({ username: 'family', password: '123456' })
const loading = ref(false)

const accounts = [
  { u: 'family', label: '家属', type: 'warning' },
  { u: 'doctor', label: '家庭医生', type: 'success' },
  { u: 'nurse1', label: '护士', type: 'primary' },
  { u: 'admin', label: '管理者', type: 'danger' },
  { u: 'finance', label: '财务', type: 'info' },
  { u: 'insurance', label: '医保经办', type: '' }
]
function quick(u) { form.username = u; form.password = '123456' }

async function doLogin() {
  if (!form.username || !form.password) return ElMessage.warning('请输入账号密码')
  loading.value = true
  try {
    const res = await api.post('/auth/login', form)
    auth.setLogin(res.data.token, res.data.user)
    const home = { ADMIN: '/dashboard', FAMILY: '/applications', FAMILY_DOCTOR: '/applications',
      NURSE: '/visits', FINANCE: '/bills', INSURANCE_AUDITOR: '/compliance' }[res.data.user.role]
    router.push(home)
  } finally { loading.value = false }
}
</script>

<style scoped>
.login-wrap {
  height: 100vh; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #15403a 0%, #1f6f5c 60%, #3a8a75 100%);
}
.login-card {
  width: 420px; background: #fff; border-radius: 14px; padding: 34px 36px;
  box-shadow: 0 18px 50px rgba(0,0,0,.25);
}
.brand { display: flex; gap: 12px; align-items: center; margin-bottom: 24px; }
.b1 { font-size: 19px; font-weight: 700; color: #1f2d3d; }
.b2 { font-size: 12px; color: #909399; margin-top: 2px; }
.accounts { display: flex; flex-wrap: wrap; gap: 8px; }
.acc { cursor: pointer; }
</style>

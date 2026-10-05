import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    user: JSON.parse(localStorage.getItem('user') || 'null')
  }),
  getters: {
    role: s => s.user?.role,
    isFamily: s => s.user?.role === 'FAMILY',
    isDoctor: s => s.user?.role === 'FAMILY_DOCTOR',
    isNurse: s => s.user?.role === 'NURSE',
    isAdmin: s => s.user?.role === 'ADMIN',
    isFinance: s => s.user?.role === 'FINANCE',
    isInsurance: s => s.user?.role === 'INSURANCE_AUDITOR'
  },
  actions: {
    setLogin(token, user) {
      this.token = token
      this.user = user
      localStorage.setItem('token', token)
      localStorage.setItem('user', JSON.stringify(user))
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.clear()
    }
  }
})

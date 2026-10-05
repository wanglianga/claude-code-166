import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', component: () => import('./views/Login.vue'), meta: { public: true } },
  {
    path: '/',
    component: () => import('./views/Shell.vue'),
    children: [
      { path: '', redirect: () => {
          const role = JSON.parse(localStorage.getItem('user') || 'null')?.role
          return homeByRole(role)
        } },
      { path: 'applications', component: () => import('./views/Applications.vue') },
      { path: 'applications/:id', component: () => import('./views/ApplicationDetail.vue') },
      { path: 'board', component: () => import('./views/Board.vue') },
      { path: 'review', component: () => import('./views/Review.vue') },
      { path: 'bills', component: () => import('./views/Bills.vue') },
      { path: 'compliance', component: () => import('./views/Compliance.vue') },
      { path: 'questions', component: () => import('./views/Questions.vue') },
      { path: 'risks', component: () => import('./views/Risks.vue') },
      { path: 'catalog', component: () => import('./views/Catalog.vue') },
      { path: 'dashboard', component: () => import('./views/Dashboard.vue') },
      { path: 'patients', component: () => import('./views/Patients.vue') }
    ]
  }
]

export function homeByRole(role) {
  return {
    FAMILY: '/applications',
    NURSE: '/board',
    DOCTOR: '/applications',
    ADMIN: '/dashboard',
    INSURANCE: '/compliance'
  }[role] || '/login'
}

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  if (to.meta.public) return true
  if (!token) return '/login'
  return true
})

export default router

import { createRouter, createWebHistory } from 'vue-router'
import { authApi } from '../api/auth'
import LoginView from '../views/LoginView.vue'
import ClientManagement from '../views/ClientManagement.vue'

const routes = [
  { path: '/login', component: LoginView },
  { path: '/', redirect: '/clients' },
  { path: '/clients', component: ClientManagement, meta: { requiresAuth: true } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to) => {
  if (!to.meta.requiresAuth) return true
  try {
    const { data } = await authApi.status()
    if (data.loggedIn) return true
    return '/login'
  } catch {
    return '/login'
  }
})

export default router

import { createRouter, createWebHistory } from 'vue-router'
import { authApi } from '../api/auth'
import LoginView from '../views/LoginView.vue'
import ClientManagement from '../views/ClientManagement.vue'
import FlowsDemo from '../views/FlowsDemo.vue'
import CallbackView from '../views/CallbackView.vue'
import ConsentView from '../views/ConsentView.vue'
import DeviceVerificationView from '../views/DeviceVerificationView.vue'

const routes = [
  { path: '/login', component: LoginView },
  { path: '/oauth2/callback', component: CallbackView },
  { path: '/callback', component: CallbackView },
  { path: '/consent', component: ConsentView },
  { path: '/device-verification', component: DeviceVerificationView },
  { path: '/', redirect: '/flows' },
  { path: '/clients', component: ClientManagement, meta: { requiresAuth: true } },
  { path: '/flows', component: FlowsDemo, meta: { requiresAuth: true } }
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
    return {
      path: '/login',
      query: { returnTo: to.fullPath }
    }
  } catch {
    return {
      path: '/login',
      query: { returnTo: to.fullPath }
    }
  }
})

export default router

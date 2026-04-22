import { createRouter, createWebHistory } from 'vue-router'
import { authApi } from '../api/auth'
import LoginView from '../views/LoginView.vue'
import ClientManagement from '../views/ClientManagement.vue'
import PkceFlowView from '../views/PkceFlowView.vue'
import ClientCredentialsView from '../views/ClientCredentialsView.vue'
import ClientAuthView from '../views/ClientAuthView.vue'
import NoPkceComparisonView from '../views/NoPkceComparisonView.vue'
import TokenLifecycleView from '../views/TokenLifecycleView.vue'
import DynamicClientView from '../views/DynamicClientView.vue'
import ParFlowView from '../views/ParFlowView.vue'
import DeviceFlowView from '../views/DeviceFlowView.vue'
import JwtClaimsView from '../views/JwtClaimsView.vue'
import ScenariosView from '../views/ScenariosView.vue'
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
  { path: '/', redirect: '/pkce' },

  // OAuth2 功能演示页面（扁平化路由）
  { path: '/pkce', component: PkceFlowView, meta: { requiresAuth: true, title: 'Authorization Code + PKCE' } },
  { path: '/m2m', component: ClientCredentialsView, meta: { requiresAuth: true, title: 'Client Credentials' } },
  { path: '/client-auth', component: ClientAuthView, meta: { requiresAuth: true, title: 'Client 认证方式差异' } },
  { path: '/no-pkce', component: NoPkceComparisonView, meta: { requiresAuth: true, title: 'Authorization Code without PKCE 对比' } },
  { path: '/token-lifecycle', component: TokenLifecycleView, meta: { requiresAuth: true, title: 'Token 生命周期' } },
  { path: '/dynamic-client', component: DynamicClientView, meta: { requiresAuth: true, title: 'Dynamic Client Registration' } },
  { path: '/par', component: ParFlowView, meta: { requiresAuth: true, title: 'Pushed Authorization Request' } },
  { path: '/device', component: DeviceFlowView, meta: { requiresAuth: true, title: 'Device Code Flow' } },
  { path: '/claims', component: JwtClaimsView, meta: { requiresAuth: true, title: 'JWT Claims 差异' } },
  { path: '/scenarios', component: ScenariosView, meta: { requiresAuth: true, title: '场景说明' } },
  { path: '/flows-demo', component: FlowsDemo, meta: { requiresAuth: true, title: 'Flows Demo' } },

  // 管理页面
  { path: '/clients', component: ClientManagement, meta: { requiresAuth: true, title: 'Client 管理' } },

  // 兼容旧路由（重定向）
  { path: '/flows', redirect: '/pkce' }
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

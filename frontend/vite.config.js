import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const backendOrigin = env.VITE_BACKEND_ORIGIN || 'http://localhost:30000'
  const host = env.VITE_DEV_HOST || 'authlab.test'

  return {
    plugins: [vue()],
    server: {
      host,
      port: 5173,
      proxy: {
        '/api': {
          target: backendOrigin,
          changeOrigin: true,
          credentials: true
        },
        '^/oauth2/(authorize|token|jwks|device_authorization|par|introspect|revoke)(/.*)?$': {
          target: backendOrigin,
          changeOrigin: true
        },
        '/userinfo': {
          target: backendOrigin,
          changeOrigin: true
        },
        '/resource': {
          target: backendOrigin,
          changeOrigin: true
        },
        '/.well-known': {
          target: backendOrigin,
          changeOrigin: true
        }
      }
    }
  }
})

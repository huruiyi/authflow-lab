import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:9000',
        changeOrigin: true,
        credentials: true
      },
      '^/oauth2/(authorize|token|jwks|device_authorization)(/.*)?$': {
        target: 'http://localhost:9000',
        changeOrigin: true
      },
      '/userinfo': {
        target: 'http://localhost:9000',
        changeOrigin: true
      },
      '/resource': {
        target: 'http://localhost:9000',
        changeOrigin: true
      },
      '/.well-known': {
        target: 'http://localhost:9000',
        changeOrigin: true
      }
    }
  }
})

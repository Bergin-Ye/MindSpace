import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'

// 后端没有开启 CORS，本地开发统一走代理，前端只请求同源的 /api。
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})

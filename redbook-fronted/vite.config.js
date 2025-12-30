import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 3000,
    open: true,
    proxy: {
      // 开发环境兜底：若本地未启动网关/注册中心，用户服务可直连
      // 访问 /api/user/** 会被转发到 http://localhost:8080/user/**
      '/api/user': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      },
      // 统一代理到网关，后端路由已添加 /api 前缀
      '/api': {
        target: 'http://localhost:10010',
        changeOrigin: true,
        rewrite: (path) => path
      }
    }
  }
})

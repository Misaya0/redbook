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
      // 统一代理到网关
      '/user': {
        target: 'http://localhost:10010',
        changeOrigin: true,
        rewrite: (path) => path
      },
      '/note': {
        target: 'http://localhost:10010',
        changeOrigin: true,
        rewrite: (path) => path
      },
      '/comment': {
        target: 'http://localhost:10010',
        changeOrigin: true,
        rewrite: (path) => path
      },
      '/search': {
        target: 'http://localhost:10010',
        changeOrigin: true,
        rewrite: (path) => path
      },
      '/product': {
        target: 'http://localhost:10010',
        changeOrigin: true,
        rewrite: (path) => path
      },
      '/order': {
        target: 'http://localhost:10010',
        changeOrigin: true,
        rewrite: (path) => path
      },
      '/api': {
        target: 'http://localhost:10010',
        changeOrigin: true,
        rewrite: (path) => path
      }
    }
  }
})
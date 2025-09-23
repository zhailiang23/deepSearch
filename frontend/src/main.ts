import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { useAuthStore } from './stores/auth'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)

// 初始化认证状态 - 暂时注释掉以避免自动调用API
// const authStore = useAuthStore()
// authStore.initialize().catch(error => {
//   console.warn('Failed to initialize auth state:', error)
// })

app.mount('#app')

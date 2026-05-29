import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' }
})

request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

request.interceptors.response.use(
  response => response.data,
  error => {
    const { response } = error
    if (!response) {
      ElMessage.error('网络连接失败，请检查网络')
      return Promise.reject(error)
    }
    switch (response.status) {
      case 401: {
        // 登录接口本身的 401 是密码错误，不应清除 token 或跳转
        const isLoginRequest = error.config?.url?.includes('/auth/login')
        if (!isLoginRequest) {
          localStorage.removeItem('token')
          localStorage.removeItem('userInfo')
          router.push('/auth/login')
          ElMessage.warning('登录已过期，请重新登录')
        }
        break
      }
      case 403:
        ElMessage.error('无权限访问该资源')
        break
      case 404:
        ElMessage.error(response.data?.message || '请求的资源不存在')
        break
      case 500:
        ElMessage.error(response.data?.message || '服务器内部错误')
        break
      default:
        ElMessage.error(response.data?.message || '请求失败')
    }
    return Promise.reject(error)
  }
)

export default request

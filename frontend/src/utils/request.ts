import axios, {
  type AxiosError,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from 'axios'
import { ElMessage } from 'element-plus'

export interface ApiResponse<T = unknown> {
  code?: number
  message?: string
  data?: T
  [key: string]: unknown
}

const request = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 8000,
})

request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers = config.headers ?? {}
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error: AxiosError) => Promise.reject(error),
)

request.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    if (response.data?.code === 401) {
      window.location.href = '/login'
      return Promise.reject(new Error('Unauthorized'))
    }
    return response
  },
  (error: AxiosError<ApiResponse>) => {
    if (error.response?.data?.code === 401 || error.response?.status === 401) {
      window.location.href = '/login'
    }

    const message =
      error.response?.data?.message || error.message || '请求失败，请稍后重试'

    ElMessage.error(message)

    return Promise.reject(error)
  },
)

export default request


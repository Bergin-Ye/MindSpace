import axios from 'axios'

export const TOKEN_KEY = 'mindspace.token'

export class ApiError extends Error {
  constructor(code, message) {
    super(message || '请求失败')
    this.name = 'ApiError'
    this.code = code
  }
}

let unauthorizedHandler = null

export function setUnauthorizedHandler(handler) {
  unauthorizedHandler = handler
}

const http = axios.create({
  baseURL: '/api',
  timeout: 25000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

/**
 * 后端所有业务响应都是 HTTP 200 + { code, message, data }。
 * 这里统一拆包：成功直接返回 data，失败抛 ApiError。
 */
http.interceptors.response.use(
  async (response) => {
    // 下载接口返回文件流；若实际返回 JSON，说明业务失败。
    if (response.config.responseType === 'blob') {
      const blob = response.data
      if (blob && blob.type && blob.type.includes('application/json')) {
        const text = await blob.text()
        let body = null
        try {
          body = JSON.parse(text)
        } catch {
          throw new ApiError(500, '下载失败')
        }
        if (body.code === 401) unauthorizedHandler?.()
        throw new ApiError(body.code, body.message)
      }
      return response
    }

    const body = response.data
    if (!body || typeof body.code === 'undefined') {
      return body
    }
    if (body.code === 200) {
      return body.data
    }
    if (body.code === 401) {
      unauthorizedHandler?.()
    }
    throw new ApiError(body.code, body.message)
  },
  (error) => {
    if (error.response?.status === 401) {
      unauthorizedHandler?.()
      return Promise.reject(new ApiError(401, '未登录或登录已过期'))
    }

    const body = error.response?.data
    if (body && typeof body.code !== 'undefined') {
      return Promise.reject(new ApiError(body.code, body.message))
    }

    if (error.code === 'ECONNABORTED') {
      return Promise.reject(new ApiError(-1, '请求超时，请稍后重试'))
    }
    if (!error.response) {
      return Promise.reject(
        new ApiError(-1, '连接不到后端服务，请确认已在 8080 端口启动')
      )
    }
    return Promise.reject(new ApiError(error.response.status, '请求失败'))
  }
)

export function errorMessage(error) {
  if (error instanceof ApiError) return error.message
  return error?.message || '操作失败'
}

export default http

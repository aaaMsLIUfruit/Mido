import request from '../utils/request'

export interface RegisterRequest {
  username: string
  email: string
  password: string
  confirmPassword: string
}

export interface LoginRequest {
  identifier: string
  password: string
}

export interface AuthResponse {
  token?: string
  message?: string
  data?: {
    token?: string
  }
}

export const register = (
  payload: RegisterRequest,
): Promise<AuthResponse> =>
  request.post<AuthResponse>('/api/auth/register', payload).then((res) => res.data)

export const login = (
  payload: LoginRequest,
): Promise<AuthResponse> =>
  request.post<AuthResponse>('/api/auth/login', payload).then((res) => res.data)

export const authApi = {
  register,
  login,
}

export default authApi


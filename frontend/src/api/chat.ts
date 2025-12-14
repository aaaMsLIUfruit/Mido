import request, { type ApiResponse } from '../utils/request'

export interface ChatCreateRequest {
  title?: string
  folderId?: string
}

export interface ChatUpdateRequest {
  id: string
  title?: string
  folderId?: string
}

export interface ChatMessageCreateRequest {
  chatId: string
  role: 'user' | 'assistant'
  content: string
}

export interface ChatResponse {
  id: string
  folderId?: string
  title?: string
  createdAt: string
  updatedAt: string
}

export interface ChatMessageResponse {
  id: string
  chatId: string
  role: 'user' | 'assistant'
  content: string
  createdAt: string
}

export interface ChatDetailResponse extends ChatResponse {
  messages: ChatMessageResponse[]
}

export interface ChatListParams {
  folderId?: string
  page?: number
  pageSize?: number
}

export interface PaginatedResult<T> {
  list: T[]
  total: number
  page: number
  pageSize: number
}

type BackendPageResponse<T> = {
  page?: number
  size?: number
  total?: number
  records?: T[]
}

const unwrap = <T>(
  payload: ApiResponse<T> | T | undefined,
): T | undefined => {
  if (!payload) {
    return undefined
  }
  if (typeof payload === 'object' && 'data' in payload) {
    return (payload as ApiResponse<T>).data
  }
  return payload as T
}

const isBackendPageResponse = <T>(
  payload: PaginatedResult<T> | BackendPageResponse<T>,
): payload is BackendPageResponse<T> =>
  typeof payload === 'object' && payload !== null && 'records' in payload

const normalizePaginatedResult = <T>(
  payload: PaginatedResult<T> | BackendPageResponse<T> | undefined,
  params: ChatListParams,
): PaginatedResult<T> => {
  const fallbackPage = params.page ?? 1
  const fallbackSize = params.pageSize ?? 100

  if (!payload) {
    return {
      list: [],
      total: 0,
      page: fallbackPage,
      pageSize: fallbackSize,
    }
  }

  if (isBackendPageResponse(payload)) {
    return {
      list: payload.records ?? [],
      total: payload.total ?? 0,
      page: payload.page ?? fallbackPage,
      pageSize: payload.size ?? fallbackSize,
    }
  }

  return {
    list: payload.list ?? [],
    total: payload.total ?? 0,
    page: payload.page ?? fallbackPage,
    pageSize: payload.pageSize ?? fallbackSize,
  }
}

export const createChat = async (
  payload: ChatCreateRequest,
): Promise<ChatResponse | undefined> => {
  const res = await request
    .post<ApiResponse<ChatResponse> | ChatResponse>('/api/chat/create', payload)
    .then((r) => r.data)
  return unwrap(res)
}

export const fetchChatList = async (
  params: ChatListParams = {},
): Promise<PaginatedResult<ChatResponse>> => {
  const res = await request
    .get<ApiResponse<PaginatedResult<ChatResponse>> | PaginatedResult<ChatResponse> | BackendPageResponse<ChatResponse>>(
      '/api/chat/list',
      { params },
    )
    .then((r) => r.data)

  const data = unwrap<PaginatedResult<ChatResponse> | BackendPageResponse<ChatResponse>>(res)
  return normalizePaginatedResult(data, params)
}

export const fetchChatById = async (
  id: string,
): Promise<ChatDetailResponse | undefined> => {
  const res = await request
    .get<ApiResponse<ChatDetailResponse> | ChatDetailResponse>(`/api/chat/${id}`)
    .then((r) => r.data)
  return unwrap(res)
}

export const updateChat = async (
  payload: ChatUpdateRequest,
): Promise<ChatResponse | undefined> => {
  const res = await request
    .put<ApiResponse<ChatResponse> | ChatResponse>('/api/chat/update', payload)
    .then((r) => r.data)
  return unwrap(res)
}

export const deleteChat = (id: string) =>
  request.delete<ApiResponse<null>>(`/api/chat/${id}`).then((res) => res.data)

export const createChatMessage = async (
  payload: ChatMessageCreateRequest,
): Promise<ChatMessageResponse | undefined> => {
  const res = await request
    .post<ApiResponse<ChatMessageResponse> | ChatMessageResponse>('/api/chat/message/create', payload)
    .then((r) => r.data)
  return unwrap(res)
}

export const fetchChatMessages = async (
  chatId: string,
): Promise<ChatMessageResponse[]> => {
  const res = await request
    .get<ApiResponse<ChatMessageResponse[]> | ChatMessageResponse[]>(`/api/chat/${chatId}/messages`)
    .then((r) => r.data)
  return unwrap(res) ?? []
}

export const chatApi = {
  createChat,
  fetchChatList,
  fetchChatById,
  updateChat,
  deleteChat,
  createChatMessage,
  fetchChatMessages,
}

export default chatApi


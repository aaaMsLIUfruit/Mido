import request, { type ApiResponse } from '../utils/request'

export interface NoteCreateRequest {
  title: string
  content: string
  folderId?: string
  coverUrl?: string
  tags?: string[]
}

export interface NoteUpdateRequest {
  id: string
  title?: string
  content?: string
  folderId?: string
  coverUrl?: string
  tags?: string[]
}

export interface NoteListParams {
  folderId?: string
  keyword?: string
  page?: number
  pageSize?: number
}

export interface NoteSummary {
  id: string
  title: string
  folderId?: string
  updatedAt: string
  tags?: string[]
}

export interface NoteDetail extends NoteSummary {
  content: string
  createdAt: string
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

export const createNote = async (
  payload: NoteCreateRequest,
): Promise<NoteDetail | undefined> => {
  const res = await request
    .post<ApiResponse<NoteDetail> | NoteDetail>('/api/note/create', payload)
    .then((r) => r.data)
  return unwrap(res)
}

const isBackendPageResponse = (
  payload: PaginatedResult<NoteSummary> | BackendPageResponse<NoteSummary>,
): payload is BackendPageResponse<NoteSummary> =>
  typeof payload === 'object' && payload !== null && 'records' in payload

const normalizePaginatedResult = (
  payload: PaginatedResult<NoteSummary> | BackendPageResponse<NoteSummary> | undefined,
  params: NoteListParams,
): PaginatedResult<NoteSummary> => {
  const fallbackPage = params.page ?? 1
  const fallbackSize = params.pageSize ?? 10

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

export const fetchNoteList = async (
  params: NoteListParams = {},
): Promise<PaginatedResult<NoteSummary>> => {
  const res = await request
    .get<ApiResponse<PaginatedResult<NoteSummary>> | PaginatedResult<NoteSummary> | BackendPageResponse<NoteSummary>>(
      '/api/note/list',
      { params },
    )
    .then((r) => r.data)

  const data = unwrap<PaginatedResult<NoteSummary> | BackendPageResponse<NoteSummary>>(res)
  return normalizePaginatedResult(data, params)
}

export const fetchNoteById = async (
  id: string,
): Promise<NoteDetail | undefined> => {
  const res = await request
    .get<ApiResponse<NoteDetail> | NoteDetail>(`/api/note/${id}`)
    .then((r) => r.data)
  return unwrap(res)
}

export const updateNote = async (
  payload: NoteUpdateRequest,
): Promise<NoteDetail | undefined> => {
  const res = await request
    .put<ApiResponse<NoteDetail> | NoteDetail>('/api/note/update', payload)
    .then((r) => r.data)
  return unwrap(res)
}

export const deleteNote = (id: string) =>
  request.delete<ApiResponse<null>>(`/api/note/${id}`).then((res) => res.data)

export const noteApi = {
  createNote,
  fetchNoteList,
  fetchNoteById,
  updateNote,
  deleteNote,
}

export default noteApi

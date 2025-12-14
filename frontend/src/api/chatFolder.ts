import request, { type ApiResponse } from '../utils/request'

export interface ChatFolderNode {
  id: string
  name: string
  parentId?: string
  createdAt?: string
  children?: ChatFolderNode[]
}

export interface ChatFolderCreateRequest {
  name: string
  parentId?: string
}

export interface ChatFolderRenameRequest {
  id: string
  name: string
}

export interface ChatFolderMoveRequest {
  id: string
  newParentId?: string
}

export type ChatFolderTreeResponse = ChatFolderNode[] | ApiResponse<ChatFolderNode[]>
export type ChatFolderResponse = ApiResponse<ChatFolderNode>

const unwrapList = (payload: ChatFolderTreeResponse): ChatFolderNode[] => {
  if (Array.isArray(payload)) {
    return payload
  }
  return payload?.data ?? []
}

export const fetchChatFolderTree = async (): Promise<ChatFolderNode[]> => {
  const res = await request.get<ChatFolderTreeResponse>('/api/chat/folder/tree').then((r) => r.data)
  return unwrapList(res)
}

export const createChatFolder = (
  payload: ChatFolderCreateRequest,
): Promise<ChatFolderResponse> =>
  request
    .post<ChatFolderResponse>('/api/chat/folder/create', payload)
    .then((res) => res.data)

export const renameChatFolder = (
  payload: ChatFolderRenameRequest,
): Promise<ChatFolderResponse> =>
  request
    .post<ChatFolderResponse>('/api/chat/folder/rename', payload)
    .then((res) => res.data)

export const moveChatFolder = (
  payload: ChatFolderMoveRequest,
): Promise<ChatFolderResponse> =>
  request
    .post<ChatFolderResponse>('/api/chat/folder/move', payload)
    .then((res) => res.data)

export const deleteChatFolder = (
  id: string,
): Promise<ApiResponse<null>> =>
  request
    .delete<ApiResponse<null>>(`/api/chat/folder/${id}`)
    .then((res) => res.data)

export const chatFolderApi = {
  fetchChatFolderTree,
  createChatFolder,
  renameChatFolder,
  moveChatFolder,
  deleteChatFolder,
}

export default chatFolderApi


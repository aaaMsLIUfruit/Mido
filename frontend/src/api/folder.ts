import request, { type ApiResponse } from '../utils/request'

export interface FolderNode {
  id: string
  name: string
  parentId?: string
  children?: FolderNode[]
}

export interface FolderCreateRequest {
  name: string
  parentId?: string
}

export interface FolderRenameRequest {
  id: string
  name: string
}

export interface FolderMoveRequest {
  id: string
  newParentId?: string
}

export type FolderTreeResponse = FolderNode[] | ApiResponse<FolderNode[]>
export type FolderResponse = ApiResponse<FolderNode>

const unwrapList = (payload: FolderTreeResponse): FolderNode[] => {
  if (Array.isArray(payload)) {
    return payload
  }
  return payload?.data ?? []
}

export const fetchFolderTree = async (): Promise<FolderNode[]> => {
  const res = await request.get<FolderTreeResponse>('/api/note/folder/tree').then((r) => r.data)
  return unwrapList(res)
}

export const createFolder = (
  payload: FolderCreateRequest,
): Promise<FolderResponse> =>
  request
    .post<FolderResponse>('/api/note/folder/create', payload)
    .then((res) => res.data)

export const renameFolder = (
  payload: FolderRenameRequest,
): Promise<FolderResponse> =>
  request
    .post<FolderResponse>('/api/note/folder/rename', payload)
    .then((res) => res.data)

export const moveFolder = (
  payload: FolderMoveRequest,
): Promise<FolderResponse> =>
  request
    .post<FolderResponse>('/api/note/folder/move', payload)
    .then((res) => res.data)

export const deleteFolder = (
  id: string,
): Promise<ApiResponse<null>> =>
  request
    .delete<ApiResponse<null>>(`/api/note/folder/${id}`)
    .then((res) => res.data)

export const folderApi = {
  fetchFolderTree,
  createFolder,
  renameFolder,
  moveFolder,
  deleteFolder,
}

export default folderApi



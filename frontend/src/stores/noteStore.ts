import { computed, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FolderMoveRequest, FolderNode } from '../api/folder'
import folderApi from '../api/folder'
import {
  type NoteCreateRequest,
  type NoteDetail,
  type NoteSummary,
  type NoteUpdateRequest,
  noteApi,
} from '../api/note'

type Nullable<T> = T | null

interface NoteState {
  folders: FolderNode[]
  notesMap: Record<string, NoteSummary[]>
  selectedFolderId: string | null
  selectedNote: Nullable<NoteDetail>
  loadingFolders: boolean
  loadingNotes: boolean
  loadingNoteDetail: boolean
  saving: boolean
}

const state = reactive<NoteState>({
  folders: [],
  notesMap: {},
  selectedFolderId: null,
  selectedNote: null,
  loadingFolders: false,
  loadingNotes: false,
  loadingNoteDetail: false,
  saving: false,
})

const findFolder = (id?: string | null, nodes: FolderNode[] = state.folders): FolderNode | null => {
  for (const node of nodes) {
    if (node.id === id) {
      return node
    }
    if (node.children?.length) {
      const found = findFolder(id, node.children)
      if (found) return found
    }
  }
  return null
}

export const useNoteStore = () => {
  const loadFolders = async () => {
    state.loadingFolders = true
    try {
      const folders = await folderApi.fetchFolderTree()
      state.folders = folders
    } finally {
      state.loadingFolders = false
    }
  }

  const loadNotes = async (folderId?: string | null) => {
    const targetId = folderId ?? state.selectedFolderId ?? null
    if (!targetId) {
      state.notesMap = {}
      return
    }
    state.loadingNotes = true
    try {
      const res = await noteApi.fetchNoteList({ folderId: targetId })
      state.notesMap[targetId] = res.list ?? []
      const first = state.notesMap[targetId][0]
      if (first && state.selectedNote?.id !== first.id) {
        await selectNote(first.id)
      } else if (!first && state.selectedNote?.folderId === targetId) {
        state.selectedNote = null
      }
    } finally {
      state.loadingNotes = false
    }
  }

  const selectFolder = async (folderId?: string | null) => {
    state.selectedFolderId = folderId ?? null
    await loadNotes(state.selectedFolderId)
  }

  const selectNote = async (noteId: string) => {
    state.loadingNoteDetail = true
    try {
      const res = await noteApi.fetchNoteById(noteId)
      state.selectedNote = res ?? null
    } finally {
      state.loadingNoteDetail = false
    }
  }

  const resolveFolderName = (folderId?: string | null) =>
    findFolder(folderId)?.name ?? '未分类'

  const createNote = async (payload: NoteCreateRequest) => {
    const created = await noteApi.createNote(payload)
    if (created) {
      const targetFolder = payload.folderId ?? state.selectedFolderId
      await selectFolder(targetFolder)
      await selectNote(created.id)
      ElMessage.success(`笔记已创建，位置：${resolveFolderName(targetFolder)}`)
    }
    return created
  }

  const updateNote = async (payload: NoteUpdateRequest) => {
    state.saving = true
    try {
      const updated = await noteApi.updateNote(payload)
      if (updated) {
        state.selectedNote = updated
        const targetFolder = payload.folderId ?? state.selectedFolderId
        await selectFolder(targetFolder)
        ElMessage.success(`笔记已保存，位置：${resolveFolderName(targetFolder)}`)
        localStorage.removeItem(`draft-${updated.id}`)
      }
      return updated
    } finally {
      state.saving = false
    }
  }

  const deleteNote = async (noteId: string) => {
    await ElMessageBox.confirm('确定要删除这篇笔记吗？', '删除笔记', {
      type: 'warning',
    })
    await noteApi.deleteNote(noteId)
    ElMessage.success('笔记已删除')
    localStorage.removeItem(`draft-${noteId}`)
    if (state.selectedNote?.id === noteId) {
      state.selectedNote = null
    }
    await selectFolder(state.selectedFolderId)
  }

  const createFolder = async (name: string, parentId?: string) => {
    await folderApi.createFolder({ name, parentId })
    ElMessage.success('文件夹已创建')
    await loadFolders()
    if (parentId) {
      await selectFolder(parentId)
    }
  }

  const renameFolder = async (id: string, name: string) => {
    await folderApi.renameFolder({ id, name })
    ElMessage.success('文件夹已重命名')
    await loadFolders()
  }

  const deleteFolder = async (id: string) => {
    await ElMessageBox.confirm('删除文件夹将同时删除其下所有笔记，是否继续？', '删除文件夹', {
      type: 'warning',
    })
    await folderApi.deleteFolder(id)
    ElMessage.success('文件夹已删除')
    if (state.selectedFolderId === id) {
      state.selectedFolderId = null
    }
    await loadFolders()
    await selectFolder(state.selectedFolderId)
  }

  const moveFolder = async (payload: FolderMoveRequest) => {
    await folderApi.moveFolder(payload)
    ElMessage.success('文件夹已移动')
    await loadFolders()
  }

  const renameNote = async (noteId: string, name: string) => {
    state.saving = true
    try {
      const updated = await noteApi.updateNote({ id: noteId, title: name })
      if (updated) {
        const targetFolder = updated.folderId ?? state.selectedFolderId
        if (state.selectedNote?.id === noteId) {
          state.selectedNote = updated
        }
        if (targetFolder) {
          await loadNotes(targetFolder)
        } else {
          await loadNotes()
        }
        ElMessage.success('笔记已重命名')
      }
    } finally {
      state.saving = false
    }
  }

  const moveNote = async (noteId: string, targetFolderId?: string) => {
    const sourceFolderId = Object.keys(state.notesMap).find((key) =>
      (state.notesMap[key] ?? []).some((note) => note.id === noteId),
    )
    const updated = await noteApi.updateNote({ id: noteId, folderId: targetFolderId } as NoteUpdateRequest)
    if (updated) {
      if (sourceFolderId) {
        await loadNotes(sourceFolderId)
      }
      if (targetFolderId && targetFolderId !== sourceFolderId) {
        await loadNotes(targetFolderId)
      }
      if (state.selectedNote?.id === noteId) {
        state.selectedNote = updated
      }
      ElMessage.success('笔记已移动')
    }
  }

  const selectedFolder = computed(() => findFolder(state.selectedFolderId))

  const init = async () => {
    await loadFolders()
    const initialId = state.selectedFolderId ?? state.folders[0]?.id ?? null
    if (initialId) {
      await selectFolder(initialId)
    } else {
      state.selectedNote = null
    }
  }

  return {
    state,
    init,
    loadFolders,
    loadNotes,
    selectFolder,
    selectNote,
    createNote,
    updateNote,
    deleteNote,
    createFolder,
    renameFolder,
    deleteFolder,
    moveFolder,
    moveNote,
    renameNote,
    selectedFolder,
  }
}

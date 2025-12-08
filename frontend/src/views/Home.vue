<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
// @ts-ignore
import FolderTree, { type TreeNode } from '../components/FolderTree.vue'
// @ts-ignore
import NoteEditor from '../components/NoteEditor.vue'
import { useNoteStore } from '../stores/noteStore'
import type { FolderNode } from '../api/folder'
import type { NoteSummary } from '../api/note'

const noteStore = useNoteStore()
const router = useRouter()
const isSidebarCollapsed = ref(false)
const sidebarWidth = ref(320)
const minSidebarWidth = 220
const maxSidebarWidth = 520
const isResizingSidebar = ref(false)
let resizeStartX = 0
let resizeStartWidth = 320
let resizeRaf: number | null = null

const defaultRootId = computed(() => {
  const root = noteStore.state.folders.find((folder) => !folder.parentId && folder.name === '未分类')
  return root ? String(root.id) : null
})

const workspaceNav = [
  { icon: '🔍', label: '搜索', action: 'search' },
  { icon: '✨', label: 'Mido AI', action: 'ai' },
  { icon: '✎', label: '新建笔记', action: 'create-note' },
  { icon: '📁', label: '新建文件夹', action: 'create-folder' },
]

const isSearchPanelVisible = ref(false)
const searchKeyword = ref('')
const workspaceSettingsVisible = ref(false)
const userDisplayName = ref(localStorage.getItem('username') || 'Mido 用户')
const userEmail = ref(localStorage.getItem('email') || '未设置邮箱')

onMounted(async () => {
  await noteStore.init()
})

const folderTreeWithNotes = computed<TreeNode[]>(() => {
  const build = (folders: FolderNode[]): TreeNode[] =>
    folders.map((folder) => {
      const isDefaultRoot = !folder.parentId && folder.name === '未分类'
      return {
        treeKey: `folder-${folder.id}`,
        id: String(folder.id),
        name: folder.name,
        type: 'folder' as const,
        meta: {
          isDefaultRoot,
        },
        children: [
          ...(folder.children ? build(folder.children) : []),
          ...((noteStore.state.notesMap[folder.id] ?? []).map((note) => ({
            treeKey: `note-${note.id}`,
            id: String(note.id),
            name: note.title,
            type: 'note' as const,
          })) as TreeNode[]),
        ],
      }
    })

  return build(noteStore.state.folders)
})

const currentTreeKey = computed(() => {
  if (noteStore.state.selectedNote) {
    return `note-${noteStore.state.selectedNote.id}`
  }
  if (noteStore.state.selectedFolderId) {
    return `folder-${noteStore.state.selectedFolderId}`
  }
  return null
})

const handleSelectFolder = async (id: string | null) => {
  await noteStore.selectFolder(id)
}

const handleSelectNote = async (id: string) => {
  await noteStore.selectNote(id)
}

const handleSaveNote = async (payload: {
  id: string
  title: string
  content: string
  folderId?: string
  coverUrl?: string
}) => {
  await noteStore.updateNote({
    id: payload.id,
    title: payload.title,
    content: payload.content,
    folderId: payload.folderId,
    coverUrl: payload.coverUrl,
  } as any)
}

const handleCreateFolder = async (payload: { parentId?: string; name: string; parentIsDefaultRoot?: boolean }) => {
  if (payload.parentIsDefaultRoot) {
    ElMessage.error('“未分类”下不能创建文件夹')
    return
  }
  await noteStore.createFolder(payload.name, payload.parentId)
}

const generateDefaultName = (type: 'note' | 'folder') => {
  const date = new Date()
  const timestamp = `${(date.getMonth() + 1).toString().padStart(2, '0')}/${date
    .getDate()
    .toString()
    .padStart(2, '0')} ${date.getHours().toString().padStart(2, '0')}:${date
    .getMinutes()
    .toString()
    .padStart(2, '0')}`
  return type === 'note' ? `新建笔记 ${timestamp}` : `新建文件夹 ${timestamp}`
}

const handleCreate = async (type: 'note' | 'folder') => {
  const name = generateDefaultName(type)
  try {
    if (type === 'folder') {
      if (defaultRootId.value && noteStore.state.selectedFolderId === defaultRootId.value) {
        ElMessage.error('“未分类”下不能创建文件夹')
        return
      }
      await noteStore.createFolder(name, noteStore.state.selectedFolderId ?? undefined)
    } else {
      await noteStore.createNote({
        title: name,
        content: '# 新笔记',
        folderId: noteStore.state.selectedFolderId ?? undefined,
      })
    }
  } catch {
    ElMessage.error('创建失败，请稍后重试')
  }
}

const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('username')
  localStorage.removeItem('email')
  workspaceSettingsVisible.value = false
  router.replace('/login')
}

const handleRenameFolder = async (payload: { id: string; name: string }) => {
  await noteStore.renameFolder(payload.id, payload.name)
}

const handleMoveFolder = async (payload: { id: string; newParentId?: string }) => {
  await noteStore.moveFolder({ id: payload.id, newParentId: payload.newParentId })
}

const handleDeleteFolder = async (payload: { id: string }) => {
  await noteStore.deleteFolder(payload.id)
}

const handleDeleteNoteFromTree = async (payload: { id: string }) => {
  await noteStore.deleteNote(payload.id)
}

const handleRenameNoteFromTree = async (payload: { id: string; name: string }) => {
  await noteStore.renameNote(payload.id, payload.name)
}

const handleMoveNote = async (payload: { id: string; targetFolderId?: string }) => {
  await noteStore.moveNote(payload.id, payload.targetFolderId)
}

const openSearchPanel = () => {
  isSearchPanelVisible.value = true
}

const closeSearchPanel = () => {
  isSearchPanelVisible.value = false
  searchKeyword.value = ''
}

const handleNavClick = (item: { label: string; action?: string }) => {
  if (item.action === 'search') {
    openSearchPanel()
    return
  }
  if (item.action === 'ai') {
    ElMessageBox.alert('Mido AI 即将上线，敬请期待。', '提示', {
      confirmButtonText: '好的',
    })
    return
  }
  if (item.action === 'create-note') {
    handleCreate('note')
    return
  }
  if (item.action === 'create-folder') {
    handleCreate('folder')
    return
  }
}

const allNotes = computed<NoteSummary[]>(() => {
  const map = noteStore.state.notesMap
  const entries = Object.keys(map).reduce<NoteSummary[]>((acc, key) => {
    const list = map[key] ?? []
    return acc.concat(list)
  }, [])
  if (entries.length === 0 && noteStore.state.selectedNote) {
    return [noteStore.state.selectedNote]
  }
  return entries
})

const searchResults = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  if (!keyword) {
    return allNotes.value
  }
  return allNotes.value.filter((note) => note.title.toLowerCase().includes(keyword))
})

const toggleSidebar = () => {
  isSidebarCollapsed.value = !isSidebarCollapsed.value
}

const sidebarStyle = computed(() => ({
  width: isSidebarCollapsed.value ? '0px' : `${sidebarWidth.value}px`,
}))

const stopSidebarResizing = () => {
  if (!isResizingSidebar.value) {
    return
  }
  isResizingSidebar.value = false
  document.removeEventListener('mousemove', handleSidebarMouseMove)
  document.removeEventListener('mouseup', stopSidebarResizing)
  if (resizeRaf) {
    cancelAnimationFrame(resizeRaf)
    resizeRaf = null
  }
  document.body.style.userSelect = ''
  document.body.style.cursor = ''
}

const handleSidebarMouseMove = (event: MouseEvent) => {
  if (!isResizingSidebar.value) return
  event.preventDefault()
  const delta = event.clientX - resizeStartX
  const nextWidth = Math.min(maxSidebarWidth, Math.max(minSidebarWidth, resizeStartWidth + delta))
  if (resizeRaf) {
    cancelAnimationFrame(resizeRaf)
  }
  resizeRaf = requestAnimationFrame(() => {
    sidebarWidth.value = nextWidth
    resizeRaf = null
  })
}

const startSidebarResizing = (event: MouseEvent) => {
  if (isSidebarCollapsed.value) return
  isResizingSidebar.value = true
  resizeStartX = event.clientX
  resizeStartWidth = sidebarWidth.value
  document.addEventListener('mousemove', handleSidebarMouseMove)
  document.addEventListener('mouseup', stopSidebarResizing)
  document.body.style.userSelect = 'none'
  document.body.style.cursor = 'col-resize'
}

onBeforeUnmount(() => {
  stopSidebarResizing()
})
</script>

<template>
  <!-- eslint-disable vue/no-v-model-argument -->
  <div class="workspace-layout">
    <div
      class="workspace-panel-wrapper"
      :style="sidebarStyle"
      :class="{ collapsed: isSidebarCollapsed, resizing: isResizingSidebar }"
    >
      <aside class="workspace-panel" v-show="!isSidebarCollapsed">
        <div class="workspace-panel__header">
          <el-popover
            v-model:visible="workspaceSettingsVisible"
            placement="right-start"
            trigger="click"
            width="260"
            popper-class="workspace-settings-popover"
          >
            <div class="workspace-settings-card">
              <div class="workspace-settings-card__user">
                <div class="workspace-settings-card__avatar">{{ userDisplayName.charAt(0).toUpperCase() }}</div>
                <div>
                  <p class="settings-name">{{ userDisplayName }}</p>
                  <span class="settings-email">{{ userEmail }}</span>
                </div>
              </div>
              <el-button text type="primary" @click="handleLogout">登出</el-button>
            </div>
            <template #reference>
              <button class="workspace-avatar" type="button">
                {{ userDisplayName.charAt(0).toUpperCase() }}
              </button>
            </template>
          </el-popover>
          <div class="workspace-info">
            <p class="workspace-title">{{ userDisplayName }}</p>
            <span class="workspace-subtitle">私人</span>
          </div>
          <button class="workspace-panel__collapse" title="收起侧栏" @click="toggleSidebar">
            «
          </button>
        </div>
        <div class="workspace-panel__section">
          <p class="section-label">导航</p>
          <ul class="nav-list">
            <li v-for="item in workspaceNav" :key="item.label" @click="handleNavClick(item)">
              <span class="nav-icon">{{ item.icon }}</span>
              <span class="nav-text">{{ item.label }}</span>
            </li>
          </ul>
        </div>
        <FolderTree
          class="folder-tree-wrapper"
          :data="folderTreeWithNotes"
          :current-key="currentTreeKey"
          :loading="noteStore.state.loadingFolders"
          :show-toolbar="false"
          @select-folder="handleSelectFolder"
          @select-note="handleSelectNote"
          @create-folder="handleCreateFolder"
          @rename-folder="handleRenameFolder"
          @delete-folder="handleDeleteFolder"
          @rename-note="handleRenameNoteFromTree"
          @delete-note="handleDeleteNoteFromTree"
          @move-folder="handleMoveFolder"
          @move-note="handleMoveNote"
        />
      </aside>
      <div
        v-if="!isSidebarCollapsed"
        class="workspace-panel__resizer"
        @mousedown.prevent="startSidebarResizing"
      ></div>
    </div>
    <main class="workspace-main">
      <section class="note-workspace">
        <div class="note-editor-wrapper">
          <NoteEditor
            :note="noteStore.state.selectedNote"
            :folders="noteStore.state.folders"
            :saving="noteStore.state.saving"
            @save="handleSaveNote"
          />
        </div>
      </section>
      <button
        v-if="isSidebarCollapsed"
        class="sidebar-floating-toggle"
        type="button"
        title="展开侧栏"
        @click="toggleSidebar"
      >
        »
      </button>
    </main>

    <div
      v-if="isSearchPanelVisible"
      class="search-overlay"
      @click.self="closeSearchPanel"
    >
      <div class="search-panel">
        <div class="search-panel__header">
          <div class="search-panel__input">
            <span class="search-icon">🔍</span>
            <input
              v-model="searchKeyword"
              type="text"
              placeholder="在工作空间中搜索..."
            />
          </div>
          <button class="search-panel__close" @click="closeSearchPanel">×</button>
        </div>

        <div class="search-panel__results">
          <p class="search-panel__section">最近</p>
          <div
            v-for="note in searchResults"
            :key="note.id"
            class="search-result"
            @click="handleSelectNote(note.id)"
          >
            <div>
              <p class="result-title">{{ note.title }}</p>
              <p class="result-meta">
                {{ note.folderId ? 'Folder ' + note.folderId : '未分类' }}
              </p>
            </div>
            <span class="result-date">
              {{ note.updatedAt ? new Date(note.updatedAt).toLocaleDateString() : '' }}
            </span>
          </div>
          <p v-if="searchResults.length === 0" class="search-panel__empty">暂无匹配结果</p>
        </div>
      </div>
    </div>

  </div>
</template>

<style scoped>
.workspace-layout {
  display: flex;
  height: 100%;
  background: #f5f7f9;
  color: #2f3437;
  font-family: 'Inter', 'PingFang SC', 'Segoe UI', sans-serif;
  position: relative;
}

.workspace-panel-wrapper {
  display: flex;
  align-items: stretch;
  transition: width 0.2s ease;
  background: #f5f7f9;
  border-right: 1px solid #d0d8e2;
  min-width: 0;
}

.workspace-panel-wrapper.resizing {
  transition: none;
}

.workspace-panel-wrapper.collapsed {
  width: 0 !important;
}

.workspace-panel {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding: 24px 18px 18px;
  min-height: 0;
}

.workspace-panel__header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.workspace-avatar {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: #2f3437;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
}

.workspace-title {
  margin: 0;
  font-weight: 600;
}

.workspace-subtitle {
  font-size: 12px;
  color: #8a8f95;
}

.workspace-panel__collapse {
  margin-left: auto;
  border: none;
  border-radius: 8px;
  background: rgba(47, 52, 55, 0.08);
  width: 32px;
  height: 32px;
  cursor: pointer;
  font-size: 18px;
  color: #2f3437;
}

.workspace-panel__create {
  border: none;
  background: rgba(47, 52, 55, 0.08);
  border-radius: 8px;
  width: 32px;
  height: 32px;
  cursor: pointer;
  font-size: 16px;
  margin-right: 6px;
  color: #2f3437;
}

.workspace-settings-popover {
  padding: 0 !important;
  border-radius: 16px !important;
  border: 1px solid rgba(47, 52, 55, 0.08);
  box-shadow: 0 15px 35px rgba(15, 23, 42, 0.2);
}

.workspace-settings-card {
  padding: 16px;
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.workspace-settings-card__user {
  display: flex;
  gap: 12px;
  align-items: center;
}

.workspace-settings-card__avatar {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: #2f3437;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
}

.settings-name {
  margin: 0;
  font-weight: 600;
}

.settings-email {
  font-size: 12px;
  color: #8a8f95;
}

.create-popover-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.create-popover-item {
  border: none;
  background: rgba(47, 52, 55, 0.08);
  border-radius: 10px;
  padding: 8px 12px;
  text-align: left;
  cursor: pointer;
  color: #2f3437;
  font-size: 14px;
}

.create-popover-item:hover {
  background: rgba(47, 52, 55, 0.13);
}

.section-label {
  margin: 0 0 8px;
  font-size: 12px;
  letter-spacing: 0.04em;
  color: #a5a3a1;
  text-transform: uppercase;
}

.nav-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.nav-list li {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s ease;
}

.nav-list li:hover {
  background: rgba(47, 52, 55, 0.08);
}

.nav-icon {
  font-size: 16px;
}

.folder-tree-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.workspace-panel__resizer {
  width: 6px;
  cursor: col-resize;
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(148, 163, 184, 0.6), rgba(148, 163, 184, 0.2));
}

.workspace-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  background: #fff;
}

.note-workspace {
  flex: 1;
  padding: 24px 48px 48px;
  background: #fff;
}

.note-editor-wrapper {
  height: 100%;
  background: #fff;
  border-radius: 0;
  box-shadow: none;
}

.sidebar-floating-toggle {
  position: absolute;
  left: 12px;
  top: 16px;
  width: 32px;
  height: 32px;
  border: 1px solid rgba(47, 52, 55, 0.15);
  border-radius: 10px;
  background: #fffdf8;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.15);
  color: #2f3437;
  cursor: pointer;
  font-size: 18px;
  font-weight: 600;
}

.search-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.35);
  backdrop-filter: blur(6px);
  display: flex;
  justify-content: center;
  padding: 40px 24px;
  z-index: 2000;
}

.search-panel {
  width: min(920px, 100%);
  max-height: 90vh;
  background: #f5f3ef;
  border-radius: 18px;
  box-shadow: 0 30px 80px rgba(15, 23, 42, 0.2);
  border: 1px solid rgba(47, 52, 55, 0.08);
  display: flex;
  flex-direction: column;
  padding: 24px;
}

.search-panel__header {
  display: flex;
  align-items: center;
  gap: 16px;
}

.search-panel__input {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 14px;
  padding: 0 16px;
  border: 1px solid rgba(15, 23, 42, 0.08);
}

.search-panel__input input {
  flex: 1;
  border: none;
  height: 44px;
  font-size: 16px;
  background: transparent;
  outline: none;
}

.search-panel__close {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: none;
  background: rgba(47, 52, 55, 0.1);
  font-size: 20px;
  cursor: pointer;
}

.search-panel__filters {
  display: flex;
  gap: 12px;
  margin: 18px 0;
  flex-wrap: wrap;
}

.filter-chip {
  border-radius: 999px;
  border: 1px solid rgba(47, 52, 55, 0.15);
  background: rgba(255, 255, 255, 0.9);
  padding: 6px 16px;
  font-size: 13px;
  cursor: pointer;
}

.search-panel__results {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.search-panel__section {
  margin: 0;
  font-size: 13px;
  letter-spacing: 0.04em;
  color: #a5a3a1;
  text-transform: uppercase;
}

.search-result {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 14px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid transparent;
  cursor: pointer;
}

.search-result:hover {
  border-color: rgba(47, 52, 55, 0.18);
}

.result-title {
  margin: 0;
  font-weight: 600;
}

.result-meta {
  margin: 4px 0 0;
  font-size: 12px;
  color: #8a8f95;
}

.result-date {
  font-size: 12px;
  color: #8a8f95;
}

.search-panel__empty {
  text-align: center;
  color: #8a8f95;
  margin-top: 16px;
}

</style>



<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
// @ts-ignore
import FolderTree, { type TreeNode } from '../components/FolderTree.vue'
// @ts-ignore
import NoteEditor from '../components/NoteEditor.vue'
import { useNoteStore } from '../stores/noteStore'
import type { FolderNode } from '../api/folder'
import type { NoteSummary } from '../api/note'
import { sendAiChat, type AiMessage } from '../api/ai'

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

type AiConversationMessage = {
  id: string
  role: AiMessage['role']
  content: string
  timestamp: number
}

type AiSession = {
  id: string
  title: string
  createdAt: number
  messages: AiConversationMessage[]
}

const AI_STORAGE_KEY = 'mido-ai-sessions'

const readAiSessions = (): AiSession[] => {
  const raw = localStorage.getItem(AI_STORAGE_KEY)
  if (!raw) return []
  try {
    const parsed = JSON.parse(raw) as AiSession[]
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

const aiSessions = ref<AiSession[]>(readAiSessions())
const activeAiSessionId = ref<string | null>(aiSessions.value[0]?.id ?? null)
const aiInput = ref('')
const aiLoading = ref(false)
const isAiMode = ref(false)
const aiSystemPrompt =
  '你是 Mido AI，一位友好且高效的创作助手。回答要使用简体中文，保持专业又有温度的语气。'
const isFolderTreeCollapsed = ref(false)
const isAiSessionCollapsed = ref(false)

const createId = () => `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`

const saveAiSessions = () => {
  localStorage.setItem(AI_STORAGE_KEY, JSON.stringify(aiSessions.value))
}

watch(
  aiSessions,
  () => {
    saveAiSessions()
  },
  { deep: true },
)

const currentAiSession = computed<AiSession | null>(() => {
  if (!activeAiSessionId.value) {
    return null
  }
  return aiSessions.value.find((session) => session.id === activeAiSessionId.value) ?? null
})

const currentAiMessages = computed<AiConversationMessage[]>(() => currentAiSession.value?.messages ?? [])

const startNewAiSession = () => {
  const session: AiSession = {
    id: createId(),
    title: '新的对话',
    createdAt: Date.now(),
    messages: [],
  }
  aiSessions.value = [session, ...aiSessions.value]
  activeAiSessionId.value = session.id
  isAiMode.value = true
  return session
}

const selectAiSession = (id: string) => {
  activeAiSessionId.value = id
  isAiMode.value = true
}

const deleteAiSession = (id: string) => {
  const index = aiSessions.value.findIndex((session) => session.id === id)
  if (index === -1) return
  aiSessions.value.splice(index, 1)
  if (activeAiSessionId.value === id) {
    activeAiSessionId.value = aiSessions.value[0]?.id ?? null
    if (!activeAiSessionId.value) {
      isAiMode.value = false
    }
  }
}

const fallbackAiTitle = (text: string) => {
  const clean = text.replace(/[\s\r\n]+/g, '').trim()
  if (!clean) return '新的对话'
  return clean.slice(0, 8)
}

const aiTitleSystemPrompt =
  '你是标题助手，请将用户的原始问题概括为不超过8个汉字的标题，只输出标题本身，不要额外说明。'

const summarizeQuestionTitle = async (question: string) => {
  try {
    const reply = await sendAiChat([
      { role: 'system', content: aiTitleSystemPrompt },
      { role: 'user', content: question },
    ])
    const clean = reply.replace(/["'“”。，,!?！？\s]/g, '').trim().slice(0, 8)
    return clean || fallbackAiTitle(question)
  } catch (err) {
    return fallbackAiTitle(question)
  }
}

const ensureAiSession = (): AiSession => {
  if (currentAiSession.value) {
    return currentAiSession.value
  }
  return startNewAiSession()
}

const openAiPanel = () => {
  isAiMode.value = true
  if (!currentAiSession.value) {
    startNewAiSession()
  }
}

const exitAiMode = () => {
  isAiMode.value = false
}

const toggleFolderTreeSection = () => {
  isFolderTreeCollapsed.value = !isFolderTreeCollapsed.value
}

const toggleAiSessionSection = () => {
  isAiSessionCollapsed.value = !isAiSessionCollapsed.value
}

const handleSendAiMessage = async () => {
  const content = aiInput.value.trim()
  if (!content || aiLoading.value) return
  const session = ensureAiSession()
  const shouldSummarizeTitle = session.messages.length === 0
  if (!isAiMode.value) {
    isAiMode.value = true
  }
  const userMessage: AiConversationMessage = {
    id: createId(),
    role: 'user',
    content,
    timestamp: Date.now(),
  }
  session.messages.push(userMessage)
  aiInput.value = ''
  aiLoading.value = true
  try {
    const payload: AiMessage[] = [
      { role: 'system', content: aiSystemPrompt },
      ...session.messages.map(({ role, content: msgContent }) => ({ role, content: msgContent })),
    ]
    const answerPromise = sendAiChat(payload)
    const titlePromise = shouldSummarizeTitle ? summarizeQuestionTitle(content) : null
    const answer = await answerPromise
    session.messages.push({
      id: createId(),
      role: 'assistant',
      content: answer,
      timestamp: Date.now(),
    })
    if (titlePromise) {
      session.title = await titlePromise
    }
  } catch (error) {
    session.messages.push({
      id: createId(),
      role: 'assistant',
      content: '抱歉，Mido AI 暂时无法响应，请稍后重试。',
      timestamp: Date.now(),
    })
  } finally {
    aiLoading.value = false
  }
}

const formatAiSessionDate = (timestamp: number) => {
  const date = new Date(timestamp)
  return `${date.getMonth() + 1}/${date.getDate()}`
}

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
  exitAiMode()
  await noteStore.selectFolder(id)
}

const handleSelectNote = async (id: string) => {
  exitAiMode()
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
    openAiPanel()
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
        <div class="workspace-panel__section folder-section" :class="{ expanded: !isFolderTreeCollapsed }">
          <div class="collapsible-header">
            <p class="section-label">内容目录</p>
            <button class="collapse-toggle" type="button" @click="toggleFolderTreeSection">
              {{ isFolderTreeCollapsed ? '展开' : '收起' }}
            </button>
          </div>
          <FolderTree
            v-show="!isFolderTreeCollapsed"
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
        </div>
        <div class="workspace-panel__section ai-session-section">
          <div class="ai-session-header">
            <p class="section-label">Mido AI 对话</p>
            <div class="ai-session-actions">
              <button class="ai-session-new" type="button" @click="startNewAiSession">+ 新对话</button>
              <button class="collapse-toggle" type="button" @click="toggleAiSessionSection">
                {{ isAiSessionCollapsed ? '展开' : '收起' }}
              </button>
            </div>
          </div>
          <transition name="section-fade">
            <div v-show="!isAiSessionCollapsed">
              <p v-if="aiSessions.length === 0" class="ai-session-placeholder">
                还没有对话，点击上方按钮开启一次灵感碰撞。
              </p>
              <ul v-else class="ai-session-list">
                <li
                  v-for="session in aiSessions"
                  :key="session.id"
                  :class="['ai-session-item', { active: session.id === activeAiSessionId && isAiMode }]"
                  @click="selectAiSession(session.id)"
                >
                  <div class="ai-session-item__title">{{ session.title }}</div>
                  <div class="ai-session-item__meta">
                    <span>{{ formatAiSessionDate(session.createdAt) }}</span>
                    <button type="button" class="ai-session-delete" @click.stop="deleteAiSession(session.id)">×</button>
                  </div>
                </li>
              </ul>
            </div>
          </transition>
        </div>
      </aside>
      <div
        v-if="!isSidebarCollapsed"
        class="workspace-panel__resizer"
        @mousedown.prevent="startSidebarResizing"
      ></div>
    </div>
    <main class="workspace-main">
      <section class="note-workspace">
        <div v-if="isAiMode" class="ai-panel">
          <div v-if="currentAiMessages.length" class="ai-conversation">
            <div
              v-for="message in currentAiMessages"
              :key="message.id"
              :class="['ai-message', message.role]"
            >
              <div class="ai-message__avatar">
                {{ message.role === 'assistant' ? '✨' : userDisplayName.charAt(0).toUpperCase() }}
              </div>
              <div class="ai-message__bubble">
                <p>{{ message.content }}</p>
              </div>
            </div>
          </div>
          <div v-else class="ai-hero">
            <div class="ai-hero__avatar">🍎</div>
            <h2>甜你心，知你意。</h2>
            <p>询问、搜索或制作任何内容...</p>
          </div>
          <form class="ai-input-card" @submit.prevent="handleSendAiMessage">
            <button class="ai-meta-button" type="button">@ 添加背景信息</button>
            <textarea
              v-model="aiInput"
              :disabled="aiLoading"
              rows="4"
              placeholder="询问、搜索或制作任何内容..."
            ></textarea>
            <div class="ai-input-footer">
              <div class="ai-input-hints">
                <span>自动</span>
                <span>👓</span>
                <span>全部信息源</span>
              </div>
              <button class="ai-submit" type="submit" :disabled="aiLoading || !aiInput.trim()">
                {{ aiLoading ? '思考中…' : '发送' }}
              </button>
            </div>
          </form>
        </div>
        <div v-else class="note-editor-wrapper">
          <NoteEditor
            :note="noteStore.state.selectedNote"
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
  overflow-y: auto;
  overflow-x: hidden;
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

.collapsible-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.collapse-toggle {
  border: none;
  background: rgba(47, 52, 55, 0.08);
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 12px;
  color: #5c5e62;
  cursor: pointer;
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

.ai-session-section {
  border-top: 1px solid rgba(47, 52, 55, 0.08);
  padding-top: 18px;
  margin-top: 0;
  flex-shrink: 0;
  order: 2;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.ai-session-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.ai-session-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.ai-session-new {
  border: none;
  background: transparent;
  color: #5c5e62;
  font-size: 12px;
  cursor: pointer;
}

.ai-session-placeholder {
  margin: 4px 0 0;
  font-size: 13px;
  color: #8a8f95;
}

.ai-session-section .section-fade > div {
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: visible;
}

.ai-session-list {
  list-style: none;
  margin: 10px 0 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
  overflow: visible;
}

.ai-session-item {
  padding: 8px;
  border-radius: 10px;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.65);
  border: 1px solid transparent;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.ai-session-item.active {
  border-color: rgba(47, 52, 55, 0.18);
  background: #fff;
}

.ai-session-item__title {
  font-size: 13px;
  color: #2f3437;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ai-session-item__meta {
  font-size: 12px;
  color: #a5a3a1;
  display: flex;
  align-items: center;
  gap: 6px;
}

.ai-session-delete {
  border: none;
  background: transparent;
  color: #a5a3a1;
  cursor: pointer;
  font-size: 16px;
  line-height: 1;
}

.folder-tree-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: visible;
}

.folder-section {
  display: flex;
  flex-direction: column;
  min-height: 0;
  transition: flex 0.2s ease;
  order: 1;
  flex-shrink: 0;
}

.section-fade-enter-active,
.section-fade-leave-active {
  transition: opacity 0.3s cubic-bezier(0.4, 0, 0.2, 1), transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.section-fade-enter-from,
.section-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

.section-fade-enter-to,
.section-fade-leave-from {
  opacity: 1;
  transform: translateY(0);
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

.ai-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.ai-conversation {
  flex: 1;
  overflow-y: auto;
  padding-right: 8px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.ai-message {
  display: flex;
  gap: 12px;
}

.ai-message.user {
  flex-direction: row-reverse;
}

.ai-message__avatar {
  width: 40px;
  height: 40px;
  border-radius: 999px;
  background: #f2f4f8;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.ai-message.user .ai-message__avatar {
  background: #2f3437;
  color: #fff;
}

.ai-message__bubble {
  padding: 14px 18px;
  border-radius: 20px;
  background: #f5f7f9;
  color: #2f3437;
  max-width: 640px;
  line-height: 1.6;
}

.ai-message.user .ai-message__bubble {
  background: #2f3437;
  color: #fff;
}

.ai-hero {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  text-align: center;
  color: #2f3437;
}

.ai-hero__avatar {
  width: 96px;
  height: 96px;
  border-radius: 999px;
  background: #fff;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 42px;
}

.ai-input-card {
  border: 1px solid rgba(47, 52, 55, 0.12);
  border-radius: 24px;
  padding: 18px 22px;
  background: #fff;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.ai-meta-button {
  border: none;
  background: rgba(47, 52, 55, 0.06);
  border-radius: 999px;
  padding: 6px 14px;
  font-size: 13px;
  color: #5c5e62;
  align-self: flex-start;
  cursor: pointer;
}

.ai-input-card textarea {
  border: none;
  resize: none;
  font-size: 16px;
  font-family: inherit;
  outline: none;
  min-height: 96px;
}

.ai-input-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.ai-input-hints {
  display: flex;
  gap: 12px;
  font-size: 13px;
  color: #8a8f95;
}

.ai-submit {
  border: none;
  border-radius: 999px;
  background: #2f3437;
  color: #fff;
  padding: 8px 20px;
  font-size: 14px;
  cursor: pointer;
}

.ai-submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
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



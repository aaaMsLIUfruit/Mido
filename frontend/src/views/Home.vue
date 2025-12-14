<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
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
import chatApi, { type ChatResponse, type ChatDetailResponse, type ChatMessageResponse } from '../api/chat'
import chatFolderApi, { type ChatFolderNode } from '../api/chatFolder'
import noteApi from '../api/note'
import { ElMessageBox, ElDialog, ElScrollbar, ElSkeleton, ElTag, ElEmpty } from 'element-plus'

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
  { icon: '🗂️', label: '新建对话文件夹', action: 'create-chat-folder' },
]

const isSearchPanelVisible = ref(false)
const searchKeyword = ref('')
const workspaceSettingsVisible = ref(false)
const userDisplayName = ref('Mido 用户')
const userEmail = ref('未设置邮箱')

// Base64 URL 解码并正确处理 UTF-8 编码的中文字符
const base64UrlDecode = (str: string): string => {
  // 将 base64url 转换为 base64
  let base64 = str.replace(/-/g, '+').replace(/_/g, '/')
  
  // 添加必要的 padding
  while (base64.length % 4) {
    base64 += '='
  }
  
  try {
    // 使用 atob 解码 base64
    const binaryString = atob(base64)
    
    // 将 Latin-1 字符串转换为 UTF-8
    const bytes = new Uint8Array(binaryString.length)
    for (let i = 0; i < binaryString.length; i++) {
      bytes[i] = binaryString.charCodeAt(i)
    }
    
    // 使用 TextDecoder 将 UTF-8 bytes 解码为字符串
    return new TextDecoder('utf-8').decode(bytes)
  } catch (error) {
    console.error('Base64 解码失败:', error)
    throw error
  }
}

// 从 JWT token 中解析用户信息
const loadUserInfo = () => {
  try {
    const token = localStorage.getItem('token')
    console.log('=== 开始加载用户信息 ===')
    console.log('Token 存在:', !!token)
    
    if (token) {
      const parts = token.split('.')
      console.log('Token 部分数量:', parts.length)
      
      if (parts.length >= 2 && parts[1]) {
        try {
          // 使用改进的 base64 解码方法，正确处理中文字符
          const decoded = base64UrlDecode(parts[1])
          console.log('解码后的 payload 字符串:', decoded)
          const payload = JSON.parse(decoded)
          console.log('JWT Payload 对象:', payload)
          console.log('Payload keys:', Object.keys(payload))
          console.log('sub (用户名):', payload.sub)
          console.log('email:', payload.email)
          
          if (payload?.sub) {
            userDisplayName.value = payload.sub
            console.log('✅ 设置用户名:', payload.sub)
            // 同时更新 localStorage
            localStorage.setItem('username', payload.sub)
          } else {
            console.warn('⚠️ Payload 中没有 sub 字段')
          }
          
          if (payload?.email) {
            userEmail.value = payload.email
            console.log('✅ 设置邮箱:', payload.email)
            // 同时更新 localStorage
            localStorage.setItem('email', payload.email)
          } else {
            console.warn('⚠️ Payload 中没有 email 字段')
            userEmail.value = '未设置邮箱'
          }
        } catch (parseError) {
          console.error('解析 payload JSON 失败:', parseError)
          throw parseError
        }
      } else {
        console.warn('⚠️ Token 格式不正确，parts.length:', parts.length)
      }
    } else {
      console.warn('⚠️ 没有找到 token')
    }
    
    console.log('最终用户名:', userDisplayName.value)
    console.log('最终邮箱:', userEmail.value)
    console.log('=== 用户信息加载完成 ===')
  } catch (error) {
    console.error('❌ Failed to parse JWT token:', error)
    // 如果解析失败，尝试从 localStorage 读取
    const storedUsername = localStorage.getItem('username')
    const storedEmail = localStorage.getItem('email')
    console.log('尝试从 localStorage 读取:', { storedUsername, storedEmail })
    
    if (storedUsername) {
      userDisplayName.value = storedUsername
      console.log('从 localStorage 读取用户名:', storedUsername)
    }
    if (storedEmail) {
      userEmail.value = storedEmail
      console.log('从 localStorage 读取邮箱:', storedEmail)
    }
  }
}

// 立即加载用户信息（在组件初始化时）
loadUserInfo()

// 立即加载用户信息（在组件初始化时）
loadUserInfo()

const chatSessions = ref<ChatResponse[]>([])
const activeChatId = ref<string | null>(null)
const currentChatDetail = ref<ChatDetailResponse | null>(null)
const chatLoading = ref(false)
const selectedChatFolderId = ref<string | null>(null)
const chatFolders = ref<ChatFolderNode[]>([])
const loadingChatFolders = ref(false)

const selectedMessageIds = ref<Set<string>>(new Set())
const isSelectingMessages = ref(false)

const aiInput = ref('')
const aiLoading = ref(false)
const isAiMode = ref(false)
const aiSystemPrompt =
  '你是 Mido AI，一位友好且高效的创作助手。回答要使用简体中文，保持专业又有温度的语气。'
const aiContextInfo = ref<string>('') // 背景信息
const contextNoteTitle = ref<string>('') // 选中的笔记标题
const showContextNoteDialog = ref(false) // 显示笔记选择对话框
const contextNoteSearchKeyword = ref('') // 笔记搜索关键词
const allNotesForContext = ref<{ id: string; title: string; folderId?: string; folderName: string }[]>([]) // 所有笔记列表
const loadingContextNotes = ref(false) // 加载笔记中
const isFolderTreeCollapsed = ref(false)
const isAiSessionCollapsed = ref(false)

const currentAiMessages = computed<ChatMessageResponse[]>(() => currentChatDetail.value?.messages ?? [])

// Load chat sessions
const loadChatSessions = async () => {
  chatLoading.value = true
  try {
    const result = await chatApi.fetchChatList({
      folderId: selectedChatFolderId.value ?? undefined,
      page: 1,
      pageSize: 100,
    })
    chatSessions.value = result.list
    if (chatSessions.value.length > 0 && !activeChatId.value) {
      activeChatId.value = chatSessions.value[0]?.id ?? null
      if (activeChatId.value) {
        await loadChatDetail(activeChatId.value)
      }
    }
  } catch (error) {
    ElMessage.error('加载对话列表失败')
  } finally {
    chatLoading.value = false
  }
}

// Load chat detail with messages
const loadChatDetail = async (chatId: string) => {
  try {
    const detail = await chatApi.fetchChatById(chatId)
    if (detail) {
      currentChatDetail.value = detail
      activeChatId.value = chatId
    }
  } catch (error) {
    ElMessage.error('加载对话详情失败')
  }
}

const startNewAiSession = async () => {
  try {
    const newChat = await chatApi.createChat({
      title: '新的对话',
      folderId: selectedChatFolderId.value ?? undefined,
    })
    if (newChat) {
      await loadChatSessions()
      if (newChat.id) {
        activeChatId.value = newChat.id
        isAiMode.value = true
        currentChatDetail.value = { ...newChat, messages: [] }
      }
    }
  } catch (error) {
    ElMessage.error('创建对话失败')
  }
}

const selectAiSession = async (id: string) => {
  activeChatId.value = id
  isAiMode.value = true
  await loadChatDetail(id)
  selectedMessageIds.value.clear()
  isSelectingMessages.value = false
}

const deleteAiSession = async (payload: { id: string }) => {
  const id = payload.id
  try {
    await ElMessageBox.confirm('确定要删除这个对话吗？', '删除对话', {
      type: 'warning',
    })
    await chatApi.deleteChat(id)
    ElMessage.success('对话已删除')
    if (activeChatId.value === id) {
      activeChatId.value = null
      currentChatDetail.value = null
      if (chatSessions.value.length === 0) {
        isAiMode.value = false
      } else {
        activeChatId.value = chatSessions.value[0]?.id ?? null
        if (activeChatId.value) {
          await loadChatDetail(activeChatId.value)
        }
      }
    }
    await loadChatSessions()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('删除对话失败')
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

const openAiPanel = async () => {
  isAiMode.value = true
  if (!activeChatId.value) {
    await startNewAiSession()
  }
}

const exitAiMode = () => {
  isAiMode.value = false
  selectedMessageIds.value.clear()
  isSelectingMessages.value = false
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
  
  // Ensure we have a chat session
  if (!activeChatId.value) {
    await startNewAiSession()
    if (!activeChatId.value) {
      ElMessage.error('创建对话失败')
      return
    }
  }

  const shouldSummarizeTitle = !currentChatDetail.value || currentChatDetail.value.messages.length === 0
  
  // Save user message
  try {
    const userMessage = await chatApi.createChatMessage({
      chatId: activeChatId.value,
      role: 'user',
      content,
    })
    if (userMessage && currentChatDetail.value) {
      currentChatDetail.value.messages.push(userMessage)
    }
  } catch (error) {
    ElMessage.error('保存消息失败')
    return
  }

  aiInput.value = ''
  aiLoading.value = true

  try {
    // Prepare messages for AI
    const messages = currentChatDetail.value?.messages.map(msg => ({
      role: msg.role,
      content: msg.content,
    })) ?? []
    
    // 构建system message，包含基础提示和背景信息
    let systemContent = aiSystemPrompt
    if (aiContextInfo.value) {
      systemContent += `\n\n以下是背景信息，请参考这些信息来回答用户的问题：\n\n${aiContextInfo.value}`
    }
    
    const payload: AiMessage[] = [
      { role: 'system', content: systemContent },
      ...messages,
    ]
    
    const answerPromise = sendAiChat(payload)
    const titlePromise = shouldSummarizeTitle ? summarizeQuestionTitle(content) : null
    const answer = await answerPromise
    
    // Save assistant message
    const assistantMessage = await chatApi.createChatMessage({
      chatId: activeChatId.value,
      role: 'assistant',
      content: answer,
    })
    if (assistantMessage && currentChatDetail.value) {
      currentChatDetail.value.messages.push(assistantMessage)
    }
    
    // Update title if needed
    if (titlePromise && currentChatDetail.value) {
      const newTitle = await titlePromise
      await chatApi.updateChat({
        id: activeChatId.value,
        title: newTitle,
      })
      if (currentChatDetail.value) {
        currentChatDetail.value.title = newTitle
      }
      await loadChatSessions()
    }
  } catch (error) {
    ElMessage.error('AI 响应失败，请稍后重试')
    if (currentChatDetail.value) {
      const errorMessage = await chatApi.createChatMessage({
        chatId: activeChatId.value,
        role: 'assistant',
        content: '抱歉，Mido AI 暂时无法响应，请稍后重试。',
      })
      if (errorMessage) {
        currentChatDetail.value.messages.push(errorMessage)
      }
    }
  } finally {
    aiLoading.value = false
  }
}

// Message selection for saving as note
const toggleMessageSelection = (messageId: string) => {
  if (selectedMessageIds.value.has(messageId)) {
    selectedMessageIds.value.delete(messageId)
  } else {
    selectedMessageIds.value.add(messageId)
  }
}

const toggleSelectMode = () => {
  isSelectingMessages.value = !isSelectingMessages.value
  if (!isSelectingMessages.value) {
    selectedMessageIds.value.clear()
  }
}

const handleSaveSelectedMessagesAsNote = async () => {
  if (selectedMessageIds.value.size === 0) {
    ElMessage.warning('请先选择要保存的消息')
    return
  }

  if (!currentChatDetail.value) return

  const selectedMessages = currentChatDetail.value.messages.filter(msg => 
    selectedMessageIds.value.has(msg.id)
  ).sort((a, b) => {
    const dateA = new Date(a.createdAt).getTime()
    const dateB = new Date(b.createdAt).getTime()
    return dateA - dateB
  })

  // Build note content
  const noteTitle = currentChatDetail.value.title || '保存的对话'
  let noteContent = `# ${noteTitle}\n\n`
  
  selectedMessages.forEach(msg => {
    const roleLabel = msg.role === 'user' ? '用户' : 'AI助手'
    noteContent += `## ${roleLabel}\n\n${msg.content}\n\n---\n\n`
  })

  // 直接保存到根目录（未分类），不询问用户
  try {
    const createdNote = await noteApi.createNote({
      title: noteTitle,
      content: noteContent,
      folderId: undefined, // 保存到根目录
    })

    if (createdNote) {
      ElMessage.success('已保存为笔记')
      selectedMessageIds.value.clear()
      isSelectingMessages.value = false
      // Switch to note editor
      isAiMode.value = false
      await noteStore.selectNote(createdNote.id)
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('保存笔记失败')
    }
  }
}

// 加载所有笔记用于背景信息选择
const loadAllNotesForContext = async () => {
  loadingContextNotes.value = true
  allNotesForContext.value = []
  
  try {
    // 递归收集所有文件夹的笔记
    const collectNotes = async (folders: FolderNode[]) => {
      for (const folder of folders) {
        // 确保该文件夹的笔记已加载
        if (!noteStore.state.notesMap[folder.id]) {
          try {
            const res = await noteApi.fetchNoteList({ folderId: folder.id, page: 1, pageSize: 100 })
            noteStore.state.notesMap[folder.id] = res.list || []
          } catch (error) {
            console.error(`Failed to load notes for folder ${folder.id}:`, error)
          }
        }
        
        const notes = noteStore.state.notesMap[folder.id] || []
        notes.forEach(note => {
          allNotesForContext.value.push({
            id: note.id,
            title: note.title,
            folderId: folder.id,
            folderName: folder.name,
          })
        })
        
        if (folder.children && folder.children.length > 0) {
          await collectNotes(folder.children)
        }
      }
    }
    
    // 也收集根目录的笔记（未分类）
    try {
      const rootFolder = noteStore.state.folders.find((folder) => !folder.parentId && folder.name === '未分类')
      if (rootFolder) {
        if (!noteStore.state.notesMap[rootFolder.id]) {
          const res = await noteApi.fetchNoteList({ folderId: rootFolder.id, page: 1, pageSize: 100 })
          noteStore.state.notesMap[rootFolder.id] = res.list || []
        }
        const notes = noteStore.state.notesMap[rootFolder.id] || []
        notes.forEach(note => {
          allNotesForContext.value.push({
            id: note.id,
            title: note.title,
            folderId: rootFolder.id,
            folderName: '未分类',
          })
        })
      }
    } catch (error) {
      console.error('Failed to load root notes:', error)
    }
    
    await collectNotes(noteStore.state.folders)
  } finally {
    loadingContextNotes.value = false
  }
}

// 打开笔记选择对话框
const handleSelectContextNote = async () => {
  contextNoteSearchKeyword.value = ''
  await loadAllNotesForContext()
  if (allNotesForContext.value.length === 0) {
    ElMessage.warning('没有可用的笔记')
    return
  }
  showContextNoteDialog.value = true
}

// 确认选择笔记作为背景信息
const confirmContextNote = async (noteId: string) => {
  try {
    const noteDetail = await noteApi.fetchNoteById(noteId)
    if (noteDetail) {
      // 提取纯文本内容（如果是HTML，去掉标签）
      let content = noteDetail.content
      // 如果包含HTML标签，提取文本内容
      if (/<[a-z]+[^>]*>/i.test(content)) {
        const div = document.createElement('div')
        div.innerHTML = content
        content = div.textContent || div.innerText || ''
      }
      aiContextInfo.value = content
      contextNoteTitle.value = noteDetail.title
      showContextNoteDialog.value = false
      ElMessage.success(`已加载背景信息：${noteDetail.title}`)
    } else {
      ElMessage.error('获取笔记内容失败')
    }
  } catch (error) {
    ElMessage.error('加载笔记内容失败')
  }
}

// 过滤笔记列表
const filteredContextNotes = computed(() => {
  if (!contextNoteSearchKeyword.value) {
    return allNotesForContext.value
  }
  const keyword = contextNoteSearchKeyword.value.toLowerCase()
  return allNotesForContext.value.filter(note => 
    note.title.toLowerCase().includes(keyword) || 
    note.folderName.toLowerCase().includes(keyword)
  )
})

// Chat folder management
const loadChatFolders = async () => {
  loadingChatFolders.value = true
  try {
    chatFolders.value = await chatFolderApi.fetchChatFolderTree()
  } catch (error) {
    ElMessage.error('加载对话文件夹失败')
  } finally {
    loadingChatFolders.value = false
  }
}

const handleCreateChatFolder = async (payload: { parentId?: string; name: string; parentIsDefaultRoot?: boolean }) => {
  try {
    await chatFolderApi.createChatFolder({ name: payload.name, parentId: payload.parentId })
    ElMessage.success('文件夹已创建')
    await loadChatFolders()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '创建文件夹失败')
  }
}

const handleRenameChatFolder = async (payload: { id: string; name: string }) => {
  try {
    await chatFolderApi.renameChatFolder(payload)
    ElMessage.success('文件夹已重命名')
    await loadChatFolders()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '重命名文件夹失败')
  }
}

const handleMoveChatFolder = async (payload: { id: string; newParentId?: string }) => {
  try {
    await chatFolderApi.moveChatFolder(payload)
    ElMessage.success('文件夹已移动')
    await loadChatFolders()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '移动文件夹失败')
  }
}

const handleDeleteChatFolder = async (payload: { id: string }) => {
  const id = payload.id
  try {
    await ElMessageBox.confirm('删除文件夹将同时删除其下所有对话，是否继续？', '删除文件夹', {
      type: 'warning',
    })
    await chatFolderApi.deleteChatFolder(id)
    ElMessage.success('文件夹已删除')
    await loadChatFolders()
    if (selectedChatFolderId.value === id) {
      selectedChatFolderId.value = null
    }
    await loadChatSessions()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '删除文件夹失败')
    }
  }
}

const handleSelectChatFolder = async (id: string | null) => {
  if (!id) {
    selectedChatFolderId.value = null
    await loadChatSessions()
    return
  }
  
  // Check if it's a chat ID (starts with 'chat-')
  if (id.startsWith('chat-')) {
    await selectAiSession(id.replace('chat-', ''))
    return
  }
  
  // It's a folder ID - don't do anything, just let the tree expand/collapse naturally
  // The folder structure is already shown in the tree, we don't need to filter
}

// 监听 localStorage 中 token 的变化
if (typeof window !== 'undefined') {
  window.addEventListener('storage', (e) => {
    if (e.key === 'token') {
      console.log('检测到 token 变化，重新加载用户信息')
      loadUserInfo()
    }
  })
}

onMounted(async () => {
  // 确保在挂载时重新加载用户信息
  loadUserInfo()
  await noteStore.init()
  await loadChatFolders()
  await loadChatSessions()
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

const chatFolderTreeWithChats = computed<TreeNode[]>(() => {
  const build = (folders: ChatFolderNode[]): TreeNode[] =>
    folders.map((folder) => {
      const folderChats = chatSessions.value.filter(chat => chat.folderId === folder.id)
      const childFolders = folder.children ? build(folder.children) : []
      const children = [
        ...childFolders,
        ...folderChats.map((chat) => ({
          treeKey: `chat-${chat.id}`,
          id: String(chat.id),
          name: chat.title || '未命名对话',
          type: 'note' as const, // Use 'note' type so FolderTree can handle it
        })),
      ]
      return {
        treeKey: `folder-${folder.id}`,
        id: String(folder.id),
        name: folder.name,
        type: 'folder' as const,
        children: children.length > 0 ? children : [], // Keep empty array for empty folders
      }
    })

  const rootChats = chatSessions.value.filter(chat => !chat.folderId)
  const tree = build(chatFolders.value)
  
  // Add root chats if any
  if (rootChats.length > 0 || tree.length === 0) {
    return [
      ...tree,
      ...rootChats.map((chat) => ({
        treeKey: `chat-${chat.id}`,
        id: String(chat.id),
        name: chat.title || '未命名对话',
        type: 'note' as const,
      })),
    ]
  }

  return tree
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
    ElMessage.error('"未分类"下不能创建文件夹')
    return
  }
  try {
    // 如果指定了 parentId，验证它是否存在
    let validParentId: string | undefined = payload.parentId
    if (validParentId) {
      const targetId = validParentId
      const folderExists = noteStore.state.folders.some(f => 
        String(f.id) === targetId || 
        (f.children && findFolderInTree(f.children, targetId))
      )
      if (!folderExists) {
        console.warn('指定的父文件夹不存在，将创建为根文件夹')
        validParentId = undefined
      }
    }
    await noteStore.createFolder(payload.name, validParentId)
  } catch (error: any) {
    // 如果后端返回"父文件夹不存在"错误，尝试创建为根文件夹
    if (error?.response?.data?.message?.includes('父文件夹不存在')) {
      try {
        await noteStore.createFolder(payload.name, undefined)
      } catch (retryError) {
        ElMessage.error('创建失败，请稍后重试')
      }
    } else {
      ElMessage.error(error?.response?.data?.message || '创建失败，请稍后重试')
    }
  }
}

// 辅助函数：在文件夹树中查找文件夹
const findFolderInTree = (folders: FolderNode[], targetId: string): boolean => {
  for (const folder of folders) {
    if (String(folder.id) === targetId) {
      return true
    }
    if (folder.children && findFolderInTree(folder.children, targetId)) {
      return true
    }
  }
  return false
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
        ElMessage.error('"未分类"下不能创建文件夹')
        return
      }
      // 验证选中的文件夹是否存在
      let validParentId: string | undefined = noteStore.state.selectedFolderId ?? undefined
      if (validParentId) {
        const targetId = validParentId
        const folderExists = noteStore.state.folders.some(f => 
          String(f.id) === targetId || 
          (f.children && findFolderInTree(f.children, targetId))
        )
        if (!folderExists) {
          console.warn('选中的文件夹不存在，将创建为根文件夹')
          validParentId = undefined
        }
      }
      await noteStore.createFolder(name, validParentId)
    } else {
      await noteStore.createNote({
        title: name,
        content: '# 新笔记',
        folderId: noteStore.state.selectedFolderId ?? undefined,
      })
    }
  } catch (error: any) {
    // 如果后端返回"父文件夹不存在"错误，尝试创建为根文件夹
    if (type === 'folder' && error?.response?.data?.message?.includes('父文件夹不存在')) {
      try {
        await noteStore.createFolder(name, undefined)
      } catch (retryError) {
        ElMessage.error('创建失败，请稍后重试')
      }
    } else {
      ElMessage.error(error?.response?.data?.message || '创建失败，请稍后重试')
    }
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

// AI 对话拖拽移动到对话文件夹
const handleMoveChat = async (payload: { id: string; targetFolderId?: string }) => {
  try {
    await chatApi.updateChat({
      id: payload.id,
      folderId: payload.targetFolderId,
    })

    // 同步本地状态，避免 UI 闪烁
    const target = chatSessions.value.find((c) => c.id === payload.id)
    if (target) {
      target.folderId = payload.targetFolderId
    }
    if (currentChatDetail.value?.id === payload.id) {
      currentChatDetail.value.folderId = payload.targetFolderId
    }

    ElMessage.success('对话已移动')
    await loadChatSessions()
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '移动对话失败')
  }
}

const openSearchPanel = () => {
  isSearchPanelVisible.value = true
}

const closeSearchPanel = () => {
  isSearchPanelVisible.value = false
  searchKeyword.value = ''
}

const handleNavClick = async (item: { label: string; action?: string }) => {
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
  if (item.action === 'create-chat-folder') {
    const name = generateDefaultName('folder')
    await handleCreateChatFolder({ name, parentId: selectedChatFolderId.value ?? undefined })
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
          <transition name="section-fade">
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
          </transition>
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
              <!-- Chat folder tree with chats -->
              <FolderTree
                class="folder-tree-wrapper chat-tree-wrapper"
                :data="chatFolderTreeWithChats"
                :current-key="activeChatId ? `chat-${activeChatId}` : (selectedChatFolderId ? `folder-${selectedChatFolderId}` : null)"
                :loading="loadingChatFolders || chatLoading"
                :show-toolbar="false"
                @select-folder="handleSelectChatFolder"
                @create-folder="handleCreateChatFolder"
                @rename-folder="handleRenameChatFolder"
                @delete-folder="handleDeleteChatFolder"
                @move-folder="handleMoveChatFolder"
                @select-note="selectAiSession"
                @delete-note="deleteAiSession"
                @move-note="handleMoveChat"
              />
              <p v-if="chatFolderTreeWithChats.length === 0 && !loadingChatFolders && !chatLoading" class="ai-session-placeholder">
                还没有对话，点击上方按钮开启一次灵感碰撞。
              </p>
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
            <div class="ai-conversation-toolbar" v-if="isSelectingMessages || selectedMessageIds.size > 0">
              <button class="ai-toolbar-button" @click="toggleSelectMode">
                {{ isSelectingMessages ? '取消选择' : '选择消息' }}
              </button>
              <button 
                v-if="selectedMessageIds.size > 0" 
                class="ai-toolbar-button ai-toolbar-button--primary" 
                @click="handleSaveSelectedMessagesAsNote"
              >
                保存为笔记 ({{ selectedMessageIds.size }})
              </button>
            </div>
            <div
              v-for="message in currentAiMessages"
              :key="message.id"
              :class="['ai-message', message.role, { 'ai-message--selected': selectedMessageIds.has(message.id) }]"
              @click="isSelectingMessages ? toggleMessageSelection(message.id) : null"
            >
              <div v-if="isSelectingMessages" class="ai-message__checkbox">
                <input 
                  type="checkbox" 
                  :checked="selectedMessageIds.has(message.id)"
                  @click.stop="toggleMessageSelection(message.id)"
                />
              </div>
              <div class="ai-message__avatar">
                {{ message.role === 'assistant' ? '✨' : userDisplayName.charAt(0).toUpperCase() }}
              </div>
              <div class="ai-message__bubble">
                <div v-if="message.role === 'assistant'" class="ai-message__markdown">
                  <v-md-preview :text="message.content"></v-md-preview>
                </div>
                <p v-else>{{ message.content }}</p>
              </div>
            </div>
          </div>
          <div v-else class="ai-hero">
            <div class="ai-hero__avatar">🍎</div>
            <h2>甜你心，知你意。</h2>
            <p>询问、搜索或制作任何内容...</p>
            <button 
              v-if="currentAiMessages.length > 0" 
              class="ai-hero-button" 
              @click="toggleSelectMode"
            >
              选择消息
            </button>
          </div>
          <form class="ai-input-card" @submit.prevent="handleSendAiMessage">
            <button class="ai-meta-button" type="button" @click="handleSelectContextNote">
              @ 添加背景信息{{ contextNoteTitle ? ` (${contextNoteTitle})` : '' }}
            </button>
            <textarea
              v-model="aiInput"
              :disabled="aiLoading"
              rows="4"
              placeholder="询问、搜索或制作任何内容..."
            ></textarea>
            <div class="ai-input-footer">
              <div class="ai-input-hints">
                <button 
                  type="button" 
                  class="ai-hint-button"
                  @click="toggleSelectMode"
                >
                  {{ isSelectingMessages ? '取消选择' : '选择消息' }}
                </button>
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

    <!-- 笔记选择对话框 -->
    <el-dialog
      v-model="showContextNoteDialog"
      title="选择背景信息"
      width="600px"
      :close-on-click-modal="false"
    >
      <div class="context-note-selector">
        <el-input
          v-model="contextNoteSearchKeyword"
          placeholder="搜索笔记标题或文件夹..."
          clearable
          prefix-icon="Search"
          style="margin-bottom: 16px"
        />
        <el-scrollbar height="400px">
          <el-skeleton v-if="loadingContextNotes" :rows="5" animated />
          <div v-else-if="filteredContextNotes.length === 0" class="empty-notes">
            <el-empty description="没有找到笔记" />
          </div>
          <div v-else class="note-list-items">
            <div
              v-for="note in filteredContextNotes"
              :key="note.id"
              class="note-item-card"
              @click="confirmContextNote(note.id)"
            >
              <div class="note-item-card__title">{{ note.title }}</div>
              <div class="note-item-card__meta">
                <el-tag size="small" type="info">{{ note.folderName }}</el-tag>
              </div>
            </div>
          </div>
        </el-scrollbar>
      </div>
      <template #footer>
        <el-button @click="showContextNoteDialog = false">取消</el-button>
      </template>
    </el-dialog>

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

.chat-tree-wrapper {
  max-height: 400px;
  overflow-y: auto;
  margin-top: 8px;
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

.ai-conversation-toolbar {
  display: flex;
  gap: 8px;
  padding: 12px;
  background: rgba(47, 52, 55, 0.05);
  border-radius: 12px;
  margin-bottom: 16px;
  align-items: center;
}

.ai-toolbar-button {
  border: none;
  background: rgba(47, 52, 55, 0.1);
  border-radius: 8px;
  padding: 6px 12px;
  font-size: 13px;
  color: #2f3437;
  cursor: pointer;
  transition: background 0.15s ease;
}

.ai-toolbar-button:hover {
  background: rgba(47, 52, 55, 0.15);
}

.ai-toolbar-button--primary {
  background: #2f3437;
  color: #fff;
  margin-left: auto;
}

.ai-toolbar-button--primary:hover {
  background: #1a1d20;
}

.ai-message--selected {
  background: rgba(47, 52, 55, 0.08);
  border-radius: 12px;
  padding: 8px;
  margin: -8px;
}

.ai-message__checkbox {
  display: flex;
  align-items: center;
  padding-right: 8px;
}

.ai-message__checkbox input[type="checkbox"] {
  width: 18px;
  height: 18px;
  cursor: pointer;
}

.ai-message__markdown {
  width: 100%;
}

.ai-message__markdown :deep(.v-md-pre-wrapper) {
  background: transparent;
}

.ai-hint-button {
  border: none;
  background: transparent;
  color: #5c5e62;
  font-size: 13px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: background 0.15s ease;
}

.ai-hint-button:hover {
  background: rgba(47, 52, 55, 0.08);
}

.ai-hero-button {
  margin-top: 16px;
  border: 1px solid rgba(47, 52, 55, 0.2);
  background: #fff;
  border-radius: 12px;
  padding: 10px 20px;
  font-size: 14px;
  color: #2f3437;
  cursor: pointer;
  transition: all 0.15s ease;
}

.ai-hero-button:hover {
  background: #f5f7f9;
  border-color: rgba(47, 52, 55, 0.3);
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

/* 笔记选择对话框样式 */
.context-note-selector {
  padding: 8px 0;
}

.note-list-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.note-item-card {
  padding: 12px 16px;
  border: 1px solid rgba(47, 52, 55, 0.12);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  background: #fff;
}

.note-item-card:hover {
  border-color: #3b82f6;
  background: #f8fafc;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.1);
}

.note-item-card__title {
  font-size: 15px;
  font-weight: 500;
  color: #2f3437;
  margin-bottom: 6px;
}

.note-item-card__meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.empty-notes {
  padding: 40px 0;
}

</style>



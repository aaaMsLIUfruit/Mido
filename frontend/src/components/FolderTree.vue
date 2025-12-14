<script setup lang="ts">
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

export interface TreeNode {
  treeKey: string
  id: string
  name: string
  type: 'folder' | 'note'
  children?: TreeNode[]
  meta?: Record<string, any>
}

const props = defineProps<{
  data: TreeNode[]
  currentKey: string | null
  loading?: boolean
  showToolbar?: boolean
}>()

const emit = defineEmits<{
  (e: 'select-folder', id: string | null): void
  (e: 'select-note', id: string): void
  (e: 'create-folder', payload: { parentId?: string; name: string; parentIsDefaultRoot?: boolean }): void
  (e: 'rename-folder', payload: { id: string; name: string }): void
  (e: 'delete-folder', payload: { id: string }): void
  (e: 'rename-note', payload: { id: string; name: string }): void
  (e: 'delete-note', payload: { id: string }): void
  (e: 'move-folder', payload: { id: string; newParentId?: string }): void
  (e: 'move-note', payload: { id: string; targetFolderId?: string }): void
}>()

const filterText = ref('')
const treeRef = ref()
const showToolbar = computed(() => props.showToolbar !== false)

const contextMenu = reactive({
  visible: false,
  x: 0,
  y: 0,
  node: null as TreeNode | null,
})

const editingFolderId = ref<string | null>(null)
const editingFolderName = ref('')
const editingFolderOriginalName = ref('')
const editingNoteId = ref<string | null>(null)
const editingNoteName = ref('')
const editingNoteOriginalName = ref('')
const editingNoteInputRef = ref<HTMLInputElement>()
const editingFolderInputRef = ref<HTMLInputElement>()

watch(filterText, (val) => treeRef.value?.filter(val))

const filterNode = (value: string, data: TreeNode) => {
  if (!value) return true
  return data.name.toLowerCase().includes(value.toLowerCase())
}

const handleNodeClickWrapper = (data: TreeNode) => {
  if (data.type === 'note') {
    emit('select-note', data.id)
  } else {
    emit('select-folder', data.id)
  }
}

const openContextMenu = (event: MouseEvent, data: TreeNode) => {
  event.preventDefault()
  contextMenu.visible = true
  contextMenu.node = data
  contextMenu.x = event.clientX
  contextMenu.y = event.clientY
  document.addEventListener('click', closeContextMenu, { once: true })
}

const handleContextMenuWrapper = (event: MouseEvent, data: TreeNode) => {
  openContextMenu(event, data)
}

const closeContextMenu = () => {
  contextMenu.visible = false
  contextMenu.node = null
}

const promptName = async (defaultValue = '', title = '新建文件夹') => {
  const { value } = await ElMessageBox.prompt('请输入名称', title, {
    inputValue: defaultValue,
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputPattern: /\S+/,
    inputErrorMessage: '名称不能为空',
  })
  return value.trim()
}

const createFolder = async (parentId?: string, parentIsDefaultRoot = false) => {
  if (parentIsDefaultRoot) {
    ElMessage.error('“未分类”下不能创建文件夹')
    closeContextMenu()
    return
  }
  const name = await promptName()
  emit('create-folder', { parentId, name, parentIsDefaultRoot })
  closeContextMenu()
}

const startInlineFolderRename = () => {
  if (!contextMenu.node || contextMenu.node.type !== 'folder') return
  editingFolderId.value = contextMenu.node.id
  editingFolderName.value = contextMenu.node.name
  editingFolderOriginalName.value = contextMenu.node.name
  closeContextMenu()
  nextTick(() => {
    editingFolderInputRef.value?.focus()
    editingFolderInputRef.value?.select()
  })
}

const commitFolderRename = () => {
  if (!editingFolderId.value) return
  const newName = editingFolderName.value.trim()
  if (!newName) {
    ElMessage.error('名称不能为空')
    editingFolderName.value = editingFolderOriginalName.value
    nextTick(() => editingFolderInputRef.value?.focus())
    return
  }
  if (newName !== editingFolderOriginalName.value) {
    emit('rename-folder', { id: editingFolderId.value, name: newName })
  }
  editingFolderId.value = null
  editingFolderName.value = ''
  editingFolderOriginalName.value = ''
}

const cancelFolderRename = () => {
  editingFolderId.value = null
  editingFolderName.value = ''
  editingFolderOriginalName.value = ''
}

const deleteFolder = () => {
  if (!contextMenu.node || contextMenu.node.type !== 'folder') return
  emit('delete-folder', { id: contextMenu.node.id })
  closeContextMenu()
}

const startInlineNoteRename = () => {
  if (!contextMenu.node || contextMenu.node.type !== 'note') return
  editingNoteId.value = contextMenu.node.id
  editingNoteName.value = contextMenu.node.name
  editingNoteOriginalName.value = contextMenu.node.name
  closeContextMenu()
  nextTick(() => {
    editingNoteInputRef.value?.focus()
    editingNoteInputRef.value?.select()
  })
}

const commitNoteRename = () => {
  if (!editingNoteId.value) {
    return
  }
  const newName = editingNoteName.value.trim()
  if (!newName) {
    ElMessage.error('标题不能为空')
    editingNoteName.value = editingNoteOriginalName.value
    nextTick(() => editingNoteInputRef.value?.focus())
    return
  }
  if (newName !== editingNoteOriginalName.value) {
    emit('rename-note', { id: editingNoteId.value, name: newName })
  }
  editingNoteId.value = null
  editingNoteName.value = ''
  editingNoteOriginalName.value = ''
}

const cancelNoteRename = () => {
  editingNoteId.value = null
  editingNoteName.value = ''
  editingNoteOriginalName.value = ''
}

const deleteNote = () => {
  if (!contextMenu.node || contextMenu.node.type !== 'note') return
  emit('delete-note', { id: contextMenu.node.id })
  closeContextMenu()
}

const allowDrag = (node: any) => Boolean(node.data)

const allowDrop = (draggingNode: any, dropNode: any, type: 'prev' | 'next' | 'inner') => {
  const dragType = draggingNode.data?.type
  const dropType = dropNode?.data?.type
  if (dragType === 'folder') {
    if (!dropNode) {
      return type !== 'inner'
    }
    if (dropType === 'note' && type === 'inner') {
      return false
    }
    return true
  }
  if (dragType === 'note') {
    // Allow dropping into folders
    if (dropNode && dropType === 'folder' && type === 'inner') {
      return true
    }
    // Allow dropping to root (when dropNode is null or at root level)
    if (!dropNode || type !== 'inner') {
      return true
    }
    return false
  }
  return false
}

const handleDrop = (dragNode: any, dropNode: any, dropType: 'before' | 'after' | 'inner') => {
  const dragType = dragNode.data?.type
  const dragId = dragNode.data.id as string

  if (dragType === 'folder') {
    let newParentId: string | undefined
    if (!dropNode) {
      newParentId = undefined
    } else if (dropType === 'inner') {
      if (dropNode.data?.type !== 'folder') return
      newParentId = dropNode.data.id
    } else {
      const parent = dropNode.parent
      newParentId = parent && parent.data?.type === 'folder' ? (parent.data.id as string) : undefined
    }
    emit('move-folder', { id: dragId, newParentId })
    return
  }

  if (dragType === 'note') {
    let targetFolderId: string | undefined
    if (dropNode && dropNode.data?.type === 'folder' && dropType === 'inner') {
      // Dropped into a folder
      targetFolderId = dropNode.data.id as string
    } else if (!dropNode || dropType !== 'inner') {
      // Dropped to root level (before/after another node or to empty area)
      targetFolderId = undefined
    } else {
      return
    }
    emit('move-note', { id: dragId, targetFolderId })
  }
}
</script>

<template>
  <div class="folder-tree">
    <div v-if="showToolbar" class="tree-toolbar">
      <el-input
        v-model="filterText"
        placeholder="搜索文件夹"
        size="small"
        prefix-icon="Search"
      />
      <el-button size="small" type="primary" @click="createFolder()">新建</el-button>
    </div>
    <el-tree
      class="notion-tree"
      ref="treeRef"
      :data="props.data"
      :props="{ children: 'children', label: 'name' }"
      node-key="treeKey"
      highlight-current
      :filter-node-method="filterNode"
      :expand-on-click-node="false"
      :current-node-key="props.currentKey ?? ''"
      :draggable="true"
      :allow-drag="allowDrag"
      :allow-drop="allowDrop"
      empty-text="暂无内容"
      @node-click="handleNodeClickWrapper"
      @node-contextmenu="handleContextMenuWrapper"
      @node-drop="handleDrop"
    >
      <template #default="{ data }">
        <div class="tree-node" :class="data.type">
          <template v-if="data.type === 'folder' && editingFolderId === data.id">
            <input
              ref="editingFolderInputRef"
              v-model="editingFolderName"
              class="inline-input"
              @blur="commitFolderRename"
              @keyup.enter.stop.prevent="commitFolderRename"
              @keyup.esc.stop.prevent="cancelFolderRename"
              @mousedown.stop
              @click.stop
            />
          </template>
          <template v-else-if="data.type === 'note' && editingNoteId === data.id">
            <input
              ref="editingNoteInputRef"
              v-model="editingNoteName"
              class="inline-input"
              @blur="commitNoteRename"
              @keyup.enter.stop.prevent="commitNoteRename"
              @keyup.esc.stop.prevent="cancelNoteRename"
              @mousedown.stop
              @click.stop
            />
          </template>
          <template v-else>
            <span>{{ data.name }}</span>
          </template>
        </div>
      </template>
    </el-tree>

    <transition name="fade">
      <ul
        v-if="contextMenu.visible && contextMenu.node"
        class="context-menu"
        :style="{ top: `${contextMenu.y}px`, left: `${contextMenu.x}px` }"
      >
        <template v-if="contextMenu.node.type === 'folder'">
          <li @click="createFolder(contextMenu.node.id, contextMenu.node.meta?.isDefaultRoot)">新建子文件夹</li>
          <li @click="startInlineFolderRename">重命名</li>
          <li class="danger" @click="deleteFolder">删除文件夹</li>
        </template>
        <template v-else-if="contextMenu.node.type === 'note'">
          <li @click="startInlineNoteRename">重命名</li>
          <li class="danger" @click="deleteNote">删除笔记</li>
        </template>
      </ul>
    </transition>
  </div>
</template>

<style scoped>
.folder-tree {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.tree-toolbar {
  display: flex;
  gap: 8px;
  padding-bottom: 12px;
}

.notion-tree {
  flex: 1;
  overflow: visible;
  background: transparent;
  padding-right: 4px;
}

.notion-tree :deep(.el-tree-node__content) {
  border-radius: 8px;
  padding: 4px 8px;
  color: #2f3437;
}

.notion-tree :deep(.el-tree-node__content:hover) {
  background: rgba(47, 52, 55, 0.08);
}

.notion-tree :deep(.el-tree-node.is-current > .el-tree-node__content) {
  background: rgba(47, 52, 55, 0.13);
  color: #1d1f21;
}

.notion-tree :deep(.el-tree__empty-text) {
  color: #b6b1a5;
  font-size: 13px;
}

.tree-node {
  display: flex;
  align-items: center;
  width: 100%;
}

.tree-node.note span {
  color: #475569;
  font-size: 13px;
}

.context-menu {
  position: fixed;
  z-index: 2000;
  list-style: none;
  margin: 0;
  padding: 8px 0;
  background: #fff;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 8px;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.12);
  width: 160px;
}

.context-menu li {
  padding: 8px 16px;
  cursor: pointer;
}

.context-menu li:hover {
  background: rgba(99, 102, 241, 0.08);
}

.context-menu li.danger {
  color: #f87171;
}

.inline-input {
  width: 100%;
  border: 1px solid #cbd5f5;
  border-radius: 6px;
  padding: 2px 6px;
  font-size: 13px;
  outline: none;
}

.inline-input:focus {
  border-color: #6366f1;
  box-shadow: 0 0 0 1px rgba(99, 102, 241, 0.2);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>


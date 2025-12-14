<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useNoteStore } from '../stores/noteStore'
import FolderTree, { type TreeNode } from '../components/FolderTree.vue'

const noteStore = useNoteStore()

onMounted(async () => {
  await noteStore.init()
})

const folderTreeData = computed<TreeNode[]>(() => {
  const convertFolder = (folder: any): TreeNode => ({
    treeKey: `folder-${folder.id}`,
    id: String(folder.id),
    name: folder.name,
    type: 'folder',
    children: folder.children?.map(convertFolder) ?? [],
  })
  return noteStore.state.folders.map(convertFolder)
})

const handleSelectFolder = (id: string | null) => {
  noteStore.selectFolder(id)
}

const handleCreateFolder = async (payload: {
  parentId?: string
  name: string
  parentIsDefaultRoot?: boolean
}) => {
  await noteStore.createFolder(payload.name, payload.parentId)
}

const handleRenameFolder = async (payload: { id: string; name: string }) => {
  await noteStore.renameFolder(payload.id, payload.name)
}

const handleDeleteFolder = async (payload: { id: string }) => {
  await noteStore.deleteFolder(payload.id)
}

const handleMoveFolder = async (payload: { id: string; newParentId?: string }) => {
  await noteStore.moveFolder({ id: payload.id, newParentId: payload.newParentId })
}
</script>

<template>
  <div class="folder-view-page">
    <div class="folder-view-header">
      <h1>文件夹管理</h1>
    </div>
    <div class="folder-view-content">
      <FolderTree
        v-if="folderTreeData.length > 0"
        :data="folderTreeData"
        :current-key="noteStore.state.selectedFolderId"
        @select-folder="handleSelectFolder"
        @create-folder="handleCreateFolder"
        @rename-folder="handleRenameFolder"
        @delete-folder="handleDeleteFolder"
        @move-folder="handleMoveFolder"
      />
      <div v-else class="empty-state">
        <p>暂无文件夹</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.folder-view-page {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.folder-view-header {
  padding: 24px 32px;
  border-bottom: 1px solid #e5e7eb;
}

.folder-view-header h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
}

.folder-view-content {
  flex: 1;
  padding: 24px 32px;
  overflow: auto;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #9ca3af;
}
</style>


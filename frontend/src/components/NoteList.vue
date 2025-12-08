<script setup lang="ts">
import { computed, ref } from 'vue'
import { MoreFilled } from '@element-plus/icons-vue'
import type { NoteSummary } from '../api/note'

const props = defineProps<{
  notes: NoteSummary[]
  selectedId: string | null
  loading?: boolean
}>()

const emit = defineEmits<{
  (e: 'select', id: string): void
  (e: 'create'): void
  (e: 'delete', id: string): void
}>()

const keyword = ref('')

const filteredNotes = computed(() => {
  if (!keyword.value) return props.notes
  return props.notes.filter((item) =>
    item.title.toLowerCase().includes(keyword.value.toLowerCase()),
  )
})

const handleCommand = (command: { action: string; id: string }) => {
  if (command.action === 'delete') {
    emit('delete', command.id)
  }
}
</script>

<template>
  <div class="note-list">
    <div class="list-toolbar">
      <el-input
        v-model="keyword"
        placeholder="搜索笔记"
        size="small"
        prefix-icon="Search"
      />
      <el-button type="primary" size="small" @click="emit('create')">新建笔记</el-button>
    </div>

    <el-skeleton v-if="loading" animated :count="3" />

    <el-scrollbar v-else class="list-scroll">
      <el-empty v-if="filteredNotes.length === 0" description="暂无笔记" />
      <el-card
        v-for="note in filteredNotes"
        :key="note.id"
        class="note-item"
        :class="{ active: note.id === selectedId }"
        shadow="hover"
        @click="emit('select', note.id)"
      >
        <div class="note-item__header">
          <h4>{{ note.title }}</h4>
          <el-dropdown @command="handleCommand" trigger="click">
            <el-icon><MoreFilled /></el-icon>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item :command="{ action: 'delete', id: note.id }">
                  删除
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
        <p class="note-item__date">{{ new Date(note.updatedAt).toLocaleString() }}</p>
      </el-card>
    </el-scrollbar>
  </div>
</template>

<style scoped>
.note-list {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding-right: 8px;
}

.list-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.list-scroll {
  flex: 1;
}

.note-item {
  margin-bottom: 12px;
  cursor: pointer;
  border-radius: 16px;
  transition: border-color 0.2s ease;
}

.note-item.active {
  border-color: #3b82f6;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.15);
}

.note-item__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.note-item__header h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.note-item__date {
  margin: 4px 0 0;
  color: #94a3b8;
  font-size: 12px;
}
</style>


<script setup lang="ts">
import { useRouter } from 'vue-router'

defineProps<{
  username?: string
}>()

const router = useRouter()

const handleLogout = () => {
  localStorage.removeItem('token')
  router.replace('/login')
}
</script>

<template>
  <header class="app-header">
    <div class="brand">
      <span class="brand-badge">M</span>
      <div>
        <p class="brand-title">Mido Workspace</p>
        <p class="brand-desc">Notes · Planning · Knowledge</p>
      </div>
    </div>
    <div class="header-actions">
      <el-input
        class="search"
        size="small"
        placeholder="搜索笔记或文件夹"
        prefix-icon="Search"
        clearable
      />
      <div class="divider"></div>
      <div class="user-meta">
        <el-avatar size="small">{{ username?.charAt(0)?.toUpperCase() ?? 'M' }}</el-avatar>
        <span class="username">{{ username ?? '未命名用户' }}</span>
        <el-button size="small" text @click="handleLogout">退出</el-button>
      </div>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  height: 60px;
  padding: 0 28px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid rgba(47, 52, 55, 0.08);
  background: #fbfaf8;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: #ece8df;
  color: #2f3437;
  font-weight: 600;
  font-size: 18px;
}

.brand-title {
  margin: 0;
  font-weight: 600;
  color: #2f3135;
  font-size: 16px;
}

.brand-desc {
  margin: 0;
  font-size: 12px;
  color: #8b8f93;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #2f3437;
  font-weight: 500;
}

.header-actions .search {
  width: 220px;
}

.divider {
  width: 1px;
  height: 26px;
  background: rgba(47, 52, 55, 0.1);
}

.user-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>


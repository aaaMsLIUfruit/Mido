<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()
const noteId = route.params.id as string
</script>

<template>
  <div class="page page-note">
    <h1>笔记详情占位</h1>
    <p>当前笔记 ID：{{ noteId }}</p>
  </div>
</template>

<style scoped>
.page {
  padding: 32px;
}
</style>



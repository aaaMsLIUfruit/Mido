<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useNoteStore } from '../stores/noteStore'
import NoteEditor from '../components/NoteEditor.vue'

const route = useRoute()
const noteStore = useNoteStore()
const noteId = route.params.id as string

onMounted(async () => {
  if (noteId) {
    await noteStore.selectNote(noteId)
  }
})

const handleSave = async (payload: {
  id: string
  title: string
  content: string
  folderId?: string
  coverUrl?: string
}) => {
  await noteStore.updateNote(payload)
}
</script>

<template>
  <div class="note-detail-page">
    <NoteEditor
      :note="noteStore.state.selectedNote"
      :saving="noteStore.state.saving"
      @save="handleSave"
    />
  </div>
</template>

<style scoped>
.note-detail-page {
  width: 100%;
  height: 100vh;
  overflow: auto;
}
</style>


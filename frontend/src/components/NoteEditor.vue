<script setup lang="ts">
import { onBeforeUnmount, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { UploadFile } from 'element-plus'
import type { NoteDetail } from '../api/note'
import { uploadImage } from '../api/upload'
import { EditorContent, useEditor } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Image from '@tiptap/extension-image'
import { Extension, markInputRule } from '@tiptap/core'

const props = defineProps<{
  note: NoteDetail | null
  saving?: boolean
}>()

const emit = defineEmits<{
  (e: 'save', payload: { id: string; title: string; content: string; folderId?: string; coverUrl?: string }): void
}>()

const form = reactive({
  id: '',
  title: '',
  folderId: '',
  coverUrl: '',
  content: '',
})

const imageWidth = ref(680)

const boldInputRegex = /(?:^|[\s>])(\*\*|__|＊＊|＿＿)([^*_]+?)(\1)$/
const italicInputRegex = /(?:^|[\s>])(\*|_|＊|＿)([^*_]+?)(\1)$/

const markdownShortcuts = Extension.create({
  name: 'markdownShortcuts',
  addInputRules() {
    const rules = []
    if (this.editor.schema.marks.strong) {
      rules.push(
        markInputRule({
          find: boldInputRegex,
          type: this.editor.schema.marks.strong,
        }),
      )
    }
    if (this.editor.schema.marks.em) {
      rules.push(
        markInputRule({
          find: italicInputRegex,
          type: this.editor.schema.marks.em,
        }),
      )
    }
    return rules
  },
})

const editor = useEditor({
  extensions: [
    StarterKit.configure({
      heading: {
        levels: [1, 2, 3, 4, 5, 6],
      },
    }),
    markdownShortcuts,
    Image.extend({
      inline: false,
      addAttributes() {
        return {
          ...this.parent?.(),
          width: {
            default: imageWidth.value,
            parseHTML: (element) => {
              const inlineWidth = element.style.width?.replace('px', '') || element.getAttribute('data-width')
              return inlineWidth ? parseInt(inlineWidth, 10) : imageWidth.value
            },
            renderHTML: (attributes) => ({
              'data-width': attributes.width,
              style: `width: ${attributes.width}px;`,
            }),
          },
        }
      },
    }),
  ],
  content: '',
  autofocus: 'end',
  onUpdate: ({ editor }) => {
    form.content = editor.getHTML()
  },
})

watch(
  editor,
  (instance) => {
    if (instance) {
      instance.commands.setContent(form.content || '<p></p>', false)
      instance.on('selectionUpdate', ({ editor }) => {
        if (editor.isActive('image')) {
          const width = editor.getAttributes('image').width
          if (width) {
            imageWidth.value = width
          }
        }
      })
    }
  },
)

const applyEditorContent = () => {
  if (editor.value && form.content !== editor.value.getHTML()) {
    editor.value.commands.setContent(form.content || '<p></p>', false)
  }
}

const loadNote = (note: NoteDetail | null) => {
  if (!note) {
    form.id = ''
    form.title = ''
    form.folderId = ''
    form.coverUrl = ''
    form.content = '<p></p>'
    applyEditorContent()
    return
  }
  form.id = note.id
  form.title = note.title
  form.folderId = note.folderId ?? ''
  form.coverUrl = (note as any).coverUrl ?? ''
  const draftRaw = localStorage.getItem(`draft-${note.id}`)
  if (draftRaw) {
    try {
      const draft = JSON.parse(draftRaw)
      form.title = draft.title ?? note.title
      form.content = draft.content ?? note.content
    } catch {
      form.content = note.content
    }
  } else {
    form.content = note.content
  }
  applyEditorContent()
}

watch(
  () => props.note,
  (val) => loadNote(val),
  { immediate: true },
)

watch(
  () => [form.id, form.title, form.content],
  () => {
    if (form.id) {
      localStorage.setItem(
        `draft-${form.id}`,
        JSON.stringify({
          title: form.title,
          content: form.content,
        }),
      )
    }
  },
)

const handleSave = () => {
  if (!form.id) return
  emit('save', {
    id: form.id,
    title: form.title,
    content: form.content,
    coverUrl: form.coverUrl || undefined,
    folderId: form.folderId || undefined,
  })
}

const insertImageToEditor = async (file?: File) => {
  if (!file || !editor.value) return
  const res = await uploadImage(file)
  const url = res.url ?? res.path
  if (url) {
    editor.value.chain().focus().setImage({ src: url, width: imageWidth.value } as any).run()
    ElMessage.success('图片已插入')
  }
}

const handleInsertImage = (uploadFile: UploadFile) => {
  insertImageToEditor(uploadFile.raw as File | undefined)
}

const applyImageWidth = (value: number) => {
  imageWidth.value = value
  if (editor.value?.isActive('image')) {
    editor.value.chain().focus().updateAttributes('image', { width: value } as any).run()
  }
}

onBeforeUnmount(() => {
  editor.value?.destroy()
})
</script>

<template>
  <div class="note-editor">
    <el-empty v-if="!note" description="选择一篇笔记开始创作" />
    <template v-else>
      <div class="editor-header">
        <el-input v-model="form.title" class="title-input" placeholder="无标题文档" />
        <div class="editor-actions">
          <el-upload
            class="cover-upload"
            :auto-upload="false"
            :show-file-list="false"
            accept="image/*"
            @change="handleInsertImage"
          >
            <el-button text>插入图片</el-button>
          </el-upload>
          <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
        </div>
      </div>
      <div class="image-resize" v-if="editor?.isActive('image')">
        <span>图像宽度</span>
        <el-slider
          v-model="imageWidth"
          :min="120"
          :max="960"
          :show-tooltip="false"
          @change="applyImageWidth"
        />
        <span class="image-resize__value">{{ imageWidth }}px</span>
      </div>
      <div v-if="form.coverUrl" class="cover-preview">
        <img :src="form.coverUrl" alt="封面" />
      </div>
      <div class="notion-editor">
        <div class="notion-editor__page">
          <EditorContent v-if="editor" :editor="editor" />
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.note-editor {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.editor-header {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.title-input :deep(.el-input__wrapper) {
  padding: 0;
  border: none;
  box-shadow: none;
  background: transparent;
  min-height: auto;
}

.title-input :deep(.el-input__inner) {
  font-size: 40px;
  font-weight: 600;
  border: none;
  background: transparent;
  padding: 16px 0 16px;
  line-height: 1.4;
  height: auto;
}

.title-input :deep(.el-input__inner::placeholder) {
  color: #c4c1ba;
}
.editor-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}
.editor-actions :deep(.el-button.is-text) {
  color: #6d6f73;
}

.editor-actions :deep(.el-button--primary) {
  background: #2f3437;
  border-color: #2f3437;
  border-radius: 10px;
  padding: 8px 18px;
}

.image-resize {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 4px 0 12px;
  color: #8a8f95;
}

.image-resize :deep(.el-slider) {
  flex: 1;
}

.image-resize__value {
  font-size: 13px;
  color: #6d6f73;
}

.cover-preview {
  width: 100%;
  border-radius: 12px;
  overflow: hidden;
}

.cover-preview img {
  width: 100%;
  object-fit: cover;
}

.notion-editor {
  flex: 1;
  padding: 16px 0 80px;
  overflow-y: auto;
}

.notion-editor__page {
  max-width: 880px;
  margin: 0 auto;
  padding: 0 24px 120px;
}

.notion-editor :deep(.ProseMirror) {
  min-height: 100%;
  outline: none;
  font-size: 16px;
  line-height: 1.8;
}

.notion-editor :deep(h1),
.notion-editor :deep(h2),
.notion-editor :deep(h3) {
  margin: 1.2em 0 0.6em;
  font-weight: 600;
}

.notion-editor :deep(ul) {
  padding-left: 1.4em;
}
</style>


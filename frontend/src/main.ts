import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

import VMdEditor from '@kangc/v-md-editor'
import VMdPreview from '@kangc/v-md-editor/lib/preview'
import githubTheme from '@kangc/v-md-editor/lib/theme/github'
import '@kangc/v-md-editor/lib/style/base-editor.css'
import '@kangc/v-md-editor/lib/style/preview.css'
import '@kangc/v-md-editor/lib/theme/style/github.css'
import Prism from 'prismjs'

import './style.css'

VMdEditor.use(githubTheme, {
  Prism,
})
VMdPreview.use(githubTheme, {
  Prism,
})

const app = createApp(App)

app.use(router)
app.use(ElementPlus)
app.use(VMdEditor)
app.use(VMdPreview)

app.mount('#app')

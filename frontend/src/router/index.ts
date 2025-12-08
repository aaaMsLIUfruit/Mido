import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
  },
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue'),
  },
  {
    path: '/note/:id',
    name: 'NoteDetail',
    component: () => import('../views/NoteDetail.vue'),
    props: true,
  },
  {
    path: '/folder',
    name: 'FolderView',
    component: () => import('../views/FolderView.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')

  if (!token && to.path !== '/login') {
    next({ path: '/login', replace: true })
    return
  }

  if (token && to.path === '/login') {
    next({ path: '/', replace: true })
    return
  }

  next()
})

export default router



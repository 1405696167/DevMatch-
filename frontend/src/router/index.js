import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  // 认证页面（无需登录）
  {
    path: '/auth',
    component: () => import('@/layouts/AuthLayout.vue'),
    children: [
      { path: 'login', name: 'Login', component: () => import('@/views/auth/Login.vue') },
      { path: 'register', name: 'Register', component: () => import('@/views/auth/Register.vue') },
      { path: 'forgot-password', name: 'ForgotPassword', component: () => import('@/views/auth/ForgotPassword.vue') }
    ]
  },

  // 首页
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue')
  },

  // 开发者端（面向开发者/自由职业者）
  {
    path: '/developer',
    component: () => import('@/layouts/DeveloperLayout.vue'),
    meta: { requiresAuth: true, role: 'DEVELOPER' },
    children: [
      { path: '', redirect: '/developer/dashboard' },
      { path: 'dashboard', name: 'DevDashboard', component: () => import('@/views/developer/Dashboard.vue') },
      { path: 'profile', name: 'DevProfile', component: () => import('@/views/developer/Profile.vue') },
      { path: 'tasks', name: 'TaskMarket', component: () => import('@/views/developer/TaskMarket.vue') },
      { path: 'tasks/:id', name: 'TaskDetail', component: () => import('@/views/developer/TaskDetail.vue') },
      { path: 'orders', name: 'MyOrders', component: () => import('@/views/developer/MyOrders.vue') },
      { path: 'projects', name: 'DevProjects', component: () => import('@/views/developer/Projects.vue') },
      { path: 'projects/:id', name: 'DevProjectWorkspace', component: () => import('@/views/developer/ProjectWorkspace.vue') },
      { path: 'wallet', name: 'DevWallet', component: () => import('@/views/common/Wallet.vue') },
      { path: 'credit', name: 'DevCredit', component: () => import('@/views/common/Credit.vue') }
    ]
  },

  // 企业端（面向需求发布方/中小企业）
  {
    path: '/enterprise',
    component: () => import('@/layouts/EnterpriseLayout.vue'),
    meta: { requiresAuth: true, role: 'ENTERPRISE' },
    children: [
      { path: '', redirect: '/enterprise/dashboard' },
      { path: 'dashboard', name: 'EntDashboard', component: () => import('@/views/enterprise/Dashboard.vue') },
      { path: 'profile', name: 'EntProfile', component: () => import('@/views/enterprise/Profile.vue') },
      { path: 'publish', name: 'PublishTask', component: () => import('@/views/enterprise/PublishTask.vue') },
      { path: 'tasks', name: 'MyTasks', component: () => import('@/views/enterprise/MyTasks.vue') },
      { path: 'tasks/:id/bids', name: 'TaskBids', component: () => import('@/views/enterprise/TaskBids.vue') },
      { path: 'developers', name: 'DeveloperSearch', component: () => import('@/views/enterprise/DeveloperSearch.vue') },
      { path: 'developers/:id', name: 'DeveloperProfile', component: () => import('@/views/enterprise/DeveloperProfile.vue') },
      { path: 'projects', name: 'EntProjects', component: () => import('@/views/enterprise/Projects.vue') },
      { path: 'projects/:id', name: 'EntProjectWorkspace', component: () => import('@/views/enterprise/ProjectWorkspace.vue') },
      { path: 'wallet', name: 'EntWallet', component: () => import('@/views/common/Wallet.vue') },
      { path: 'credit', name: 'EntCredit', component: () => import('@/views/common/Credit.vue') }
    ]
  },

  // 后台管理端
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { requiresAuth: true, role: 'ADMIN' },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', name: 'AdminDashboard', component: () => import('@/views/admin/Dashboard.vue') },
      { path: 'tasks', name: 'AdminTasks', component: () => import('@/views/admin/TaskAudit.vue') },
      { path: 'users', name: 'AdminUsers', component: () => import('@/views/admin/UserManage.vue') },
      { path: 'kyc', name: 'AdminKyc', component: () => import('@/views/admin/KycAudit.vue') },
      { path: 'complaints', name: 'AdminComplaints', component: () => import('@/views/admin/Complaints.vue') },
      { path: 'config', name: 'AdminConfig', component: () => import('@/views/admin/SystemConfig.vue') }
    ]
  },

  // 404
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) return savedPosition
    return { top: 0 }
  }
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  if (to.path === '/') {
    return next()
  }

  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    return next({ name: 'Login', query: { redirect: to.fullPath } })
  }

  if (to.meta.role && userStore.userRole !== to.meta.role && userStore.userRole !== 'ADMIN') {
    const roleMap = {
      DEVELOPER: '/developer/dashboard',
      ENTERPRISE: '/enterprise/dashboard',
      ADMIN: '/admin/dashboard'
    }
    return next(roleMap[userStore.userRole] || '/auth/login')
  }

  next()
})

export default router

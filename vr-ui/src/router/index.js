import { createRouter, createWebHistory } from 'vue-router';
import { getToken } from '@/utils/auth';

// Static routes (accessible to all)
export const constantRoutes = [
  { path: '/login', component: () => import('@/views/login.vue'), hidden: true },
  { path: '/', redirect: '/admin', hidden: true },
  { path: '/404', component: () => import('@/views/error/404.vue'), hidden: true },
];

// Dynamic routes (loaded from backend menus)
export const asyncRoutes = [
  {
    path: '/admin',
    component: () => import('@/layout/index.vue'),
    redirect: '/admin/index',
    children: [
      {
        path: 'index',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/index.vue'),
        meta: { title: '首页', icon: 'home-filled' },
      },
    ],
  },
  {
    path: '/tenant',
    component: () => import('@/layout/index.vue'),
    children: [
      {
        path: '',
        name: 'TenantDashboard',
        component: () => import('@/views/tenant/index.vue'),
        meta: { title: '工作台', icon: 'monitor' },
      },
    ],
  },
  {
    path: '/operator',
    component: () => import('@/layout/index.vue'),
    children: [
      {
        path: '',
        name: 'OperatorDashboard',
        component: () => import('@/views/operator/index.vue'),
        meta: { title: '运维监控', icon: 'cpu' },
      },
    ],
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes: constantRoutes,
  scrollBehavior: () => ({ top: 0 }),
});

// White list — paths that don't need authentication
const whiteList = ['/login', '/404'];

router.beforeEach(async (to, from, next) => {
  const token = getToken();

  if (token) {
    if (to.path === '/login') {
      next('/');
    } else {
      next();
    }
  } else {
    if (whiteList.includes(to.path)) {
      next();
    } else {
      next('/login');
    }
  }
});

export default router;

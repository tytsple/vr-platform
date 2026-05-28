import { createRouter, createWebHistory } from 'vue-router';
import { getToken } from '@/utils/auth';

// Static routes (accessible to all)
export const constantRoutes = [
  { path: '/login', component: () => import('@/views/login.vue'), hidden: true },
  { path: '/', redirect: '/admin', hidden: true },
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
      {
        path: 'tenants',
        name: 'Tenants',
        component: () => import('@/views/vr/tenant/index.vue'),
        meta: { title: '租户管理', icon: 'office-building' },
      },
      {
        path: 'venues',
        name: 'Venues',
        component: () => import('@/views/vr/venue/index.vue'),
        meta: { title: '场地管理', icon: 'location' },
      },
      {
        path: 'applications',
        name: 'Applications',
        component: () => import('@/views/vr/application/index.vue'),
        meta: { title: '应用管理', icon: 'video-camera' },
      },
      {
        path: 'licenses',
        name: 'Licenses',
        component: () => import('@/views/vr/license/index.vue'),
        meta: { title: '授权管理', icon: 'key' },
      },
      {
        path: 'users',
        name: 'Users',
        component: () => import('@/views/system/user/index.vue'),
        meta: { title: '用户管理', icon: 'user' },
      },
      {
        path: 'stats',
        name: 'Stats',
        component: () => import('@/views/vr/stats/index.vue'),
        meta: { title: '使用统计', icon: 'data-analysis' },
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

export default router;

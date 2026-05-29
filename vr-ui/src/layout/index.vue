<template>
  <div class="app-wrapper">
    <Sidebar :menu-routes="menuRoutes" :collapsed="appStore.sidebarCollapsed" />
    <div class="main-container" :class="{ collapsed: appStore.sidebarCollapsed }">
      <Navbar @toggle="appStore.toggleSidebar" />
      <TagsView />
      <section class="app-main">
        <router-view />
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import { useAppStore } from '@/store/modules/app';
import { useUserStore } from '@/store/modules/user';
import Sidebar from './components/Sidebar/index.vue';
import Navbar from './components/Navbar.vue';
import TagsView from './components/TagsView/index.vue';

const appStore = useAppStore();
const userStore = useUserStore();
const route = useRoute();

const menuRoutes = computed(() => {
  const roles = userStore.roles || [];
  const parentPath = route.matched[0]?.path || '/';

  if (roles.includes('admin')) {
    return [{
      path: '/admin',
      meta: { title: '管理后台', icon: 'setting' },
      children: [
        { path: '/admin/index', meta: { title: '首页', icon: 'home-filled' } },
        { path: '/admin/tenants', meta: { title: '租户管理', icon: 'office-building' } },
        { path: '/admin/venues', meta: { title: '场地管理', icon: 'location' } },
        { path: '/admin/applications', meta: { title: '应用管理', icon: 'video-camera' } },
        { path: '/admin/licenses', meta: { title: '授权管理', icon: 'key' } },
        { path: '/admin/users', meta: { title: '用户管理', icon: 'user' } },
        { path: '/admin/stats', meta: { title: '使用统计', icon: 'data-analysis' } },
      ],
    }];
  }
  if (roles.includes('tenant')) {
    return [{
      path: '/tenant',
      meta: { title: '工作台', icon: 'monitor' },
      children: [
        { path: '/tenant', meta: { title: '首页', icon: 'home-filled' } },
      ],
    }];
  }
  if (roles.includes('operator')) {
    return [{
      path: '/operator',
      meta: { title: '运维监控', icon: 'cpu' },
      children: [
        { path: '/operator', meta: { title: '首页', icon: 'home-filled' } },
      ],
    }];
  }
  return [];
});
</script>

<style scoped>
.app-wrapper {
  display: flex;
  min-height: 100vh;
}
.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: margin-left 0.3s;
}
.app-main {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  background: #f0f2f5;
}
</style>

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
import Sidebar from './components/Sidebar/index.vue';
import Navbar from './components/Navbar.vue';
import TagsView from './components/TagsView/index.vue';

const appStore = useAppStore();
const route = useRoute();

const menuRoutes = computed(() => {
  const matched = route.matched;
  if (matched.length > 0) {
    return matched[0]?.children?.filter(r => !r.meta?.hidden) || [];
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

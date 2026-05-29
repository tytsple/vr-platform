<template>
  <el-menu
    :default-active="activeMenu"
    :collapse="collapsed"
    background-color="#304156"
    text-color="#bfcbd9"
    active-text-color="#409EFF"
    router
    class="sidebar-menu"
    :style="{ width: collapsed ? '64px' : '220px' }"
  >
    <div class="sidebar-logo">
      <span v-if="!collapsed">VR 管理平台</span>
      <span v-else>VR</span>
    </div>
    <template v-for="item in menuRoutes" :key="item.path">
      <el-menu-item :index="item.path" v-if="!item.children?.length">
        <el-icon><component :is="item.meta?.icon || 'menu'" /></el-icon>
        <template #title>{{ item.meta?.title }}</template>
      </el-menu-item>
      <el-sub-menu :index="item.path" v-else>
        <template #title>
          <el-icon><component :is="item.meta?.icon || 'folder'" /></el-icon>
          <span>{{ item.meta?.title }}</span>
        </template>
        <el-menu-item v-for="child in item.children" :key="child.path" :index="child.path">
          {{ child.meta?.title }}
        </el-menu-item>
      </el-sub-menu>
    </template>
  </el-menu>
</template>

<script setup>
import { computed } from 'vue';
import { useRoute } from 'vue-router';

const props = defineProps({
  menuRoutes: { type: Array, default: () => [] },
  collapsed: { type: Boolean, default: false },
});

const route = useRoute();
const activeMenu = computed(() => route.path);
</script>

<style scoped>
.sidebar-menu {
  min-height: 100vh;
  border-right: none;
  transition: width 0.3s;
}
.sidebar-menu:not(.el-menu--collapse) {
  width: 220px;
}
.sidebar-logo {
  height: 50px;
  line-height: 50px;
  text-align: center;
  font-size: 18px;
  font-weight: 600;
  color: #fff;
  border-bottom: 1px solid rgba(255,255,255,0.08);
  white-space: nowrap;
  overflow: hidden;
}
</style>

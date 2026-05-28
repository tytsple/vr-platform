<template>
  <header class="navbar">
    <div class="navbar-left">
      <el-button @click="$emit('toggle')" :icon="Fold" link />
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: homePath }">首页</el-breadcrumb-item>
        <el-breadcrumb-item v-for="(crumb, i) in breadcrumbs" :key="i">
          {{ crumb }}
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>
    <div class="navbar-right">
      <span class="username">{{ userStore.user?.username }}</span>
      <el-button size="small" @click="logout">登出</el-button>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useUserStore } from '@/store/modules/user';
import { Fold } from '@element-plus/icons-vue';

defineEmits(['toggle']);

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const homePath = computed(() => {
  const role = userStore.user?.role;
  if (role === 'tenant' || role === 'tenant_user') return '/tenant';
  if (role === 'operator') return '/operator';
  return '/admin';
});

const breadcrumbs = computed(() => {
  const segs = route.path.split('/').filter(Boolean);
  if (segs.length === 0 || (segs.length === 1 && segs[0] === userStore.user?.role)) return [];
  return segs.map(s => {
    const matched = route.matched.find(m => m.path === s || m.path === '/' + s);
    return matched?.meta?.title || s;
  });
});

function logout() {
  userStore.logout();
  router.push('/login');
}
</script>

<style scoped>
.navbar {
  height: 50px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.08);
  flex-shrink: 0;
}
.navbar-left { display: flex; align-items: center; gap: 12px; }
.navbar-right { display: flex; align-items: center; gap: 16px; color: #606266; }
.username { font-size: 14px; }
</style>

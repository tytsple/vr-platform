<template>
  <div class="dashboard">
    <el-row :gutter="16" class="stat-row">
      <el-col :span="6" v-for="s in stats" :key="s.label">
        <el-card shadow="hover">
          <div class="stat-card">
            <el-icon :size="32" :color="s.color"><component :is="s.icon" /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ s.value }}</div>
              <div class="stat-label">{{ s.label }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top:16px">
      <template #header>活跃会话</template>
      <el-table :data="sessions" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="venueId" label="场地ID" width="100" />
        <el-table-column prop="applicationId" label="应用ID" width="100" />
        <el-table-column prop="version" label="版本" width="100" />
        <el-table-column prop="startedAt" label="开始时间" width="180" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'info'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { Monitor } from '@element-plus/icons-vue';
import { getActiveSessions } from '@/api/vr/stats';

const loading = ref(false);
const sessions = ref([]);

const stats = ref([
  { label: '活跃会话', value: 0, icon: Monitor, color: '#F56C6C' },
]);

onMounted(async () => {
  loading.value = true;
  try {
    const res = await getActiveSessions();
    sessions.value = res.data || res.rows || [];
    stats.value[0].value = sessions.value.length;
  } finally { loading.value = false; }
});
</script>

<style scoped>
.stat-row .el-card { cursor: default; }
.stat-card { display: flex; align-items: center; gap: 16px; }
.stat-value { font-size: 28px; font-weight: 600; color: #303133; }
.stat-label { font-size: 14px; color: #909399; margin-top: 4px; }
</style>

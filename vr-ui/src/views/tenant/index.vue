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
      <template #header>我的场地</template>
      <el-table :data="venues" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="address" label="地址" />
        <el-table-column prop="controllerToken" label="Token" :show-overflow-tooltip="true" />
      </el-table>
    </el-card>

    <el-card style="margin-top:16px">
      <template #header>授权信息</template>
      <el-table :data="licenses" stripe v-loading="licLoading">
        <el-table-column prop="applicationId" label="应用ID" width="120" />
        <el-table-column prop="granted" label="授权" width="80">
          <template #default="{ row }">
            <el-tag :type="row.granted ? 'success' : 'danger'">{{ row.granted ? '已授权' : '未授权' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="quotaType" label="配额类型" width="100" />
        <el-table-column label="限额" width="100">
          <template #default="{ row }">
            {{ row.quotaLimit == null ? '无限制' : `${row.quotaUsed || 0}/${row.quotaLimit}` }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { Location, Key, VideoCamera, DataAnalysis } from '@element-plus/icons-vue';
import { useUserStore } from '@/store/modules/user';
import { listTenantVenues } from '@/api/vr/venue';
import { listTenantLicenses } from '@/api/vr/license';
import { getTenantStats } from '@/api/vr/stats';

const userStore = useUserStore();
const loading = ref(false);
const licLoading = ref(false);
const venues = ref([]);
const licenses = ref([]);

const stats = ref([
  { label: '场地', value: 0, icon: Location, color: '#67C23A' },
  { label: '授权', value: 0, icon: Key, color: '#F56C6C' },
  { label: '应用', value: 0, icon: VideoCamera, color: '#E6A23C' },
  { label: '总使用次数', value: 0, icon: DataAnalysis, color: '#409EFF' },
]);

onMounted(async () => {
  const tenantId = userStore.user?.tenant_id;
  loading.value = true;
  try {
    const res = await listTenantVenues();
    venues.value = res.data || res.rows || [];
    stats.value[0].value = venues.value.length;
  } finally { loading.value = false; }

  licLoading.value = true;
  try {
    const res = await listTenantLicenses();
    licenses.value = res.data || res.rows || [];
    stats.value[1].value = licenses.value.length;
    const appIds = new Set(licenses.value.map(l => l.applicationId).filter(Boolean));
    stats.value[2].value = appIds.size;
  } finally { licLoading.value = false; }

  try {
    const res = await getTenantStats();
    const rows = res.data || res.rows || [];
    stats.value[3].value = rows.reduce((sum, r) => sum + (r.count || 0), 0);
  } catch {}
});
</script>

<style scoped>
.stat-row .el-card { cursor: default; }
.stat-card { display: flex; align-items: center; gap: 16px; }
.stat-value { font-size: 28px; font-weight: 600; color: #303133; }
.stat-label { font-size: 14px; color: #909399; margin-top: 4px; }
</style>

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
      <template #header>场地列表</template>
      <el-table :data="venues" stripe v-loading="loading">
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="address" label="地址" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'warning'">{{ row.status === 'active' ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="token" label="Token" :show-overflow-tooltip="true" />
      </el-table>
    </el-card>

    <el-card style="margin-top:16px">
      <template #header>授权信息</template>
      <el-table :data="licenses" stripe v-loading="licLoading">
        <el-table-column prop="app_name" label="应用" />
        <el-table-column prop="license_type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag>{{ row.license_type === 'perpetual' ? '永久' : row.license_type === 'subscription' ? '订阅' : '试用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="max_quota" label="限额" width="100">
          <template #default="{ row }">
            {{ row.max_quota === -1 ? '无限制' : row.max_quota }}
          </template>
        </el-table-column>
        <el-table-column prop="start_date" label="开始日期" width="120" />
        <el-table-column prop="end_date" label="结束日期" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'warning'">{{ row.status === 'active' ? '有效' : '停用' }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { Location, VideoCamera, Key, DataAnalysis } from '@element-plus/icons-vue';
import { useUserStore } from '@/store/modules/user';
import { listVenue } from '@/api/vr/venue';
import { listLicense } from '@/api/vr/license';
import { getStats } from '@/api/vr/stats';

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
    const res = await listVenue(tenantId);
    venues.value = res.data || res.rows || [];
    stats.value[0].value = venues.value.length;
  } finally { loading.value = false; }

  licLoading.value = true;
  try {
    const res = await listLicense(tenantId);
    licenses.value = res.data || res.rows || [];
    stats.value[1].value = licenses.value.length;
    const appSet = new Set(licenses.value.map(l => l.app_name || l.application_name).filter(Boolean));
    stats.value[2].value = appSet.size;
  } finally { licLoading.value = false; }

  try {
    const res = await getStats({ tenant_id: tenantId });
    const rows = res.data || res.rows || [];
    stats.value[3].value = rows.reduce((sum, r) => sum + (r.session_count || 0), 0);
  } catch {}
});
</script>

<style scoped>
.stat-row .el-card { cursor: default; }
.stat-card { display: flex; align-items: center; gap: 16px; }
.stat-value { font-size: 28px; font-weight: 600; color: #303133; }
.stat-label { font-size: 14px; color: #909399; margin-top: 4px; }
</style>

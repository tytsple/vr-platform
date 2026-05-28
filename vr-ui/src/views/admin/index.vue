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

    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="14">
        <el-card>
          <template #header>各租户使用统计</template>
          <div ref="barChartRef" style="height:320px"></div>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card>
          <template #header>应用授权分布</template>
          <div ref="pieChartRef" style="height:320px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top:16px">
      <template #header>最近会话记录</template>
      <el-table :data="sessions" stripe size="small" v-loading="sessionLoading">
        <el-table-column prop="venue_name" label="场地" />
        <el-table-column prop="app_name" label="应用" />
        <el-table-column prop="tenant_name" label="租户" />
        <el-table-column prop="started_at" label="开始时间" width="180" />
        <el-table-column prop="ended_at" label="结束时间" width="180" />
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
import * as echarts from 'echarts';
import { OfficeBuilding, Location, VideoCamera, Key } from '@element-plus/icons-vue';
import { getStats, getSessions } from '@/api/vr/stats';
import { listTenant } from '@/api/vr/tenant';
import { listVenue } from '@/api/vr/venue';
import { listApp } from '@/api/vr/application';
import { listLicense } from '@/api/vr/license';

const barChartRef = ref(null);
const pieChartRef = ref(null);
const sessionLoading = ref(false);
const sessions = ref([]);

const stats = ref([
  { label: '租户', value: 0, icon: OfficeBuilding, color: '#409EFF' },
  { label: '场地', value: 0, icon: Location, color: '#67C23A' },
  { label: '应用', value: 0, icon: VideoCamera, color: '#E6A23C' },
  { label: '授权', value: 0, icon: Key, color: '#F56C6C' },
]);

onMounted(async () => {
  const [t, v, a, l] = await Promise.all([
    listTenant().catch(() => ({ data: [] })),
    listVenue().catch(() => ({ data: [] })),
    listApp().catch(() => ({ data: [] })),
    listLicense().catch(() => ({ data: [] })),
  ]);
  stats.value[0].value = Array.isArray(t.data) ? t.data.length : (t.rows?.length || 0);
  stats.value[1].value = Array.isArray(v.data) ? v.data.length : (v.rows?.length || 0);
  stats.value[2].value = Array.isArray(a.data) ? a.data.length : (a.rows?.length || 0);
  stats.value[3].value = Array.isArray(l.data) ? l.data.length : (l.rows?.length || 0);

  // Bar chart
  if (barChartRef.value) {
    const bar = echarts.init(barChartRef.value);
    const tenantData = await getStats({}).catch(() => ({ data: [] }));
    const rows = tenantData.data || [];
    bar.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: rows.map(r => r.tenant_name || r.name || '') },
      yAxis: { type: 'value' },
      series: [{
        name: '使用次数', type: 'bar', data: rows.map(r => r.session_count || 0),
        itemStyle: { color: '#409EFF' }
      }],
    });
  }

  // Pie chart
  if (pieChartRef.value) {
    const pie = echarts.init(pieChartRef.value);
    const allLicenses = Array.isArray(l.data) ? l.data : (l.rows || []);
    const appMap = {};
    allLicenses.forEach(lic => {
      const name = lic.app_name || lic.application_name || '未知';
      appMap[name] = (appMap[name] || 0) + 1;
    });
    pie.setOption({
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie', radius: ['40%', '70%'],
        data: Object.entries(appMap).map(([name, value]) => ({ name, value })),
      }],
    });
  }

  // Sessions
  sessionLoading.value = true;
  const s = await getSessions({ limit: 10 }).catch(() => ({ data: [] }));
  sessions.value = s.data || s.rows || [];
  sessionLoading.value = false;
});
</script>

<style scoped>
.stat-row .el-card { cursor: default; }
.stat-card { display: flex; align-items: center; gap: 16px; }
.stat-value { font-size: 28px; font-weight: 600; color: #303133; }
.stat-label { font-size: 14px; color: #909399; margin-top: 4px; }
</style>

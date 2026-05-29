<template>
  <div class="crud-page">
    <el-card>
      <div class="crud-header">
        <div>
          <el-date-picker v-model="dateRange" type="daterange" range-separator="至"
            start-placeholder="开始日期" end-placeholder="结束日期"
            value-format="YYYY-MM-DD" @change="fetchData" style="margin-right:12px" />
        </div>
        <el-button type="primary" @click="fetchData">查询</el-button>
      </div>
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="tenantId" label="租户ID" width="120" />
        <el-table-column prop="applicationId" label="应用ID" width="120" />
        <el-table-column prop="count" label="使用次数" sortable />
        <el-table-column prop="durationMinutes" label="总时长(分钟)" sortable>
          <template #default="{ row }">
            {{ row.durationMinutes ? row.durationMinutes.toFixed(1) : 0 }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { getStats } from '@/api/vr/stats';

const dateRange = ref([]);
const loading = ref(false);
const tableData = ref([]);

async function fetchData() {
  loading.value = true;
  try {
    const res = await getStats();
    tableData.value = res.data || res.rows || [];
  } finally { loading.value = false; }
}

onMounted(fetchData);
</script>

<style scoped>
.crud-header { display: flex; justify-content: space-between; margin-bottom: 16px; }
</style>

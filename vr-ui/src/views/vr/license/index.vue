<template>
  <div class="crud-page">
    <el-card>
      <div class="crud-header">
        <el-select v-model="filterTenantId" placeholder="按租户筛选" clearable style="width:200px" @change="fetchData">
          <el-option v-for="t in tenants" :key="t.id" :label="t.name" :value="t.id" />
        </el-select>
        <el-button type="primary" @click="openAdd">新增授权</el-button>
      </div>
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="租户" width="120">
          <template #default="{ row }">{{ tenantMap[row.tenantId] || row.tenantId }}</template>
        </el-table-column>
        <el-table-column label="应用" width="120">
          <template #default="{ row }">{{ appMap[row.applicationId] || row.applicationId }}</template>
        </el-table-column>
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
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" link type="danger" @click="handleDel(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="500px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="租户" prop="tenantId">
          <el-select v-model="form.tenantId" style="width:100%" placeholder="请选择">
            <el-option v-for="t in tenants" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="应用" prop="applicationId">
          <el-select v-model="form.applicationId" style="width:100%" placeholder="请选择">
            <el-option v-for="a in apps" :key="a.id" :label="a.name" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="授权状态">
          <el-switch v-model="form.granted" active-text="已授权" inactive-text="未授权" />
        </el-form-item>
        <el-form-item label="配额类型">
          <el-input v-model="form.quotaType" placeholder="留空表示无配额限制" />
        </el-form-item>
        <el-form-item label="配额上限">
          <el-input-number v-model="form.quotaLimit" :min="0" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { listLicense, getLicense, addLicense, updateLicense, delLicense } from '@/api/vr/license';
import { listTenant } from '@/api/vr/tenant';
import { listApp } from '@/api/vr/application';

const filterTenantId = ref('');
const loading = ref(false);
const tableData = ref([]);
const tenants = ref([]);
const apps = ref([]);
const tenantMap = ref({});
const appMap = ref({});
const dialogVisible = ref(false);
const submitting = ref(false);
const editingId = ref(null);
const formRef = ref(null);

const form = reactive({ tenantId: '', applicationId: '', granted: true, quotaType: '', quotaLimit: 0 });
const rules = {
  tenantId: [{ required: true, message: '请选择租户', trigger: 'change' }],
  applicationId: [{ required: true, message: '请选择应用', trigger: 'change' }],
};
const dialogTitle = computed(() => editingId.value ? '编辑授权' : '新增授权');

async function fetchData() {
  loading.value = true;
  try {
    const res = await listLicense(filterTenantId.value || 0);
    tableData.value = res.data || res.rows || [];
  } finally { loading.value = false; }
}

async function loadRefs() {
  try {
    const r = await listTenant();
    const list = r.data || r.rows || [];
    tenants.value = list;
    list.forEach(t => { tenantMap.value[t.id] = t.name; });
  } catch { console.warn('加载租户列表失败'); }
  try {
    const r = await listApp();
    const list = r.data || r.rows || [];
    apps.value = list;
    list.forEach(a => { appMap.value[a.id] = a.name; });
  } catch { console.warn('加载应用列表失败'); }
}

function openAdd() {
  editingId.value = null;
  Object.assign(form, { tenantId: '', applicationId: '', granted: true, quotaType: '', quotaLimit: 0 });
  dialogVisible.value = true;
}

async function openEdit(row) {
  editingId.value = row.id;
  try {
    const res = await getLicense(row.id);
    const d = res.data || row;
    Object.assign(form, {
      tenantId: d.tenantId, applicationId: d.applicationId,
      granted: d.granted, quotaType: d.quotaType || '', quotaLimit: d.quotaLimit || 0
    });
  } catch {
    Object.assign(form, {
      tenantId: row.tenantId, applicationId: row.applicationId,
      granted: row.granted, quotaType: row.quotaType || '', quotaLimit: row.quotaLimit || 0
    });
  }
  dialogVisible.value = true;
}

async function handleDel(row) {
  await ElMessageBox.confirm('确认删除该授权？', '提示', { type: 'warning' });
  await delLicense(row.id);
  ElMessage.success('删除成功');
  fetchData();
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;
  submitting.value = true;
  try {
    const payload = {
      tenantId: form.tenantId,
      applicationId: form.applicationId,
      granted: form.granted,
      quotaType: form.quotaType,
      quotaLimit: form.quotaLimit,
      quotaUsed: 0,
    };
    if (editingId.value) {
      await updateLicense(editingId.value, payload);
      ElMessage.success('更新成功');
    } else {
      await addLicense(payload);
      ElMessage.success('新增成功');
    }
    dialogVisible.value = false;
    fetchData();
  } finally { submitting.value = false; }
}

function resetForm() { formRef.value?.resetFields(); }

onMounted(() => { fetchData(); loadRefs(); });
</script>

<style scoped>
.crud-header { display: flex; justify-content: space-between; margin-bottom: 16px; }
</style>

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
        <el-table-column prop="tenant_name" label="租户" />
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
        <el-form-item label="租户" prop="tenant_id">
          <el-select v-model="form.tenant_id" style="width:100%" placeholder="请选择">
            <el-option v-for="t in tenants" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="应用" prop="app_id">
          <el-select v-model="form.app_id" style="width:100%" placeholder="请选择">
            <el-option v-for="a in apps" :key="a.id" :label="a.name" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型" prop="license_type">
          <el-select v-model="form.license_type" style="width:100%">
            <el-option label="永久" value="perpetual" />
            <el-option label="订阅" value="subscription" />
            <el-option label="试用" value="trial" />
          </el-select>
        </el-form-item>
        <el-form-item label="限额" prop="max_quota">
          <el-input-number v-model="form.max_quota" :min="-1" style="width:100%" />
          <span style="color:#909399;font-size:12px;margin-left:8px">-1 表示无限制</span>
        </el-form-item>
        <el-form-item label="开始日期" prop="start_date">
          <el-date-picker v-model="form.start_date" type="date" style="width:100%" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="结束日期">
          <el-date-picker v-model="form.end_date" type="date" style="width:100%" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" style="width:100%">
            <el-option label="有效" value="active" />
            <el-option label="停用" value="inactive" />
          </el-select>
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
const dialogVisible = ref(false);
const submitting = ref(false);
const editingId = ref(null);
const formRef = ref(null);

const form = reactive({ tenant_id: '', app_id: '', license_type: 'trial', max_quota: 100, start_date: '', end_date: '', status: 'active' });
const rules = {
  tenant_id: [{ required: true, message: '请选择租户', trigger: 'change' }],
  app_id: [{ required: true, message: '请选择应用', trigger: 'change' }],
  license_type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  max_quota: [{ required: true, message: '请输入限额', trigger: 'blur' }],
  start_date: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
};
const dialogTitle = computed(() => editingId.value ? '编辑授权' : '新增授权');

async function fetchData() {
  loading.value = true;
  try {
    const res = await listLicense(filterTenantId.value || undefined);
    tableData.value = res.data || res.rows || [];
  } finally { loading.value = false; }
}

async function loadRefs() {
  try { const r = await listTenant(); tenants.value = r.data || r.rows || []; } catch {}
  try { const r = await listApp(); apps.value = r.data || r.rows || []; } catch {}
}

function openAdd() {
  editingId.value = null;
  Object.assign(form, { tenant_id: '', app_id: '', license_type: 'trial', max_quota: 100, start_date: '', end_date: '', status: 'active' });
  dialogVisible.value = true;
}

async function openEdit(row) {
  editingId.value = row.id;
  try {
    const res = await getLicense(row.id);
    const d = res.data || row;
    Object.assign(form, d);
  } catch { Object.assign(form, row); }
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
    if (editingId.value) {
      await updateLicense(editingId.value, form);
      ElMessage.success('更新成功');
    } else {
      await addLicense(form);
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

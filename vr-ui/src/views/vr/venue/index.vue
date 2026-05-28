<template>
  <div class="crud-page">
    <el-card>
      <div class="crud-header">
        <el-input v-model="search" placeholder="搜索名称" clearable style="width:220px" @input="fetchData" />
        <el-button type="primary" @click="openAdd">新增场地</el-button>
      </div>
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="address" label="地址" />
        <el-table-column prop="tenant_name" label="所属租户" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'warning'">{{ row.status === 'active' ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="token" label="Token" :show-overflow-tooltip="true" />
        <el-table-column label="操作" width="240">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" link type="success" @click="regen(row)">重置Token</el-button>
            <el-button size="small" link type="danger" @click="handleDel(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="500px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="form.address" />
        </el-form-item>
        <el-form-item label="租户" prop="tenant_id">
          <el-select v-model="form.tenant_id" style="width:100%" placeholder="请选择租户">
            <el-option v-for="t in tenants" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" style="width:100%">
            <el-option label="启用" value="active" />
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
import { listVenue, getVenue, addVenue, updateVenue, delVenue, regenToken } from '@/api/vr/venue';
import { listTenant } from '@/api/vr/tenant';

const search = ref('');
const loading = ref(false);
const tableData = ref([]);
const tenants = ref([]);
const dialogVisible = ref(false);
const submitting = ref(false);
const editingId = ref(null);
const formRef = ref(null);

const form = reactive({ name: '', address: '', tenant_id: '', status: 'active' });
const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  tenant_id: [{ required: true, message: '请选择租户', trigger: 'change' }],
};
const dialogTitle = computed(() => editingId.value ? '编辑场地' : '新增场地');

async function fetchData() {
  loading.value = true;
  try {
    const res = await listVenue();
    let data = res.data || res.rows || [];
    if (search.value) data = data.filter(d => d.name && d.name.includes(search.value));
    tableData.value = data;
  } finally { loading.value = false; }
}

async function loadTenants() {
  try {
    const res = await listTenant();
    tenants.value = res.data || res.rows || [];
  } catch {}
}

function openAdd() {
  editingId.value = null;
  Object.assign(form, { name: '', address: '', tenant_id: '', status: 'active' });
  dialogVisible.value = true;
}

async function openEdit(row) {
  editingId.value = row.id;
  try {
    const res = await getVenue(row.id);
    const d = res.data || row;
    Object.assign(form, { name: d.name, address: d.address, tenant_id: d.tenant_id, status: d.status });
  } catch { Object.assign(form, row); }
  dialogVisible.value = true;
}

async function handleDel(row) {
  await ElMessageBox.confirm('确认删除该场地？', '提示', { type: 'warning' });
  await delVenue(row.id);
  ElMessage.success('删除成功');
  fetchData();
}

async function regen(row) {
  await ElMessageBox.confirm('确认重置Token？旧Token将立即失效', '提示', { type: 'warning' });
  const res = await regenToken(row.id);
  ElMessage.success('新Token: ' + (res.data?.token || '已重置'));
  fetchData();
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;
  submitting.value = true;
  try {
    if (editingId.value) {
      await updateVenue(editingId.value, form);
      ElMessage.success('更新成功');
    } else {
      await addVenue(form);
      ElMessage.success('新增成功');
    }
    dialogVisible.value = false;
    fetchData();
  } finally { submitting.value = false; }
}

function resetForm() { formRef.value?.resetFields(); }

onMounted(() => { fetchData(); loadTenants(); });
</script>

<style scoped>
.crud-header { display: flex; justify-content: space-between; margin-bottom: 16px; }
</style>

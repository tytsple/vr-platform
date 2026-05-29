<template>
  <div class="crud-page">
    <el-card>
      <div class="crud-header">
        <el-input v-model="search" placeholder="搜索名称" clearable style="width:220px" />
        <el-button type="primary" @click="openAdd">新增场地</el-button>
      </div>
      <el-table :data="filteredData" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="address" label="地址" :show-overflow-tooltip="true" />
        <el-table-column prop="tenantId" label="所属租户ID" width="120" />
        <el-table-column prop="controllerToken" label="Token" :show-overflow-tooltip="true" />
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
        <el-form-item label="地址">
          <el-input v-model="form.address" type="textarea" />
        </el-form-item>
        <el-form-item label="租户" prop="tenantId">
          <el-select v-model="form.tenantId" style="width:100%" placeholder="请选择租户">
            <el-option v-for="t in tenants" :key="t.id" :label="t.name" :value="t.id" />
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

const form = reactive({ name: '', address: '', tenantId: '' });
const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  tenantId: [{ required: true, message: '请选择租户', trigger: 'change' }],
};
const dialogTitle = computed(() => editingId.value ? '编辑场地' : '新增场地');
const filteredData = computed(() => {
  if (!search.value) return tableData.value;
  return tableData.value.filter(d => d.name && d.name.includes(search.value));
});

async function fetchData() {
  loading.value = true;
  try {
    const res = await listVenue();
    tableData.value = res.data || res.rows || [];
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
  form.name = '';
  form.address = '';
  form.tenantId = '';
  dialogVisible.value = true;
}

async function openEdit(row) {
  editingId.value = row.id;
  try {
    const res = await getVenue(row.id);
    const d = res.data || row;
    form.name = d.name || '';
    form.address = d.address || '';
    form.tenantId = d.tenantId || '';
  } catch {
    form.name = row.name || '';
    form.address = row.address || '';
    form.tenantId = row.tenantId || '';
  }
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
  const token = res.data?.controller_token || res.data?.token || '已重置';
  ElMessage.success('新Token: ' + token);
  fetchData();
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;
  submitting.value = true;
  try {
    if (editingId.value) {
      await updateVenue(editingId.value, { name: form.name, address: form.address, tenantId: form.tenantId });
      ElMessage.success('更新成功');
    } else {
      await addVenue({ name: form.name, address: form.address, tenantId: form.tenantId });
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

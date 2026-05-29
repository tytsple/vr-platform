<template>
  <div class="crud-page">
    <el-card>
      <div class="crud-header">
        <el-input v-model="search" placeholder="搜索名称" clearable style="width:220px" />
        <el-button type="primary" @click="openAdd">新增租户</el-button>
      </div>
      <el-table :data="filteredData" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="contactInfo" label="联系方式" :show-overflow-tooltip="true" />
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
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="联系方式">
          <el-input v-model="form.contactInfo" type="textarea" />
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
import { listTenant, getTenant, addTenant, updateTenant, delTenant } from '@/api/vr/tenant';

const search = ref('');
const loading = ref(false);
const tableData = ref([]);
const dialogVisible = ref(false);
const submitting = ref(false);
const editingId = ref(null);
const formRef = ref(null);

const form = reactive({ name: '', contactInfo: '' });
const rules = { name: [{ required: true, message: '请输入名称', trigger: 'blur' }] };
const dialogTitle = computed(() => editingId.value ? '编辑租户' : '新增租户');
const filteredData = computed(() => {
  if (!search.value) return tableData.value;
  return tableData.value.filter(d => d.name && d.name.includes(search.value));
});

async function fetchData() {
  loading.value = true;
  try {
    const res = await listTenant();
    tableData.value = res.data || res.rows || [];
  } finally { loading.value = false; }
}

function openAdd() {
  editingId.value = null;
  form.name = '';
  form.contactInfo = '';
  dialogVisible.value = true;
}

async function openEdit(row) {
  editingId.value = row.id;
  try {
    const res = await getTenant(row.id);
    const d = res.data || row;
    form.name = d.name || '';
    form.contactInfo = d.contactInfo || '';
  } catch {
    form.name = row.name || '';
    form.contactInfo = row.contactInfo || '';
  }
  dialogVisible.value = true;
}

async function handleDel(row) {
  await ElMessageBox.confirm('确认删除该租户？', '提示', { type: 'warning' });
  await delTenant(row.id);
  ElMessage.success('删除成功');
  fetchData();
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;
  submitting.value = true;
  try {
    if (editingId.value) {
      await updateTenant(editingId.value, form);
      ElMessage.success('更新成功');
    } else {
      await addTenant(form);
      ElMessage.success('新增成功');
    }
    dialogVisible.value = false;
    fetchData();
  } finally { submitting.value = false; }
}

function resetForm() { formRef.value?.resetFields(); }

onMounted(fetchData);
</script>

<style scoped>
.crud-header { display: flex; justify-content: space-between; margin-bottom: 16px; }
</style>

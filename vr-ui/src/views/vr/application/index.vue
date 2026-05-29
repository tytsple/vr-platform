<template>
  <div class="crud-page">
    <el-card>
      <div class="crud-header">
        <el-input v-model="search" placeholder="搜索名称" clearable style="width:220px" />
        <el-button type="primary" @click="openAdd">新增应用</el-button>
      </div>
      <el-table :data="filteredData" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="description" label="描述" :show-overflow-tooltip="true" />
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="260">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" link type="success" @click="openVersions(row)">版本</el-button>
            <el-button size="small" link type="danger" @click="handleDel(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="500px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog title="版本管理" v-model="versionVisible" width="500px">
      <div style="margin-bottom:12px; display:flex; gap:8px">
        <el-input v-model="newVersion" placeholder="版本号" style="flex:1" />
        <el-button type="primary" @click="addVer">添加版本</el-button>
      </div>
      <el-table :data="versions" stripe size="small">
        <el-table-column prop="version" label="版本号" />
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="80">
          <template #default="{ row: vr }">
            <el-button size="small" link type="danger" @click="delVer(vr.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { listApp, getApp, addApp, updateApp, delApp, listVersions, addVersion, delVersion } from '@/api/vr/application';

const search = ref('');
const loading = ref(false);
const tableData = ref([]);
const dialogVisible = ref(false);
const submitting = ref(false);
const editingId = ref(null);
const formRef = ref(null);

const form = reactive({ name: '', description: '' });
const rules = { name: [{ required: true, message: '请输入名称', trigger: 'blur' }] };
const dialogTitle = computed(() => editingId.value ? '编辑应用' : '新增应用');
const filteredData = computed(() => {
  if (!search.value) return tableData.value;
  return tableData.value.filter(d => d.name && d.name.includes(search.value));
});

const versionVisible = ref(false);
const newVersion = ref('');
const versions = ref([]);
const versionAppId = ref(null);

async function fetchData() {
  loading.value = true;
  try {
    const res = await listApp();
    tableData.value = res.data || res.rows || [];
  } finally { loading.value = false; }
}

function openAdd() {
  editingId.value = null;
  form.name = '';
  form.description = '';
  dialogVisible.value = true;
}

async function openEdit(row) {
  editingId.value = row.id;
  try {
    const res = await getApp(row.id);
    const d = res.data || row;
    form.name = d.name || '';
    form.description = d.description || '';
  } catch {
    form.name = row.name || '';
    form.description = row.description || '';
  }
  dialogVisible.value = true;
}

async function handleDel(row) {
  await ElMessageBox.confirm('确认删除该应用？', '提示', { type: 'warning' });
  await delApp(row.id);
  ElMessage.success('删除成功');
  fetchData();
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;
  submitting.value = true;
  try {
    if (editingId.value) {
      await updateApp(editingId.value, { name: form.name, description: form.description });
      ElMessage.success('更新成功');
    } else {
      await addApp({ name: form.name, description: form.description });
      ElMessage.success('新增成功');
    }
    dialogVisible.value = false;
    fetchData();
  } finally { submitting.value = false; }
}

function resetForm() { formRef.value?.resetFields(); }

async function openVersions(row) {
  versionAppId.value = row.id;
  try {
    const res = await listVersions(row.id);
    versions.value = res.data || res.rows || [];
  } catch { versions.value = []; }
  versionVisible.value = true;
}

async function addVer() {
  if (!newVersion.value) return;
  await addVersion(versionAppId.value, newVersion.value);
  ElMessage.success('版本添加成功');
  newVersion.value = '';
  openVersions({ id: versionAppId.value });
}

async function delVer(vid) {
  await ElMessageBox.confirm('确认删除该版本？', '提示', { type: 'warning' });
  await delVersion(versionAppId.value, vid);
  ElMessage.success('删除成功');
  openVersions({ id: versionAppId.value });
}

onMounted(fetchData);
</script>

<style scoped>
.crud-header { display: flex; justify-content: space-between; margin-bottom: 16px; }
</style>

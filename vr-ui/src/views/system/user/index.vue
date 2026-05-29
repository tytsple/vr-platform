<template>
  <div class="crud-page">
    <el-card>
      <div class="crud-header">
        <el-input v-model="search" placeholder="搜索用户名" clearable style="width:220px" />
        <el-button type="primary" @click="openAdd">新增用户</el-button>
      </div>
      <el-table :data="filteredData" stripe v-loading="loading">
        <el-table-column prop="userId" label="ID" width="80" />
        <el-table-column prop="userName" label="用户名" />
        <el-table-column prop="nickName" label="昵称" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === '0' ? 'success' : 'danger'">{{ row.status === '0' ? '正常' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" link type="danger" @click="handleDel(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="500px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户名" prop="userName">
          <el-input v-model="form.userName" :disabled="!!editingId" />
        </el-form-item>
        <el-form-item label="密码" :prop="editingId ? '' : 'password'">
          <el-input v-model="form.password" type="password" show-password :placeholder="editingId ? '留空不修改' : '请输入密码'" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickName" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width:100%">
            <el-option label="正常" value="0" />
            <el-option label="禁用" value="1" />
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
import { listUser, getUser, addUser, updateUser, delUser } from '@/api/system/user';

const search = ref('');
const loading = ref(false);
const tableData = ref([]);
const dialogVisible = ref(false);
const submitting = ref(false);
const editingId = ref(null);
const formRef = ref(null);

const form = reactive({ userName: '', password: '', nickName: '', status: '0' });
const rules = {
  userName: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
};
const dialogTitle = computed(() => editingId.value ? '编辑用户' : '新增用户');
const filteredData = computed(() => {
  if (!search.value) return tableData.value;
  return tableData.value.filter(d => d.userName && d.userName.includes(search.value));
});

async function fetchData() {
  loading.value = true;
  try {
    const res = await listUser();
    tableData.value = res.data || res.rows || [];
  } finally { loading.value = false; }
}

function openAdd() {
  editingId.value = null;
  Object.assign(form, { userName: '', password: '', nickName: '', status: '0' });
  dialogVisible.value = true;
}

async function openEdit(row) {
  editingId.value = row.userId || row.id;
  try {
    const res = await getUser(row.userId || row.id);
    const d = res.data || row;
    form.userName = d.userName || '';
    form.nickName = d.nickName || '';
    form.status = d.status || '0';
    form.password = '';
  } catch {
    form.userName = row.userName || '';
    form.nickName = row.nickName || '';
    form.status = row.status || '0';
    form.password = '';
  }
  dialogVisible.value = true;
}

async function handleDel(row) {
  await ElMessageBox.confirm('确认删除该用户？', '提示', { type: 'warning' });
  await delUser(row.userId || row.id);
  ElMessage.success('删除成功');
  fetchData();
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;
  submitting.value = true;
  try {
    const payload = { userName: form.userName, nickName: form.nickName, status: form.status };
    if (form.password) payload.password = form.password;
    if (editingId.value) {
      await updateUser(editingId.value, payload);
      ElMessage.success('更新成功');
    } else {
      await addUser(payload);
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

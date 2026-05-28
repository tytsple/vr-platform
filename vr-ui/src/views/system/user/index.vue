<template>
  <div class="crud-page">
    <el-card>
      <div class="crud-header">
        <el-input v-model="search" placeholder="搜索用户名" clearable style="width:220px" @input="fetchData" />
        <el-button type="primary" @click="openAdd">新增用户</el-button>
      </div>
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column prop="phone" label="电话" />
        <el-table-column prop="role_name" label="角色" width="100">
          <template #default="{ row }">
            <el-tag>{{ { admin: '管理员', tenant: '租户', tenant_user: '租户用户', operator: '运维' }[row.role] || row.role }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'warning'">{{ row.status === 'active' ? '启用' : '停用' }}</el-tag>
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
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="!!editingId" />
        </el-form-item>
        <el-form-item label="密码" :prop="editingId ? '' : 'password'">
          <el-input v-model="form.password" type="password" show-password :placeholder="editingId ? '留空不修改' : '请输入密码'" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" style="width:100%">
            <el-option label="管理员" value="admin" />
            <el-option label="租户" value="tenant" />
            <el-option label="租户用户" value="tenant_user" />
            <el-option label="运维" value="operator" />
          </el-select>
        </el-form-item>
        <el-form-item label="租户" v-if="form.role === 'tenant' || form.role === 'tenant_user'">
          <el-select v-model="form.tenant_id" style="width:100%" placeholder="请选择">
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
import { listUser, getUser, addUser, updateUser, delUser } from '@/api/system/user';
import { listTenant } from '@/api/vr/tenant';

const search = ref('');
const loading = ref(false);
const tableData = ref([]);
const tenants = ref([]);
const dialogVisible = ref(false);
const submitting = ref(false);
const editingId = ref(null);
const formRef = ref(null);

const form = reactive({ username: '', password: '', email: '', phone: '', role: 'tenant', tenant_id: '', status: 'active' });
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
};
const dialogTitle = computed(() => editingId.value ? '编辑用户' : '新增用户');

async function fetchData() {
  loading.value = true;
  try {
    const res = await listUser();
    let data = res.data || res.rows || [];
    if (search.value) data = data.filter(d => d.username && d.username.includes(search.value));
    tableData.value = data;
  } finally { loading.value = false; }
}

async function loadTenants() {
  try { const r = await listTenant(); tenants.value = r.data || r.rows || []; } catch {}
}

function openAdd() {
  editingId.value = null;
  Object.assign(form, { username: '', password: '', email: '', phone: '', role: 'tenant', tenant_id: '', status: 'active' });
  dialogVisible.value = true;
}

async function openEdit(row) {
  editingId.value = row.id;
  try {
    const res = await getUser(row.id);
    const d = res.data || row;
    Object.assign(form, { ...d, password: '' });
  } catch { Object.assign(form, { ...row, password: '' }); }
  dialogVisible.value = true;
}

async function handleDel(row) {
  await ElMessageBox.confirm('确认删除该用户？', '提示', { type: 'warning' });
  await delUser(row.id);
  ElMessage.success('删除成功');
  fetchData();
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;
  submitting.value = true;
  try {
    const payload = { ...form };
    if (editingId.value && !payload.password) delete payload.password;
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

onMounted(() => { fetchData(); loadTenants(); });
</script>

<style scoped>
.crud-header { display: flex; justify-content: space-between; margin-bottom: 16px; }
</style>

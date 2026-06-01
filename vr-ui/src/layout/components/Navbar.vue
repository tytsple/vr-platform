<template>
  <header class="navbar">
    <div class="navbar-left">
      <el-button @click="$emit('toggle')" :icon="Fold" link />
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: homePath }">首页</el-breadcrumb-item>
        <el-breadcrumb-item v-for="(crumb, i) in breadcrumbs" :key="i">
          {{ crumb }}
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>
    <div class="navbar-right">
      <span class="username">{{ userStore.user?.username }}</span>
      <el-button size="small" @click="openPwdDialog">修改密码</el-button>
      <el-button size="small" @click="logout">登出</el-button>
    </div>

    <el-dialog title="修改密码" v-model="pwdDialogVisible" width="400px" @close="resetPwdForm">
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="90px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="pwdSubmitting" @click="handleChangePassword">确认</el-button>
      </template>
    </el-dialog>
  </header>
</template>

<script setup>
import { computed, ref, reactive } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useUserStore } from '@/store/modules/user';
import { useTagsViewStore } from '@/store/modules/tagsView';
import { changePassword } from '@/api/login';
import { Fold } from '@element-plus/icons-vue';

defineEmits(['toggle']);

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const tagsStore = useTagsViewStore();

const homePath = computed(() => {
  const roles = userStore.user?.roles || [];
  if (roles.includes('tenant')) return '/tenant';
  if (roles.includes('operator')) return '/operator';
  return '/admin';
});

const breadcrumbs = computed(() => {
  const segs = route.path.split('/').filter(Boolean);
  if (segs.length === 0) return [];
  return segs.map(s => {
    const matched = route.matched.find(m => m.path === s || m.path === '/' + s);
    return matched?.meta?.title || s;
  });
});

const pwdDialogVisible = ref(false);
const pwdSubmitting = ref(false);
const pwdFormRef = ref(null);
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' });

const validateConfirm = (rule, value, callback) => {
  if (value !== pwdForm.newPassword) callback(new Error('两次密码不一致'));
  else callback();
};

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [{ required: true, min: 6, message: '新密码至少6位', trigger: 'blur' }],
  confirmPassword: [{ required: true, validator: validateConfirm, trigger: 'blur' }],
};

function openPwdDialog() {
  pwdDialogVisible.value = true;
}

function resetPwdForm() {
  pwdForm.oldPassword = '';
  pwdForm.newPassword = '';
  pwdForm.confirmPassword = '';
  pwdFormRef.value?.resetFields();
}

async function handleChangePassword() {
  const valid = await pwdFormRef.value.validate().catch(() => false);
  if (!valid) return;
  pwdSubmitting.value = true;
  try {
    await changePassword(pwdForm.oldPassword, pwdForm.newPassword);
    ElMessage.success('密码修改成功');
    pwdDialogVisible.value = false;
  } finally {
    pwdSubmitting.value = false;
  }
}

function logout() {
  userStore.logout();
  tagsStore.closeAllViews();
  router.push('/login');
}
</script>

<style scoped>
.navbar {
  height: 50px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.08);
  flex-shrink: 0;
}
.navbar-left { display: flex; align-items: center; gap: 12px; }
.navbar-right { display: flex; align-items: center; gap: 16px; color: #606266; }
.username { font-size: 14px; }
</style>

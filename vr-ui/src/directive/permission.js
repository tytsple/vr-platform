import { useUserStore } from '@/store/modules/user';

export function setupPermission(app) {
  // v-hasPermi: show element only if user has the permission
  app.directive('hasPermi', {
    mounted(el, binding) {
      const value = binding.value;
      if (value && Array.isArray(value) && value.length > 0) {
        const store = useUserStore();
        const perms = store.permissions || [];
        const hasPerm = perms.includes('*:*:*') || value.some(p => perms.includes(p));
        if (!hasPerm) {
          el.parentNode && el.parentNode.removeChild(el);
        }
      }
    }
  });

  // v-hasRole: show element only if user has the role
  app.directive('hasRole', {
    mounted(el, binding) {
      const value = binding.value;
      if (value && Array.isArray(value) && value.length > 0) {
        const store = useUserStore();
        const roles = store.roles || [];
        const hasRole = value.some(r => roles.includes(r));
        if (!hasRole) {
          el.parentNode && el.parentNode.removeChild(el);
        }
      }
    }
  });
}

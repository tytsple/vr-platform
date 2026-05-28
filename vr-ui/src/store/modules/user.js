import { defineStore } from 'pinia';
import { login as loginApi, getInfo, getRouters } from '@/api/login';
import { getToken, setToken, removeToken, setUser, removeUser, getUser } from '@/utils/auth';

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken(),
    user: getUser(),
    roles: [],
    permissions: [],
  }),
  actions: {
    async login(username, password) {
      const res = await loginApi(username, password);
      setToken(res.data.token);
      this.token = res.data.token;

      // Decode JWT payload for basic info
      const payload = JSON.parse(atob(res.data.token.split('.')[1]));
      const user = {
        id: payload.user_id,
        username: payload.username,
        role: payload.role,
        tenant_id: payload.tenant_id,
      };
      setUser(user);
      this.user = user;
      this.roles = [user.role];
      this.permissions = ['*:*:*'];
    },

    async loadInfo() {
      try {
        const info = await getInfo();
        this.user = info.data?.user || this.user;
        this.roles = info.data?.roles || [];
        this.permissions = info.data?.permissions || [];
      } catch (e) {
        // info load failed — use decoded JWT data
      }
    },

    async generateRoutes() {
      try {
        const res = await getRouters();
        return res.data || [];
      } catch (e) {
        return [];
      }
    },

    logout() {
      removeToken();
      removeUser();
      this.token = null;
      this.user = null;
      this.roles = [];
      this.permissions = [];
    },
  },
});

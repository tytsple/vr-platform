import axios from 'axios';
import { getToken, removeToken } from './auth';
import { ElMessage } from 'element-plus';

const service = axios.create({
  baseURL: '',
  timeout: 15000,
});

// Request interceptor — inject token
service.interceptors.request.use(config => {
  const token = getToken();
  if (token) {
    config.headers['Authorization'] = 'Bearer ' + token;
  }
  return config;
}, error => Promise.reject(error));

// Response interceptor — unified error handling
service.interceptors.response.use(
  response => {
    const data = response.data;
    // RuoYi AjaxResult format
    if (data && data.code !== undefined) {
      if (data.code === 401) {
        removeToken();
        window.location.href = '/login';
        return Promise.reject(new Error('登录已过期'));
      }
      if (data.code !== 200) {
        ElMessage.error(data.msg || '请求失败');
        return Promise.reject(new Error(data.msg));
      }
    }
    return data;
  },
  error => {
    ElMessage.error(error.message || '网络错误');
    return Promise.reject(error);
  }
);

export default service;

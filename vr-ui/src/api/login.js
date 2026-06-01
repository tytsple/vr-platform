import request from '@/utils/request';

export function login(username, password) {
  return request({ url: '/api/auth/login', method: 'post', data: { username, password } });
}

export function getInfo() {
  return request({ url: '/api/getInfo', method: 'get' });
}

export function getRouters() {
  return request({ url: '/api/getRouters', method: 'get' });
}

export function changePassword(oldPassword, newPassword) {
  return request({ url: '/api/auth/change-password', method: 'put', data: { oldPassword, newPassword } });
}

import request from '@/utils/request';

export function listUser() { return request({ url: '/api/admin/users', method: 'get' }); }
export function getUser(id) { return request({ url: `/api/admin/users/${id}`, method: 'get' }); }
export function addUser(data) { return request({ url: '/api/admin/users', method: 'post', data }); }
export function updateUser(id, data) { return request({ url: `/api/admin/users/${id}`, method: 'put', data }); }
export function delUser(id) { return request({ url: `/api/admin/users/${id}`, method: 'delete' }); }

import request from '@/utils/request';

export function listTenant() { return request({ url: '/api/admin/tenants', method: 'get' }); }
export function getTenant(id) { return request({ url: `/api/admin/tenants/${id}`, method: 'get' }); }
export function addTenant(data) { return request({ url: '/api/admin/tenants', method: 'post', data }); }
export function updateTenant(id, data) { return request({ url: `/api/admin/tenants/${id}`, method: 'put', data }); }
export function delTenant(id) { return request({ url: `/api/admin/tenants/${id}`, method: 'delete' }); }

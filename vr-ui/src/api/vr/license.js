import request from '@/utils/request';

export function listLicense(tenantId) {
  return request({ url: '/api/admin/licenses', method: 'get', params: tenantId ? { tenant_id: tenantId } : {} });
}
export function getLicense(id) { return request({ url: `/api/admin/licenses/${id}`, method: 'get' }); }
export function addLicense(data) { return request({ url: '/api/admin/licenses', method: 'post', data }); }
export function updateLicense(id, data) { return request({ url: `/api/admin/licenses/${id}`, method: 'put', data }); }
export function delLicense(id) { return request({ url: `/api/admin/licenses/${id}`, method: 'delete' }); }

// Tenant-specific
export function listTenantLicenses() { return request({ url: '/api/tenant/licenses', method: 'get' }); }

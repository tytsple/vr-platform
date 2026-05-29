import request from '@/utils/request';

export function listVenue(tenantId) {
  return request({ url: '/api/admin/venues', method: 'get', params: tenantId ? { tenant_id: tenantId } : {} });
}
export function getVenue(id) { return request({ url: `/api/admin/venues/${id}`, method: 'get' }); }
export function addVenue(data) { return request({ url: '/api/admin/venues', method: 'post', data }); }
export function updateVenue(id, data) { return request({ url: `/api/admin/venues/${id}`, method: 'put', data }); }
export function delVenue(id) { return request({ url: `/api/admin/venues/${id}`, method: 'delete' }); }
export function regenToken(id) { return request({ url: `/api/admin/venues/${id}/regenerate-token`, method: 'post' }); }

// Tenant-specific
export function listTenantVenues() { return request({ url: '/api/tenant/venues', method: 'get' }); }

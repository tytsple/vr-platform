import request from '@/utils/request';

export function getStats(params) { return request({ url: '/api/admin/stats', method: 'get', params }); }
export function getSessions(params) { return request({ url: '/api/admin/sessions', method: 'get', params }); }

// Tenant-specific
export function getTenantStats() { return request({ url: '/api/tenant/stats', method: 'get' }); }
export function getTenantSessions() { return request({ url: '/api/tenant/sessions', method: 'get' }); }

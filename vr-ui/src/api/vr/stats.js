import request from '@/utils/request';

export function getStats(params) { return request({ url: '/api/admin/stats', method: 'get', params }); }
export function getFilteredStats(from, to) { return request({ url: '/api/admin/stats', method: 'get', params: { from, to } }); }
export function getTenantFilteredStats(from, to) { return request({ url: '/api/tenant/stats', method: 'get', params: { from, to } }); }
export function getSessions(params) { return request({ url: '/api/admin/sessions', method: 'get', params }); }

// Tenant-specific
export function getTenantStats() { return request({ url: '/api/tenant/stats', method: 'get' }); }
export function getTenantSessions() { return request({ url: '/api/tenant/sessions', method: 'get' }); }

// Operator-specific
export function getActiveSessions() { return request({ url: '/api/operator/sessions/active', method: 'get' }); }
export function getVenuesStatus() { return request({ url: '/api/operator/venues/status', method: 'get' }); }

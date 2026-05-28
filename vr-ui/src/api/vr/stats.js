import request from '@/utils/request';

export function getStats(params) { return request({ url: '/api/admin/stats', method: 'get', params }); }
export function getSessions(params) { return request({ url: '/api/admin/sessions', method: 'get', params }); }

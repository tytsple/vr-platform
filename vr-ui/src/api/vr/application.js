import request from '@/utils/request';

export function listApp() { return request({ url: '/api/admin/applications', method: 'get' }); }
export function getApp(id) { return request({ url: `/api/admin/applications/${id}`, method: 'get' }); }
export function addApp(data) { return request({ url: '/api/admin/applications', method: 'post', data }); }
export function updateApp(id, data) { return request({ url: `/api/admin/applications/${id}`, method: 'put', data }); }
export function delApp(id) { return request({ url: `/api/admin/applications/${id}`, method: 'delete' }); }
export function listVersions(id) { return request({ url: `/api/admin/applications/${id}/versions`, method: 'get' }); }
export function addVersion(id, version) { return request({ url: `/api/admin/applications/${id}/versions`, method: 'post', data: { version } }); }
export function delVersion(appId, vid) { return request({ url: `/api/admin/applications/${appId}/versions/${vid}`, method: 'delete' }); }

import request from './request'

export const adminApi = {
  getDashboard: () => request.get('/admin/dashboard'),
  getUserGrowthChart: () => request.get('/admin/charts/user-growth'),
  getTransactionChart: () => request.get('/admin/charts/transactions'),
  getConfigs: () => request.get('/admin/configs'),
  updateConfig: (key, value) => request.put(`/admin/configs/${key}`, { value: String(value) }),
  getComplaints: (params) => request.get('/admin/complaints', { params }),
  handleComplaint: (id, data) => request.post(`/admin/complaints/${id}/handle`, data),
  getAnnouncements: () => request.get('/admin/announcements'),
  createAnnouncement: (data) => request.post('/admin/announcements', data),
  updateAnnouncement: (id, data) => request.put(`/admin/announcements/${id}`, data),
  deleteAnnouncement: (id) => request.delete(`/admin/announcements/${id}`),
  getKycList: (params) => request.get('/users/admin/kyc', { params }),
  auditKyc: (kycId, data) => request.post(`/users/admin/kyc/${kycId}/audit`, data),
}

import request from './request'

export const usersApi = {
  getMyProfile: () => request.get('/users/profile'),
  getProfile: (userId) => request.get(`/users/${userId}/profile`),
  updateProfile: (data) => request.put('/users/profile', data),
  changePassword: (data) => request.post('/users/change-password', data),
  uploadAvatar: (formData) => request.post('/users/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),

  // 开发者简历
  getResume: () => request.get('/users/resume'),
  updateResume: (data) => request.put('/users/resume', data),
  addSkill: (data) => request.post('/users/skills', data),
  updateSkill: (skillId, data) => request.put(`/users/skills/${skillId}`, data),
  deleteSkill: (skillId) => request.delete(`/users/skills/${skillId}`),
  addProject: (data) => request.post('/users/projects', data),
  updateProject: (id, data) => request.put(`/users/projects/${id}`, data),
  deleteProject: (id) => request.delete(`/users/projects/${id}`),

  // 实名认证
  submitKyc: (formData) => request.post('/users/kyc', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),
  getKycStatus: () => request.get('/users/kyc/status'),

  // 企业认证
  submitEnterpriseKyc: (formData) => request.post('/enterprise/kyc', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),

  // 信用评分
  getCreditHistory: (userId) => request.get(`/users/${userId}/credit`),

  // 开发者详情（带评价和评分）
  getDeveloperProfile: (id) => request.get(`/users/developers/${id}`),

  // 开发者搜索
  searchDevelopers: (params) => request.get('/users/developers/search', { params }),

  // 管理员操作
  getAdminList: (params) => request.get('/admin/users', { params }),
  toggleUserStatus: (userId, status) => request.put(`/admin/users/${userId}/status`, { status }),
  auditKyc: (kycId, data) => request.post(`/admin/kyc/${kycId}/audit`, data)
}

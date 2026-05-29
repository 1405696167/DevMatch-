import request from './request'

export const tasksApi = {
  getList: (params) => request.get('/tasks', { params }),
  /** 企业发布押金预览：params.budgetMax */
  publishDepositPreview: (params) => request.get('/tasks/publish-deposit-preview', { params }),
  getDetail: (id) => request.get(`/tasks/${id}`),
  create: (data) => request.post('/tasks', data),
  update: (id, data) => request.put(`/tasks/${id}`, data),
  delete: (id) => request.delete(`/tasks/${id}`),
  publish: (id) => request.post(`/tasks/${id}/publish`),
  close: (id) => request.post(`/tasks/${id}/close`),

  // 投标
  bid: (taskId, data) => request.post(`/tasks/${taskId}/bids`, data),
  getBids: (taskId, params) => request.get(`/tasks/${taskId}/bids`, { params }),
  selectBid: (taskId, bidId) => request.post(`/tasks/${taskId}/bids/${bidId}/select`),
  cancelBid: (taskId, bidId) => request.delete(`/tasks/${taskId}/bids/${bidId}`),

  // 我的投标
  getMyBids: (params) => request.get('/bids/my', { params }),

  // 管理员审核
  auditTask: (id, data) => request.post(`/tasks/${id}/audit`, data)
}

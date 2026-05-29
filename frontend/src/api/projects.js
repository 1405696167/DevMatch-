import request from './request'
import { ElMessage } from 'element-plus'

/**
 * 触发浏览器原生下载（新标签打开 URL）。
 * fetch/axios 会被 IDM「Advanced Integration」拦截并向页面返回 204，导致 Blob 损坏；
 * 原生导航不走 XHR，由浏览器处理 Content-Disposition，与 IDM 直连行为一致。
 *
 * 鉴权：URL 上带 access_token（后端 JwtAuthFilter 仅对「交付物下载」GET 路径接受此参数）。
 */
export function triggerDeliverableDownload(deliverableId) {
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录')
    return
  }
  const q = new URLSearchParams({ access_token: token })
  const url = `/api/projects/deliverables/${deliverableId}/download?${q.toString()}`
  const a = document.createElement('a')
  a.href = url
  a.target = '_blank'
  a.rel = 'noopener noreferrer'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
}

export const projectsApi = {
  getList: (params) => request.get('/projects', { params }),
  getDetail: (id) => request.get(`/projects/${id}`),
  create: (data) => request.post('/projects', data),
  update: (id, data) => request.put(`/projects/${id}`, data),

  // 里程碑（后端路径：/projects/{id}/milestones 和 /projects/milestones/{milestoneId}/...）
  getMilestones: (projectId) => request.get(`/projects/${projectId}/milestones`),
  createMilestone: (projectId, data) => request.post(`/projects/${projectId}/milestones`, data),
  updateMilestone: (projectId, milestoneId, data) => request.put(`/projects/milestones/${milestoneId}`, data),
  deleteMilestone: (projectId, milestoneId) => request.delete(`/projects/milestones/${milestoneId}`),
  startMilestone: (projectId, milestoneId) => request.post(`/projects/milestones/${milestoneId}/start`),
  submitMilestone: (projectId, milestoneId) => request.post(`/projects/milestones/${milestoneId}/submit`),
  acceptMilestone: (projectId, milestoneId) => request.post(`/projects/milestones/${milestoneId}/accept`),
  rejectMilestone: (projectId, milestoneId, data) => request.post(`/projects/milestones/${milestoneId}/reject`, data),

  // 交付物（后端路径：/projects/milestones/{milestoneId}/deliverables）
  getDeliverables: (projectId, milestoneId) => request.get(`/projects/milestones/${milestoneId}/deliverables`),
  uploadDeliverable: (projectId, milestoneId, formData) => request.post(`/projects/milestones/${milestoneId}/deliverables`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),
  /** @deprecated 使用 triggerDeliverableDownload，避免 IDM 拦截 XHR/fetch */
  downloadDeliverable: (deliverableId) => {
    triggerDeliverableDownload(deliverableId)
    return Promise.resolve()
  },

  // 评价（后端统一为 POST /api/reviews，body 含 projectId）
  submitReview: (projectId, data) => {
    const payload = { projectId, ...data }
    if (typeof payload.rating === 'number') {
      payload.rating = Math.round(payload.rating)
    }
    return request.post('/reviews', payload)
  },
  /** 预留：当前后端无按项目拉取评价列表接口 */
  getReviews: (projectId) => request.get(`/projects/${projectId}/reviews`)
}

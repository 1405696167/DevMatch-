import request from './request'

/** 站内通知相关 API（私信会话已移除） */
export const messagesApi = {
  getNotifications: (params) => request.get('/messages/notifications', { params }),
  markNotificationRead: (id) => request.put(`/messages/notifications/${id}/read`),
  markAllNotificationsRead: () => request.put('/messages/notifications/read-all')
}

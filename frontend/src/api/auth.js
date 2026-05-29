import request from './request'

export const authApi = {
  login: (data) => request.post('/auth/login', data),
  register: (data) => request.post('/auth/register', data),
  logout: () => request.post('/auth/logout'),
  refreshToken: () => request.post('/auth/refresh'),
  getUserInfo: () => request.get('/auth/me'),
  sendSmsCode: (phone) => request.post('/auth/sms/send', { phone }),
  sendEmailCode: (email) => request.post('/auth/email/send', { email }),
  resetPassword: (data) => request.post('/auth/password/reset', data),
  changePassword: (data) => request.put('/auth/password/change', data)
}

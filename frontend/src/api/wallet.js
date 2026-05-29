import request from './request'

export const walletApi = {
  getBalance: () => request.get('/wallet/balance'),
  getTransactions: (params) => request.get('/wallet/transactions', { params }),
  recharge: (data) => request.post('/wallet/recharge', data),
  withdraw: (data) => request.post('/wallet/withdraw', data),
  getWithdrawals: (params) => request.get('/wallet/withdrawals', { params }),
  getPayOrders: (params) => request.get('/wallet/orders', { params }),
  // 管理员操作
  auditWithdrawal: (id, data) => request.post(`/admin/withdrawals/${id}/audit`, data)
}

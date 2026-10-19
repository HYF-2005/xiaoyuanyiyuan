import http from './http'

// 财务只读接口：金额由订单状态推导，不发起真实支付。
export async function fetchFinanceSummary() {
  const { data } = await http.get('/admin/finance/summary')
  return data
}

export async function fetchFinanceLedger(params) {
  const { data } = await http.get('/admin/finance/ledger', { params })
  return data
}

<template>
  <div class="page-container">
    <h2 class="page-title">我的钱包</h2>

    <!-- 余额卡片 -->
    <div class="balance-cards">
      <div class="balance-card balance-main">
        <div class="balance-label">资产合计（可用+冻结）</div>
        <div class="balance-amount">¥{{ formatMoney(totalAssets) }}</div>
        <div class="balance-sub">
          <span>可用（可提现）：¥{{ formatMoney(wallet.balance) }}</span>
          <span>冻结：¥{{ formatMoney(wallet.frozen) }}</span>
        </div>
        <div class="balance-actions">
          <el-button @click="openRecharge" style="background:rgba(255,255,255,0.2);border-color:rgba(255,255,255,0.4);color:white">充值</el-button>
          <el-button v-if="isDeveloper" @click="withdrawVisible = true" style="background:rgba(255,255,255,0.2);border-color:rgba(255,255,255,0.4);color:white">提现</el-button>
        </div>
      </div>
      <div class="balance-stat-card card">
        <div class="stat-icon" style="background:#d1fae5"><el-icon size="20" color="#10b981"><TrendCharts /></el-icon></div>
        <div>
          <div class="stat-value">¥{{ formatMoney(monthlyFlowAmount) }}</div>
          <div class="stat-label">本月{{ isDeveloper ? '收入' : '支出' }}</div>
        </div>
      </div>
      <div class="balance-stat-card card">
        <div class="stat-icon" style="background:#ede9fe"><el-icon size="20" color="#4f46e5"><Money /></el-icon></div>
        <div>
          <div class="stat-value">¥{{ formatMoney(wallet.totalIncome) }}</div>
          <div class="stat-label">累计{{ isDeveloper ? '收入' : '支出' }}</div>
        </div>
      </div>
      <div class="balance-stat-card card">
        <div class="stat-icon" style="background:#fef3c7"><el-icon size="20" color="#f59e0b"><Timer /></el-icon></div>
        <div>
          <div class="stat-value">{{ wallet.pendingCount }}</div>
          <div class="stat-label">待结算订单</div>
        </div>
      </div>
    </div>

    <!-- 交易记录 -->
    <div class="card transaction-panel">
      <div class="panel-header">
        <div class="panel-title">交易流水</div>
        <div class="panel-filters">
          <el-select v-model="txType" size="small" style="width:110px" @change="fetchTransactions">
            <el-option label="全部类型" value="" />
            <el-option label="收入" value="INCOME" />
            <el-option label="支出" value="EXPENSE" />
            <el-option label="充值" value="RECHARGE" />
            <el-option label="提现" value="WITHDRAW" />
            <el-option label="冻结" value="FREEZE" />
          </el-select>
          <el-date-picker v-model="dateRange" type="daterange" size="small" range-separator="-" start-placeholder="开始日期" end-placeholder="结束日期" style="width:240px" @change="fetchTransactions" />
        </div>
      </div>

      <el-table :data="transactions" v-loading="loading" stripe>
        <el-table-column label="时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="getTxType(row.type)" size="small">{{ getTxLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="说明" />
        <el-table-column label="金额" width="130" align="right">
          <template #default="{ row }">
            <span :class="row.amount > 0 ? 'tx-income' : 'tx-expense'">
              {{ row.amount > 0 ? '+' : '' }}¥{{ formatMoney(Math.abs(row.amount)) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="余额" width="130" align="right">
          <template #default="{ row }">¥{{ formatMoney(row.balance) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'warning'" size="small">
              {{ row.status === 'SUCCESS' ? '成功' : '处理中' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="total > 0"
        v-model:current-page="page"
        :total="total"
        layout="total, prev, pager, next"
        background
        class="pagination"
        @change="fetchTransactions"
      />
    </div>

    <!-- 充值对话框 -->
    <el-dialog v-model="rechargeVisible" title="充值" width="420px">
      <div class="recharge-amounts">
        <div
          v-for="amt in [500, 1000, 2000, 5000, 10000, 20000]"
          :key="amt"
          class="amount-btn"
          :class="{ selected: rechargeForm.amount === amt }"
          @click="rechargeForm.amount = amt"
        >¥{{ amt.toLocaleString() }}</div>
      </div>
      <el-input-number v-model="rechargeForm.amount" :min="1" placeholder="自定义金额" style="width:100%;margin-top:12px" />
      <div class="pay-methods">
        <el-radio-group v-model="rechargeForm.method">
          <el-radio label="ALIPAY">支付宝</el-radio>
          <el-radio label="WECHAT">微信支付</el-radio>
        </el-radio-group>
      </div>
      <template #footer>
        <el-button @click="rechargeVisible = false">取消</el-button>
        <el-button type="primary" :loading="recharging" @click="handleRecharge">立即充值</el-button>
      </template>
    </el-dialog>

    <!-- 提现对话框 -->
    <el-dialog v-model="withdrawVisible" title="申请提现" width="420px">
      <el-form :model="withdrawForm" label-width="100px">
        <el-form-item label="提现金额">
          <el-input-number v-model="withdrawForm.amount" :min="1" :max="withdrawableMax" style="width:100%" />
          <div class="form-tip">可提现金额：¥{{ formatMoney(withdrawableMax) }}</div>
        </el-form-item>
        <el-form-item label="提现方式">
          <el-radio-group v-model="withdrawForm.method">
            <el-radio label="ALIPAY">支付宝</el-radio>
            <el-radio label="BANK">银行卡</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="账号信息">
          <el-input v-model="withdrawForm.account" placeholder="支付宝账号 / 银行卡号" />
        </el-form-item>
        <el-form-item label="真实姓名">
          <el-input v-model="withdrawForm.realName" placeholder="与账户一致的真实姓名" />
        </el-form-item>
      </el-form>
      <el-alert type="warning" :closable="false" style="margin-bottom:0">
        提现申请将在1-3个工作日内处理，手续费0%
      </el-alert>
      <template #footer>
        <el-button @click="withdrawVisible = false">取消</el-button>
        <el-button type="primary" :loading="withdrawing" @click="handleWithdraw">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'
import { walletApi } from '@/api/wallet'
import { formatMoney, formatDateTime } from '@/utils/format'
import { ElMessage, ElMessageBox } from 'element-plus'

const userStore = useUserStore()
const router = useRouter()
const isDeveloper = computed(() => userStore.userRole === 'DEVELOPER')
const isEnterprise = computed(() => userStore.userRole === 'ENTERPRISE')
const loading = ref(false)
const rechargeVisible = ref(false)
const withdrawVisible = ref(false)
const recharging = ref(false)
const withdrawing = ref(false)
const transactions = ref([])
const total = ref(0)
const page = ref(1)
const txType = ref('')
const dateRange = ref(null)

const wallet = ref({ balance: 0, frozen: 0, monthlyIncome: 0, monthlyExpense: 0, totalIncome: 0, pendingCount: 0 })

/** 后端仅返回 balance / frozen；提现按 balance 校验，与冻结无关 */
const monthlyFlowAmount = computed(() =>
  isDeveloper.value ? wallet.value.monthlyIncome : wallet.value.monthlyExpense
)

const withdrawableMax = computed(() => {
  const n = Number(wallet.value.balance)
  return Number.isFinite(n) && n > 0 ? n : 0
})
const totalAssets = computed(() => {
  const b = Number(wallet.value.balance)
  const f = Number(wallet.value.frozen)
  const x = (Number.isFinite(b) ? b : 0) + (Number.isFinite(f) ? f : 0)
  return x
})
const rechargeForm = reactive({ amount: 1000, method: 'ALIPAY' })
const withdrawForm = reactive({ amount: 0, method: 'ALIPAY', account: '', realName: '' })

function getTxType(t) {
  return { INCOME: 'success', EXPENSE: 'danger', RECHARGE: 'primary', WITHDRAW: 'warning', FREEZE: 'info' }[t] || 'info'
}
function getTxLabel(t) {
  return { INCOME: '收入', EXPENSE: '支出', RECHARGE: '充值', WITHDRAW: '提现', FREEZE: '冻结' }[t] || t
}

function normalizeWalletPayload(d) {
  if (!d || typeof d !== 'object') {
    return { balance: 0, frozen: 0, monthlyIncome: 0, monthlyExpense: 0, totalIncome: 0, pendingCount: 0 }
  }
  const num = (v) => {
    const n = Number(v)
    return Number.isFinite(n) ? n : 0
  }
  return {
    balance: num(d.balance),
    frozen: num(d.frozen),
    totalIncome: num(d.totalIncome),
    totalExpense: num(d.totalExpense),
    monthlyIncome: num(d.monthlyIncome),
    monthlyExpense: num(d.monthlyExpense),
    pendingCount: Number(d.pendingCount) || 0
  }
}

async function fetchWallet() {
  try {
    const res = await walletApi.getBalance()
    wallet.value = normalizeWalletPayload(res.data)
  } catch {
    wallet.value = normalizeWalletPayload(null)
  }
}

async function fetchTransactions() {
  loading.value = true
  try {
    const res = await walletApi.getTransactions({ type: txType.value, page: page.value, size: 10 })
    transactions.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch {
    transactions.value = [
      { id: 1, createdAt: new Date(), type: 'INCOME', description: '项目「企业官网重构」里程碑1验收款', amount: 2000, balance: 20000, status: 'SUCCESS' },
      { id: 2, createdAt: new Date(Date.now() - 86400000), type: 'RECHARGE', description: '账户充值', amount: 5000, balance: 18000, status: 'SUCCESS' },
      { id: 3, createdAt: new Date(Date.now() - 172800000), type: 'FREEZE', description: '项目「小程序开发」资金冻结', amount: -8600, balance: 13000, status: 'SUCCESS' }
    ]
    total.value = 3
  } finally { loading.value = false }
}

async function openRecharge() {
  // 企业用户充值前必须完成认证
  if (isEnterprise.value) {
    try { await userStore.refreshUserInfo() } catch {}
    const kyc = userStore.kycStatus
    if (kyc !== 'VERIFIED') {
      if (kyc === 'AUDITING') {
        await ElMessageBox.alert('您的企业认证正在审核中，审核通过后即可充值。', '认证审核中', { confirmButtonText: '我知道了', type: 'warning' })
      } else {
        try {
          await ElMessageBox.confirm(
            '充值需要先完成企业认证，认证通过后才能进行充值。是否前往进行企业认证？',
            '需要企业认证',
            { confirmButtonText: '去认证', cancelButtonText: '取消', type: 'warning' }
          )
          router.push('/enterprise/profile?tab=kyc')
        } catch {}
      }
      return
    }
  }
  rechargeVisible.value = true
}

async function handleRecharge() {
  recharging.value = true
  try {
    const res = await walletApi.recharge({ amount: rechargeForm.amount, method: rechargeForm.method })
    if (res.data?.payUrl) window.open(res.data.payUrl)
    else ElMessage.success('充值请求已提交')
    rechargeVisible.value = false
  } finally { recharging.value = false }
}

async function handleWithdraw() {
  if (!withdrawForm.amount || !withdrawForm.account) { ElMessage.warning('请填写完整提现信息'); return }
  withdrawing.value = true
  try {
    await walletApi.withdraw(withdrawForm)
    ElMessage.success('提现申请已提交，预计1-3个工作日到账')
    withdrawVisible.value = false
    fetchWallet()
  } finally { withdrawing.value = false }
}

watch(withdrawVisible, (open) => {
  if (open && isDeveloper.value) {
    const max = withdrawableMax.value
    withdrawForm.amount = max > 0 ? max : 0
  }
})

onMounted(() => { fetchWallet(); fetchTransactions() })
</script>

<style scoped lang="scss">
.balance-cards {
  display: grid;
  grid-template-columns: 360px repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.balance-card {
  border-radius: 16px;
  padding: 28px;
  color: white;
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
}

.balance-label { font-size: 13px; opacity: 0.85; margin-bottom: 8px; }
.balance-amount { font-size: 36px; font-weight: 700; margin-bottom: 10px; }
.balance-sub { display: flex; gap: 20px; font-size: 13px; opacity: 0.8; margin-bottom: 20px; }
.balance-actions { display: flex; gap: 10px; }

.balance-stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  .stat-icon { width: 44px; height: 44px; border-radius: 10px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
  .stat-value { font-size: 20px; font-weight: 700; }
  .stat-label { font-size: 12px; color: var(--text-muted); }
}

.transaction-panel {}
.panel-header {
  display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px;
  .panel-title { font-size: 15px; font-weight: 600; }
  .panel-filters { display: flex; gap: 10px; }
}

.tx-income { color: #10b981; font-weight: 600; }
.tx-expense { color: #ef4444; font-weight: 600; }

.pagination { margin-top: 16px; display: flex; justify-content: center; }

.recharge-amounts {
  display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; margin-bottom: 4px;
}
.amount-btn {
  border: 2px solid var(--border-color); border-radius: 8px; padding: 12px;
  text-align: center; cursor: pointer; font-weight: 500; transition: all 0.15s;
  &:hover { border-color: var(--primary-color); color: var(--primary-color); }
  &.selected { border-color: var(--primary-color); background: #ede9fe; color: var(--primary-color); }
}

.pay-methods { margin-top: 16px; }
.form-tip { font-size: 12px; color: var(--text-muted); margin-top: 4px; }
</style>

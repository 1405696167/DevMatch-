<template>
  <div class="page-container">
    <h2 class="page-title">信用与评价</h2>

    <div class="credit-overview">
      <div class="credit-score-card">
        <div class="score-circle">
          <svg viewBox="0 0 100 100" class="score-ring">
            <circle cx="50" cy="50" r="42" fill="none" stroke="#e2e8f0" stroke-width="8"/>
            <circle cx="50" cy="50" r="42" fill="none" :stroke="scoreColor" stroke-width="8"
              stroke-linecap="round" stroke-dasharray="264"
              :stroke-dashoffset="264 - (264 * creditScore / 100)" transform="rotate(-90 50 50)" />
          </svg>
          <div class="score-value">{{ creditScore }}</div>
          <div class="score-label">信用分</div>
        </div>
        <div class="score-level">
          <el-tag :type="scoreLevel.type" size="large">{{ scoreLevel.label }}</el-tag>
          <p class="score-desc">{{ scoreLevel.desc }}</p>
        </div>
      </div>

      <div class="credit-breakdown">
        <h3 style="margin-bottom:16px">信用构成</h3>
        <div v-for="item in breakdownItems" :key="item.label" class="breakdown-item">
          <div class="breakdown-label">{{ item.label }}</div>
          <div class="breakdown-bar-wrap">
            <el-progress :percentage="item.percentage" :color="item.color" :stroke-width="10" />
          </div>
          <div class="breakdown-score">{{ item.score }}</div>
        </div>
      </div>

      <div class="credit-stats">
        <h3 style="margin-bottom:16px">我的表现</h3>
        <div class="stats-list">
          <div v-for="s in performanceStats" :key="s.label" class="stat-row">
            <div class="stat-icon-sm" :style="{ background: s.bg }">
              <el-icon :color="s.color"><component :is="s.icon" /></el-icon>
            </div>
            <div class="stat-info-row">
              <span>{{ s.label }}</span>
              <strong>{{ s.value }}</strong>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 评价列表 -->
    <div class="card reviews-panel">
      <div class="panel-header">
        <div class="panel-title">评价记录</div>
        <el-tabs v-model="reviewTab" class="review-tabs">
          <el-tab-pane label="收到的评价" name="received" />
          <el-tab-pane label="发出的评价" name="sent" />
        </el-tabs>
      </div>

      <div v-loading="loading" class="reviews-list">
        <div v-for="r in reviews" :key="r.id" class="review-card">
          <div class="review-header">
            <div class="reviewer-info">
              <el-avatar :size="40" :src="r.reviewer?.avatar">{{ r.reviewer?.name?.charAt(0) }}</el-avatar>
              <div>
                <div class="reviewer-name">{{ r.reviewer?.name }}</div>
                <div class="review-project">{{ r.projectName }}</div>
              </div>
            </div>
            <div class="review-rating">
              <el-rate :model-value="r.rating" disabled />
              <span class="review-time">{{ fromNow(r.createdAt) }}</span>
            </div>
          </div>
          <p class="review-content">{{ r.content }}</p>
          <div v-if="r.tags?.length" class="review-tags">
            <el-tag v-for="t in r.tags" :key="t" size="small" type="success">{{ t }}</el-tag>
          </div>
        </div>
      </div>

      <el-pagination
        v-if="total > 0"
        v-model:current-page="page"
        :total="total"
        layout="total, prev, pager, next"
        background class="pagination"
        @change="fetchReviews"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { usersApi } from '@/api/users'
import { useUserStore } from '@/stores/user'
import { fromNow } from '@/utils/format'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const creditScore = ref(100)
const avgRating = ref(0)
const goodReviewCount = ref(0)
const loading = ref(false)
const reviews = ref([])
const total = ref(0)
const page = ref(1)
const reviewTab = ref('received')

const scoreLevel = computed(() => {
  const s = creditScore.value
  if (s >= 90) return { label: '优秀', type: 'success', desc: '您的信用评分优秀，享受平台最高权益' }
  if (s >= 75) return { label: '良好', type: '', desc: '您的信用评分良好，继续保持' }
  if (s >= 60) return { label: '一般', type: 'warning', desc: '您的信用评分一般，请注意提升' }
  return { label: '较差', type: 'danger', desc: '您的信用评分较低，请提升服务质量' }
})

const scoreColor = computed(() => {
  const s = creditScore.value
  if (s >= 90) return '#10b981'
  if (s >= 75) return '#4f46e5'
  if (s >= 60) return '#f59e0b'
  return '#ef4444'
})

const breakdownItems = computed(() => {
  const score = creditScore.value
  const good = goodReviewCount.value
  const rating = avgRating.value || 0
  const ratingPct = Math.round(rating * 20)
  return [
    { label: '综合评分', percentage: ratingPct, score: `${rating ? rating.toFixed(1) : '-'}分`, color: '#10b981' },
    { label: '好评数量', percentage: Math.min(good * 5, 100), score: `${good}个`, color: '#4f46e5' },
    { label: '信用总分', percentage: score, score: `${score}分`, color: '#0ea5e9' }
  ]
})

const performanceStats = computed(() => [
  { label: '好评数', value: `${goodReviewCount.value}个`, icon: 'Star', color: '#f59e0b', bg: '#fef3c7' },
  { label: '平均评分', value: avgRating.value ? `${Number(avgRating.value).toFixed(1)}分` : '暂无', icon: 'StarFilled', color: '#4f46e5', bg: '#ede9fe' },
  { label: '信用分', value: `${creditScore.value}分`, icon: 'Medal', color: '#10b981', bg: '#d1fae5' }
])

async function fetchCreditInfo() {
  const userId = userStore.userId
  if (!userId) return
  loading.value = true
  try {
    const res = await usersApi.getCreditHistory(userId)
    const data = res.data || {}
    creditScore.value = data.creditScore ?? 100
    avgRating.value = data.avgRating ?? 0
    goodReviewCount.value = data.goodReviewCount ?? 0
    const allReviews = data.reviews || []
    reviews.value = allReviews
    total.value = allReviews.length
  } catch (err) {
    ElMessage.error('加载信用信息失败')
    reviews.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

watch(reviewTab, fetchCreditInfo)
onMounted(fetchCreditInfo)
</script>

<style scoped lang="scss">
.credit-overview {
  display: grid;
  grid-template-columns: 300px 1fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

.credit-score-card {
  background: white;
  border-radius: 12px;
  border: 1px solid var(--border-color);
  padding: 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.score-circle {
  position: relative;
  width: 140px;
  height: 140px;
  .score-ring { width: 140px; height: 140px; }
  .score-value {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -60%);
    font-size: 36px;
    font-weight: 700;
  }
  .score-label {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, 30%);
    font-size: 12px;
    color: var(--text-muted);
  }
}

.score-level { text-align: center; .score-desc { font-size: 12px; color: var(--text-secondary); margin-top: 8px; } }

.credit-breakdown {
  background: white; border-radius: 12px; border: 1px solid var(--border-color); padding: 24px;
}

.breakdown-item {
  display: flex; align-items: center; gap: 12px; margin-bottom: 12px;
}
.breakdown-label { width: 80px; font-size: 13px; flex-shrink: 0; }
.breakdown-bar-wrap { flex: 1; }
.breakdown-score { width: 50px; text-align: right; font-size: 12px; color: var(--text-muted); }

.credit-stats {
  background: white; border-radius: 12px; border: 1px solid var(--border-color); padding: 24px;
}

.stats-list { display: flex; flex-direction: column; gap: 10px; }
.stat-row { display: flex; align-items: center; gap: 10px; }
.stat-icon-sm { width: 36px; height: 36px; border-radius: 8px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.stat-info-row { display: flex; justify-content: space-between; flex: 1; font-size: 13px; color: var(--text-secondary); strong { color: var(--text-primary); } }

.reviews-panel {}
.panel-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 4px; }
.panel-title { font-size: 15px; font-weight: 600; }
.review-tabs { margin-bottom: -1px; }

.reviews-list { display: flex; flex-direction: column; gap: 16px; }
.review-card { padding: 16px 0; border-bottom: 1px solid var(--border-color); &:last-child { border-bottom: none; } }
.review-header { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 10px; }
.reviewer-info { display: flex; gap: 10px; }
.reviewer-name { font-size: 14px; font-weight: 600; margin-bottom: 2px; }
.review-project { font-size: 12px; color: var(--text-muted); }
.review-rating { display: flex; flex-direction: column; align-items: flex-end; gap: 4px; }
.review-time { font-size: 11px; color: var(--text-muted); }
.review-content { font-size: 13px; color: var(--text-secondary); line-height: 1.6; margin-bottom: 8px; }
.review-tags { display: flex; flex-wrap: wrap; gap: 4px; }
.pagination { margin-top: 16px; display: flex; justify-content: center; }
</style>

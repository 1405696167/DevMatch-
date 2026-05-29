<template>
  <div class="page-container" v-loading="loading">
    <el-button link @click="router.back()" style="margin-bottom:16px"><el-icon><ArrowLeft /></el-icon> 返回</el-button>
    <div v-if="developer" class="dev-profile-layout">
      <aside class="dev-profile-side">
        <div class="card text-center">
          <el-avatar :size="80" :src="developer.avatar">{{ developer.name?.charAt(0) }}</el-avatar>
          <div class="dev-name">{{ developer.name }}</div>
          <div class="dev-title-text">{{ developer.title }}</div>
          <div class="dev-tags">
            <el-tag v-if="developer.kycVerified" type="success" size="small">已实名</el-tag>
          </div>
          <div class="credit-display">
            <div class="credit-num">{{ developer.creditScore }}</div>
            <div class="credit-lbl">信用分</div>
          </div>
          <el-divider />
          <div class="dev-side-stats">
            <div><strong>{{ developer.completedProjects ?? 0 }}</strong><div>完成项目</div></div>
            <div><strong>{{ avgRatingText }}</strong><div>平均评分</div></div>
          </div>
          <el-divider />
          <el-button type="primary" style="width:100%" @click="inviteDev">邀请投标</el-button>
        </div>
      </aside>
      <div class="dev-profile-main">
        <div class="card">
          <h3 style="margin-bottom:12px">个人简介</h3>
          <p v-if="developer.bio" class="dev-bio">{{ developer.bio }}</p>
          <el-empty v-else description="未填写个人简介" :image-size="40" />
        </div>
        <div class="card" style="margin-top:16px">
          <h3 style="margin-bottom:12px">技能标签</h3>
          <div v-if="skillRows.length" class="skill-rows">
            <div v-for="s in skillRows" :key="s.id" class="skill-row">
              <el-tag type="primary" effect="plain">{{ s.name }}</el-tag>
              <el-rate :model-value="s.level || 0" disabled size="small" />
            </div>
          </div>
          <el-empty v-else description="暂无技能信息" :image-size="40" />
        </div>
        <div class="card" style="margin-top:16px">
          <h3 style="margin-bottom:12px">项目经验</h3>
          <div v-if="portfolioList.length" class="portfolio-list">
            <div v-for="p in portfolioList" :key="p.id" class="portfolio-item">
              <h4>{{ p.name }}</h4>
              <p v-if="p.description">{{ p.description }}</p>
              <div v-if="p.tags && p.tags.length" class="portfolio-tags">
                <el-tag v-for="(t, i) in p.tags" :key="i" size="small" type="info">{{ t }}</el-tag>
              </div>
              <div v-if="p.link" style="margin-top:8px">
                <el-link :href="p.link" target="_blank" rel="noopener noreferrer" type="primary">相关链接</el-link>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无项目经验" :image-size="40" />
        </div>
        <div class="card" style="margin-top:16px">
          <h3 style="margin-bottom:12px">评价历史</h3>
          <div v-if="reviews.length">
            <div v-for="r in reviews" :key="r.id" class="review-item">
              <div class="review-header">
                <span class="review-project">{{ r.projectName || '已完成项目' }}</span>
                <el-rate :model-value="Number(r.score ?? r.rating ?? 0)" disabled size="small" />
              </div>
              <p class="review-content">{{ r.comment || r.content || '（无文字评价）' }}</p>
              <div class="review-time">{{ fromNow(r.createdAt) }}</div>
            </div>
          </div>
          <el-empty v-else description="暂无评价记录" :image-size="40" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { usersApi } from '@/api/users'
import { fromNow } from '@/utils/format'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const developer = ref(null)
const reviews = ref([])
const avgRating = ref(0)
const skillRows = ref([])
const portfolioList = ref([])

const avgRatingText = computed(() => {
  const n = Number(avgRating.value)
  return Number.isFinite(n) && n > 0 ? n.toFixed(1) : '-'
})

function normalizePortfolioTags(tags) {
  if (!tags) return []
  if (Array.isArray(tags)) return tags
  if (typeof tags === 'string') {
    try {
      const parsed = JSON.parse(tags)
      return Array.isArray(parsed) ? parsed : []
    } catch {
      return []
    }
  }
  return []
}

async function fetchDeveloper() {
  const id = route.params.id
  if (!id || id === 'undefined') {
    ElMessage.error('开发者ID无效')
    router.back()
    return
  }
  loading.value = true
  try {
    const res = await usersApi.getDeveloperProfile(id)
    const data = res?.data ?? res
    if (!data?.user) {
      ElMessage.error('加载开发者信息失败')
      router.back()
      return
    }
    const user = data.user
    const ratingNum = Number(data.avgRating)
    avgRating.value = Number.isFinite(ratingNum) ? ratingNum : 0
    skillRows.value = Array.isArray(data.skills) ? data.skills : []
    portfolioList.value = (Array.isArray(data.portfolios) ? data.portfolios : []).map((p) => ({
      ...p,
      tags: normalizePortfolioTags(p.tags)
    }))
    developer.value = {
      id: user.id,
      name: user.nickname || user.username,
      title: user.city ? `${user.city} · 开发者` : '开发者',
      avatar: user.avatar,
      kycVerified: user.kycStatus === 'VERIFIED',
      creditScore: user.creditScore || 0,
      completedProjects: data.completedProjects ?? 0,
      rateGood: avgRating.value,
      bio: user.bio || '',
      city: user.city || '',
      homepage: user.homepage || '',
      skills: skillRows.value.map((s) => s.name),
      portfolioProjects: portfolioList.value,
      reviews: []
    }
    reviews.value = data.reviews || []
    developer.value.reviews = reviews.value
  } catch (err) {
    ElMessage.error('加载开发者信息失败')
    router.back()
  } finally { loading.value = false }
}

function inviteDev() { ElMessage.success('已发送邀请') }

onMounted(fetchDeveloper)
</script>

<style scoped lang="scss">
.dev-profile-layout { display: grid; grid-template-columns: 240px 1fr; gap: 20px; }
.text-center { text-align: center; }
.dev-name { font-size: 18px; font-weight: 700; margin: 10px 0 4px; }
.dev-title-text { font-size: 13px; color: var(--text-secondary); margin-bottom: 8px; }
.dev-tags { display: flex; justify-content: center; gap: 6px; margin-bottom: 12px; }
.credit-display { padding: 12px 0; }
.credit-num { font-size: 28px; font-weight: 700; color: #10b981; }
.credit-lbl { font-size: 12px; color: var(--text-muted); }
.dev-side-stats { display: flex; justify-content: space-around; strong { display: block; font-size: 18px; font-weight: 700; } div { font-size: 11px; color: var(--text-muted); } }
.dev-bio { font-size: 14px; color: var(--text-secondary); line-height: 1.7; }
.skill-tags { display: flex; flex-direction: column; gap: 10px; }
.skill-item { display: flex; align-items: center; gap: 12px; .skill-name { width: 100px; } }
.skill-tags-simple { display: flex; flex-wrap: wrap; gap: 8px; }
.skill-rows { display: flex; flex-direction: column; gap: 10px; }
.skill-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; flex-wrap: wrap; }
.portfolio-list { display: flex; flex-direction: column; gap: 12px; }
.portfolio-item { padding: 12px; border: 1px solid var(--border-color); border-radius: 8px; h4 { margin-bottom: 6px; } p { font-size: 13px; color: var(--text-secondary); } }
.portfolio-tags { display: flex; flex-wrap: wrap; gap: 4px; margin-top: 6px; }
.review-item { padding: 12px 0; border-bottom: 1px solid var(--border-color); &:last-child { border-bottom: none; } }
.review-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 6px; }
.review-project { font-weight: 500; font-size: 13px; }
.review-content { font-size: 13px; color: var(--text-secondary); }
.review-time { font-size: 11px; color: var(--text-muted); margin-top: 4px; }
</style>

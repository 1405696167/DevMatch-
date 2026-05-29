<template>
  <div class="page-container">
    <h2 class="page-title">开发者检索</h2>

    <div class="search-area card" style="padding:20px;margin-bottom:20px">
      <div class="search-row">
        <el-input v-model="keyword" placeholder="搜索开发者名称、技能..." size="large" clearable style="flex:1" @keyup.enter="fetchDevelopers">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="filters.skill" placeholder="技能筛选" size="large" clearable style="width:160px">
          <el-option v-for="s in popularSkills" :key="s" :label="s" :value="s" />
        </el-select>
        <el-select v-model="filters.experience" placeholder="经验" size="large" clearable style="width:130px">
          <el-option label="不限" value="" />
          <el-option label="1-3年" value="JUNIOR" />
          <el-option label="3-5年" value="SENIOR" />
          <el-option label="5年以上" value="EXPERT" />
        </el-select>
        <el-select v-model="filters.sort" size="large" style="width:140px">
          <el-option label="综合推荐" value="RECOMMEND" />
          <el-option label="信用分最高" value="CREDIT" />
          <el-option label="完成项目最多" value="PROJECTS" />
          <el-option label="好评率最高" value="RATE" />
        </el-select>
        <el-button type="primary" size="large" @click="fetchDevelopers">搜索</el-button>
      </div>
      <div class="skill-quick-filter">
        <span class="filter-label">快速筛选：</span>
        <el-check-tag
          v-for="s in hotSkills"
          :key="s"
          :checked="filters.skill === s"
          @change="filters.skill = filters.skill === s ? '' : s"
        >{{ s }}</el-check-tag>
      </div>
    </div>

    <div class="result-header">
      <span>找到 <strong>{{ total }}</strong> 位开发者</span>
    </div>

    <div v-loading="loading" class="developers-grid">
      <div v-if="developers.length === 0 && !loading">
        <el-empty description="暂无匹配的开发者" />
      </div>
      <div
        v-for="dev in developers"
        :key="dev.id"
        class="dev-card card"
        @click="router.push(`/enterprise/developers/${dev.id}`)"
      >
        <div class="dev-header">
          <el-avatar :size="56" :src="dev.avatar">{{ dev.name?.charAt(0) }}</el-avatar>
          <div class="dev-basic">
            <div class="dev-name">
              {{ dev.name }}
              <el-tag v-if="dev.kycVerified" type="success" size="small" style="margin-left:6px">已实名</el-tag>
            </div>
            <div class="dev-title">{{ dev.title }}</div>
            <div class="dev-location"><el-icon><Location /></el-icon> {{ dev.city || '远程' }}</div>
          </div>
          <div class="dev-credit">
            <div class="credit-score">{{ dev.creditScore }}</div>
            <div class="credit-label">信用分</div>
          </div>
        </div>

        <p class="dev-bio">{{ dev.bio }}</p>

        <div class="dev-skills">
          <span v-for="s in dev.skills?.slice(0, 6)" :key="s" class="tag tag-primary">{{ s }}</span>
        </div>

        <div class="dev-stats">
          <div class="dev-stat">
            <strong>{{ dev.completedProjects }}</strong>
            <span>完成项目</span>
          </div>
          <div class="dev-stat">
            <strong>{{ dev.rateGood }}%</strong>
            <span>好评率</span>
          </div>
          <div class="dev-stat">
            <el-rate :model-value="dev.rating" disabled size="small" />
            <span>{{ dev.rating }}</span>
          </div>
        </div>

        <div class="dev-footer">
          <div class="dev-price">¥{{ dev.hourlyRate }}/小时</div>
          <div class="dev-btns" @click.stop>
            <el-button size="small" @click="inviteDev(dev)">邀请投标</el-button>
          </div>
        </div>
      </div>
    </div>

    <el-pagination
      v-if="total > 0"
      v-model:current-page="page"
      :total="total"
      layout="total, prev, pager, next"
      background
      class="pagination"
      @change="fetchDevelopers"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { usersApi } from '@/api/users'
import { SKILL_TAGS } from '@/utils/format'
import { ElMessage } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const developers = ref([])
const total = ref(0)
const page = ref(1)
const keyword = ref('')

const filters = reactive({ skill: '', experience: '', sort: 'RECOMMEND' })
const popularSkills = SKILL_TAGS
const hotSkills = ['Vue', 'React', 'Spring Boot', 'Python', 'Flutter', 'Node.js', '小程序', 'Java']

async function fetchDevelopers() {
  loading.value = true
  try {
    const res = await usersApi.searchDevelopers({ keyword: keyword.value, ...filters, page: page.value, size: 12 })
    developers.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch {
    developers.value = [
      { id: 1, name: '张小明', title: '全栈开发工程师', city: '北京', avatar: '', kycVerified: true, creditScore: 98, bio: '5年全栈开发经验，擅长Vue3+Spring Boot，完成多个企业级项目', skills: ['Vue3', 'Spring Boot', 'MySQL', 'Redis', 'TypeScript'], completedProjects: 15, rateGood: 98, rating: 4.8, hourlyRate: 200 },
      { id: 2, name: '李大伟', title: '移动端开发工程师', city: '上海', avatar: '', kycVerified: true, creditScore: 95, bio: '专注移动端3年，iOS/Android双端开发，React Native专家', skills: ['React Native', 'iOS', 'Android', 'TypeScript'], completedProjects: 10, rateGood: 95, rating: 4.6, hourlyRate: 180 },
      { id: 3, name: '王小芳', title: 'Python数据工程师', city: '深圳', avatar: '', kycVerified: false, creditScore: 90, bio: '数据分析与可视化专家，擅长爬虫、数据处理和机器学习应用', skills: ['Python', 'Django', '数据分析', '机器学习', 'Vue'], completedProjects: 6, rateGood: 92, rating: 4.4, hourlyRate: 150 }
    ]
    total.value = 3
  } finally { loading.value = false }
}

function inviteDev(dev) {
  ElMessage.success(`已向 ${dev.name} 发送投标邀请`)
}

onMounted(fetchDevelopers)
</script>

<style scoped lang="scss">
.search-row { display: flex; gap: 12px; margin-bottom: 12px; }
.skill-quick-filter {
  display: flex; align-items: center; flex-wrap: wrap; gap: 6px;
  .filter-label { font-size: 12px; color: var(--text-muted); }
}
.result-header { font-size: 13px; color: var(--text-secondary); margin-bottom: 16px; }

.developers-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.dev-card {
  cursor: pointer;
  transition: all 0.2s;
  &:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.1); transform: translateY(-2px); }
}

.dev-header {
  display: flex; gap: 12px; align-items: flex-start; margin-bottom: 12px;
}
.dev-basic { flex: 1; }
.dev-name { font-size: 16px; font-weight: 600; margin-bottom: 3px; }
.dev-title { font-size: 13px; color: var(--text-secondary); margin-bottom: 3px; }
.dev-location { display: flex; align-items: center; gap: 3px; font-size: 12px; color: var(--text-muted); }

.dev-credit { text-align: center; }
.credit-score { font-size: 22px; font-weight: 700; color: #10b981; }
.credit-label { font-size: 11px; color: var(--text-muted); }

.dev-bio {
  font-size: 13px; color: var(--text-secondary); line-height: 1.5;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
  margin-bottom: 10px;
}

.dev-skills { display: flex; flex-wrap: wrap; gap: 4px; margin-bottom: 12px; }

.dev-stats {
  display: flex; gap: 20px; padding: 10px 0;
  border-top: 1px solid var(--border-color); border-bottom: 1px solid var(--border-color); margin-bottom: 12px;
}
.dev-stat { display: flex; align-items: center; gap: 4px; font-size: 12px; color: var(--text-secondary); strong { color: var(--text-primary); } }

.dev-footer { display: flex; align-items: center; justify-content: space-between; }
.dev-price { font-size: 15px; font-weight: 600; color: var(--primary-color); }
.dev-btns { display: flex; gap: 8px; }

.pagination { margin-top: 20px; display: flex; justify-content: center; }
</style>

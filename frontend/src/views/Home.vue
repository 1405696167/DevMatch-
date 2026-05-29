<template>
  <div class="home-page">
    <!-- 导航栏 -->
    <header class="home-nav">
      <div class="nav-inner">
        <div class="nav-logo">
          <el-icon size="28" color="#4f46e5"><Connection /></el-icon>
          <span>DevMatch</span>
        </div>
        <nav class="nav-links">
          <a href="#features">产品特性</a>
          <a href="#how">如何运作</a>
          <a href="#tasks">热门任务</a>
        </nav>
        <div class="nav-actions">
          <el-button @click="router.push('/auth/login')">登录</el-button>
          <el-button type="primary" @click="router.push('/auth/register')">免费注册</el-button>
        </div>
      </div>
    </header>

    <!-- Hero 区域 -->
    <section class="hero-section">
      <div class="hero-inner">
        <div class="hero-badge">🚀 最专业的软件项目对接平台</div>
        <h1 class="hero-title">
          连接<span class="highlight">优秀开发者</span><br/>与<span class="highlight">软件需求方</span>
        </h1>
        <p class="hero-desc">
          在 DevMatch，企业轻松发布开发需求，开发者高效承接项目，<br/>
          资金托管保障双方权益，让软件开发合作更简单、更安全。
        </p>
        <div class="hero-actions">
          <el-button type="primary" size="large" @click="router.push('/auth/register?role=DEVELOPER')">
            作为开发者加入
          </el-button>
          <el-button size="large" @click="router.push('/auth/register?role=ENTERPRISE')">
            发布项目需求
          </el-button>
        </div>
        <div class="hero-stats">
          <div v-for="s in heroStats" :key="s.label" class="hero-stat">
            <div class="hs-value">{{ s.value }}</div>
            <div class="hs-label">{{ s.label }}</div>
          </div>
        </div>
      </div>
    </section>

    <!-- 特性 -->
    <section id="features" class="features-section">
      <div class="section-inner">
        <div class="section-label">为什么选择 DevMatch</div>
        <h2 class="section-title">专为软件开发设计的对接平台</h2>
        <div class="features-grid">
          <div v-for="f in features" :key="f.title" class="feature-card">
            <div class="feature-icon" :style="{ background: f.bg }">
              <el-icon :size="28" :color="f.color"><component :is="f.icon" /></el-icon>
            </div>
            <h3>{{ f.title }}</h3>
            <p>{{ f.desc }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- 如何运作 -->
    <section id="how" class="how-section">
      <div class="section-inner">
        <h2 class="section-title">简单三步，开启合作</h2>
        <div class="how-tabs">
          <el-tabs v-model="howTab">
            <el-tab-pane label="我是开发者" name="developer">
              <div class="steps-list">
                <div v-for="(s, i) in devSteps" :key="s.title" class="step-item">
                  <div class="step-num">{{ i + 1 }}</div>
                  <div>
                    <h4>{{ s.title }}</h4>
                    <p>{{ s.desc }}</p>
                  </div>
                </div>
              </div>
            </el-tab-pane>
            <el-tab-pane label="我是需求方" name="enterprise">
              <div class="steps-list">
                <div v-for="(s, i) in entSteps" :key="s.title" class="step-item">
                  <div class="step-num">{{ i + 1 }}</div>
                  <div>
                    <h4>{{ s.title }}</h4>
                    <p>{{ s.desc }}</p>
                  </div>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </div>
    </section>

    <!-- CTA -->
    <section class="cta-section">
      <h2>准备好开始了吗？</h2>
      <p>加入数千名开发者和企业用户，在 DevMatch 上发现无限可能</p>
      <div class="cta-actions">
        <el-button type="primary" size="large" @click="router.push('/auth/register')">立即免费注册</el-button>
      </div>
    </section>

    <!-- 页脚 -->
    <footer class="home-footer">
      <div class="footer-inner">
        <div class="footer-brand">
          <div class="nav-logo" style="margin-bottom:8px">
            <el-icon size="24" color="#4f46e5"><Connection /></el-icon>
            <span>DevMatch</span>
          </div>
          <p>软件开发项目供需双方对接平台</p>
        </div>
        <div class="footer-links">
          <div v-for="col in footerLinks" :key="col.title" class="footer-col">
            <div class="footer-col-title">{{ col.title }}</div>
            <a v-for="l in col.links" :key="l">{{ l }}</a>
          </div>
        </div>
      </div>
      <div class="footer-bottom">
        <span>© 2026 DevMatch. 软件开发项目供需对接平台</span>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const howTab = ref('developer')

if (userStore.isLoggedIn) {
  const roleMap = { DEVELOPER: '/developer/dashboard', ENTERPRISE: '/enterprise/dashboard', ADMIN: '/admin/dashboard' }
  router.replace(roleMap[userStore.userRole] || '/auth/login')
}

const heroStats = [
  { value: '12,000+', label: '注册开发者' },
  { value: '4,000+', label: '合作企业' },
  { value: '8,000+', label: '完成项目' },
  { value: '99%', label: '按时交付率' }
]

const features = [
  { icon: 'Search', title: '精准匹配', desc: '基于技术栈、预算、经验等多维度匹配，快速找到最合适的开发者或项目', color: '#4f46e5', bg: '#ede9fe' },
  { icon: 'Lock', title: '资金托管', desc: '里程碑验收后自动释放资金，保障企业不受损失，开发者按时获酬', color: '#10b981', bg: '#d1fae5' },
  { icon: 'ChatDotRound', title: '实时沟通', desc: '内置即时通讯、文件传输、视频会议，项目沟通无障碍', color: '#0ea5e9', bg: '#e0f2fe' },
  { icon: 'Medal', title: '信用体系', desc: '双向评价机制建立信用档案，优质用户享受更多权益和曝光', color: '#f59e0b', bg: '#fef3c7' },
  { icon: 'DocumentChecked', title: '合同保障', desc: '电子合同存证，明确双方权责，出现争议有据可查', color: '#6d28d9', bg: '#f5f3ff' },
  { icon: 'TrendCharts', title: '项目管理', desc: '里程碑看板、任务分解、进度跟踪，项目过程透明可视', color: '#dc2626', bg: '#fee2e2' }
]

const devSteps = [
  { title: '完善开发者档案', desc: '填写技能标签、项目经验、作品集，通过实名认证提升可信度' },
  { title: '浏览任务市场', desc: '按技术栈、预算筛选感兴趣的项目，查看需求详情后提交投标' },
  { title: '中标启动项目', desc: '被选中后进入项目工作台，按里程碑推进开发，完成验收获取报酬' }
]

const entSteps = [
  { title: '发布开发需求', desc: '描述项目需求、技术要求、预算范围，提交审核后对开发者可见' },
  { title: '筛选开发者', desc: '查看投标列表，根据信用分、经验、报价选择最合适的开发者' },
  { title: '管控项目进度', desc: '实时跟踪开发进度，里程碑验收合格后释放对应款项' }
]

const footerLinks = [
  { title: '产品', links: ['任务市场', '开发者检索', '项目管理', '消息系统'] },
  { title: '支持', links: ['帮助中心', '用户协议', '隐私政策', '联系我们'] },
  { title: '公司', links: ['关于我们', '加入我们', '合作伙伴', '新闻动态'] }
]
</script>

<style scoped lang="scss">
.home-page { min-height: 100vh; }

.home-nav {
  position: sticky; top: 0; z-index: 100;
  background: rgba(255,255,255,0.95); backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--border-color);
}
.nav-inner { max-width: 1200px; margin: 0 auto; padding: 0 24px; height: 64px; display: flex; align-items: center; justify-content: space-between; }
.nav-logo { display: flex; align-items: center; gap: 8px; font-size: 20px; font-weight: 700; color: var(--primary-color); }
.nav-links { display: flex; gap: 24px; a { color: var(--text-secondary); font-size: 14px; &:hover { color: var(--primary-color); } } }
.nav-actions { display: flex; gap: 10px; }

.hero-section {
  background: linear-gradient(135deg, #f0f4ff 0%, #faf5ff 100%);
  padding: 80px 24px;
  text-align: center;
}
.hero-inner { max-width: 800px; margin: 0 auto; }
.hero-badge { display: inline-block; padding: 6px 16px; background: #ede9fe; color: #6d28d9; border-radius: 99px; font-size: 14px; margin-bottom: 20px; }
.hero-title { font-size: 52px; font-weight: 800; line-height: 1.2; margin-bottom: 20px; .highlight { color: var(--primary-color); } }
.hero-desc { font-size: 16px; color: var(--text-secondary); line-height: 1.8; margin-bottom: 32px; }
.hero-actions { display: flex; justify-content: center; gap: 16px; margin-bottom: 48px; }
.hero-stats { display: flex; justify-content: center; gap: 48px; }
.hero-stat { .hs-value { font-size: 28px; font-weight: 700; color: var(--primary-color); } .hs-label { font-size: 13px; color: var(--text-muted); } }

.features-section { padding: 80px 24px; background: white; }
.section-inner { max-width: 1200px; margin: 0 auto; text-align: center; }
.section-label { font-size: 13px; color: var(--primary-color); font-weight: 600; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 12px; }
.section-title { font-size: 36px; font-weight: 700; margin-bottom: 48px; }
.features-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 24px; text-align: left; }
.feature-card { padding: 24px; border-radius: 12px; border: 1px solid var(--border-color); &:hover { box-shadow: 0 4px 20px rgba(0,0,0,0.08); } }
.feature-icon { width: 56px; height: 56px; border-radius: 14px; display: flex; align-items: center; justify-content: center; margin-bottom: 16px; }
.feature-card h3 { font-size: 16px; font-weight: 600; margin-bottom: 8px; }
.feature-card p { font-size: 13px; color: var(--text-secondary); line-height: 1.6; }

.how-section { padding: 80px 24px; background: var(--bg-color); }
.how-tabs { max-width: 700px; margin: 0 auto; }
.steps-list { display: flex; flex-direction: column; gap: 24px; padding: 24px 0; }
.step-item { display: flex; align-items: flex-start; gap: 16px; text-align: left; h4 { font-size: 16px; font-weight: 600; margin-bottom: 6px; } p { font-size: 14px; color: var(--text-secondary); } }
.step-num { width: 36px; height: 36px; border-radius: 50%; background: var(--primary-color); color: white; display: flex; align-items: center; justify-content: center; font-weight: 700; flex-shrink: 0; }

.cta-section { background: linear-gradient(135deg, #4f46e5, #7c3aed); padding: 80px 24px; text-align: center; color: white; h2 { font-size: 36px; font-weight: 700; margin-bottom: 12px; } p { font-size: 16px; opacity: 0.85; margin-bottom: 32px; } }
.cta-actions { .el-button { height: 50px; padding: 0 32px; font-size: 16px; font-weight: 600; } }

.home-footer { background: #1e1e2e; color: rgba(255,255,255,0.7); padding: 48px 24px 0; }
.footer-inner { max-width: 1200px; margin: 0 auto; display: flex; gap: 48px; padding-bottom: 40px; border-bottom: 1px solid rgba(255,255,255,0.1); }
.footer-brand { .nav-logo span { color: white; } p { font-size: 13px; } }
.footer-links { display: flex; gap: 48px; flex: 1; justify-content: flex-end; }
.footer-col { display: flex; flex-direction: column; gap: 8px; }
.footer-col-title { color: white; font-weight: 600; margin-bottom: 4px; }
.footer-col a { font-size: 13px; cursor: pointer; &:hover { color: white; } }
.footer-bottom { max-width: 1200px; margin: 0 auto; padding: 16px 0; font-size: 12px; opacity: 0.5; }
</style>

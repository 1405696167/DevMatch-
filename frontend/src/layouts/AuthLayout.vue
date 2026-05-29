<template>
  <div class="auth-layout">
    <div class="auth-brand">
      <div class="brand-logo">
        <el-icon size="36" color="#4f46e5"><Connection /></el-icon>
        <span class="brand-name">DevMatch</span>
      </div>
      <div class="brand-slogan">连接需求与才华，共创软件未来</div>
      <div class="brand-features">
        <div v-for="f in features" :key="f.title" class="feature-item">
          <el-icon :size="20" color="#818cf8"><component :is="f.icon" /></el-icon>
          <div>
            <div class="feature-title">{{ f.title }}</div>
            <div class="feature-desc">{{ f.desc }}</div>
          </div>
        </div>
      </div>
    </div>
    <div class="auth-form-area">
      <router-view />
    </div>
  </div>
</template>

<script setup>
const features = [
  { icon: 'Briefcase', title: '海量项目需求', desc: '覆盖Web、移动端、AI等各类软件开发需求' },
  { icon: 'User', title: '专业开发者', desc: '严格认证的专业技术人才，保障交付质量' },
  { icon: 'Lock', title: '安全资金托管', desc: '里程碑付款机制，资金安全有保障' },
  { icon: 'Star', title: '信用评价体系', desc: '双向评价机制，打造诚信交易生态' }
]
</script>

<style scoped lang="scss">
.auth-layout {
  min-height: 100vh;
  display: flex;
}

.auth-brand {
  width: 480px;
  flex-shrink: 0;
  background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);
  padding: 60px 48px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  color: white;

  @media (max-width: 900px) {
    display: none;
  }
}

.brand-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  .brand-name {
    font-size: 28px;
    font-weight: 700;
    letter-spacing: -0.5px;
  }
}

.brand-slogan {
  font-size: 18px;
  opacity: 0.9;
  margin-bottom: 48px;
  line-height: 1.6;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 28px;
}

.feature-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  .el-icon {
    margin-top: 3px;
    flex-shrink: 0;
  }
  .feature-title {
    font-size: 15px;
    font-weight: 600;
    margin-bottom: 4px;
  }
  .feature-desc {
    font-size: 13px;
    opacity: 0.75;
  }
}

.auth-form-area {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
  background: #f8fafc;
  overflow-y: auto;
}
</style>

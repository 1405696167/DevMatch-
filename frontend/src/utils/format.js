import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

export function formatDate(date, fmt = 'YYYY-MM-DD') {
  if (!date) return ''
  return dayjs(date).format(fmt)
}

export function formatDateTime(date) {
  if (!date) return ''
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

export function fromNow(date) {
  if (!date) return ''
  return dayjs(date).fromNow()
}

export function formatMoney(amount) {
  if (amount === null || amount === undefined) return '0.00'
  return Number(amount).toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

export function formatFileSize(bytes) {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(2))} ${sizes[i]}`
}

export const TASK_STATUS_MAP = {
  DRAFT: { label: '草稿', type: 'info' },
  AUDITING: { label: '审核中', type: 'warning' },
  PUBLISHED: { label: '招募中', type: 'success' },
  EXPIRED: { label: '已过期', type: 'info' },
  IN_PROGRESS: { label: '进行中', type: 'primary' },
  CLOSED: { label: '已截止', type: 'danger' },
  COMPLETED: { label: '已完成', type: 'success' },
  REJECTED: { label: '已驳回', type: 'danger' }
}

export const PROJECT_STATUS_MAP = {
  IN_PROGRESS: { label: '进行中', type: 'primary' },
  PENDING_REVIEW: { label: '待验收', type: 'warning' },
  COMPLETED: { label: '已完成', type: 'success' },
  DISPUTE: { label: '争议中', type: 'danger' }
}

export const MILESTONE_STATUS_MAP = {
  PENDING: { label: '待开始', type: 'info' },
  IN_PROGRESS: { label: '进行中', type: 'primary' },
  SUBMITTED: { label: '已提交', type: 'warning' },
  ACCEPTED: { label: '已验收', type: 'success' },
  REJECTED: { label: '已驳回', type: 'danger' }
}

export const SKILL_TAGS = [
  'Vue', 'React', 'Angular', 'TypeScript', 'JavaScript',
  'Node.js', 'Python', 'Java', 'Go', 'PHP', 'C++',
  'Spring Boot', 'Django', 'FastAPI', 'Express',
  'MySQL', 'PostgreSQL', 'MongoDB', 'Redis',
  'Docker', 'Kubernetes', 'AWS', 'Linux',
  'Flutter', 'React Native', 'iOS', 'Android',
  'UI/UX', '微信小程序', '数据分析', '机器学习'
]

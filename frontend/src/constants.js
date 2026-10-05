export const ROLE_NAMES = {
  FAMILY: '家属', NURSE: '护士', DOCTOR: '家庭医生', ADMIN: '卫生院管理', INSURANCE: '医保经办'
}

export const STATUS_NAMES = {
  SUBMITTED: '待评估', ASSESSED: '待审批', APPROVED: '已批准', ACTIVE: '在建床',
  PAUSED: '已暂停', REJECTED: '已驳回', DISCHARGED: '已撤床'
}
export const STATUS_TYPE = {
  SUBMITTED: 'warning', ASSESSED: 'primary', APPROVED: 'success', ACTIVE: 'success',
  PAUSED: 'info', REJECTED: 'danger', DISCHARGED: 'info'
}

export const RISK_NAMES = { LOW: '低风险', MEDIUM: '中风险', HIGH: '高风险', CRITICAL: '危重' }
export const RISK_TYPE = { LOW: 'success', MEDIUM: 'warning', HIGH: 'danger', CRITICAL: 'danger' }

export const BILL_STATUS = { DRAFT: '待确认', CONFIRMED: '已确认', QUESTIONED: '有质疑', SETTLED: '已结算' }
export const BILL_TYPE = { DRAFT: 'info', CONFIRMED: 'success', QUESTIONED: 'warning', SETTLED: 'primary' }

export const CATEGORY_NAMES = {
  SERVICE: '护理服务', CONSUMABLE: '普通耗材', DRUG: '普通药品', DRUG_COLD: '冷链药品'
}
export const CATEGORY_TYPE = {
  SERVICE: 'primary', CONSUMABLE: 'warning', DRUG: 'success', DRUG_COLD: 'danger'
}

// 领用/账单明细类别（UsageKind）
export const KN = { SERVICE: '护理服务', CONSUMABLE: '耗材', DRUG: '药品' }
export const KN2 = KN

export const REVIEW_NAMES = { NONE: '无需复核', PENDING: '待复核', REVIEWED: '已复核', ESCALATED: '已升级' }
export const VISIT_STATUS = { PLANNED: '待执行', IN_PROGRESS: '进行中', COMPLETED: '已完成', CANCELED: '未完成' }

export const ABORT_NAMES = {
  NONE: '正常', FAMILY_ABSENT: '家属不在家', PATIENT_HOSPITALIZED: '患者临时住院',
  STOCK_OUT: '耗材缺货', PATIENT_REFUSED: '患者拒绝', OTHER: '其他'
}

export const RISK_EVENT_NAMES = {
  DETERIORATION: '病情恶化', PRESSURE_ULCER: '压疮', OVERTIME: '上门超时',
  COLD_CHAIN: '冷链异常', COMPLAINT: '收费投诉', OTHER: '其他异常'
}

export const ORDER_NAMES = {
  PRESCRIPTION_ADJUST: '调整处方', REVISIT: '安排复诊', REFERRAL: '建议转诊', CARE_PLAN: '调整护理方案'
}

export const QUESTION_NAMES = { OPEN: '待处理', REPLIED: '已答复', RESOLVED: '已解决', REJECTED: '不予采纳' }
export const QUESTION_TYPE = { OPEN: 'danger', REPLIED: 'warning', RESOLVED: 'success', REJECTED: 'info' }

export function money(n) {
  return '¥' + Number(n || 0).toFixed(2)
}

// ===== 短别名（页面内常用） =====
export const SN = STATUS_NAMES
export const ST = STATUS_TYPE
export const RN = RISK_NAMES
export const RT = RISK_TYPE
export const BS = BILL_STATUS
export const BT = BILL_TYPE
export const VS = VISIT_STATUS
export const AN = ABORT_NAMES
export const ON = ORDER_NAMES
export const CN = CATEGORY_NAMES
export const CT = CATEGORY_TYPE
export const RV = REVIEW_NAMES
export const EN = RISK_EVENT_NAMES
export const QN = QUESTION_NAMES
export const QT = QUESTION_TYPE

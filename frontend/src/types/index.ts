export interface Material {
  id: number
  taskId?: number
  themeId?: number
  themeName?: string
  region?: string
  materialName: string
  materialType?: string
  sourceType?: string
  sourceUrl?: string
  organization?: string
  reportPeriod?: string
  status: string
  confidence?: number
  irJson?: string
  errorMessage?: string
  createTime?: string
  updateTime?: string
}

export interface MaterialSlide {
  id: number
  materialId: number
  slideNo: number
  title?: string
  rawText?: string
  structuredContent?: string
  parseStatus?: string
}

export interface Task {
  id: number
  taskName?: string
  taskType?: string
  status: string
  progress: number
  currentAgent?: string
  creatorId?: string
  errorMessage?: string
  createTime?: string
  startTime?: string
  finishTime?: string
}

export interface TaskProgress {
  taskId: number
  status?: string
  progress?: number
  currentAgent?: string
  message?: string
  eventLog?: string[]
}

export interface ValidationResult {
  id: number
  taskId?: number
  materialId: number
  ruleId?: number
  ruleCode?: string
  category: string
  severity: string
  metricName?: string
  actualValue?: string
  expectedValue?: string
  message: string
  suggestion?: string
  sourceRefs?: string
  status: string
  createTime?: string
}

export interface Rule {
  id: number
  ruleCode: string
  name: string
  category: string
  description?: string
  severity: string
  params?: string
  builtin?: boolean
  enabled: boolean
}

export interface ModelConfig {
  id: number
  name: string
  provider: string
  baseUrl: string
  apiKey?: string
  modelName: string
  capabilities?: string
  temperature?: number
  timeoutSeconds?: number
  enabled: boolean
}

export interface Metric {
  id: number
  materialId: number
  slideId?: number
  metricName: string
  normalizedName?: string
  value?: number
  unit?: string
  period?: string
  sourceRefs?: string
  confidence?: number
}

export interface Knowledge {
  id: number
  materialId: number
  knowledgeType: string
  content: string
  sourceRefs?: string
  confidence?: number
}

export interface Summary {
  materialId: number
  organization?: string
  reportPeriod?: string
  materialName?: string
  slideCount: number
  metricCount: number
  findingCount: number
  criticalCount: number
  highCount: number
  mediumCount: number
  lowCount: number
  riskCount: number
  highlightCount: number
  businessScore?: number
  summaryText?: string
}

export interface LlmTrace {
  id: number
  taskId?: number
  materialId?: number
  agentName?: string
  skillName?: string
  modelName?: string
  provider?: string
  prompt?: string
  response?: string
  inputTokens?: number
  outputTokens?: number
  latencyMs?: number
  status: string
  errorMessage?: string
  createTime?: string
}

export interface ThemeModelField {
  id?: number
  modelId?: number
  fieldCode: string
  fieldName: string
  fieldType: string
  unit?: string
  comment?: string
  seqNo?: number
}

export interface ThemeModel {
  id: number
  code: string
  name: string
  themeId: number
  version: number
  currentVersion: boolean
  fields: ThemeModelField[]
}

export interface RulePackageItem {
  id?: number
  packageId?: number
  ruleCode: string
  name: string
  ruleType?: string
  scope?: string
  inputFields?: string
  executionStrategy?: string
  description?: string
  severity?: string
  enabled: boolean
}

export interface RulePackage {
  id?: number
  code: string
  name: string
  themeId?: number
  packageType: string
  description?: string
  enabled: boolean
  items?: RulePackageItem[]
}

export interface Theme {
  id: number
  code: string
  name: string
  description?: string
  model?: ThemeModel
  rulePackages?: RulePackage[]
}

export interface FactSource {
  id: number
  materialId: number
  organization?: string
  period?: string
  chapter: string
  slideRange?: string
  structuredFacts?: string
  parseJson?: string
  status: string
  version: number
}

export interface ModelData {
  id: number
  materialId: number
  organization?: string
  period?: string
  modelId?: number
  modelVersion?: number
  factSourceId?: number
  fieldCode: string
  fieldValue?: number
  unit?: string
  status: string
}

export interface AnalysisFinding {
  severity: string
  message: string
  subject?: string
  sourceIds?: string[]
  ruleId?: string
  suggestion?: string
  evidence?: string[]
  reason?: string
}

export interface AnalysisResult {
  id: number
  materialId: number
  themeId?: number
  packageId?: number
  resultType: string
  verdict: string
  resultJson: string
  status: string
  version: number
  errorMessage?: string
  createTime?: string
}

export interface FollowUp {
  id: number
  materialId: number
  title: string
  message?: string
  suggestion?: string
  sourceLabel?: string
  status: string
  createTime?: string
  updateTime?: string
}

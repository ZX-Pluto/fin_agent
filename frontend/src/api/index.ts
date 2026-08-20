import { http } from './client'
import type {
  AnalysisResult,
  FactSource,
  FollowUp,
  Knowledge,
  LlmTrace,
  Material,
  MaterialSlide,
  Metric,
  ModelData,
  ModelConfig,
  Rule,
  RulePackage,
  RulePackageItem,
  Summary,
  Task,
  TaskProgress,
  Theme,
  ValidationResult
} from '../types'

export const listMaterials = (params?: { status?: string; organization?: string }) =>
  http.get<Material[]>('/materials', { params }).then((r) => r.data)

export const getMaterial = (id: number) => http.get<Material>(`/materials/${id}`).then((r) => r.data)

export const retryMaterial = (id: number) => http.post<Task>(`/materials/${id}/retry`).then((r) => r.data)

export const uploadMaterial = (file: File, region: string, organization: string, reportPeriod: string, themeId: number) => {
  const form = new FormData()
  form.append('file', file)
  form.append('region', region)
  form.append('organization', organization)
  form.append('reportPeriod', reportPeriod)
  form.append('themeId', String(themeId))
  return http.post('/materials/upload', form).then((r) => r.data)
}

export const getSlides = (id: number) => http.get<MaterialSlide[]>(`/materials/${id}/slides`).then((r) => r.data)

export const getIr = (id: number) => http.get(`/materials/${id}/ir`).then((r) => r.data)

export const getSummary = (id: number) => http.get<Summary>(`/materials/${id}/summary`).then((r) => r.data)

export const getMetrics = (id: number) => http.get<Metric[]>(`/materials/${id}/metrics`).then((r) => r.data)

export const getKnowledge = (id: number, type?: string) =>
  http.get<Knowledge[]>(`/materials/${id}/knowledge`, { params: { type } }).then((r) => r.data)

export const getMaterialTraces = (id: number) => http.get<LlmTrace[]>(`/materials/${id}/traces`).then((r) => r.data)

export const getTaskTraces = (id: number) => http.get<LlmTrace[]>(`/tasks/${id}/traces`).then((r) => r.data)

export const listTasks = () => http.get<Task[]>('/tasks').then((r) => r.data)

export const getTask = (id: number) => http.get<{ task: Task; progress: TaskProgress }>(`/tasks/${id}`).then((r) => r.data)

export const cancelTask = (id: number) => http.post<Task>(`/tasks/${id}/cancel`).then((r) => r.data)

export const listValidations = (params?: { materialId?: number; category?: string; status?: string; severity?: string }) =>
  http.get<ValidationResult[]>('/validations', { params }).then((r) => r.data)

export const confirmValidation = (id: number) => http.post<ValidationResult>(`/validations/${id}/confirm`).then((r) => r.data)

export const ignoreValidation = (id: number) => http.post<ValidationResult>(`/validations/${id}/ignore`).then((r) => r.data)

export const listRules = () => http.get<Rule[]>('/rules').then((r) => r.data)

export const createRule = (rule: Partial<Rule>) => http.post<Rule>('/rules', rule).then((r) => r.data)

export const updateRule = (id: number, rule: Partial<Rule>) => http.put<Rule>(`/rules/${id}`, rule).then((r) => r.data)

export const deleteRule = (id: number) => http.delete(`/rules/${id}`).then((r) => r.data)

export const toggleRule = (id: number) => http.post<Rule>(`/rules/${id}/toggle`).then((r) => r.data)

export const listModels = () => http.get<ModelConfig[]>('/models').then((r) => r.data)

export const createModel = (model: Partial<ModelConfig>) => http.post<ModelConfig>('/models', model).then((r) => r.data)

export const updateModel = (id: number, model: Partial<ModelConfig>) =>
  http.put<ModelConfig>(`/models/${id}`, model).then((r) => r.data)

export const deleteModel = (id: number) => http.delete(`/models/${id}`).then((r) => r.data)

export const toggleModel = (id: number) => http.post<ModelConfig>(`/models/${id}/toggle`).then((r) => r.data)

export const testModel = (id: number) => http.post<{ success: boolean; message: string }>(`/models/${id}/test`).then((r) => r.data)

export const listThemes = () => http.get<Theme[]>('/themes').then((r) => r.data)

export const createTheme = (theme: Partial<Theme>) => http.post<Theme>('/themes', theme).then((r) => r.data)

export const updateTheme = (id: number, theme: Partial<Theme>) =>
  http.put<Theme>(`/themes/${id}`, theme).then((r) => r.data)

export const deleteTheme = (id: number) => http.delete(`/themes/${id}`).then((r) => r.data)

export const listRulePackages = (themeId?: number) =>
  http.get<RulePackage[]>('/rule-packages', { params: { themeId } }).then((r) => r.data)

export const createRulePackage = (pkg: Partial<RulePackage>) =>
  http.post<RulePackage>('/rule-packages', pkg).then((r) => r.data)

export const updateRulePackage = (id: number, pkg: Partial<RulePackage>) =>
  http.put<RulePackage>(`/rule-packages/${id}`, pkg).then((r) => r.data)

export const deleteRulePackage = (id: number) => http.delete(`/rule-packages/${id}`).then((r) => r.data)

export const toggleRulePackage = (id: number) =>
  http.post<RulePackage>(`/rule-packages/${id}/toggle`).then((r) => r.data)

export const createRulePackageItem = (packageId: number, item: Partial<RulePackageItem>) =>
  http.post<RulePackageItem>(`/rule-packages/${packageId}/items`, item).then((r) => r.data)

export const updateRulePackageItem = (packageId: number, itemId: number, item: Partial<RulePackageItem>) =>
  http.put<RulePackageItem>(`/rule-packages/${packageId}/items/${itemId}`, item).then((r) => r.data)

export const deleteRulePackageItem = (packageId: number, itemId: number) =>
  http.delete(`/rule-packages/${packageId}/items/${itemId}`).then((r) => r.data)

export const toggleRulePackageItem = (packageId: number, itemId: number) =>
  http.post<RulePackageItem>(`/rule-packages/${packageId}/items/${itemId}/toggle`).then((r) => r.data)

export const listFactSources = (materialId: number) =>
  http.get<FactSource[]>(`/materials/${materialId}/fact-sources`).then((r) => r.data)

export const regenerateFactSources = (materialId: number) =>
  http.post<FactSource[]>(`/materials/${materialId}/fact-sources/generate`).then((r) => r.data)

export const runPreAudit = (materialId: number, themeId: number) =>
  http.post<AnalysisResult>(`/materials/${materialId}/pre-audit`, null, { params: { themeId } }).then((r) => r.data)

export const getPreAudit = (materialId: number) =>
  http.get<AnalysisResult[]>(`/materials/${materialId}/pre-audit`).then((r) => r.data)

export const mapModelData = (materialId: number, themeId: number) =>
  http.post<ModelData[]>(`/materials/${materialId}/model-data/map`, null, { params: { themeId } }).then((r) => r.data)

export const getModelData = (materialId: number) =>
  http.get<ModelData[]>(`/materials/${materialId}/model-data`).then((r) => r.data)

export const runAnalysis = (materialId: number, themeId: number) =>
  http.post<AnalysisResult>(`/materials/${materialId}/analysis`, null, { params: { themeId } }).then((r) => r.data)

export const getAnalysis = (materialId: number) =>
  http.get<AnalysisResult[]>(`/materials/${materialId}/analysis`).then((r) => r.data)

export const listFollowUps = () => http.get<FollowUp[]>('/follow-ups').then((r) => r.data)

export const createFollowUp = (item: Partial<FollowUp>) =>
  http.post<FollowUp>('/follow-ups', item).then((r) => r.data)

export const updateFollowUpStatus = (id: number, status: string) =>
  http.put<FollowUp>(`/follow-ups/${id}/status`, null, { params: { status } }).then((r) => r.data)

export const syncFollowUps = () => http.post<{ created: number }>('/follow-ups/sync').then((r) => r.data)

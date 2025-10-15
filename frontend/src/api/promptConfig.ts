import http from '@/utils/http'

export interface PromptConfig {
  id?: number
  configKey: string
  configName: string
  promptContent: string
  description?: string
  enabled: boolean
  createdAt?: string
  updatedAt?: string
  createdBy?: string
  updatedBy?: string
}

/**
 * 获取所有提示词配置
 */
export const getAllPromptConfigs = async (): Promise<PromptConfig[]> => {
  const response: any = await http.get('/prompt-config')
  return response.data || response
}

/**
 * 根据ID获取提示词配置
 */
export const getPromptConfigById = async (id: number): Promise<PromptConfig> => {
  const response: any = await http.get(`/prompt-config/${id}`)
  return response.data || response
}

/**
 * 根据键名获取提示词配置
 */
export const getPromptConfigByKey = async (configKey: string): Promise<PromptConfig> => {
  const response: any = await http.get(`/prompt-config/key/${configKey}`)
  return response.data || response
}

/**
 * 创建新的提示词配置
 */
export const createPromptConfig = async (config: PromptConfig): Promise<PromptConfig> => {
  const response: any = await http.post('/prompt-config', config)
  return response.data || response
}

/**
 * 更新提示词配置
 */
export const updatePromptConfig = async (id: number, config: PromptConfig): Promise<PromptConfig> => {
  const response: any = await http.put(`/prompt-config/${id}`, config)
  return response.data || response
}

/**
 * 删除提示词配置
 */
export const deletePromptConfig = async (id: number): Promise<void> => {
  await http.delete(`/prompt-config/${id}`)
}

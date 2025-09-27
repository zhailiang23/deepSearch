/**
 * 数据导出工具
 * 支持 Excel、CSV、JSON 格式导出
 */

/**
 * 导出数据类型
 */
export type ExportData = Record<string, any>[]

/**
 * 导出格式类型
 */
export type ExportFormat = 'excel' | 'csv' | 'json'

/**
 * 导出选项
 */
export interface ExportOptions {
  filename?: string
  format?: ExportFormat
  encoding?: string
  delimiter?: string
  sheetName?: string
  includeHeaders?: boolean
  dateFormat?: string
  numberFormat?: string
}

/**
 * Excel 导出选项
 */
export interface ExcelExportOptions extends ExportOptions {
  format: 'excel'
  sheetName?: string
  autoWidth?: boolean
  headerStyle?: any
  cellStyle?: any
}

/**
 * CSV 导出选项
 */
export interface CSVExportOptions extends ExportOptions {
  format: 'csv'
  delimiter?: string
  encoding?: string
  bom?: boolean
}

/**
 * JSON 导出选项
 */
export interface JSONExportOptions extends ExportOptions {
  format: 'json'
  pretty?: boolean
  space?: number
}

/**
 * 默认导出选项
 */
const defaultOptions: ExportOptions = {
  format: 'excel',
  encoding: 'utf-8',
  delimiter: ',',
  sheetName: 'Sheet1',
  includeHeaders: true,
  dateFormat: 'YYYY-MM-DD HH:mm:ss',
  numberFormat: '#,##0.00'
}

/**
 * 通用导出函数
 */
export async function exportData(
  data: ExportData,
  options: Partial<ExportOptions> = {}
): Promise<void> {
  const opts = { ...defaultOptions, ...options }

  if (!data || data.length === 0) {
    throw new Error('导出数据不能为空')
  }

  const filename = opts.filename || generateFilename(opts.format!)

  switch (opts.format) {
    case 'excel':
      return exportToExcel(data, { ...opts, filename } as ExcelExportOptions)
    case 'csv':
      return exportToCSV(data, { ...opts, filename } as CSVExportOptions)
    case 'json':
      return exportToJSON(data, { ...opts, filename } as JSONExportOptions)
    default:
      throw new Error(`不支持的导出格式: ${opts.format}`)
  }
}

/**
 * 导出到 Excel
 */
export async function exportToExcel(
  data: ExportData,
  options: Partial<ExcelExportOptions> = {}
): Promise<void> {
  const opts: ExcelExportOptions = {
    ...defaultOptions,
    format: 'excel',
    sheetName: 'Sheet1',
    autoWidth: true,
    ...options
  }

  try {
    // 动态导入 SheetJS，如果不可用则降级到CSV
    const XLSX = await import('xlsx').catch(() => null)

    if (!XLSX) {
      console.warn('XLSX 库不可用，降级到 CSV 导出')
      return exportToCSV(data, {
        ...opts,
        format: 'csv',
        filename: opts.filename?.replace('.xlsx', '.csv')
      })
    }

    // 创建工作簿
    const workbook = XLSX.utils.book_new()

    // 创建工作表
    const worksheet = XLSX.utils.json_to_sheet(data, {
      header: opts.includeHeaders ? undefined : []
    })

    // 自动列宽
    if (opts.autoWidth) {
      const colWidths = getColumnWidths(data)
      worksheet['!cols'] = colWidths
    }

    // 添加工作表到工作簿
    XLSX.utils.book_append_sheet(workbook, worksheet, opts.sheetName)

    // 生成 Excel 文件
    const excelBuffer = XLSX.write(workbook, {
      bookType: 'xlsx',
      type: 'array'
    })

    // 下载文件
    downloadFile(
      new Blob([excelBuffer], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }),
      opts.filename || generateFilename('excel')
    )
  } catch (error) {
    console.error('Excel 导出失败:', error)
    // 降级到 CSV 导出
    console.warn('降级到 CSV 导出')
    return exportToCSV(data, {
      ...opts,
      format: 'csv',
      filename: opts.filename?.replace('.xlsx', '.csv')
    })
  }
}

/**
 * 导出到 CSV
 */
export function exportToCSV(
  data: ExportData,
  options: Partial<CSVExportOptions> = {}
): void {
  const opts: CSVExportOptions = {
    ...defaultOptions,
    format: 'csv',
    delimiter: ',',
    bom: true,
    ...options
  }

  if (!data || data.length === 0) {
    throw new Error('CSV 导出数据不能为空')
  }

  try {
    let csvContent = ''

    // 添加 BOM (Byte Order Mark) 支持中文
    if (opts.bom) {
      csvContent = '\uFEFF'
    }

    // 获取表头
    const headers = Object.keys(data[0])

    // 添加表头
    if (opts.includeHeaders) {
      csvContent += headers.map(header => `"${header}"`).join(opts.delimiter!) + '\n'
    }

    // 添加数据行
    for (const row of data) {
      const values = headers.map(header => {
        let value = row[header]

        // 处理不同数据类型
        if (value === null || value === undefined) {
          value = ''
        } else if (typeof value === 'object') {
          value = JSON.stringify(value)
        } else {
          value = String(value)
        }

        // 转义双引号并用双引号包围
        return `"${value.replace(/"/g, '""')}"`
      })

      csvContent += values.join(opts.delimiter!) + '\n'
    }

    // 创建并下载文件
    const blob = new Blob([csvContent], {
      type: `text/csv;charset=${opts.encoding}`
    })

    downloadFile(blob, opts.filename || generateFilename('csv'))
  } catch (error) {
    console.error('CSV 导出失败:', error)
    throw new Error(`CSV 导出失败: ${error}`)
  }
}

/**
 * 导出到 JSON
 */
export function exportToJSON(
  data: ExportData,
  options: Partial<JSONExportOptions> = {}
): void {
  const opts: JSONExportOptions = {
    ...defaultOptions,
    format: 'json',
    pretty: true,
    space: 2,
    ...options
  }

  try {
    const jsonString = opts.pretty
      ? JSON.stringify(data, null, opts.space)
      : JSON.stringify(data)

    const blob = new Blob([jsonString], {
      type: 'application/json;charset=utf-8'
    })

    downloadFile(blob, opts.filename || generateFilename('json'))
  } catch (error) {
    console.error('JSON 导出失败:', error)
    throw new Error(`JSON 导出失败: ${error}`)
  }
}

/**
 * 生成文件名
 */
function generateFilename(format: ExportFormat): string {
  const timestamp = new Date().toISOString().slice(0, 19).replace(/[:-]/g, '')
  const extensions = {
    excel: 'xlsx',
    csv: 'csv',
    json: 'json'
  }

  return `export_${timestamp}.${extensions[format]}`
}

/**
 * 下载文件
 */
function downloadFile(blob: Blob, filename: string): void {
  try {
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')

    link.href = url
    link.download = filename
    link.style.display = 'none'

    document.body.appendChild(link)
    link.click()

    // 清理
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
  } catch (error) {
    console.error('文件下载失败:', error)
    throw new Error(`文件下载失败: ${error}`)
  }
}

/**
 * 计算列宽
 */
function getColumnWidths(data: ExportData): any[] {
  if (!data || data.length === 0) return []

  const headers = Object.keys(data[0])
  const widths: any[] = []

  headers.forEach((header, index) => {
    let maxWidth = header.length

    // 计算该列的最大宽度
    for (const row of data) {
      const value = row[header]
      const cellWidth = value ? String(value).length : 0
      maxWidth = Math.max(maxWidth, cellWidth)
    }

    // 设置合理的列宽范围 (最小10，最大50)
    widths[index] = {
      wch: Math.min(Math.max(maxWidth + 2, 10), 50)
    }
  })

  return widths
}

/**
 * 数据预处理
 */
export function preprocessData(
  data: any[],
  options: {
    dateFields?: string[]
    numberFields?: string[]
    excludeFields?: string[]
    fieldMapping?: Record<string, string>
  } = {}
): ExportData {
  const {
    dateFields = [],
    numberFields = [],
    excludeFields = [],
    fieldMapping = {}
  } = options

  return data.map(item => {
    const processedItem: Record<string, any> = {}

    Object.keys(item).forEach(key => {
      // 跳过排除的字段
      if (excludeFields.includes(key)) return

      const mappedKey = fieldMapping[key] || key
      let value = item[key]

      // 处理日期字段
      if (dateFields.includes(key) && value) {
        try {
          value = new Date(value).toLocaleString('zh-CN')
        } catch {
          // 如果日期解析失败，保持原值
        }
      }

      // 处理数字字段
      if (numberFields.includes(key) && value !== null && value !== undefined) {
        const numValue = Number(value)
        if (!isNaN(numValue)) {
          value = numValue
        }
      }

      processedItem[mappedKey] = value
    })

    return processedItem
  })
}

/**
 * 批量导出大数据集
 */
export async function exportLargeDataset(
  dataProvider: () => Promise<ExportData>,
  options: Partial<ExportOptions> = {},
  batchSize: number = 5000
): Promise<void> {
  const opts = { ...defaultOptions, ...options }

  try {
    const data = await dataProvider()

    if (data.length <= batchSize) {
      return exportData(data, opts)
    }

    // 分批处理大数据集
    console.log(`处理大数据集，共 ${data.length} 条记录，分批大小: ${batchSize}`)

    const batches = []
    for (let i = 0; i < data.length; i += batchSize) {
      batches.push(data.slice(i, i + batchSize))
    }

    if (opts.format === 'excel') {
      // Excel 格式合并所有批次到一个文件
      return exportToExcel(data, opts as ExcelExportOptions)
    } else {
      // CSV 和 JSON 格式分别导出每个批次
      for (let i = 0; i < batches.length; i++) {
        const batchFilename = opts.filename
          ? opts.filename.replace(/(\.[^.]+)$/, `_batch${i + 1}$1`)
          : `export_batch${i + 1}_${new Date().toISOString().slice(0, 10)}`

        await exportData(batches[i], {
          ...opts,
          filename: batchFilename
        })
      }
    }
  } catch (error) {
    console.error('大数据集导出失败:', error)
    throw error
  }
}

/**
 * 验证导出数据
 */
export function validateExportData(data: any[]): { valid: boolean; errors: string[] } {
  const errors: string[] = []

  if (!Array.isArray(data)) {
    errors.push('数据必须是数组格式')
    return { valid: false, errors }
  }

  if (data.length === 0) {
    errors.push('导出数据不能为空')
    return { valid: false, errors }
  }

  // 检查数据结构一致性
  const firstRowKeys = Object.keys(data[0] || {})
  if (firstRowKeys.length === 0) {
    errors.push('数据行不能为空对象')
  }

  // 检查后续行的键是否一致
  for (let i = 1; i < Math.min(data.length, 10); i++) {
    const currentKeys = Object.keys(data[i] || {})
    const missingKeys = firstRowKeys.filter(key => !currentKeys.includes(key))
    const extraKeys = currentKeys.filter(key => !firstRowKeys.includes(key))

    if (missingKeys.length > 0 || extraKeys.length > 0) {
      errors.push(`第 ${i + 1} 行数据结构不一致`)
      break
    }
  }

  return {
    valid: errors.length === 0,
    errors
  }
}

// 便捷导出函数
export const exportExcel = (data: ExportData, filename?: string) =>
  exportToExcel(data, { filename })

export const exportCSV = (data: ExportData, filename?: string) =>
  exportToCSV(data, { filename })

export const exportJSON = (data: ExportData, filename?: string) =>
  exportToJSON(data, { filename })
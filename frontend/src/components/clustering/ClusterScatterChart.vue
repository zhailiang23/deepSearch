<template>
  <div ref="chartRef" class="cluster-scatter-chart" :style="{ height: height }"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import type { ScatterPoint } from '@/api/clustering'

interface Props {
  data: ScatterPoint[]
  height?: string
}

const props = withDefaults(defineProps<Props>(), {
  height: '500px'
})

const chartRef = ref<HTMLElement>()
let chartInstance: echarts.ECharts | null = null

// 生成颜色方案(淡绿色主题)
const generateColors = (count: number): string[] => {
  const colors = [
    '#52c41a', // 主绿色
    '#73d13d', // 亮绿色
    '#95de64', // 淡绿色
    '#bae637', // 黄绿色
    '#13c2c2', // 青色
    '#36cfc9', // 亮青色
    '#5cdbd3', // 淡青色
    '#87e8de', // 浅青色
    '#389e0d', // 深绿色
    '#237804'  // 暗绿色
  ]

  // 如果簇数量超过预定义颜色,循环使用
  if (count <= colors.length) {
    return colors.slice(0, count)
  }

  const result: string[] = []
  for (let i = 0; i < count; i++) {
    result.push(colors[i % colors.length])
  }
  return result
}

// 初始化图表
const initChart = () => {
  if (!chartRef.value) return

  chartInstance = echarts.init(chartRef.value)

  window.addEventListener('resize', () => {
    chartInstance?.resize()
  })
}

// 更新图表数据
const updateChart = () => {
  if (!chartInstance || !props.data || props.data.length === 0) return

  // 按簇 ID 分组数据
  const groupedData = new Map<number, ScatterPoint[]>()
  props.data.forEach(point => {
    if (!groupedData.has(point.cluster)) {
      groupedData.set(point.cluster, [])
    }
    groupedData.get(point.cluster)!.push(point)
  })

  // 获取所有非噪声点的簇 ID 并排序
  const clusterIds = Array.from(groupedData.keys())
    .filter(id => id !== -1)
    .sort((a, b) => a - b)

  // 生成颜色
  const colors = generateColors(clusterIds.length + 1)

  // 构建 series 数据
  const series: any[] = []

  // 添加有效簇
  clusterIds.forEach((clusterId, index) => {
    const points = groupedData.get(clusterId)!
    series.push({
      name: `簇 ${clusterId}`,
      type: 'scatter',
      data: points.map(p => [p.x, p.y, p.text]),
      symbolSize: 8,
      itemStyle: {
        color: colors[index]
      },
      emphasis: {
        itemStyle: {
          borderColor: '#333',
          borderWidth: 2
        }
      }
    })
  })

  // 添加噪声点
  if (groupedData.has(-1)) {
    const noisePoints = groupedData.get(-1)!
    series.push({
      name: '噪声点',
      type: 'scatter',
      data: noisePoints.map(p => [p.x, p.y, p.text]),
      symbolSize: 6,
      itemStyle: {
        color: '#d9d9d9',
        opacity: 0.5
      }
    })
  }

  // 配置图表选项
  const option: echarts.EChartsOption = {
    title: {
      text: '聚类散点图',
      left: 'center',
      textStyle: {
        fontSize: 16,
        color: '#333'
      }
    },
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => {
        const data = params.data
        return `<div style="max-width: 300px;">
          <strong>${params.seriesName}</strong><br/>
          查询词: ${data[2]}<br/>
          坐标: (${data[0].toFixed(2)}, ${data[1].toFixed(2)})
        </div>`
      }
    },
    legend: {
      type: 'scroll',
      orient: 'vertical',
      right: 10,
      top: 50,
      bottom: 20,
      textStyle: {
        fontSize: 12
      }
    },
    grid: {
      left: '3%',
      right: '15%',
      top: '15%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'value',
      name: 'UMAP X',
      splitLine: {
        lineStyle: {
          type: 'dashed',
          color: '#e0e0e0'
        }
      }
    },
    yAxis: {
      type: 'value',
      name: 'UMAP Y',
      splitLine: {
        lineStyle: {
          type: 'dashed',
          color: '#e0e0e0'
        }
      }
    },
    series
  }

  chartInstance.setOption(option, true)
}

onMounted(() => {
  initChart()
  updateChart()
})

watch(() => props.data, () => {
  nextTick(() => {
    updateChart()
  })
}, { deep: true })
</script>

<style scoped>
.cluster-scatter-chart {
  width: 100%;
}
</style>

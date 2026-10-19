<template>
  <div ref="chartEl" class="status-chart"></div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts/core'
import { PieChart } from 'echarts/charts'
import { LegendComponent, TooltipComponent, GraphicComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([PieChart, LegendComponent, TooltipComponent, GraphicComponent, CanvasRenderer])

const props = defineProps({
  items: { type: Array, default: () => [] }
})

const chartEl = ref()
let chart
let observer

const total = computed(() => props.items.reduce((sum, item) => sum + Number(item.total || 0), 0))

function buildOption() {
  return {
    color: ['#2f7df6', '#67a9ff', '#7fd4c3', '#1aa88d', '#c7d3e1'],
    tooltip: {
      trigger: 'item',
      backgroundColor: '#0b1f3a',
      borderWidth: 0,
      padding: [7, 10],
      textStyle: { color: '#eaf3ff', fontSize: 10 },
      formatter: '{b}<br/>{c} 单（{d}%）'
    },
    legend: {
      bottom: 0,
      left: 'center',
      icon: 'circle',
      itemWidth: 7,
      itemHeight: 7,
      itemGap: 14,
      textStyle: { color: '#8393a8', fontSize: 9.5 }
    },
    series: [
      {
        type: 'pie',
        radius: ['55%', '73%'],
        center: ['50%', '43%'],
        avoidLabelOverlap: true,
        itemStyle: { borderColor: '#fff', borderWidth: 3, borderRadius: 5 },
        label: { show: false },
        emphasis: { scale: true, scaleSize: 4 },
        data: props.items.map((item) => ({ name: item.status, value: Number(item.total || 0) }))
      }
    ],
    graphic: [
      {
        type: 'text',
        left: 'center',
        top: '33%',
        style: {
          text: String(total.value),
          fill: '#172b45',
          fontSize: 25,
          fontWeight: 650,
          textAlign: 'center'
        }
      },
      {
        type: 'text',
        left: 'center',
        top: '45%',
        style: {
          text: '订单总数',
          fill: '#9aaabe',
          fontSize: 9,
          textAlign: 'center'
        }
      }
    ]
  }
}

function render() {
  if (!chart) chart = echarts.init(chartEl.value)
  chart.setOption(buildOption(), true)
}

onMounted(() => {
  render()
  observer = new ResizeObserver(() => chart?.resize())
  observer.observe(chartEl.value)
})

watch(() => props.items, render, { deep: true })

onBeforeUnmount(() => {
  observer?.disconnect()
  chart?.dispose()
})
</script>

<style scoped>
.status-chart {
  width: 100%;
  height: 286px;
}
</style>

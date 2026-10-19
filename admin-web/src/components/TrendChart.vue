<template>
  <div ref="chartEl" class="trend-chart"></div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([LineChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

const props = defineProps({
  points: { type: Array, default: () => [] }
})

const chartEl = ref()
let chart
let observer

function axisLabel(value) {
  return value?.slice(5) || ''
}

function buildOption() {
  const dates = props.points.map((point) => axisLabel(point.date))
  const orders = props.points.map((point) => point.orders)
  const completed = props.points.map((point) => point.completed)
  const users = props.points.map((point) => point.users)

  return {
    color: ['#2f7df6', '#1aa88d', '#c69a43'],
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#0b1f3a',
      borderWidth: 0,
      padding: [8, 10],
      textStyle: { color: '#eaf3ff', fontSize: 10 },
      axisPointer: { lineStyle: { color: '#c7d6e8', type: 'dashed' } },
      formatter(params) {
        const lines = params.map(
          (item) =>
            `<div style="display:flex;align-items:center;justify-content:space-between;gap:18px;margin-top:4px;">
              <span style="color:#9fb5ce;">${item.marker}${item.seriesName}</span>
              <strong style="color:#fff;font-weight:600;">${item.value}</strong>
            </div>`
        )
        return `<div style="font-size:10px;color:#9fb5ce;margin-bottom:2px;">${params[0]?.axisValue || ''}</div>${lines.join('')}`
      }
    },
    legend: {
      top: 0,
      right: 0,
      itemWidth: 9,
      itemHeight: 6,
      itemGap: 16,
      icon: 'roundRect',
      textStyle: { color: '#8393a8', fontSize: 10 },
      data: ['新增订单', '完成订单', '新增用户']
    },
    grid: { left: 8, right: 12, top: 38, bottom: 4, containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: dates,
      axisLine: { lineStyle: { color: '#e6edf7' } },
      axisTick: { show: false },
      axisLabel: { color: '#9aaabe', fontSize: 9.5, margin: 10 }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitNumber: 4,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#9aaabe', fontSize: 9.5 },
      splitLine: { lineStyle: { color: '#eef3f9', type: 'dashed' } }
    },
    series: [
      {
        name: '新增订单',
        type: 'line',
        smooth: 0.3,
        symbol: 'circle',
        symbolSize: 5,
        showSymbol: false,
        data: orders,
        lineStyle: { width: 2.4 },
        areaStyle: { color: 'rgba(47, 125, 246, 0.10)' },
        emphasis: { focus: 'series', scale: 1.4 }
      },
      {
        name: '完成订单',
        type: 'line',
        smooth: 0.3,
        symbol: 'circle',
        symbolSize: 5,
        showSymbol: false,
        data: completed,
        lineStyle: { width: 2.2 },
        emphasis: { focus: 'series', scale: 1.4 }
      },
      {
        name: '新增用户',
        type: 'line',
        smooth: 0.3,
        symbol: 'circle',
        symbolSize: 5,
        showSymbol: false,
        data: users,
        lineStyle: { width: 1.8, type: 'dashed' },
        itemStyle: { color: '#c69a43' },
        emphasis: { focus: 'series', scale: 1.4 }
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

watch(() => props.points, render, { deep: true })

onBeforeUnmount(() => {
  observer?.disconnect()
  chart?.dispose()
})
</script>

<style scoped>
.trend-chart {
  width: 100%;
  height: 286px;
}
</style>

<template>
  <div class="adm-page">
    <div class="adm-page-head">
      <div>
        <p class="adm-eyebrow">WALLET · NOT ENABLED</p>
        <h1 class="adm-title">钱包管理</h1>
        <p class="adm-sub">当前小程序采用线下支付，没有用户钱包、余额、提现或平台调账业务。</p>
      </div>
      <span class="adm-count">功能未接入 · 不展示虚构余额</span>
    </div>

    <div class="wallet-banner">
      <div class="wallet-icon"><el-icon><Wallet /></el-icon></div>
      <div>
        <strong>钱包系统尚未启用</strong>
        <p>后台保留菜单用于后续接入，但不会把订单金额自动当作钱包余额，也不提供人工加减款入口。这样可以避免演示数据被误认为真实资金。</p>
      </div>
      <span class="adm-tag adm-tag-gray">未接入</span>
    </div>

    <div class="wallet-tabs">
      <button type="button" :class="{ active: mode === 'accounts' }" @click="switchMode('accounts')">钱包账户</button>
      <button type="button" :class="{ active: mode === 'adjust' }" @click="switchMode('adjust')">调账记录</button>
    </div>

    <section v-if="mode === 'accounts'" class="adm-panel">
      <div class="adm-panel-head">
        <div><p class="adm-panel-eyebrow">WALLET ACCOUNTS</p><h2 class="adm-panel-title">钱包账户</h2></div>
        <span class="adm-panel-meta">0 个已开通账户</span>
      </div>
      <div class="adm-empty wallet-empty">
        <el-icon><Wallet /></el-icon>
        <strong>暂无钱包账户</strong>
        <span>小程序端没有开通钱包的入口，也没有余额或冻结金额数据</span>
      </div>
      <div class="wallet-check">
        <h3>接入前必须补齐</h3>
        <div class="check-grid">
          <div><span>01</span><p>钱包账户表与余额变更流水，所有变动必须可追溯、可幂等。</p></div>
          <div><span>02</span><p>线下支付确认、提现申请、审核和打款回调，不能只靠订单状态推导。</p></div>
          <div><span>03</span><p>用户端钱包页面、实名/收款账户校验，以及风控和异常冻结策略。</p></div>
          <div><span>04</span><p>财务对账需要真实渠道流水号，而不是当前演示账本。</p></div>
        </div>
      </div>
    </section>

    <section v-else class="adm-panel">
      <div class="adm-panel-head">
        <div><p class="adm-panel-eyebrow">ADJUSTMENTS</p><h2 class="adm-panel-title">调账记录</h2></div>
        <span class="adm-panel-meta">只读占位</span>
      </div>
      <div class="adm-empty wallet-empty">
        <el-icon><Tickets /></el-icon>
        <strong>暂无调账记录</strong>
        <span>钱包未接入，因此没有余额调整、补款或扣款记录；财务页的订单流水不能当作调账凭证</span>
      </div>
      <p class="wallet-foot">未来接入调账时，必须要求填写原因、关联工单、操作管理员，并保存调整前后余额；当前不提供任何直接改余额的接口。</p>
    </section>

    <p class="adm-foot">钱包模块是完整后台菜单中的“未接入”状态页，不是被遗漏的空页面；在支付和账务模型确定前，不应展示零余额或允许人工调账。</p>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Tickets, Wallet } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const mode = computed(() => (route.name === 'wallet-adjust' ? 'adjust' : 'accounts'))

function switchMode(value) {
  router.replace(`/wallet/${value}`)
}
</script>

<style scoped>
.wallet-banner {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 18px 20px;
  margin-bottom: 16px;
  border: 1px solid #e4edf7;
  border-radius: 14px;
  background: linear-gradient(120deg, #f8fbff, #ffffff);
}

.wallet-icon {
  display: grid;
  flex: none;
  place-items: center;
  width: 38px;
  height: 38px;
  border-radius: 11px;
  background: #eef5ff;
  color: #4a8cd5;
  font-size: 18px;
}

.wallet-banner > div:nth-child(2) {
  flex: 1;
}

.wallet-banner strong {
  color: #2a425b;
  font-size: 13px;
}

.wallet-banner p {
  margin: 6px 0 0;
  color: #8798ab;
  font-size: 11px;
  line-height: 1.7;
}

.wallet-tabs {
  display: flex;
  gap: 6px;
  margin-bottom: 14px;
}

.wallet-tabs button {
  height: 32px;
  padding: 0 14px;
  border: 1px solid #e6edf5;
  border-radius: 8px;
  background: #fff;
  color: #76889d;
  font-size: 11px;
  cursor: pointer;
}

.wallet-tabs button.active {
  border-color: #d7e6fb;
  background: #edf5ff;
  color: #2f7df6;
  font-weight: 600;
}

.wallet-empty {
  padding-bottom: 38px;
}

.wallet-check {
  padding: 0 20px 22px;
}

.wallet-check h3 {
  margin: 0 0 10px;
  color: #405970;
  font-size: 12px;
}

.check-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.check-grid div {
  display: flex;
  gap: 9px;
  padding: 12px;
  border: 1px solid #edf2f8;
  border-radius: 9px;
  background: #fbfdff;
}

.check-grid span {
  color: #b2c2d2;
  font-size: 9px;
  font-weight: 600;
}

.check-grid p {
  margin: 0;
  color: #7c8fa3;
  font-size: 10px;
  line-height: 1.7;
}

.wallet-foot {
  margin: 0;
  padding: 0 20px 22px;
  color: #96a6b7;
  font-size: 10px;
  line-height: 1.8;
}

@media (max-width: 700px) {
  .check-grid {
    grid-template-columns: 1fr;
  }
}
</style>

<template>
  <div class="app-container operations-dashboard">
    <div class="page-header">
      <div>
        <h2 class="page-title">运营驾驶舱</h2>
        <p class="page-subtitle">指标卡与风险列表展示当前运营快照；日期筛选用于趋势和分类分布。</p>
      </div>
      <div class="page-actions">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          value-format="yyyy-MM-dd"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          size="small"
          clearable
          class="date-picker"
          @change="loadDashboard"
        />
        <el-button type="primary" size="small" icon="el-icon-refresh" :loading="loading" @click="loadDashboard">
          刷新
        </el-button>
      </div>
    </div>

    <el-alert
      v-if="loadError && dashboardReady"
      :title="loadError + '，当前展示上一次成功数据。'"
      type="warning"
      :closable="false"
      show-icon
      class="data-alert"
    />

    <el-alert
      v-for="alert in operationAlerts"
      :key="alert.code"
      :title="alert.title"
      :description="alert.message"
      :type="alert.level || 'warning'"
      :closable="false"
      show-icon
      class="data-alert"
    >
      <el-button v-if="alert.route" type="text" size="mini" @click="openAlert(alert)">立即处理</el-button>
    </el-alert>

    <div v-if="dashboardReady" v-loading="loading">
      <el-row :gutter="16" class="metric-row">
        <el-col v-for="card in metricCards" :key="card.key" :xs="12" :sm="12" :md="6">
          <div class="metric-card" :class="card.cardClass" @click="goToOrders(card.query)">
            <div class="metric-card-main">
              <div class="metric-label">{{ card.label }}</div>
              <div class="metric-value">{{ formatNumber(card.value) }}</div>
              <div class="metric-hint">{{ card.hint }}</div>
            </div>
            <i :class="card.icon" class="metric-icon" />
          </div>
        </el-col>
      </el-row>

      <el-row :gutter="16" class="chart-row">
        <el-col :xs="24" :lg="14">
          <el-card shadow="never" class="dashboard-card">
            <div slot="header" class="card-header">
              <span>工单趋势</span>
              <span class="card-meta">{{ generatedAtText }}</span>
            </div>
            <div v-if="trendChartRows.length" ref="trendChart" class="chart-container" />
            <el-empty v-else :image-size="72" description="当前区间暂无趋势数据" />
          </el-card>
        </el-col>
        <el-col :xs="24" :lg="10">
          <el-card shadow="never" class="dashboard-card">
            <div slot="header" class="card-header">
              <span>工单分类占比</span>
              <span class="card-meta">按工单总量</span>
            </div>
            <div v-if="categoryChartRows.length" ref="categoryChart" class="chart-container" />
            <el-empty v-else :image-size="72" description="当前区间暂无分类数据" />
          </el-card>
        </el-col>
      </el-row>

      <el-card shadow="never" class="dashboard-card risk-card">
        <div slot="header" class="card-header">
          <div>
            <span>SLA 风险工单</span>
            <span class="card-tip">点击工单号进入工单列表并按编号筛选</span>
          </div>
          <el-button type="text" size="small" @click="goToOrders({ slaRiskFlag: '1' })">查看全部风险工单</el-button>
        </div>
        <el-table v-if="riskRows.length" :data="riskRows" border stripe size="small" @row-click="openRiskOrder">
          <el-table-column label="工单号" prop="orderNo" min-width="150">
            <template slot-scope="scope">
              <el-button type="text" class="order-link" @click.stop="openRiskOrder(scope.row)">
                {{ scope.row.orderNo || '—' }}
              </el-button>
            </template>
          </el-table-column>
          <el-table-column label="标题" prop="title" min-width="220" show-overflow-tooltip>
            <template slot-scope="scope">{{ scope.row.title || '—' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="110" align="center">
            <template slot-scope="scope">
              <el-tag size="small" :type="statusTagType(scope.row.status)">
                {{ statusText(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="当前处理人" prop="assigneeName" min-width="130">
            <template slot-scope="scope">{{ scope.row.assigneeName || '未派单' }}</template>
          </el-table-column>
          <el-table-column label="截止时间" prop="deadline" min-width="170">
            <template slot-scope="scope">{{ formatDateTime(scope.row.deadline) }}</template>
          </el-table-column>
          <el-table-column label="风险标记" width="110" align="center">
            <template slot-scope="scope">
              <el-tag v-if="isFlag(scope.row.overdueFlag)" type="danger" size="small">已超时</el-tag>
              <el-tag v-else-if="isFlag(scope.row.warningFlag)" type="warning" size="small">即将超时</el-tag>
              <span v-else class="muted">—</span>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else :image-size="72" description="当前暂无 SLA 风险工单" />
      </el-card>
    </div>

    <el-empty
      v-else-if="!loading"
      description="暂时无法获取运营数据，请稍后重试。"
      class="first-load-empty"
    >
      <el-button type="primary" size="small" @click="loadDashboard">重新加载</el-button>
    </el-empty>

    <div v-else class="initial-loading">
      <i class="el-icon-loading" />
      <span>正在加载运营数据…</span>
    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { getDashboard } from '@/api/workorder/analytics'

export default {
  name: 'OperationsDashboard',
  data() {
    return {
      // 日期为空时交给后端使用默认统计区间，不在前端制造一段假区间。
      dateRange: [],
      dashboardData: null,
      loading: false,
      loadError: '',
      dashboardRequestId: 0,
      trendChart: null,
      categoryChart: null,
      resizeHandler: null
    }
  },
  computed: {
    dashboardReady() {
      return this.dashboardData !== null
    },
    dashboardMetrics() {
      return (this.dashboardData && this.dashboardData.metrics) || {}
    },
    trendRows() {
      return (this.dashboardData && this.dashboardData.trend) || []
    },
    categoryRows() {
      return (this.dashboardData && this.dashboardData.categoryShare) || []
    },
    trendChartRows() {
      return this.trendRows.filter(item =>
        this.numberOrNull(item.total) !== null || this.numberOrNull(item.completed) !== null
      )
    },
    categoryChartRows() {
      return this.categoryRows.filter(item => this.numberOrNull(item.total) !== null)
    },
    riskRows() {
      return (this.dashboardData && this.dashboardData.slaRisks) || []
    },
    operationAlerts() {
      return (this.dashboardData && this.dashboardData.alerts) || []
    },
    generatedAtText() {
      const value = this.dashboardData && this.dashboardData.generatedAt
      return value ? '更新于 ' + this.formatDateTime(value) : '更新时间未知'
    },
    metricCards() {
      const today = this.todayString()
      return [
        {
          key: 'todayNew',
          label: '今日新增',
          value: this.dashboardMetrics.todayNew,
          hint: '按今日创建时间统计',
          icon: 'el-icon-document-add',
          cardClass: 'metric-blue',
          query: { beginTime: today, endTime: today }
        },
        {
          key: 'pendingAssign',
          label: '待派单',
          value: this.dashboardMetrics.pendingAssign,
          hint: '状态为待派单',
          icon: 'el-icon-s-operation',
          cardClass: 'metric-orange',
          query: { status: 'WAIT_ASSIGN' }
        },
        {
          key: 'processing',
          label: '处理中',
          value: this.dashboardMetrics.processing,
          hint: '状态为处理中',
          icon: 'el-icon-setting',
          cardClass: 'metric-green',
          query: { status: 'PROCESSING' }
        },
        {
          key: 'slaWarning',
          label: 'SLA 风险',
          value: this.dashboardMetrics.slaWarning,
          hint: '超时或即将超时',
          icon: 'el-icon-warning-outline',
          cardClass: 'metric-red',
          query: { slaRiskFlag: '1' }
        }
      ]
    }
  },
  created() {
    this.loadDashboard()
  },
  mounted() {
    this.resizeHandler = () => this.resizeCharts()
    window.addEventListener('resize', this.resizeHandler)
  },
  beforeDestroy() {
    if (this.resizeHandler) window.removeEventListener('resize', this.resizeHandler)
    this.disposeCharts()
  },
  methods: {
    buildDateQuery() {
      const query = {}
      if (Array.isArray(this.dateRange)) {
        if (this.dateRange[0]) query.beginTime = this.dateRange[0]
        if (this.dateRange[1]) query.endTime = this.dateRange[1]
      }
      return query
    },
    todayString() {
      const now = new Date()
      const pad = value => String(value).padStart(2, '0')
      return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
    },
    async loadDashboard() {
      const requestId = ++this.dashboardRequestId
      this.loading = true
      this.loadError = ''
      try {
        const response = await getDashboard(this.buildDateQuery())
        if (requestId !== this.dashboardRequestId) return
        const payload = response && response.data
        if (!payload || typeof payload !== 'object') throw new Error('统计接口未返回有效数据')

        // 只替换成功快照；数组缺失时显示空态，绝不补造统计行或 0。
        this.dashboardData = {
          generatedAt: payload.generatedAt,
          metrics: payload.metrics && typeof payload.metrics === 'object' ? payload.metrics : {},
          trend: Array.isArray(payload.trend) ? payload.trend : [],
          categoryShare: Array.isArray(payload.categoryShare) ? payload.categoryShare : [],
          slaRisks: Array.isArray(payload.slaRisks) ? payload.slaRisks : [],
          alerts: Array.isArray(payload.alerts) ? payload.alerts : []
        }
        this.loadError = ''
        this.$nextTick(() => this.renderCharts())
      } catch (error) {
        if (requestId !== this.dashboardRequestId) return
        this.loadError = '运营数据加载失败'
        this.$message.warning(
          this.dashboardReady
            ? '运营数据加载失败，已保留上一次成功数据。'
            : '首次加载运营数据失败，当前未展示虚构的统计数值。'
        )
      } finally {
        if (requestId === this.dashboardRequestId) this.loading = false
      }
    },
    renderCharts() {
      this.disposeCharts()
      if (!this.dashboardReady) return

      if (this.trendChartRows.length && this.$refs.trendChart) {
        this.trendChart = echarts.init(this.$refs.trendChart)
        this.trendChart.setOption({
          color: ['#409EFF', '#67C23A'],
          tooltip: { trigger: 'axis' },
          legend: { data: ['工单总量', '已完成'], top: 0, right: 0 },
          grid: { left: 16, right: 18, top: 42, bottom: 18, containLabel: true },
          xAxis: {
            type: 'category',
            boundaryGap: false,
            data: this.trendChartRows.map(item => item.date || '—'),
            axisLine: { lineStyle: { color: '#E4E7ED' } },
            axisLabel: { color: '#909399' }
          },
          yAxis: {
            type: 'value',
            minInterval: 1,
            axisLine: { show: false },
            splitLine: { lineStyle: { color: '#F0F2F5' } },
            axisLabel: { color: '#909399' }
          },
          series: [
            {
              name: '工单总量',
              type: 'line',
              smooth: true,
              showSymbol: false,
              data: this.trendChartRows.map(item => this.numberOrNull(item.total))
            },
            {
              name: '已完成',
              type: 'line',
              smooth: true,
              showSymbol: false,
              data: this.trendChartRows.map(item => this.numberOrNull(item.completed))
            }
          ]
        })
      }

      if (this.categoryChartRows.length && this.$refs.categoryChart) {
        const pieData = this.categoryChartRows
          .map(item => ({
            name: item.categoryName || '未命名分类',
            value: this.numberOrNull(item.total)
          }))
          .filter(item => item.value !== null)
        if (pieData.length) {
          this.categoryChart = echarts.init(this.$refs.categoryChart)
          this.categoryChart.setOption({
            color: ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#8E71C1'],
            tooltip: {
              trigger: 'item',
              formatter: params => `${params.name}<br/>工单数：${params.value}（${params.percent}%）`
            },
            legend: { type: 'scroll', bottom: 0, left: 12, right: 12 },
            series: [{
              name: '工单分类',
              type: 'pie',
              radius: ['42%', '70%'],
              center: ['50%', '45%'],
              avoidLabelOverlap: true,
              label: { formatter: '{b}\n{d}%' },
              data: pieData
            }]
          })
        }
      }
    },
    disposeCharts() {
      if (this.trendChart) this.trendChart.dispose()
      if (this.categoryChart) this.categoryChart.dispose()
      this.trendChart = null
      this.categoryChart = null
    },
    resizeCharts() {
      if (this.trendChart) this.trendChart.resize()
      if (this.categoryChart) this.categoryChart.resize()
    },
    goToOrders(filters) {
      const query = {}
      Object.keys(filters || {}).forEach(key => {
        const value = filters[key]
        if (value !== undefined && value !== null && value !== '') query[key] = String(value)
      })
      this.$router.push({ path: '/workorder/orders', query }).catch(() => {})
    },
    openRiskOrder(row) {
      if (!row || !row.orderNo) {
        this.$message.info('该风险记录缺少工单编号，暂无法跳转。')
        return
      }
      // 风险行与工单列表使用同一个 keyword 合同，确保可以从驾驶舱追到原单。
      this.$router.push({
        path: '/workorder/orders',
        query: { keyword: String(row.orderNo) }
      }).catch(() => {})
    },
    openAlert(alert) {
      if (alert && alert.route) this.$router.push({ path: alert.route }).catch(() => {})
    },
    formatNumber(value) {
      if (!this.hasValue(value)) return '—'
      const number = Number(value)
      return Number.isFinite(number) ? number.toLocaleString('zh-CN') : '—'
    },
    numberOrNull(value) {
      if (!this.hasValue(value)) return null
      const number = Number(value)
      return Number.isFinite(number) ? number : null
    },
    hasValue(value) {
      return value !== undefined && value !== null && value !== ''
    },
    formatDateTime(value) {
      if (!this.hasValue(value)) return '—'
      if (typeof value === 'string') return value.replace('T', ' ').replace(/\.\d{3}Z?$/, '')
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return String(value)
      const pad = number => String(number).padStart(2, '0')
      return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
    },
    isFlag(value) {
      return value === true || value === 1 || value === '1' || String(value).toLowerCase() === 'true'
    },
    statusText(value) {
      const labels = {
        DRAFT: '草稿',
        WAIT_ASSIGN: '待派单',
        WAIT_ACCEPT: '待接单',
        ACCEPTED: '已接单',
        PROCESSING: '处理中',
        WAIT_CONFIRM: '待确认',
        COMPLETED: '已完成',
        CLOSED: '已关闭',
        CANCELLED: '已取消'
      }
      return labels[value] || value || '未知状态'
    },
    statusTagType(value) {
      return {
        WAIT_ASSIGN: 'warning',
        WAIT_ACCEPT: 'warning',
        ACCEPTED: 'primary',
        PROCESSING: 'primary',
        WAIT_CONFIRM: 'warning',
        COMPLETED: 'success',
        CLOSED: 'info',
        CANCELLED: 'danger'
      }[value] || 'info'
    }
  }
}
</script>

<style lang="scss" scoped>
.operations-dashboard {
  min-height: calc(100vh - 84px);
  background: #f5f7fa;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 18px;
}

.page-title {
  margin: 0;
  color: #303133;
  font-size: 22px;
  font-weight: 600;
}

.page-subtitle {
  margin: 8px 0 0;
  color: #909399;
  font-size: 13px;
}

.page-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.date-picker {
  width: 260px;
}

.data-alert {
  margin-bottom: 16px;
}

.metric-row,
.chart-row {
  margin-bottom: 16px;
}

.metric-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 118px;
  margin-bottom: 16px;
  padding: 20px;
  overflow: hidden;
  color: #fff;
  border-radius: 6px;
  cursor: pointer;
  transition: box-shadow .2s, transform .2s;
}

.metric-card:hover {
  box-shadow: 0 8px 20px rgba(31, 45, 61, .14);
  transform: translateY(-2px);
}

.metric-blue { background: linear-gradient(135deg, #409eff, #66b1ff); }
.metric-orange { background: linear-gradient(135deg, #e6a23c, #f3c77b); }
.metric-green { background: linear-gradient(135deg, #67c23a, #95d475); }
.metric-red { background: linear-gradient(135deg, #f56c6c, #f89898); }

.metric-label {
  font-size: 13px;
  opacity: .9;
}

.metric-value {
  margin-top: 8px;
  font-size: 32px;
  font-weight: 600;
  line-height: 1;
}

.metric-hint {
  margin-top: 10px;
  font-size: 12px;
  opacity: .82;
}

.metric-icon {
  margin-left: 10px;
  font-size: 40px;
  opacity: .28;
}

.dashboard-card {
  margin-bottom: 16px;
  border: 1px solid #ebeef5;
}

.dashboard-card ::v-deep .el-card__header {
  padding: 15px 18px;
}

.dashboard-card ::v-deep .el-card__body {
  padding: 18px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #303133;
  font-size: 15px;
  font-weight: 600;
}

.card-meta,
.card-tip {
  margin-left: 12px;
  color: #909399;
  font-size: 12px;
  font-weight: 400;
}

.chart-container {
  width: 100%;
  height: 310px;
}

.risk-card {
  margin-bottom: 0;
}

.risk-card ::v-deep .el-table__row {
  cursor: pointer;
}

.order-link {
  padding: 0;
}

.muted {
  color: #c0c4cc;
}

.first-load-empty {
  padding: 90px 0;
  background: #fff;
}

.initial-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 360px;
  color: #909399;
  font-size: 14px;
  background: #fff;
}

.initial-loading i {
  margin-right: 8px;
  font-size: 20px;
}

@media (max-width: 768px) {
  .page-header {
    display: block;
  }

  .page-actions {
    margin-top: 14px;
  }

  .date-picker {
    flex: 1;
    width: auto;
  }
}
</style>

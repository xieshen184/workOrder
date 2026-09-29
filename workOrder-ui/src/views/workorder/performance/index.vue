<template>
  <div class="app-container performance-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">维修绩效分析</h2>
        <p class="page-subtitle">按时间、组织、人员和分类查看维修效率与服务质量，指标均来自同一统计快照。</p>
      </div>
      <div class="page-actions">
        <span class="generated-at">{{ generatedAtText }}</span>
        <el-button
          type="primary"
          size="small"
          icon="el-icon-download"
          :disabled="!performanceReady"
          @click="exportCsv"
        >
          导出 CSV
        </el-button>
      </div>
    </div>

    <el-card shadow="never" class="filter-card">
      <el-form
        ref="queryForm"
        :model="queryParams"
        :inline="true"
        size="small"
        label-width="72px"
        @submit.native.prevent
      >
        <el-form-item label="统计日期">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            value-format="yyyy-MM-dd"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            clearable
            class="date-picker"
          />
        </el-form-item>
        <el-form-item label="组织" class="dept-form-item">
          <treeselect
            v-model="queryParams.deptId"
            :options="deptOptions"
            :normalizer="normalizeDept"
            :clearable="true"
            :searchable="true"
            :show-count="true"
            placeholder="全部组织"
            class="dept-select"
            @input="handleDeptInput"
          />
        </el-form-item>
        <el-form-item label="工程师">
          <el-select
            v-model="queryParams.engineerId"
            filterable
            clearable
            placeholder="全部工程师"
            class="engineer-select"
          >
            <el-option
              v-for="engineer in engineerSelectOptions"
              :key="String(engineerOptionId(engineer))"
              :label="engineerOptionLabel(engineer)"
              :value="String(engineerOptionId(engineer))"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="工单分类">
          <el-select
            v-model="queryParams.categoryId"
            filterable
            clearable
            placeholder="全部分类"
            class="category-select"
          >
            <el-option
              v-for="category in categoryOptions"
              :key="String(categoryOptionId(category))"
              :label="categoryOptionLabel(category)"
              :value="String(categoryOptionId(category))"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" :loading="loading" @click="handleQuery">查询</el-button>
          <el-button icon="el-icon-refresh" :disabled="loading" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-alert
      v-if="loadError && performanceReady"
      :title="loadError + '，当前展示上一次成功数据。'"
      type="warning"
      :closable="false"
      show-icon
      class="data-alert"
    />

    <div v-if="performanceReady" v-loading="loading">
      <el-row :gutter="16" class="metric-row">
        <el-col v-for="card in metricCards" :key="card.key" :xs="12" :sm="8" :lg="4">
          <div class="metric-card">
            <div class="metric-label">{{ card.label }}</div>
            <div class="metric-value">{{ metricDisplay(card) }}</div>
            <div v-if="card.sampleKey" class="metric-sample">
              样本数：{{ formatNumber(performanceMetrics[card.sampleKey]) }}
            </div>
            <div v-else class="metric-sample">{{ card.sampleText }}</div>
            <div class="metric-definition">口径：{{ card.definition }}</div>
          </div>
        </el-col>
      </el-row>

      <el-alert
        title="按时完成率按有 SLA 截止时间且已完成的工单计算；满意度按有评价的工单计算；返工率按发生退回返工的工单计算。样本数随筛选条件变化。"
        type="info"
        :closable="false"
        show-icon
        class="definition-alert"
      />

      <el-card shadow="never" class="table-card">
        <div slot="header" class="card-header">
          <div>
            <span>工程师绩效明细</span>
            <span class="card-tip">点击行按工程师筛选当前结果</span>
          </div>
          <div v-if="queryParams.engineerId" class="selected-filter">
            <el-tag size="small" closable @close="clearEngineerFilter">
              工程师：{{ selectedEngineerName }}
            </el-tag>
          </div>
        </div>
        <el-table
          v-if="engineerRows.length"
          ref="performanceTable"
          :data="engineerRows"
          row-key="engineerId"
          border
          stripe
          highlight-current-row
          class="performance-table"
          @row-click="handleEngineerRowClick"
        >
          <el-table-column label="工程师" prop="engineerName" min-width="150">
            <template slot-scope="scope">
              <span class="engineer-name">{{ scope.row.engineerName || '—' }}</span>
              <span class="engineer-id">ID：{{ scope.row.engineerId || '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="所属组织" prop="deptName" min-width="140">
            <template slot-scope="scope">{{ scope.row.deptName || '—' }}</template>
          </el-table-column>
          <el-table-column label="完成量" prop="completedCount" width="100" align="right">
            <template slot-scope="scope">{{ formatNumber(scope.row.completedCount) }}</template>
          </el-table-column>
          <el-table-column label="平均响应" prop="avgResponseMinutes" width="120" align="right">
            <template slot-scope="scope">{{ formatMetricWithUnit(scope.row.avgResponseMinutes, ' 分', 1) }}</template>
          </el-table-column>
          <el-table-column label="平均到场" prop="avgArrivalMinutes" width="120" align="right">
            <template slot-scope="scope">{{ formatMetricWithUnit(scope.row.avgArrivalMinutes, ' 分', 1) }}</template>
          </el-table-column>
          <el-table-column label="按时率" prop="onTimeRate" width="105" align="right">
            <template slot-scope="scope">{{ formatMetricWithUnit(scope.row.onTimeRate, '%', 1) }}</template>
          </el-table-column>
          <el-table-column label="满意度" prop="satisfactionScore" width="105" align="right">
            <template slot-scope="scope">{{ formatNumber(scope.row.satisfactionScore, 1) }}</template>
          </el-table-column>
          <el-table-column label="返工率" prop="reworkRate" width="105" align="right">
            <template slot-scope="scope">{{ formatMetricWithUnit(scope.row.reworkRate, '%', 1) }}</template>
          </el-table-column>
        </el-table>
        <el-empty v-else :image-size="72" description="当前筛选条件暂无工程师绩效数据" />
      </el-card>
    </div>

    <el-empty
      v-else-if="!loading"
      description="暂时无法获取绩效数据，请稍后重试。"
      class="first-load-empty"
    >
      <el-button type="primary" size="small" @click="loadPerformance">重新加载</el-button>
    </el-empty>

    <div v-else class="initial-loading">
      <i class="el-icon-loading" />
      <span>正在加载绩效数据…</span>
    </div>
  </div>
</template>

<script>
import Treeselect from '@riophae/vue-treeselect'
import '@riophae/vue-treeselect/dist/vue-treeselect.css'
import { deptTreeSelect } from '@/api/system/user'
import { listCategories } from '@/api/workorder/order'
import { listEngineers } from '@/api/workorder/engineer'
import { getPerformance } from '@/api/workorder/analytics'

export default {
  name: 'WorkorderPerformance',
  components: { Treeselect },
  data() {
    return {
      dateRange: [],
      queryParams: {
        deptId: null,
        engineerId: null,
        categoryId: null
      },
      performanceData: null,
      loading: false,
      loadError: '',
      performanceRequestId: 0,
      deptOptions: [],
      engineerOptions: [],
      categoryOptions: []
    }
  },
  computed: {
    performanceReady() {
      return this.performanceData !== null
    },
    performanceMetrics() {
      return (this.performanceData && this.performanceData.metrics) || {}
    },
    engineerRows() {
      return (this.performanceData && this.performanceData.engineerRows) || []
    },
    generatedAtText() {
      const value = this.performanceData && this.performanceData.generatedAt
      return value ? '更新于 ' + this.formatDateTime(value) : '更新时间未知'
    },
    selectedEngineerName() {
      const selected = this.engineerOptions.find(item =>
        String(this.engineerOptionId(item)) === String(this.queryParams.engineerId)
      )
      return selected ? this.engineerOptionLabel(selected) : String(this.queryParams.engineerId || '—')
    },
    engineerSelectOptions() {
      const deptId = this.queryParams.deptId
      if (!this.hasValue(deptId)) return this.engineerOptions
      return this.engineerOptions.filter(item => {
        const itemDeptId = this.engineerDeptId(item)
        return this.hasValue(itemDeptId) && String(itemDeptId) === String(deptId)
      })
    },
    metricCards() {
      return [
        {
          key: 'completedCount',
          label: '完成量',
          value: this.performanceMetrics.completedCount,
          unit: ' 单',
          decimals: 0,
          sampleText: '统计区间内进入完成态的工单',
          definition: '统计区间内已完成工单数量'
        },
        {
          key: 'avgResponseMinutes',
          label: '平均响应时长',
          value: this.performanceMetrics.avgResponseMinutes,
          unit: ' 分',
          decimals: 1,
          sampleText: '有分派和接单时间的工单',
          definition: '最近分派至接单的平均分钟数'
        },
        {
          key: 'avgArrivalMinutes',
          label: '平均到场时长',
          value: this.performanceMetrics.avgArrivalMinutes,
          unit: ' 分',
          decimals: 1,
          sampleText: '有到场时间记录的工单',
          definition: '接单到到场的平均分钟数'
        },
        {
          key: 'onTimeRate',
          label: '按时完成率',
          value: this.performanceMetrics.onTimeRate,
          unit: '%',
          decimals: 1,
          sampleKey: 'onTimeSample',
          definition: '有 SLA 截止时间且已完成的工单'
        },
        {
          key: 'satisfactionScore',
          label: '满意度',
          value: this.performanceMetrics.satisfactionScore,
          unit: ' 分',
          decimals: 1,
          sampleKey: 'satisfactionSample',
          definition: '已提交评价的工单平均评分'
        },
        {
          key: 'reworkRate',
          label: '返工率',
          value: this.performanceMetrics.reworkRate,
          unit: '%',
          decimals: 1,
          sampleKey: 'reworkSample',
          definition: '发生退回返工的工单占比'
        }
      ]
    }
  },
  created() {
    this.loadOptions()
    this.loadPerformance()
  },
  methods: {
    async loadOptions() {
      // 下拉项失败不应阻断统计快照；已有选项也不因一次失败被清空。
      this.loadDepartments()
      this.loadEngineers()
      this.loadCategories()
    },
    async loadDepartments() {
      try {
        const response = await deptTreeSelect()
        if (Array.isArray(response && response.data)) this.deptOptions = response.data
      } catch (error) {
        this.$message.warning('组织筛选项加载失败，仍可使用其他筛选条件。')
      }
    },
    async loadEngineers() {
      try {
        const response = await listEngineers()
        const rows = this.extractRows(response)
        if (rows.length || Array.isArray(response && response.data)) this.engineerOptions = rows
      } catch (error) {
        this.$message.warning('工程师筛选项加载失败，仍可使用其他筛选条件。')
      }
    },
    async loadCategories() {
      try {
        const response = await listCategories()
        const rows = this.extractRows(response)
        if (rows.length || Array.isArray(response && response.data)) this.categoryOptions = rows
      } catch (error) {
        this.$message.warning('工单分类筛选项加载失败，仍可使用其他筛选条件。')
      }
    },
    buildQuery() {
      const query = {}
      if (Array.isArray(this.dateRange)) {
        if (this.dateRange[0]) query.beginTime = this.dateRange[0]
        if (this.dateRange[1]) query.endTime = this.dateRange[1]
      }
      ;['deptId', 'engineerId', 'categoryId'].forEach(key => {
        if (this.hasValue(this.queryParams[key])) query[key] = this.queryParams[key]
      })
      return query
    },
    async loadPerformance() {
      const requestId = ++this.performanceRequestId
      this.loading = true
      this.loadError = ''
      try {
        const response = await getPerformance(this.buildQuery())
        if (requestId !== this.performanceRequestId) return
        const payload = response && response.data
        if (!payload || typeof payload !== 'object') throw new Error('绩效接口未返回有效数据')

        // 成功响应才替换快照；缺失数组展示空表，不补造工程师或指标数值。
        this.performanceData = {
          generatedAt: payload.generatedAt,
          metrics: payload.metrics && typeof payload.metrics === 'object' ? payload.metrics : {},
          engineerRows: Array.isArray(payload.engineerRows) ? payload.engineerRows : []
        }
        this.loadError = ''
      } catch (error) {
        if (requestId !== this.performanceRequestId) return
        this.loadError = '绩效数据加载失败'
        this.$message.warning(
          this.performanceReady
            ? '绩效数据加载失败，已保留上一次成功数据。'
            : '首次加载绩效数据失败，当前未展示虚构的统计数值。'
        )
      } finally {
        if (requestId === this.performanceRequestId) this.loading = false
      }
    },
    handleQuery() {
      this.loadPerformance()
    },
    resetQuery() {
      this.dateRange = []
      this.queryParams = { deptId: null, engineerId: null, categoryId: null }
      this.loadPerformance()
    },
    handleDeptInput() {
      // 组织变化后清掉可能已不属于该组织的工程师，避免把旧筛选悄悄带给后端。
      this.queryParams.engineerId = null
    },
    clearEngineerFilter() {
      this.queryParams.engineerId = null
      this.loadPerformance()
    },
    handleEngineerRowClick(row) {
      if (!row || !this.hasValue(row.engineerId)) {
        this.$message.info('该明细缺少工程师编号，无法按工程师筛选。')
        return
      }
      this.queryParams.engineerId = String(row.engineerId)
      this.loadPerformance()
    },
    exportCsv() {
      if (!this.performanceReady) {
        this.$message.warning('暂无成功加载的绩效结果可导出。')
        return
      }

      const query = this.buildQuery()
      const rows = [
        ['维修绩效分析'],
        ['生成时间', this.csvValue(this.performanceData.generatedAt ? this.formatDateTime(this.performanceData.generatedAt) : '')],
        ['开始日期', this.csvValue(query.beginTime)],
        ['结束日期', this.csvValue(query.endTime)],
        ['组织ID', this.csvValue(query.deptId)],
        ['工程师ID', this.csvValue(query.engineerId)],
        ['工单分类ID', this.csvValue(query.categoryId)],
        [],
        ['指标', '值', '样本数', '统计口径'],
        ['完成量', this.exportMetric(this.performanceMetrics.completedCount, ' 单', 0), '', '统计区间内已完成工单数量'],
        ['平均响应时长', this.exportMetric(this.performanceMetrics.avgResponseMinutes, ' 分', 1), '', '最近分派至接单的平均分钟数'],
        ['平均到场时长', this.exportMetric(this.performanceMetrics.avgArrivalMinutes, ' 分', 1), '', '接单到到场的平均分钟数'],
        ['按时完成率', this.exportMetric(this.performanceMetrics.onTimeRate, '%', 1), this.csvValue(this.performanceMetrics.onTimeSample), '有 SLA 截止时间且已完成的工单'],
        ['满意度', this.exportMetric(this.performanceMetrics.satisfactionScore, ' 分', 1), this.csvValue(this.performanceMetrics.satisfactionSample), '已提交评价的工单平均评分'],
        ['返工率', this.exportMetric(this.performanceMetrics.reworkRate, '%', 1), this.csvValue(this.performanceMetrics.reworkSample), '发生退回返工的工单占比'],
        [],
        ['工程师', '所属组织', '完成量', '平均响应（分）', '平均到场（分）', '按时率', '满意度', '返工率']
      ]

      this.engineerRows.forEach(row => {
        rows.push([
          this.csvValue(row.engineerName),
          this.csvValue(row.deptName),
          this.csvValue(row.completedCount),
          this.exportMetric(row.avgResponseMinutes, '', 1),
          this.exportMetric(row.avgArrivalMinutes, '', 1),
          this.exportMetric(row.onTimeRate, '%', 1),
          this.exportMetric(row.satisfactionScore, '', 1),
          this.exportMetric(row.reworkRate, '%', 1)
        ])
      })

      const csv = rows.map(row => row.map(cell => this.csvEscape(cell)).join(',')).join('\r\n')
      const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8;' })
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = '维修绩效分析-' + this.todayText() + '.csv'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      URL.revokeObjectURL(url)
      this.$message.success('已导出当前筛选条件下的绩效结果。')
    },
    metricDisplay(card) {
      return this.formatMetricWithUnit(card.value, card.unit, card.decimals)
    },
    formatMetricWithUnit(value, unit, decimals) {
      if (!this.hasValue(value)) return '—'
      return this.formatNumber(value, decimals) + (unit || '')
    },
    formatNumber(value, decimals = 0) {
      if (!this.hasValue(value)) return '—'
      const number = Number(value)
      if (!Number.isFinite(number)) return String(value)
      return number.toLocaleString('zh-CN', {
        minimumFractionDigits: decimals,
        maximumFractionDigits: decimals
      })
    },
    formatDateTime(value) {
      if (!this.hasValue(value)) return '—'
      if (typeof value === 'string') return value.replace('T', ' ').replace(/\.\d{3}Z?$/, '')
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return String(value)
      const pad = number => String(number).padStart(2, '0')
      return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
    },
    todayText() {
      const date = new Date()
      const pad = number => String(number).padStart(2, '0')
      return `${date.getFullYear()}${pad(date.getMonth() + 1)}${pad(date.getDate())}`
    },
    extractRows(response) {
      const payload = response && response.data
      if (Array.isArray(payload)) return payload
      if (payload && Array.isArray(payload.rows)) return payload.rows
      return []
    },
    normalizeDept(node) {
      const id = node.id !== undefined ? node.id : node.deptId
      return {
        id,
        label: node.label || node.deptName || node.name || (id === undefined ? '未命名组织' : String(id)),
        children: node.children
      }
    },
    engineerOptionId(engineer) {
      return this.firstValue(engineer, ['engineerId', 'userId', 'id'])
    },
    engineerOptionLabel(engineer) {
      return this.firstValue(engineer, ['engineerName', 'nickName', 'userName', 'name']) || '未命名工程师'
    },
    engineerDeptId(engineer) {
      return this.firstValue(engineer, ['deptId', 'engineerDeptId', 'departmentId'])
    },
    categoryOptionId(category) {
      return this.firstValue(category, ['categoryId', 'id'])
    },
    categoryOptionLabel(category) {
      return this.firstValue(category, ['categoryName', 'name', 'categoryCode']) || '未命名分类'
    },
    firstValue(source, fields) {
      for (let index = 0; index < fields.length; index += 1) {
        const value = source && source[fields[index]]
        if (this.hasValue(value)) return value
      }
      return null
    },
    hasValue(value) {
      return value !== undefined && value !== null && value !== ''
    },
    csvValue(value) {
      return this.hasValue(value) ? value : ''
    },
    exportMetric(value, unit, decimals) {
      return this.hasValue(value) ? this.formatMetricWithUnit(value, unit, decimals) : ''
    },
    csvEscape(value) {
      const text = this.hasValue(value) ? String(value) : ''
      return /[",\r\n]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text
    }
  }
}
</script>

<style lang="scss" scoped>
.performance-page {
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
  gap: 12px;
}

.generated-at {
  color: #909399;
  font-size: 12px;
}

.filter-card,
.table-card {
  margin-bottom: 16px;
  border: 1px solid #ebeef5;
}

.filter-card ::v-deep .el-card__body {
  padding: 18px 18px 2px;
}

.filter-card ::v-deep .el-form-item {
  margin-bottom: 16px;
}

.date-picker {
  width: 260px;
}

.dept-select {
  width: 190px;
}

.engineer-select,
.category-select {
  width: 180px;
}

.data-alert,
.definition-alert {
  margin-bottom: 16px;
}

.metric-row {
  margin-bottom: 0;
}

.metric-card {
  min-height: 154px;
  margin-bottom: 16px;
  padding: 18px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 6px;
}

.metric-label {
  color: #606266;
  font-size: 13px;
}

.metric-value {
  margin-top: 12px;
  color: #303133;
  font-size: 26px;
  font-weight: 600;
  line-height: 1.1;
}

.metric-sample {
  min-height: 18px;
  margin-top: 9px;
  color: #909399;
  font-size: 12px;
}

.metric-definition {
  margin-top: 8px;
  color: #a3a6ad;
  font-size: 12px;
  line-height: 1.5;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #303133;
  font-size: 15px;
  font-weight: 600;
}

.table-card ::v-deep .el-card__header {
  padding: 15px 18px;
}

.table-card ::v-deep .el-card__body {
  padding: 0 18px 18px;
}

.card-tip {
  margin-left: 12px;
  color: #909399;
  font-size: 12px;
  font-weight: 400;
}

.selected-filter {
  font-weight: 400;
}

.performance-table ::v-deep .el-table__row {
  cursor: pointer;
}

.engineer-name {
  display: block;
  color: #303133;
  font-weight: 600;
}

.engineer-id {
  display: block;
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
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
    justify-content: space-between;
    margin-top: 14px;
  }

  .date-picker,
  .dept-select,
  .engineer-select,
  .category-select {
    width: 100%;
  }
}
</style>

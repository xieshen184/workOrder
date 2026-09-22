<template>
  <div class="app-container engineer-page">
    <el-form ref="queryForm" :model="queryParams" :inline="true" size="small" label-width="72px">
      <el-form-item label="人员搜索" prop="keyword">
        <el-input
          v-model.trim="queryParams.keyword"
          placeholder="姓名、账号或部门"
          clearable
          style="width: 220px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="值班状态" prop="dutyStatus">
        <el-select v-model="queryParams.dutyStatus" placeholder="全部" clearable style="width: 130px">
          <el-option label="值班中" value="ON_DUTY" />
          <el-option label="已离线" value="OFF_DUTY" />
        </el-select>
      </el-form-item>
      <el-form-item label="工作状态" prop="workStatus">
        <el-select v-model="queryParams.workStatus" placeholder="全部" clearable style="width: 130px">
          <el-option label="空闲" value="AVAILABLE" />
          <el-option label="处理中" value="WORKING" />
          <el-option label="忙碌" value="BUSY" />
        </el-select>
      </el-form-item>
      <el-form-item label="在线状态" prop="online">
        <el-select v-model="queryParams.online" placeholder="全部" clearable style="width: 130px">
          <el-option label="在线" value="true" />
          <el-option label="心跳过期" value="false" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="handleQuery">查询</el-button>
        <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="16" class="summary-row">
      <el-col v-for="item in summaryCards" :key="item.label" :xs="12" :sm="6">
        <div class="summary-card">
          <div class="summary-label">{{ item.label }}</div>
          <div class="summary-value" :class="item.className">{{ item.value }}</div>
        </div>
      </el-col>
    </el-row>

    <el-alert
      title="在线状态以最近 5 分钟心跳为准；从未上报或超过 5 分钟未更新时标记为心跳过期。"
      type="info"
      :closable="false"
      show-icon
      class="status-note"
    />

    <el-table v-loading="loading" :data="engineers" border stripe>
      <el-table-column label="维修人员" min-width="150">
        <template slot-scope="scope">
          <div class="engineer-name">{{ scope.row.name }}</div>
          <div class="muted">ID：{{ scope.row.id }}</div>
        </template>
      </el-table-column>
      <el-table-column label="所属部门" prop="dept" min-width="150" show-overflow-tooltip />
      <el-table-column label="在线状态" width="110" align="center">
        <template slot-scope="scope">
          <el-tag :type="scope.row.online ? 'success' : 'info'" size="small">
            {{ scope.row.online ? '在线' : '心跳过期' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="值班状态" width="100" align="center">
        <template slot-scope="scope">
          <el-tag :type="scope.row.dutyStatus === 'ON_DUTY' ? 'success' : 'info'" size="small">
            {{ dutyText(scope.row.dutyStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="工作状态" width="100" align="center">
        <template slot-scope="scope">
          <el-tag :type="workTag(scope.row.status)" size="small">{{ workText(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="当前负载" prop="load" width="100" align="center" sortable />
      <el-table-column label="超期工单" prop="overdueOrderCount" width="100" align="center" sortable>
        <template slot-scope="scope">
          <span :class="{ danger: scope.row.overdueOrderCount > 0 }">{{ scope.row.overdueOrderCount || 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态来源" width="100" align="center">
        <template slot-scope="scope">{{ scope.row.statusSource === 'MANUAL' ? '人工上报' : '系统默认' }}</template>
      </el-table-column>
      <el-table-column label="最近心跳" min-width="165">
        <template slot-scope="scope">{{ scope.row.lastHeartbeatAt || '从未上报' }}</template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && !engineers.length" description="暂无符合条件的维修人员" />
  </div>
</template>

<script>
import { listEngineers } from '@/api/workorder/engineer'

export default {
  name: 'WorkorderEngineers',
  data() {
    return {
      loading: false,
      engineers: [],
      queryParams: {
        keyword: '',
        dutyStatus: '',
        workStatus: '',
        online: ''
      }
    }
  },
  computed: {
    summaryCards() {
      return [
        { label: '维修人员', value: this.engineers.length, className: '' },
        { label: '在线', value: this.engineers.filter(item => item.online).length, className: 'success' },
        { label: '空闲', value: this.engineers.filter(item => item.dutyStatus === 'ON_DUTY' && item.status === 'AVAILABLE').length, className: 'primary' },
        { label: '在办工单', value: this.engineers.reduce((sum, item) => sum + Number(item.load || 0), 0), className: 'warning' }
      ]
    }
  },
  created() {
    this.restoreRouteQuery()
    this.getList()
  },
  methods: {
    async getList() {
      this.loading = true
      try {
        const params = { ...this.queryParams }
        if (params.online !== '') params.online = params.online === 'true'
        const response = await listEngineers(params)
        this.engineers = Array.isArray(response.data) ? response.data : []
      } finally {
        this.loading = false
      }
    },
    handleQuery() {
      this.syncRouteQuery()
      this.getList()
    },
    resetQuery() {
      this.$refs.queryForm.resetFields()
      this.syncRouteQuery()
      this.getList()
    },
    restoreRouteQuery() {
      Object.keys(this.queryParams).forEach(key => {
        if (this.$route.query[key] !== undefined) this.queryParams[key] = this.$route.query[key]
      })
    },
    syncRouteQuery() {
      const query = {}
      Object.keys(this.queryParams).forEach(key => {
        if (this.queryParams[key] !== '' && this.queryParams[key] !== undefined) query[key] = this.queryParams[key]
      })
      this.$router.replace({ query }).catch(() => {})
    },
    dutyText(value) {
      return value === 'ON_DUTY' ? '值班中' : '已离线'
    },
    workText(value) {
      return { AVAILABLE: '空闲', WORKING: '处理中', BUSY: '忙碌' }[value] || '未知'
    },
    workTag(value) {
      return { AVAILABLE: 'success', WORKING: 'primary', BUSY: 'warning' }[value] || 'info'
    }
  }
}
</script>

<style lang="scss" scoped>
.summary-row { margin-bottom: 16px; }
.summary-card { padding: 18px 20px; background: #fff; border: 1px solid #ebeef5; border-radius: 6px; }
.summary-label { color: #909399; font-size: 13px; }
.summary-value { margin-top: 8px; color: #303133; font-size: 28px; font-weight: 600; }
.summary-value.primary { color: #409eff; }
.summary-value.success { color: #67c23a; }
.summary-value.warning { color: #e6a23c; }
.status-note { margin-bottom: 16px; }
.engineer-name { color: #303133; font-weight: 600; }
.muted { margin-top: 4px; color: #909399; font-size: 12px; }
.danger { color: #f56c6c; font-weight: 600; }
</style>

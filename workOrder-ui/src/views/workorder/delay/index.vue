<template>
  <div class="app-container delay-page">
    <el-form
      ref="queryForm"
      :model="queryParams"
      :inline="true"
      size="small"
      label-width="72px"
      class="filter-form"
      @submit.native.prevent
    >
      <el-form-item label="申请状态" prop="requestStatus">
        <el-radio-group v-model="queryParams.requestStatus" size="small" @change="handleQuery">
          <el-radio-button label="PENDING">待审</el-radio-button>
          <el-radio-button label="">全部</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="关键字" prop="keyword">
        <el-input
          v-model.trim="queryParams.keyword"
          clearable
          placeholder="工单号、标题或人员"
          style="width: 250px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-alert
      v-if="listError"
      :title="listError"
      type="error"
      show-icon
      :closable="false"
      class="list-alert"
    >
      <el-button type="text" size="mini" @click="getList">重试</el-button>
    </el-alert>

    <el-table
      v-loading="loading"
      :data="delayRequests"
      row-key="id"
      border
      stripe
      class="delay-table"
    >
      <el-table-column label="工单号" min-width="155" show-overflow-tooltip>
        <template slot-scope="scope">
          {{ orderNo(scope.row) }}
        </template>
      </el-table-column>
      <el-table-column label="标题" min-width="190" show-overflow-tooltip>
        <template slot-scope="scope">
          {{ orderTitle(scope.row) }}
        </template>
      </el-table-column>
      <el-table-column label="申请人员" min-width="120" show-overflow-tooltip>
        <template slot-scope="scope">
          {{ applicantName(scope.row) }}
        </template>
      </el-table-column>
      <el-table-column label="原截止时间" min-width="165">
        <template slot-scope="scope">
          {{ formatTime(originalDeadline(scope.row)) }}
        </template>
      </el-table-column>
      <el-table-column label="申请截止时间" min-width="165">
        <template slot-scope="scope">
          {{ formatTime(requestedDeadline(scope.row)) }}
        </template>
      </el-table-column>
      <el-table-column label="延期原因" min-width="240" show-overflow-tooltip>
        <template slot-scope="scope">
          {{ rowValue(scope.row, ['reason', 'requestReason']) || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="92" align="center">
        <template slot-scope="scope">
          <el-tag :type="statusTag(scope.row)" size="small">
            {{ statusText(scope.row) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right" align="center">
        <template slot-scope="scope">
          <template v-if="isPending(scope.row)">
            <el-button
              v-hasPermi="['workorder:delay:approve']"
              type="text"
              size="mini"
              :loading="isActing(scope.row, 'approve')"
              :disabled="Boolean(actionKey)"
              @click="approve(scope.row)"
            >同意</el-button>
            <el-button
              v-hasPermi="['workorder:delay:approve']"
              type="text"
              size="mini"
              class="reject-button"
              :disabled="Boolean(actionKey)"
              @click="openReject(scope.row)"
            >拒绝</el-button>
          </template>
          <span v-else class="muted-text">已处理</span>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && !listError && !delayRequests.length" description="暂无延期审批记录" :image-size="100" />

    <pagination
      v-show="!listError && total > 0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="handlePagination"
    />

    <el-dialog
      title="拒绝延期申请"
      :visible.sync="rejectOpen"
      width="520px"
      append-to-body
      :close-on-click-modal="false"
      :show-close="!rejectSubmitting"
      @closed="resetReject"
    >
      <el-form ref="rejectForm" :model="rejectForm" :rules="rejectRules" label-width="88px">
        <el-form-item label="拒绝原因" prop="reason">
          <el-input
            v-model="rejectForm.reason"
            type="textarea"
            :rows="5"
            maxlength="500"
            show-word-limit
            placeholder="请填写拒绝该延期申请的原因"
          />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button :disabled="rejectSubmitting" @click="rejectOpen = false">取消</el-button>
        <el-button type="danger" :loading="rejectSubmitting" @click="submitReject">确认拒绝</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  approveDelayRequest,
  listDelayRequests,
  rejectDelayRequest
} from '@/api/workorder/delay'

function unwrapResponse(response) {
  if (response && Object.prototype.hasOwnProperty.call(response, 'data')) return response.data
  return response
}

function extractRows(response) {
  const payload = unwrapResponse(response)
  if (Array.isArray(payload)) return payload
  if (payload && Array.isArray(payload.rows)) return payload.rows
  if (payload && Array.isArray(payload.list)) return payload.list
  if (response && Array.isArray(response.rows)) return response.rows
  return []
}

function extractTotal(response, rows) {
  const payload = unwrapResponse(response)
  const candidates = [
    response && response.total,
    payload && payload.total,
    response && response.data && response.data.total
  ]
  const total = candidates.find(value => typeof value === 'number')
  return total === undefined ? rows.length : total
}

function messageFrom(value) {
  if (!value) return ''
  if (typeof value === 'string') return value
  if (typeof value.msg === 'string') return value.msg
  if (typeof value.message === 'string') return value.message
  return ''
}

function requestErrorMessage(error, fallback) {
  const response = error && error.response
  const data = response && response.data
  return messageFrom(error) || messageFrom(data) || messageFrom(data && data.data) || fallback
}

export default {
  name: 'WorkorderDelayRequests',
  data() {
    return {
      loading: false,
      listError: '',
      delayRequests: [],
      total: 0,
      actionKey: '',
      queryParams: {
        requestStatus: 'PENDING',
        keyword: '',
        pageNum: 1,
        pageSize: 10
      },
      rejectOpen: false,
      rejectSubmitting: false,
      rejectForm: {
        id: null,
        reason: ''
      },
      rejectRules: {
        reason: [{ validator: this.validateRejectReason, trigger: 'blur' }]
      }
    }
  },
  created() {
    this.restoreRouteQuery()
    this.getList()
  },
  methods: {
    restoreRouteQuery() {
      const query = this.$route.query || {}
      if (query.requestStatus !== undefined) this.queryParams.requestStatus = query.requestStatus
      if (query.status !== undefined && query.requestStatus === undefined) this.queryParams.requestStatus = query.status
      if (query.keyword !== undefined) this.queryParams.keyword = query.keyword
      if (query.pageNum !== undefined && !Number.isNaN(Number(query.pageNum))) this.queryParams.pageNum = Number(query.pageNum)
      if (query.pageSize !== undefined && !Number.isNaN(Number(query.pageSize))) this.queryParams.pageSize = Number(query.pageSize)
    },
    buildListQuery() {
      const query = {
        pageNum: this.queryParams.pageNum,
        pageSize: this.queryParams.pageSize
      }
      if (this.queryParams.requestStatus) query.requestStatus = this.queryParams.requestStatus
      if (this.queryParams.keyword) query.keyword = this.queryParams.keyword
      return query
    },
    syncRouteQuery() {
      const query = { ...this.$route.query }
      ;['requestStatus', 'status', 'keyword', 'pageNum', 'pageSize'].forEach(key => { delete query[key] })
      const next = this.buildListQuery()
      Object.keys(next).forEach(key => { query[key] = String(next[key]) })
      this.$router.replace({ query }).catch(() => {})
    },
    getList() {
      this.loading = true
      this.listError = ''
      return listDelayRequests(this.buildListQuery()).then(response => {
        const rows = extractRows(response)
        this.delayRequests = rows
        this.total = extractTotal(response, rows)
      }).catch(error => {
        this.delayRequests = []
        this.total = 0
        this.listError = requestErrorMessage(error, '延期审批列表加载失败，请稍后重试')
      }).then(() => {
        this.loading = false
      })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.syncRouteQuery()
      this.getList()
    },
    handlePagination() {
      this.syncRouteQuery()
      this.getList()
    },
    resetQuery() {
      this.queryParams.requestStatus = 'PENDING'
      this.queryParams.keyword = ''
      this.queryParams.pageNum = 1
      this.syncRouteQuery()
      this.getList()
    },
    rowValue(row, keys) {
      for (let i = 0; i < keys.length; i += 1) {
        const value = row && row[keys[i]]
        if (value !== undefined && value !== null && value !== '') return value
      }
      return ''
    },
    nestedRowValue(row, keys) {
      const direct = this.rowValue(row, keys)
      if (direct !== '') return direct
      const order = row && (row.order || row.workOrder)
      return this.rowValue(order, keys)
    },
    orderNo(row) {
      return this.nestedRowValue(row, ['orderNo', 'workOrderNo', 'orderNumber', 'orderId']) || '-'
    },
    orderTitle(row) {
      return this.nestedRowValue(row, ['title', 'orderTitle']) || '-'
    },
    applicantName(row) {
      return this.nestedRowValue(row, ['applicantName', 'engineerName', 'applicant', 'userName', 'name', 'applicantId']) || '-'
    },
    originalDeadline(row) {
      return this.nestedRowValue(row, ['originalDeadline', 'originalDueAt', 'oldDeadline', 'finishDeadline'])
    },
    requestedDeadline(row) {
      return this.nestedRowValue(row, ['requestedDeadline', 'requestedDueAt', 'newDeadline', 'extensionDeadline'])
    },
    requestId(row) {
      return this.rowValue(row, ['id', 'requestId', 'delayRequestId'])
    },
    requestStatus(row) {
      return String(this.rowValue(row, ['requestStatus', 'status', 'state']) || '').toUpperCase()
    },
    isPending(row) {
      return this.requestStatus(row) === 'PENDING'
    },
    statusText(row) {
      return {
        PENDING: '待审',
        APPROVED: '已同意',
        REJECTED: '已拒绝',
        CANCELLED: '已取消'
      }[this.requestStatus(row)] || this.requestStatus(row) || '-'
    },
    statusTag(row) {
      return { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', CANCELLED: 'info' }[this.requestStatus(row)] || 'info'
    },
    isActing(row, action) {
      return this.actionKey === action + ':' + this.requestId(row)
    },
    approve(row) {
      const id = this.requestId(row)
      if (id === '' || id === undefined || id === null || !this.isPending(row) || this.actionKey) return
      this.$modal.confirm('确认同意该延期申请吗？').then(() => {
        this.executeAction(row, 'approve')
      }).catch(() => {})
    },
    executeAction(row, action, data) {
      const id = this.requestId(row)
      if (id === '' || id === undefined || id === null) {
        this.$modal.msgError('延期申请缺少有效编号，无法提交审批')
        return Promise.resolve()
      }
      this.actionKey = action + ':' + id
      const actionRequest = action === 'approve'
        ? approveDelayRequest(id, data || {})
        : rejectDelayRequest(id, data || {})
      return actionRequest.then(() => {
        this.$modal.msgSuccess(action === 'approve' ? '已同意延期申请' : '已拒绝延期申请')
        this.rejectOpen = false
        return this.getList()
      }).catch(error => {
        this.$modal.msgError(requestErrorMessage(error, '审批提交失败，请稍后重试'))
      }).then(() => {
        this.actionKey = ''
      })
    },
    openReject(row) {
      const id = this.requestId(row)
      if (id === '' || id === undefined || id === null || !this.isPending(row) || this.actionKey) return
      this.rejectForm = { id, reason: '' }
      this.rejectOpen = true
      this.$nextTick(() => {
        if (this.$refs.rejectForm) this.$refs.rejectForm.clearValidate()
      })
    },
    validateRejectReason(rule, value, callback) {
      if (!String(value || '').trim()) {
        callback(new Error('拒绝原因不能为空'))
        return
      }
      callback()
    },
    submitReject() {
      if (this.rejectSubmitting || !this.rejectForm.id) return
      this.$refs.rejectForm.validate(valid => {
        if (!valid) return
        this.$modal.confirm('确认拒绝该延期申请吗？').then(() => {
          this.rejectSubmitting = true
          return this.executeAction(this.delayRequests.find(item => String(this.requestId(item)) === String(this.rejectForm.id)) || { id: this.rejectForm.id }, 'reject', {
            reason: String(this.rejectForm.reason || '').trim()
          })
        }).catch(() => {}).then(() => {
          this.rejectSubmitting = false
        })
      })
    },
    resetReject() {
      if (this.rejectSubmitting) return
      this.rejectForm = { id: null, reason: '' }
      if (this.$refs.rejectForm) this.$refs.rejectForm.clearValidate()
    },
    formatTime(value) {
      if (!value) return '-'
      return this.parseTime ? this.parseTime(value) : value
    }
  }
}
</script>

<style lang="scss" scoped>
.filter-form { margin-bottom: 12px; }
.list-alert { margin-bottom: 12px; }
.delay-table { margin-top: 8px; }
.reject-button { color: #f56c6c; }
.muted-text { color: #909399; }
</style>

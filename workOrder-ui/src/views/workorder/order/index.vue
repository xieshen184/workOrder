<template>
  <div class="app-container workorder-order-page">
    <el-form
      ref="queryForm"
      :model="queryParams"
      size="small"
      :inline="true"
      label-width="72px"
      class="filter-form"
      @submit.native.prevent
    >
      <el-form-item label="关键词" prop="keyword">
        <el-input
          v-model.trim="queryParams.keyword"
          placeholder="工单编号、标题或描述"
          clearable
          style="width: 240px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部状态" clearable style="width: 160px">
          <el-option
            v-for="dict in dictOptions('wo_order_status')"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="分类" prop="categoryId">
        <el-select
          v-model="queryParams.categoryId"
          placeholder="全部分类 / 输入分类 ID"
          clearable
          filterable
          allow-create
          default-first-option
          :loading="categoryLoading"
          style="width: 190px"
        >
          <el-option
            v-for="category in categoryOptions"
            :key="category.id || category.categoryId"
            :label="category.categoryName || category.name || category.categoryCode"
            :value="String(category.id || category.categoryId)"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="紧急程度" prop="urgencyLevel">
        <el-select v-model="queryParams.urgencyLevel" placeholder="全部" clearable style="width: 140px">
          <el-option
            v-for="dict in dictOptions('wo_urgency_level')"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-alert
      v-if="categoryLoadError"
      class="filter-alert"
      :title="categoryLoadError"
      type="warning"
      show-icon
      :closable="false"
    />

    <el-alert
      v-if="listError"
      class="list-alert"
      :title="listError"
      type="error"
      show-icon
      :closable="false"
    >
      <el-button type="text" size="mini" @click="getList">重试</el-button>
    </el-alert>

    <el-table
      v-if="orderList.length > 0 || listLoading"
      v-loading="listLoading"
      :data="orderList"
      row-key="id"
      stripe
      border
      class="order-table"
    >
      <el-table-column label="工单编号" prop="orderNo" min-width="160" show-overflow-tooltip>
        <template slot-scope="scope">
          {{ scope.row.orderNo || scope.row.id || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="标题" prop="title" min-width="220" show-overflow-tooltip />
      <el-table-column label="分类" min-width="130" show-overflow-tooltip>
        <template slot-scope="scope">
          {{ scope.row.categoryName || scope.row.categoryCode || scope.row.categoryId || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="紧急程度" prop="urgencyLevel" width="110" align="center">
        <template slot-scope="scope">
          <dict-tag
            v-if="dictOptions('wo_urgency_level').length"
            :options="dictOptions('wo_urgency_level')"
            :value="scope.row.urgencyLevel"
          />
          <span v-else>{{ scope.row.urgencyLevel || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" prop="status" width="120" align="center">
        <template slot-scope="scope">
          <dict-tag
            v-if="dictOptions('wo_order_status').length"
            :options="dictOptions('wo_order_status')"
            :value="scope.row.status"
          />
          <span v-else>{{ scope.row.status || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="当前处理人" prop="currentAssigneeName" min-width="130" show-overflow-tooltip>
        <template slot-scope="scope">
          {{ scope.row.currentAssigneeName || '未指派' }}
        </template>
      </el-table-column>
      <el-table-column label="提交时间" prop="submittedAt" width="170">
        <template slot-scope="scope">
          {{ formatTime(scope.row.submittedAt || scope.row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right" align="center">
        <template slot-scope="scope">
          <el-button type="text" size="mini" icon="el-icon-view" @click="openDetail(scope.row)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="!listLoading && listError && orderList.length === 0" class="state-panel">
      <i class="el-icon-warning-outline state-icon state-icon-error" />
      <p>{{ listError }}</p>
      <el-button type="primary" size="small" @click="getList">重新加载</el-button>
    </div>
    <el-empty
      v-if="!listLoading && !listError && orderList.length === 0"
      description="暂无符合条件的工单"
      :image-size="100"
    />

    <pagination
      v-show="!listError && total > 0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="handlePagination"
    />

    <el-drawer
      :title="detailTitle"
      :visible.sync="detailOpen"
      size="72%"
      custom-class="workorder-detail-drawer"
      append-to-body
      @close="closeDetail"
    >
      <div v-loading="detailLoading" class="detail-body">
        <div v-if="detailError" class="state-panel detail-state-panel">
          <i class="el-icon-warning-outline state-icon state-icon-error" />
          <p>{{ detailError }}</p>
          <el-button type="primary" size="small" @click="retryDetail">重新加载</el-button>
        </div>

        <template v-else-if="detail">
          <div class="detail-header">
            <div class="detail-heading">
              <div class="detail-order-no">{{ detail.orderNo || detail.id }}</div>
              <h3>{{ detail.title || '未命名工单' }}</h3>
            </div>
            <div class="detail-actions">
              <el-button
                v-if="canAction('ASSIGN')"
                v-hasPermi="['workorder:order:assign']"
                type="primary"
                size="small"
                icon="el-icon-s-custom"
                @click="openAssignment('ASSIGN')"
              >派单</el-button>
              <el-button
                v-if="canAction('REASSIGN')"
                v-hasPermi="['workorder:order:reassign']"
                type="warning"
                size="small"
                icon="el-icon-refresh"
                @click="openAssignment('REASSIGN')"
              >改派</el-button>
              <el-button
                v-if="canAction('RETURN')"
                v-hasPermi="['workorder:order:return']"
                type="danger"
                size="small"
                icon="el-icon-back"
                @click="openReturn"
              >退回处理</el-button>
            </div>
          </div>

          <el-card shadow="never" class="detail-card">
            <div slot="header" class="card-header">核心信息</div>
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="当前状态">
                <dict-tag
                  v-if="dictOptions('wo_order_status').length"
                  :options="dictOptions('wo_order_status')"
                  :value="detail.status"
                />
                <span v-else>{{ detail.status || '-' }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="版本号">{{ detail.version === undefined ? '-' : detail.version }}</el-descriptions-item>
              <el-descriptions-item label="报修分类">{{ detail.categoryName || detail.categoryCode || detail.categoryId || '-' }}</el-descriptions-item>
              <el-descriptions-item label="紧急程度">
                <dict-tag
                  v-if="dictOptions('wo_urgency_level').length"
                  :options="dictOptions('wo_urgency_level')"
                  :value="detail.urgencyLevel"
                />
                <span v-else>{{ detail.urgencyLevel || '-' }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="报修人">{{ detail.applicantName || detail.applicantId || '-' }}</el-descriptions-item>
              <el-descriptions-item label="联系电话">{{ detail.applicantPhone || '-' }}</el-descriptions-item>
              <el-descriptions-item label="所属部门">{{ detail.applicantDeptName || detail.applicantDeptId || '-' }}</el-descriptions-item>
              <el-descriptions-item label="报修位置">{{ detail.location || '-' }}</el-descriptions-item>
              <el-descriptions-item label="影响范围">
                <dict-tag
                  v-if="dictOptions('wo_impact_scope').length"
                  :options="dictOptions('wo_impact_scope')"
                  :value="detail.impactScope"
                />
                <span v-else>{{ detail.impactScope || '-' }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="提交时间">{{ formatTime(detail.submittedAt || detail.createTime) }}</el-descriptions-item>
              <el-descriptions-item label="响应截止">{{ formatTime(detail.responseDeadline) }}</el-descriptions-item>
              <el-descriptions-item label="到场截止">{{ formatTime(detail.arrivalDeadline) }}</el-descriptions-item>
              <el-descriptions-item label="完工截止">{{ formatTime(detail.finishDeadline) }}</el-descriptions-item>
              <el-descriptions-item label="问题描述" :span="2">
                <div class="detail-description">{{ detail.description || '-' }}</div>
              </el-descriptions-item>
              <el-descriptions-item label="可能原因" :span="2">
                <div class="detail-description">{{ detail.possibleCause || '-' }}</div>
              </el-descriptions-item>
            </el-descriptions>
          </el-card>

          <el-card shadow="never" class="detail-card">
            <div slot="header" class="card-header">当前处理人</div>
            <div class="assignee-summary">
              <i class="el-icon-user assignee-icon" />
              <div>
                <div class="assignee-name">{{ detail.currentAssigneeName || '未指派' }}</div>
                <div class="muted-text">
                  {{ detail.currentAssigneeId ? '人员 ID：' + detail.currentAssigneeId : '等待调度管理员派单' }}
                </div>
              </div>
            </div>
          </el-card>

          <el-card v-if="showEvaluationCard" v-loading="evaluationLoading" shadow="never" class="detail-card evaluation-card">
            <div slot="header" class="card-header">服务评价</div>
            <template v-if="evaluationView">
              <div class="evaluation-overall">
                <span class="evaluation-label">总体评分</span>
                <el-rate
                  v-if="evaluationView.overallScore !== null"
                  :value="evaluationView.overallScore"
                  disabled
                  show-score
                  text-color="#ff9900"
                  score-template="{value} 分"
                />
                <span v-else>-</span>
              </div>
              <el-descriptions :column="2" border size="small">
                <el-descriptions-item v-if="evaluationView.responseScore !== null" label="响应速度">
                  <el-rate :value="evaluationView.responseScore" disabled show-score score-template="{value} 分" />
                </el-descriptions-item>
                <el-descriptions-item v-if="evaluationView.qualityScore !== null" label="维修质量">
                  <el-rate :value="evaluationView.qualityScore" disabled show-score score-template="{value} 分" />
                </el-descriptions-item>
                <el-descriptions-item v-if="evaluationView.attitudeScore !== null" label="服务态度">
                  <el-rate :value="evaluationView.attitudeScore" disabled show-score score-template="{value} 分" />
                </el-descriptions-item>
                <el-descriptions-item label="评价人">{{ evaluationView.evaluator }}</el-descriptions-item>
                <el-descriptions-item label="评价时间">{{ formatTime(evaluationView.evaluatedAt) }}</el-descriptions-item>
                <el-descriptions-item label="评价内容" :span="2">
                  <div class="detail-description">{{ evaluationView.content }}</div>
                </el-descriptions-item>
              </el-descriptions>
            </template>
            <div v-else-if="evaluationError" class="evaluation-state">
              <el-alert :title="evaluationError" type="error" show-icon :closable="false" />
              <el-button type="text" size="small" @click="retryEvaluation">重新加载评价</el-button>
            </div>
            <el-empty v-else-if="!evaluationLoading" description="暂无评价" :image-size="80" />
          </el-card>

          <el-card shadow="never" class="detail-card">
            <div slot="header" class="card-header">处理时间线</div>
            <el-timeline v-if="detail.timeline && detail.timeline.length" class="order-timeline">
              <el-timeline-item
                v-for="(item, index) in detail.timeline"
                :key="item.id || index"
                :timestamp="formatTime(item.actionTime || item.createTime)"
                placement="top"
              >
                <div class="timeline-title">
                  {{ actionLabel(item.actionType) }}
                  <span v-if="item.operatorName" class="muted-text">· {{ item.operatorName }}</span>
                </div>
                <div v-if="item.fromStatus || item.toStatus" class="timeline-status">
                  {{ dictText('wo_order_status', item.fromStatus) }} → {{ dictText('wo_order_status', item.toStatus) }}
                </div>
                <div v-if="item.actionContent" class="timeline-content">{{ item.actionContent }}</div>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-else description="暂无处理记录" :image-size="80" />
          </el-card>

          <el-card shadow="never" class="detail-card">
            <div slot="header" class="card-header">附件</div>
            <div v-if="detail.attachments && detail.attachments.length" class="attachment-list">
              <div v-for="attachment in detail.attachments" :key="attachment.id" class="attachment-item">
                <i class="el-icon-paperclip attachment-icon" />
                <span class="attachment-name" :title="attachment.fileName">{{ attachment.fileName || '未命名附件' }}</span>
                <span class="muted-text attachment-size">{{ formatFileSize(attachment.fileSize) }}</span>
                <el-button
                  type="text"
                  size="mini"
                  icon="el-icon-download"
                  :loading="downloadingAttachmentId === attachment.id"
                  @click="downloadAttachmentFile(attachment)"
                >下载</el-button>
              </div>
            </div>
            <el-empty v-else description="暂无附件" :image-size="80" />
          </el-card>
        </template>
      </div>
    </el-drawer>

    <el-dialog
      :title="assignmentTitle"
      :visible.sync="assignmentOpen"
      width="560px"
      append-to-body
      :close-on-click-modal="false"
      :show-close="!assignmentSubmitting"
      @close="resetAssignment"
    >
      <el-alert
        v-if="assignmentAction === 'REASSIGN'"
        class="assignment-alert"
        :title="'当前处理人：' + (detail && detail.currentAssigneeName ? detail.currentAssigneeName : '未指派')"
        description="改派会保留原处理记录，请填写改派原因。"
        type="warning"
        show-icon
        :closable="false"
      />
      <div v-loading="engineersLoading" class="engineer-picker">
        <el-form ref="assignmentForm" :model="assignmentForm" :rules="assignmentRules" label-width="92px">
          <el-form-item label="维修人员" prop="engineerId">
            <el-select
              v-model="assignmentForm.engineerId"
              placeholder="请选择维修人员"
              filterable
              clearable
              style="width: 100%"
              :loading="engineersLoading"
              no-data-text="暂无可派单人员"
            >
              <el-option
                v-for="engineer in engineers"
                :key="engineerOptionId(engineer)"
                :label="engineerOptionLabel(engineer)"
                :value="engineerOptionId(engineer)"
              >
                <span class="engineer-option-name">{{ engineerName(engineer) }}</span>
                <span class="engineer-option-meta">{{ engineerOptionMeta(engineer) }}</span>
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="操作原因" prop="reason">
            <el-input
              v-model="assignmentForm.reason"
              type="textarea"
              :rows="4"
              maxlength="500"
              show-word-limit
              :placeholder="assignmentAction === 'REASSIGN' ? '请输入改派原因' : '可填写派单说明'"
            />
          </el-form-item>
        </el-form>
        <el-alert v-if="engineersError" :title="engineersError" type="error" show-icon :closable="false">
          <el-button type="text" size="mini" @click="loadEngineers">重试</el-button>
        </el-alert>
        <el-empty v-else-if="!engineersLoading && engineers.length === 0" description="暂无可用维修人员" :image-size="80" />
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button :disabled="assignmentSubmitting" @click="cancelAssignment">取消</el-button>
        <el-button
          type="primary"
          :loading="assignmentSubmitting"
          :disabled="engineersLoading || engineers.length === 0"
          @click="submitAssignment"
        >{{ assignmentAction === 'REASSIGN' ? '确认改派' : '确认派单' }}</el-button>
      </div>
    </el-dialog>

    <el-dialog
      title="退回处理"
      :visible.sync="returnOpen"
      width="520px"
      append-to-body
      :close-on-click-modal="false"
      :close-on-press-escape="!returnSubmitting && !returnConfirming"
      :show-close="!returnSubmitting && !returnConfirming"
      @close="resetReturn"
    >
      <el-alert
        class="return-alert"
        title="退回后工单将重新进入处理中，原因会写入处理时间线。"
        type="warning"
        show-icon
        :closable="false"
      />
      <el-form ref="returnForm" :model="returnForm" :rules="returnRules" label-width="88px">
        <el-form-item label="退回原因" prop="reason">
          <el-input
            v-model="returnForm.reason"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="请输入退回处理原因"
          />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button :disabled="returnSubmitting || returnConfirming" @click="cancelReturn">取消</el-button>
        <el-button
          type="danger"
          :loading="returnSubmitting"
          :disabled="returnSubmitting || returnConfirming"
          @click="submitReturn"
        >确认退回</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { saveAs } from 'file-saver'
import { blobValidate } from '@/utils/ruoyi'
import {
  assignOrder,
  createIdempotencyKey,
  downloadAttachment,
  getEngineers,
  getOrder,
  getOrderEvaluation,
  listCategories,
  listOrders,
  reassignOrder,
  returnOrder
} from '@/api/workorder/order'

function unwrapResponse(response) {
  if (response && Object.prototype.hasOwnProperty.call(response, 'data')) {
    return response.data
  }
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

function extractDetail(response) {
  const payload = unwrapResponse(response)
  if (payload && payload.data && !payload.id && !payload.orderId) return payload.data
  return payload
}

function isEvaluation(value) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return false
  return [
    'evaluationId',
    'overallScore',
    'rating',
    'score',
    'evaluationContent',
    'evaluatedAt'
  ].some(key => Object.prototype.hasOwnProperty.call(value, key))
}

function extractEvaluation(response) {
  const payload = unwrapResponse(response)
  const candidates = [
    payload && payload.evaluation,
    payload && payload.evaluationInfo,
    payload && payload.data,
    payload
  ]
  return candidates.find(isEvaluation) || null
}

function embeddedEvaluation(detail) {
  if (!detail) return null
  return [detail.evaluation, detail.evaluationInfo, detail.orderEvaluation].find(isEvaluation) || null
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
  const message = messageFrom(error) || messageFrom(data) || messageFrom(data && data.data)
  return message || fallback
}

function isConflictError(error) {
  const response = error && error.response
  const data = response && response.data
  const codeValues = [
    response && response.status,
    data && data.code,
    data && data.status,
    data && data.errorCode,
    error && error.code
  ].map(value => String(value || '').toUpperCase())
  if (codeValues.includes('409') || codeValues.includes('WO_VERSION_CONFLICT') || codeValues.includes('WO_STATE_CONFLICT')) {
    return true
  }
  const message = requestErrorMessage(error, '').toLowerCase()
  return /409|version|stale|state conflict|conflict|状态已变化|版本冲突|并发|已被其他/.test(message)
}

function isNotFoundError(error) {
  const response = error && error.response
  const data = error && error.data
  return [
    response && response.status,
    error && error.code,
    data && data.code,
    data && data.status
  ].some(value => String(value || '') === '404')
}

export default {
  name: 'WorkorderOrder',
  dicts: [
    'wo_order_status',
    'wo_urgency_level',
    'wo_impact_scope',
    'wo_source_type',
    'wo_process_stage',
    'wo_attachment_stage',
    'wo_engineer_status'
  ],
  data() {
    return {
      showSearch: true,
      orderList: [],
      total: 0,
      listLoading: false,
      listError: '',
      categoryOptions: [],
      categoryLoading: false,
      categoryLoadError: '',
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        keyword: undefined,
        status: undefined,
        categoryId: undefined,
        urgencyLevel: undefined
      },
      detailOpen: false,
      detailLoading: false,
      detailError: '',
      detail: null,
      detailOrderId: null,
      detailRequestId: 0,
      evaluation: null,
      evaluationLoading: false,
      evaluationError: '',
      evaluationRequestId: 0,
      assignmentOpen: false,
      assignmentAction: 'ASSIGN',
      assignmentForm: {
        engineerId: undefined,
        reason: '',
        version: undefined
      },
      assignmentSubmitting: false,
      assignmentIdempotencyKey: '',
      assignmentPayloadFingerprint: '',
      engineers: [],
      engineersLoading: false,
      engineersError: '',
      returnOpen: false,
      returnForm: {
        reason: '',
        version: undefined
      },
      returnSubmitting: false,
      returnConfirming: false,
      returnIdempotencyKey: '',
      returnPayloadFingerprint: '',
      downloadingAttachmentId: null
    }
  },
  computed: {
    detailTitle() {
      return this.detail && this.detail.orderNo ? '工单详情 · ' + this.detail.orderNo : '工单详情'
    },
    assignmentTitle() {
      return this.assignmentAction === 'REASSIGN' ? '改派工单' : '派单'
    },
    assignmentRules() {
      return {
        engineerId: [
          { required: true, message: '请选择维修人员', trigger: 'change' }
        ],
        reason: this.assignmentAction === 'REASSIGN'
          ? [{ required: true, message: '请输入改派原因', trigger: 'blur' }]
          : []
      }
    },
    returnRules() {
      return {
        reason: [{
          validator: (rule, value, callback) => {
            if (!String(value || '').trim()) {
              callback(new Error('请输入退回原因'))
              return
            }
            callback()
          },
          trigger: 'blur'
        }]
      }
    },
    showEvaluationCard() {
      if (this.evaluation) return true
      const status = String(this.detail && this.detail.status || '').toUpperCase()
      return status === 'COMPLETED' || status === 'CLOSED'
    },
    evaluationView() {
      if (!this.evaluation) return null
      return {
        overallScore: this.evaluationScore(['overallScore', 'rating', 'score']),
        responseScore: this.evaluationScore(['responseScore', 'responseSpeedScore']),
        qualityScore: this.evaluationScore(['qualityScore', 'repairQualityScore']),
        attitudeScore: this.evaluationScore(['attitudeScore', 'serviceAttitudeScore']),
        content: this.firstValue(this.evaluation, ['evaluationContent', 'content', 'comment']) || '-',
        evaluator: this.firstValue(this.evaluation, ['evaluatorName', 'evaluatorNickName', 'userName']) ||
          this.firstValue(this.detail, ['applicantName']) ||
          this.firstValue(this.evaluation, ['evaluatorId']) || '-',
        evaluatedAt: this.firstValue(this.evaluation, ['evaluatedAt', 'evaluationTime', 'createTime'])
      }
    }
  },
  created() {
    this.restoreQueryFromRoute()
    this.getCategories()
    this.getList()
  },
  methods: {
    dictOptions(type) {
      if (!this.dict || !this.dict.type || !this.dict.type[type]) return []
      return this.dict.type[type]
    },
    dictText(type, value) {
      if (value === undefined || value === null || value === '') return '-'
      const option = this.dictOptions(type).find(item => String(item.value) === String(value))
      return option ? option.label : value
    },
    restoreQueryFromRoute() {
      const routeQuery = (this.$route && this.$route.query) || {}
      this.queryParams.keyword = routeQuery.keyword || undefined
      this.queryParams.status = routeQuery.status || undefined
      this.queryParams.categoryId = routeQuery.categoryId || undefined
      this.queryParams.urgencyLevel = routeQuery.urgencyLevel || undefined
      this.queryParams.pageNum = parseInt(routeQuery.pageNum, 10) || 1
      this.queryParams.pageSize = parseInt(routeQuery.pageSize, 10) || 10
    },
    syncQueryToRoute() {
      if (!this.$router || !this.$route) return
      const managedKeys = ['keyword', 'status', 'categoryId', 'urgencyLevel', 'pageNum', 'pageSize']
      const query = Object.assign({}, this.$route.query)
      managedKeys.forEach(key => { delete query[key] })
      const listQuery = this.buildListQuery()
      Object.keys(listQuery).forEach(key => {
        query[key] = String(listQuery[key])
      })
      this.$router.replace({ query }).catch(() => {})
    },
    buildListQuery() {
      const query = {
        pageNum: this.queryParams.pageNum,
        pageSize: this.queryParams.pageSize
      }
      ;['keyword', 'status', 'categoryId', 'urgencyLevel'].forEach(key => {
        const value = this.queryParams[key]
        if (value !== undefined && value !== null && value !== '') query[key] = value
      })
      return query
    },
    getCategories() {
      this.categoryLoading = true
      this.categoryLoadError = ''
      return listCategories().then(response => {
        this.categoryOptions = extractRows(response)
      }).catch(error => {
        // Category loading must not hide the real order-list result. The
        // filter remains typeable because the select allows custom IDs.
        this.categoryOptions = []
        this.categoryLoadError = requestErrorMessage(error, '工单分类加载失败，可直接输入分类 ID')
      }).then(() => {
        this.categoryLoading = false
      })
    },
    getList() {
      this.listLoading = true
      this.listError = ''
      const query = this.buildListQuery()
      return listOrders(query).then(response => {
        const rows = extractRows(response)
        this.orderList = rows
        this.total = extractTotal(response, rows)
      }).catch(error => {
        this.orderList = []
        this.total = 0
        this.listError = requestErrorMessage(error, '工单列表加载失败，请稍后重试')
      }).then(() => {
        this.listLoading = false
      })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.syncQueryToRoute()
      this.getList()
    },
    handlePagination() {
      this.syncQueryToRoute()
      this.getList()
    },
    resetQuery() {
      this.queryParams.keyword = undefined
      this.queryParams.status = undefined
      this.queryParams.categoryId = undefined
      this.queryParams.urgencyLevel = undefined
      this.queryParams.pageNum = 1
      this.syncQueryToRoute()
      this.getList()
    },
    orderId(row) {
      if (!row) return undefined
      return row.id !== undefined && row.id !== null ? row.id : row.orderId
    },
    openDetail(row) {
      const orderId = this.orderId(row)
      if (orderId === undefined || orderId === null) {
        this.$modal.msgError('工单缺少有效编号，无法加载详情')
        return
      }
      this.detailOpen = true
      this.detail = null
      this.detailOrderId = orderId
      this.detailError = ''
      this.resetEvaluationState()
      this.loadDetail(orderId)
    },
    loadDetail(orderId) {
      const requestId = ++this.detailRequestId
      this.detailLoading = true
      this.detailError = ''
      return getOrder(orderId).then(response => {
        if (requestId === this.detailRequestId) {
          const nextDetail = extractDetail(response)
          this.detail = nextDetail
          this.resetEvaluationState()
          const nextEvaluation = embeddedEvaluation(nextDetail)
          if (nextEvaluation) {
            this.evaluation = nextEvaluation
          } else if (String(nextDetail && nextDetail.status || '').toUpperCase() === 'CLOSED') {
            // CLOSED details may omit the evaluation projection; load it only
            // when needed and guard the result with the active drawer identity.
            this.loadEvaluation(orderId)
          }
        }
      }).catch(error => {
        if (requestId === this.detailRequestId) {
          this.detail = null
          this.detailError = requestErrorMessage(error, '工单详情加载失败，请稍后重试')
        }
      }).then(() => {
        if (requestId === this.detailRequestId) this.detailLoading = false
      })
    },
    retryDetail() {
      if (this.detailOrderId !== undefined && this.detailOrderId !== null) {
        this.loadDetail(this.detailOrderId)
      }
    },
    closeDetail() {
      if (this.assignmentOpen && !this.assignmentSubmitting) this.assignmentOpen = false
      if (this.returnOpen && !this.returnSubmitting && !this.returnConfirming) this.returnOpen = false
      this.detailRequestId += 1
      this.resetEvaluationState()
      this.detail = null
      this.detailOrderId = null
      this.detailError = ''
    },
    canAction(action) {
      if (!this.detail || !Array.isArray(this.detail.allowedActions)) return false
      return this.detail.allowedActions.some(item => String(item).toUpperCase() === action)
    },
    resetEvaluationState() {
      this.evaluationRequestId += 1
      this.evaluation = null
      this.evaluationLoading = false
      this.evaluationError = ''
    },
    isCurrentDetail(orderId) {
      return this.detailOpen && this.detailOrderId !== null && String(this.detailOrderId) === String(orderId)
    },
    loadEvaluation(orderId) {
      const requestId = ++this.evaluationRequestId
      this.evaluationLoading = true
      this.evaluationError = ''
      return getOrderEvaluation(orderId).then(response => {
        if (requestId === this.evaluationRequestId && this.isCurrentDetail(orderId)) {
          this.evaluation = extractEvaluation(response)
        }
      }).catch(error => {
        if (requestId === this.evaluationRequestId && this.isCurrentDetail(orderId)) {
          this.evaluation = null
          this.evaluationError = isNotFoundError(error)
            ? ''
            : requestErrorMessage(error, '评价加载失败，请稍后重试')
        }
      }).then(() => {
        if (requestId === this.evaluationRequestId && this.isCurrentDetail(orderId)) {
          this.evaluationLoading = false
        }
      })
    },
    retryEvaluation() {
      if (this.detailOrderId !== undefined && this.detailOrderId !== null) {
        this.loadEvaluation(this.detailOrderId)
      }
    },
    openAssignment(action) {
      if (!this.detail || !this.canAction(action)) return
      this.assignmentAction = action
      this.assignmentForm = {
        engineerId: undefined,
        reason: '',
        version: this.detail.version
      }
      this.assignmentIdempotencyKey = ''
      this.assignmentPayloadFingerprint = ''
      this.engineers = []
      this.engineersError = ''
      this.assignmentOpen = true
      this.loadEngineers()
      this.$nextTick(() => {
        if (this.$refs.assignmentForm) this.$refs.assignmentForm.clearValidate()
      })
    },
    loadEngineers() {
      this.engineersLoading = true
      this.engineersError = ''
      return getEngineers().then(response => {
        // 休班人员不应进入派单候选项；后端仍会在提交时再次校验，避免并发状态变化导致误派。
        this.engineers = extractRows(response).filter(item =>
          this.engineerOptionId(item) !== undefined && item.dutyStatus !== 'OFF_DUTY'
        )
      }).catch(error => {
        this.engineers = []
        this.engineersError = requestErrorMessage(error, '维修人员加载失败，请重试')
      }).then(() => {
        this.engineersLoading = false
      })
    },
    firstValue(item, keys) {
      for (let i = 0; i < keys.length; i += 1) {
        const value = item && item[keys[i]]
        if (value !== undefined && value !== null && value !== '') return value
      }
      return undefined
    },
    evaluationScore(keys) {
      const value = this.firstValue(this.evaluation, keys)
      if (value === undefined) return null
      const score = Number(value)
      return Number.isNaN(score) ? null : score
    },
    engineerOptionId(engineer) {
      return this.firstValue(engineer, ['engineerId', 'userId', 'id'])
    },
    engineerName(engineer) {
      return this.firstValue(engineer, ['engineerName', 'nickName', 'userName', 'name']) || '未命名人员'
    },
    engineerStatus(engineer) {
      return this.firstValue(engineer, ['workStatus', 'status', 'dutyStatus'])
    },
    engineerLoad(engineer) {
      const load = this.firstValue(engineer, ['activeOrderCount', 'currentLoad', 'load', 'orderCount'])
      return load === undefined ? '' : '当前 ' + load + ' 单'
    },
    engineerOptionLabel(engineer) {
      return this.engineerName(engineer) + (this.engineerLoad(engineer) ? ' · ' + this.engineerLoad(engineer) : '')
    },
    engineerOptionMeta(engineer) {
      const status = this.engineerStatus(engineer)
      const department = this.firstValue(engineer, ['engineerDeptName', 'deptName', 'departmentName', 'dept'])
      return [department, status ? this.dictText('wo_engineer_status', status) : ''].filter(Boolean).join(' · ') || '状态未知'
    },
    actionLabel(action) {
      const labels = {
        SUBMIT: '提交工单',
        ASSIGN: '派单',
        REASSIGN: '改派',
        ACCEPT: '接单',
        ARRIVE: '到场确认',
        ASSESS: '故障评估',
        PROGRESS: '更新进度',
        FINISH: '提交完工',
        CONFIRM: '确认完工',
        CANCEL: '取消工单',
        RETURN: '退回处理'
      }
      return labels[String(action || '').toUpperCase()] || action || '状态变更'
    },
    assignmentBody() {
      return {
        engineerId: this.assignmentForm.engineerId,
        reason: (this.assignmentForm.reason || '').trim(),
        version: this.assignmentForm.version
      }
    },
    assignmentKey(body) {
      const fingerprint = JSON.stringify(body)
      if (!this.assignmentIdempotencyKey || this.assignmentPayloadFingerprint !== fingerprint) {
        this.assignmentIdempotencyKey = createIdempotencyKey()
        this.assignmentPayloadFingerprint = fingerprint
      }
      return this.assignmentIdempotencyKey
    },
    submitAssignment() {
      if (this.assignmentSubmitting || !this.detail) return
      this.$refs.assignmentForm.validate(valid => {
        if (!valid) return
        const body = this.assignmentBody()
        const operationText = this.assignmentAction === 'REASSIGN' ? '改派' : '派单'
        this.$modal.confirm('确认' + operationText + '给“' + this.engineerName(this.engineers.find(item => String(this.engineerOptionId(item)) === String(body.engineerId))) + '”吗？')
          .then(() => this.executeAssignment(body, operationText))
          .catch(() => {})
      })
    },
    executeAssignment(body, operationText) {
      const orderId = this.orderId(this.detail)
      const idempotencyKey = this.assignmentKey(body)
      this.assignmentSubmitting = true
      const request = this.assignmentAction === 'REASSIGN'
        ? reassignOrder(orderId, body, idempotencyKey)
        : assignOrder(orderId, body, idempotencyKey)
      return request.then(() => {
        this.assignmentSubmitting = false
        this.assignmentOpen = false
        this.$modal.msgSuccess(operationText + '成功')
        // Refresh both projections after the command; the detail version and
        // allowedActions must never be retained from the stale drawer state.
        return Promise.all([this.getList(), this.loadDetail(orderId)])
      }).catch(error => {
        this.assignmentSubmitting = false
        if (isConflictError(error)) {
          this.assignmentOpen = false
          this.$modal.msgWarning('工单状态已被其他操作更新，已重新加载最新详情和列表，请确认后重试')
          return Promise.all([this.getList(), this.loadDetail(orderId)])
        }
        this.$modal.msgError(requestErrorMessage(error, operationText + '失败，请稍后重试'))
      })
    },
    cancelAssignment() {
      if (!this.assignmentSubmitting) this.assignmentOpen = false
    },
    resetAssignment() {
      if (this.assignmentSubmitting) return
      this.assignmentForm = { engineerId: undefined, reason: '', version: undefined }
      this.assignmentIdempotencyKey = ''
      this.assignmentPayloadFingerprint = ''
      this.engineers = []
      this.engineersError = ''
    },
    openReturn() {
      if (!this.detail || !this.canAction('RETURN')) return
      if (this.detail.version === undefined || this.detail.version === null) {
        this.$modal.msgError('工单缺少版本号，请刷新详情后重试')
        return
      }
      this.returnForm = {
        reason: '',
        version: this.detail.version
      }
      this.returnIdempotencyKey = ''
      this.returnPayloadFingerprint = ''
      this.returnOpen = true
      this.$nextTick(() => {
        if (this.$refs.returnForm) this.$refs.returnForm.clearValidate()
      })
    },
    returnBody() {
      return {
        reason: (this.returnForm.reason || '').trim(),
        version: this.returnForm.version
      }
    },
    returnKey(body) {
      const fingerprint = JSON.stringify(body)
      if (!this.returnIdempotencyKey || this.returnPayloadFingerprint !== fingerprint) {
        this.returnIdempotencyKey = createIdempotencyKey()
        this.returnPayloadFingerprint = fingerprint
      }
      return this.returnIdempotencyKey
    },
    submitReturn() {
      if (this.returnSubmitting || this.returnConfirming || !this.detail) return
      this.returnConfirming = true
      this.$refs.returnForm.validate(valid => {
        if (!valid) {
          this.returnConfirming = false
          return
        }
        const body = this.returnBody()
        this.$modal.confirm('确认将该工单退回处理中吗？退回原因将写入处理时间线。')
          .then(() => {
            this.returnConfirming = false
            return this.executeReturn(body)
          })
          .catch(() => {
            this.returnConfirming = false
          })
      })
    },
    executeReturn(body) {
      const orderId = this.orderId(this.detail)
      const idempotencyKey = this.returnKey(body)
      this.returnSubmitting = true
      return returnOrder(orderId, body, idempotencyKey).then(() => {
        this.returnSubmitting = false
        this.returnOpen = false
        this.$modal.msgSuccess('退回处理成功')
        return Promise.all([this.getList(), this.loadDetail(orderId)])
      }).catch(error => {
        this.returnSubmitting = false
        if (isConflictError(error)) {
          this.returnOpen = false
          this.$modal.msgWarning('工单状态已被其他操作更新，已重新加载最新详情和列表，请确认后重试')
          return Promise.all([this.getList(), this.loadDetail(orderId)])
        }
        // Keep the form and key after ordinary failures so retrying the same
        // payload reaches the server with the same idempotency identity.
        this.$modal.msgError(requestErrorMessage(error, '退回处理失败，请稍后重试'))
      })
    },
    cancelReturn() {
      if (!this.returnSubmitting && !this.returnConfirming) this.returnOpen = false
    },
    resetReturn() {
      if (this.returnSubmitting || this.returnConfirming) return
      this.returnForm = { reason: '', version: undefined }
      this.returnIdempotencyKey = ''
      this.returnPayloadFingerprint = ''
    },
    formatTime(value) {
      if (!value) return '-'
      return this.parseTime ? this.parseTime(value) : value
    },
    formatFileSize(size) {
      if (size === undefined || size === null || size === '') return '-'
      const value = Number(size)
      if (Number.isNaN(value)) return size
      if (value < 1024) return value + ' B'
      if (value < 1024 * 1024) return (value / 1024).toFixed(1) + ' KB'
      return (value / (1024 * 1024)).toFixed(1) + ' MB'
    },
    downloadAttachmentFile(attachment) {
      const attachmentId = this.firstValue(attachment, ['id', 'attachmentId'])
      if (attachmentId === undefined) {
        this.$modal.msgError('附件缺少有效编号，无法下载')
        return
      }
      this.downloadingAttachmentId = attachmentId
      return downloadAttachment(attachmentId).then(blob => {
        if (!blobValidate(blob)) return this.readBlobError(blob)
        saveAs(blob, attachment.fileName || ('workorder-attachment-' + attachmentId))
      }).catch(error => {
        this.$modal.msgError(requestErrorMessage(error, '附件下载失败，请稍后重试'))
      }).then(() => {
        this.downloadingAttachmentId = null
      })
    },
    readBlobError(blob) {
      if (!blob || typeof blob.text !== 'function') return Promise.reject(new Error('附件下载失败'))
      return blob.text().then(text => {
        let payload
        try {
          payload = JSON.parse(text)
        } catch (e) {
          payload = null
        }
        throw new Error(requestErrorMessage(payload, '附件下载失败'))
      })
    }
  }
}
</script>

<style scoped>
.filter-form {
  margin-bottom: 12px;
}

.filter-alert,
.list-alert,
.assignment-alert,
.return-alert {
  margin-bottom: 12px;
}

.order-table {
  margin-top: 8px;
}

.state-panel {
  padding: 48px 0;
  color: #909399;
  text-align: center;
}

.state-panel p {
  margin: 10px 0 16px;
}

.state-icon {
  font-size: 32px;
}

.state-icon-error {
  color: #f56c6c;
}

.detail-body {
  min-height: 360px;
  padding-bottom: 24px;
}

.detail-state-panel {
  padding-top: 120px;
}

.detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 16px;
}

.detail-heading h3 {
  margin: 6px 0 0;
  color: #303133;
  font-size: 20px;
  font-weight: 500;
}

.detail-order-no {
  color: #909399;
  font-size: 13px;
}

.detail-actions {
  flex-shrink: 0;
  margin-left: 16px;
}

.detail-card {
  margin-bottom: 16px;
}

.card-header {
  color: #303133;
  font-weight: 500;
}

.detail-description {
  min-height: 20px;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.7;
}

.evaluation-card {
  min-height: 142px;
}

.evaluation-overall {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}

.evaluation-label {
  width: 72px;
  color: #606266;
}

.evaluation-state .el-button {
  margin-top: 8px;
}

.assignee-summary {
  display: flex;
  align-items: center;
  min-height: 56px;
}

.assignee-icon {
  margin-right: 14px;
  color: #409eff;
  font-size: 28px;
}

.assignee-name {
  color: #303133;
  font-size: 16px;
}

.muted-text {
  color: #909399;
  font-size: 12px;
}

.order-timeline {
  padding: 4px 8px 0;
}

.timeline-title {
  color: #303133;
  font-size: 14px;
}

.timeline-status {
  margin-top: 5px;
  color: #606266;
  font-size: 12px;
}

.timeline-content {
  margin-top: 5px;
  color: #606266;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.6;
}

.attachment-list {
  padding: 0 4px;
}

.attachment-item {
  display: flex;
  align-items: center;
  min-height: 36px;
  border-bottom: 1px solid #ebeef5;
}

.attachment-item:last-child {
  border-bottom: 0;
}

.attachment-icon {
  margin-right: 8px;
  color: #909399;
}

.attachment-name {
  flex: 1;
  overflow: hidden;
  color: #409eff;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.attachment-size {
  margin: 0 10px;
}

.engineer-picker {
  min-height: 180px;
}

.engineer-option-name {
  float: left;
}

.engineer-option-meta {
  float: right;
  margin-left: 20px;
  color: #8492a6;
  font-size: 12px;
}

::v-deep .workorder-detail-drawer .el-drawer__body {
  overflow: auto;
  padding: 20px;
}

@media screen and (max-width: 900px) {
  .detail-header {
    display: block;
  }

  .detail-actions {
    margin-top: 12px;
    margin-left: 0;
  }
}
</style>

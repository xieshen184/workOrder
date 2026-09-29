<template>
  <view class="page-container">
    <!-- 顶部导航栏 -->
    <u-navbar
      title="工单详情"
      :is-back="true"
      background="#36CFC9"
      title-color="#ffffff"
      left-icon-color="#ffffff"
      :border-bottom="false"
    ></u-navbar>

    <!-- 工单状态标签 -->
    <view class="status-tag" :class="getStatusClass(order.status)" v-if="!loading && !error && order.id !== undefined">
      <text class="status-text">{{ getStatusText(order.status) }}</text>
    </view>

    <!-- 内容滚动区域 -->
    <scroll-view
      class="content-scroll"
      scroll-y
      :show-scrollbar="false"
    >
      <!-- 骨架屏加载状态 -->
      <view v-if="loading" class="detail-skeleton">
        <u-skeleton
          :rows="8"
          :title="true"
          :loading="true"
          row-height="40"
          title-width="40%"
          radius="16rpx"
          bg-color="#f5f5f5"
          active-color="#eeeeee"
          class="skeleton-loading"
        ></u-skeleton>

        <u-skeleton
          :rows="5"
          :title="true"
          :loading="true"
          row-height="40"
          title-width="40%"
          radius="16rpx"
          bg-color="#f5f5f5"
          active-color="#eeeeee"
          class="skeleton-loading"
          style="margin-top: 20rpx;"
        ></u-skeleton>
      </view>

      <!-- 错误提示与重试 -->
      <view v-else-if="error" class="error-state">
        <u-icon name="info-circle" color="#F53F3F" size="44"></u-icon>
        <text class="error-text">{{ error }}</text>
        <u-button type="primary" size="mini" plain @click="loadOrderDetail">重试</u-button>
      </view>

      <u-empty v-else-if="!order.id" text="未找到工单详情" mode="search" color="#C9CDD4" class="empty-tip"></u-empty>

      <!-- 工单详情内容 -->
      <view v-else class="detail-container">
        <!-- 工单基本信息 -->
        <view class="info-card">
          <view class="card-title">
            <u-icon name="clipboard" color="#36CFC9" size="28"></u-icon>
            <text class="title-text">基本信息</text>
          </view>
          <view class="info-content">
            <view class="info-row">
              <text class="info-label">工单编号：</text>
              <text class="info-value">{{ order.orderNo || order.id }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">申请人：</text>
              <text class="info-value">{{ order.applicantName || '—' }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">联系电话：</text>
              <text class="info-value">{{ order.applicantPhone || '—' }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">所属科室：</text>
              <text class="info-value">{{ order.applicantDeptName || '—' }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">提交时间：</text>
              <text class="info-value">{{ formatTime(order.submittedAt) }}</text>
            </view>
            <view class="info-row" v-if="order.currentAssigneeName">
              <text class="info-label">处理人：</text>
              <text class="info-value">{{ order.currentAssigneeName }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">数据版本：</text>
              <text class="info-value">{{ order.version }}</text>
            </view>
          </view>
        </view>

        <!-- 故障信息 -->
        <view class="info-card">
          <view class="card-title">
            <u-icon name="warning-circle" color="#FF9C07" size="28"></u-icon>
            <text class="title-text">故障信息</text>
          </view>
          <view class="info-content">
            <view class="info-row">
              <text class="info-label">故障分类：</text>
              <text class="info-value">{{ order.categoryName || '—' }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">紧急程度：</text>
              <text class="info-value urgency-text" :class="getUrgencyClass(order.urgencyLevel)">{{ urgencyText(order.urgencyLevel) }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">影响范围：</text>
              <text class="info-value">{{ scopeText(order.impactScope) }}</text>
            </view>
            <view class="info-row full-width">
              <text class="info-label">故障位置：</text>
              <text class="info-value full-content">{{ order.location || '—' }}</text>
            </view>
            <view class="info-row full-width">
              <text class="info-label">故障描述：</text>
              <text class="info-value full-content">{{ order.description || '—' }}</text>
            </view>
            <view class="info-row full-width" v-if="order.possibleCause">
              <text class="info-label">补充说明：</text>
              <text class="info-value full-content">{{ order.possibleCause }}</text>
            </view>
          </view>
        </view>

        <!-- 处理时间线 -->
        <view class="info-card" v-if="timeline.length">
          <view class="card-title">
            <u-icon name="clock" color="#86909C" size="28"></u-icon>
            <text class="title-text">处理记录</text>
          </view>
          <view class="timeline">
            <view class="timeline-item" v-for="(item, index) in timeline" :key="item.id || index">
              <view class="timeline-node" :class="{ last: index === timeline.length - 1 }"></view>
              <view class="timeline-content">
                <view class="timeline-time">{{ formatTime(item.actionTime || item.time) }}</view>
                <view class="timeline-text">
                  <text>{{ actionText(item.actionType) }}</text>
                  <text v-if="item.actionContent">：{{ item.actionContent }}</text>
                  <text v-else-if="item.fromStatus || item.toStatus">：{{ getStatusText(item.fromStatus) }} → {{ getStatusText(item.toStatus) }}</text>
                </view>
                <view class="timeline-operator">操作人：{{ item.operatorName || item.operator || '系统' }}</view>
              </view>
            </view>
          </view>
        </view>

        <!-- 已绑定附件 -->
        <view class="info-card" v-if="attachments.length">
          <view class="card-title">
            <u-icon name="paperclip" color="#86909C" size="28"></u-icon>
            <text class="title-text">附件信息</text>
          </view>
          <view class="attachment-list">
            <view class="attachment-item" v-for="item in attachments" :key="item.id" @click="openAttachment(item)">
              <u-icon name="attach" color="#36CFC9" size="36"></u-icon>
              <view class="attachment-copy">
                <text class="attachment-name">{{ item.fileName || item.name || '未命名附件' }}</text>
                <text class="attachment-meta">{{ stageText(item.bizStage) }}</text>
              </view>
              <u-icon name="arrow-right" color="#C9CDD4" size="24"></u-icon>
            </view>
          </view>
        </view>

        <!-- 操作按钮区域：唯一依据是详情接口返回的 allowedActions。 -->
        <view class="button-container" v-if="actionButtons.length">
          <u-button
            v-for="action in actionButtons"
            :key="action"
            type="primary"
            size="large"
            :loading="submittingAction === action"
            :disabled="Boolean(submittingAction)"
            :custom-style="buttonStyle(action)"
            @click="openAction(action)"
          >
            {{ action === 'REQUEST_DELAY' ? delayActionText() : actionText(action) }}
          </u-button>
        </view>

        <!-- 到场确认 -->
        <view v-if="activeAction === 'ARRIVE'" class="action-form-card">
          <view class="card-title">
            <u-icon name="map" color="#36CFC9" size="28"></u-icon>
            <text class="title-text">到场确认</text>
          </view>
          <view class="action-form-content">
            <text class="action-label">到场说明</text>
            <u-textarea
              v-model="forms.ARRIVE.content"
              placeholder="请简要说明已到场情况"
              :height="130"
              :maxlength="200"
              border="bottom"
            ></u-textarea>
            <text class="action-label attachment-label">到场照片（至少1张）</text>
            <u-upload
              :file-list="actionFiles('ARRIVAL')"
              :max-count="5"
              :disabled="Boolean(submittingAction)"
              @afterRead="afterActionRead('ARRIVAL', $event)"
              @delete="deleteActionFile('ARRIVAL', $event)"
              name="file"
              accept="image"
              class="upload-component"
            >
              <template #add>
                <view class="add-btn">
                  <u-icon name="camera" color="#36CFC9" size="36"></u-icon>
                  <text class="add-text">上传/拍摄照片</text>
                </view>
              </template>
            </u-upload>
            <view v-for="(file, index) in actionFiles('ARRIVAL')" :key="`arrival-retry-${index}`">
              <view v-if="file.status === 'failed'" class="upload-retry-item">
                <text class="upload-retry-text">{{ file.name || '图片' }}上传失败</text>
                <u-button type="primary" size="mini" plain @click.stop="retryActionUpload('ARRIVAL', index)">重试</u-button>
              </view>
            </view>
            <view class="action-form-actions">
              <u-button type="primary" :loading="submittingAction === 'ARRIVE'" :disabled="Boolean(submittingAction)" @click="submitAction('ARRIVE')">提交到场</u-button>
              <u-button plain :disabled="Boolean(submittingAction)" @click="cancelAction">取消</u-button>
            </view>
          </view>
        </view>

        <!-- 故障评估 -->
        <view v-if="activeAction === 'ASSESS'" class="action-form-card">
          <view class="card-title">
            <u-icon name="edit-pen" color="#FF9C07" size="28"></u-icon>
            <text class="title-text">故障评估</text>
          </view>
          <view class="action-form-content">
            <text class="action-label">评估说明</text>
            <u-textarea
              v-model="forms.ASSESSMENT.content"
              placeholder="请填写故障原因、处理方案和预计完成情况"
              :height="150"
              border="bottom"
            ></u-textarea>
            <view class="switch-row">
              <text class="action-label">需要零部件</text>
              <u-switch v-model="forms.ASSESSMENT.requiresParts" active-color="#36CFC9"></u-switch>
            </view>
            <view v-if="forms.ASSESSMENT.requiresParts" class="conditional-field">
              <text class="action-label">零部件说明</text>
              <u-textarea
                v-model="forms.ASSESSMENT.partsDescription"
                placeholder="请说明所需零部件"
                :height="110"
                border="bottom"
              ></u-textarea>
            </view>
            <view class="assessment-row">
              <view class="assessment-field">
                <text class="action-label">预计工时</text>
                <u-input v-model="forms.ASSESSMENT.assessedHours" type="number" placeholder="小时" border="bottom"></u-input>
              </view>
            </view>
            <text class="action-label">评估紧急程度</text>
            <u-radio-group v-model="forms.ASSESSMENT.assessedUrgency" class="radio-group-horizontal">
              <u-radio :label="1" :name="1" class="radio-item"><text class="radio-label">一般</text></u-radio>
              <u-radio :label="2" :name="2" class="radio-item"><text class="radio-label">紧急</text></u-radio>
              <u-radio :label="3" :name="3" class="radio-item"><text class="radio-label">特急</text></u-radio>
            </u-radio-group>
            <text class="action-label">评估影响范围</text>
            <u-radio-group v-model="forms.ASSESSMENT.assessedScope" class="radio-group-horizontal">
              <u-radio :label="1" :name="1" class="radio-item"><text class="radio-label">个人</text></u-radio>
              <u-radio :label="2" :name="2" class="radio-item"><text class="radio-label">科室</text></u-radio>
              <u-radio :label="3" :name="3" class="radio-item"><text class="radio-label">多科室</text></u-radio>
              <u-radio :label="4" :name="4" class="radio-item"><text class="radio-label">全院</text></u-radio>
            </u-radio-group>
            <view class="switch-row">
              <text class="action-label">需要延期</text>
              <u-switch v-model="forms.ASSESSMENT.requiresExtension" active-color="#36CFC9"></u-switch>
            </view>
            <view class="action-form-actions">
              <u-button type="primary" :loading="submittingAction === 'ASSESS'" :disabled="Boolean(submittingAction)" @click="submitAction('ASSESS')">提交评估</u-button>
              <u-button plain :disabled="Boolean(submittingAction)" @click="cancelAction">取消</u-button>
            </view>
          </view>
        </view>

        <!-- 处理进度 -->
        <view v-if="activeAction === 'PROGRESS'" class="action-form-card">
          <view class="card-title">
            <u-icon name="clock" color="#FF9C07" size="28"></u-icon>
            <text class="title-text">更新处理进度</text>
          </view>
          <view class="action-form-content">
            <text class="action-label">进度内容</text>
            <u-textarea
              v-model="forms.PROGRESS.content"
              placeholder="请填写本次处理进展"
              :height="150"
              border="bottom"
            ></u-textarea>
            <text class="action-label attachment-label">过程附件（可选）</text>
            <u-upload
              :file-list="actionFiles('PROCESS')"
              :max-count="5"
              :disabled="Boolean(submittingAction)"
              @afterRead="afterActionRead('PROCESS', $event)"
              @delete="deleteActionFile('PROCESS', $event)"
              name="file"
              accept="image"
              class="upload-component"
            >
              <template #add>
                <view class="add-btn">
                  <u-icon name="camera" color="#36CFC9" size="36"></u-icon>
                  <text class="add-text">上传/拍摄照片</text>
                </view>
              </template>
            </u-upload>
            <view v-for="(file, index) in actionFiles('PROCESS')" :key="`process-retry-${index}`">
              <view v-if="file.status === 'failed'" class="upload-retry-item">
                <text class="upload-retry-text">{{ file.name || '图片' }}上传失败</text>
                <u-button type="primary" size="mini" plain @click.stop="retryActionUpload('PROCESS', index)">重试</u-button>
              </view>
            </view>
            <view class="action-form-actions">
              <u-button type="primary" :loading="submittingAction === 'PROGRESS'" :disabled="Boolean(submittingAction)" @click="submitAction('PROGRESS')">提交进度</u-button>
              <u-button plain :disabled="Boolean(submittingAction)" @click="cancelAction">取消</u-button>
            </view>
          </view>
        </view>

        <!-- 完工提交 -->
        <view v-if="activeAction === 'FINISH'" class="action-form-card">
          <view class="card-title">
            <u-icon name="checkmark-circle" color="#00B42A" size="28"></u-icon>
            <text class="title-text">提交完工</text>
          </view>
          <view class="action-form-content">
            <text class="action-label">处理结果</text>
            <u-textarea
              v-model="forms.FINISH.content"
              placeholder="请填写处理结果和现场清理情况"
              :height="150"
              border="bottom"
            ></u-textarea>
            <text class="action-label attachment-label">完工照片（至少1张）</text>
            <u-upload
              :file-list="actionFiles('FINISH')"
              :max-count="5"
              :disabled="Boolean(submittingAction)"
              @afterRead="afterActionRead('FINISH', $event)"
              @delete="deleteActionFile('FINISH', $event)"
              name="file"
              accept="image"
              class="upload-component"
            >
              <template #add>
                <view class="add-btn">
                  <u-icon name="camera" color="#36CFC9" size="36"></u-icon>
                  <text class="add-text">上传/拍摄照片</text>
                </view>
              </template>
            </u-upload>
            <view v-for="(file, index) in actionFiles('FINISH')" :key="`finish-retry-${index}`">
              <view v-if="file.status === 'failed'" class="upload-retry-item">
                <text class="upload-retry-text">{{ file.name || '图片' }}上传失败</text>
                <u-button type="primary" size="mini" plain @click.stop="retryActionUpload('FINISH', index)">重试</u-button>
              </view>
            </view>
            <view class="action-form-actions">
              <u-button type="primary" :loading="submittingAction === 'FINISH'" :disabled="Boolean(submittingAction)" @click="submitAction('FINISH')">提交完工</u-button>
              <u-button plain :disabled="Boolean(submittingAction)" @click="cancelAction">取消</u-button>
            </view>
          </view>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script>
import config from '@/config'
import orderApi from '@/api/order/handle.js'
import { getToken } from '@/utils/auth'

const ACTION_METHODS = {
  ACCEPT: 'acceptOrder',
  ARRIVE: 'arriveOrder',
  ASSESS: 'assessmentOrder',
  PROGRESS: 'progressOrder',
  FINISH: 'finishOrder',
  REQUEST_DELAY: null
}

const ACTION_STAGES = {
  ARRIVE: 'ARRIVAL',
  PROGRESS: 'PROCESS',
  FINISH: 'FINISH'
}

export default {
  data() {
    return {
      orderId: '',
      order: {},
      attachments: [],
      timeline: [],
      loading: true,
      error: '',
      detailLoaded: false,
      activeAction: '',
      submittingAction: '',
      // One key is retained per open action so a failed submit can be retried safely.
      actionKeys: {},
      forms: {
        ARRIVE: { content: '', attachments: [] },
        ASSESSMENT: {
          content: '',
          requiresParts: false,
          partsDescription: '',
          assessedHours: '',
          assessedUrgency: '',
          assessedScope: '',
          requiresExtension: false,
          attachments: []
        },
        PROGRESS: { content: '', attachments: [] },
        FINISH: { content: '', attachments: [] }
      }
    }
  },

  computed: {
    actionButtons() {
      const allowed = Array.isArray(this.order.allowedActions) ? this.order.allowedActions : []
      return allowed
        .map(action => String(action).toUpperCase())
        .filter(action => Object.prototype.hasOwnProperty.call(ACTION_METHODS, action))
    }
  },

  onLoad(options) {
    this.orderId = options && options.id ? options.id : ''
    if (!this.orderId) {
      this.loading = false
      this.error = '缺少工单编号，无法加载详情'
      return
    }
    this.loadOrderDetail()
  },

  onShow() {
    // Returning from M17 must show the latest delay flag, version and actions.
    // The first onShow follows onLoad, so only refresh after the first load.
    if (this.detailLoaded && this.orderId && !this.loading) this.loadOrderDetail()
  },

  async onPullDownRefresh() {
    try {
      await this.loadOrderDetail()
    } finally {
      uni.stopPullDownRefresh()
    }
  },

  methods: {
    async loadOrderDetail() {
      if (!this.orderId) {
        this.loading = false
        this.error = '缺少工单编号，无法加载详情'
        return
      }
      this.loading = true
      this.error = ''
      try {
        const response = await orderApi.getOrderDetail(this.orderId)
        const detail = response && response.data !== undefined ? response.data : response
        if (!detail || detail.id === undefined || detail.id === null) {
          throw new Error('工单详情响应缺少 id')
        }
        this.order = detail
        this.attachments = Array.isArray(detail.attachments) ? detail.attachments : []
        this.timeline = Array.isArray(detail.timeline) ? detail.timeline : []
      } catch (requestError) {
        console.error('获取工单详情失败:', requestError)
        this.error = '工单详情加载失败，请重试'
      } finally {
        this.loading = false
        this.detailLoaded = true
      }
    },

    getStatusText(status) {
      const statusMap = {
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
      return statusMap[status] || status || '未知状态'
    },

    getStatusClass(status) {
      const classMap = {
        WAIT_ASSIGN: 'status-pending',
        WAIT_ACCEPT: 'status-pending',
        ACCEPTED: 'status-processing',
        PROCESSING: 'status-processing',
        WAIT_CONFIRM: 'status-processing',
        COMPLETED: 'status-completed',
        CLOSED: 'status-completed',
        CANCELLED: 'status-normal'
      }
      return classMap[status] || 'status-normal'
    },

    urgencyText(level) {
      return { 1: '一般', 2: '紧急', 3: '特急' }[level] || '—'
    },

    scopeText(scope) {
      return { 1: '个人事件', 2: '科室事件', 3: '多科室事件', 4: '全院事件' }[scope] || '—'
    },

    getUrgencyClass(level) {
      return { 1: 'urgency-normal', 2: 'urgency-urgent', 3: 'urgency-emergency' }[level] || 'urgency-normal'
    },

    actionText(action) {
      return {
        ACCEPT: '接单',
        ARRIVE: '到场确认',
        ASSESS: '提交评估',
        PROGRESS: '更新进度',
        FINISH: '提交完工',
        REQUEST_DELAY: '申请延期'
      }[String(action || '').toUpperCase()] || action || '处理'
    },

    delayActionText() {
      return this.isDelayPending() ? '查看延期申请' : '申请延期'
    },

    isDelayPending() {
      const value = this.order && this.order.delayPendingFlag
      return value === true || value === 1 || ['1', 'true', 'y', 'yes'].indexOf(String(value || '').toLowerCase()) !== -1
    },

    stageText(stage) {
      return {
        SUBMIT: '报修附件',
        ARRIVAL: '到场附件',
        PROCESS: '过程附件',
        DELAY: '延期佐证',
        FINISH: '完工附件',
        EVALUATION: '评价附件'
      }[stage] || stage || '附件'
    },

    formatTime(value) {
      if (!value) return '—'
      return String(value).replace('T', ' ').replace(/\.000Z$/, '')
    },

    buttonStyle(action) {
      const color = action === 'FINISH' ? '#00B42A' : action === 'ACCEPT' ? '#36CFC9' : '#FF9C07'
      return {
        backgroundColor: color,
        borderColor: color,
        marginBottom: '20rpx'
      }
    },

    isActionAllowed(action) {
      return this.actionButtons.indexOf(String(action).toUpperCase()) !== -1
    },

    openAction(action) {
      const normalized = String(action || '').toUpperCase()
      if (this.submittingAction || !this.isActionAllowed(normalized)) return
      if (normalized === 'REQUEST_DELAY') {
        this.openDelayRequest()
        return
      }
      if (normalized === 'ACCEPT') {
        this.confirmAccept()
        return
      }
      this.activeAction = normalized
    },

    openDelayRequest() {
      if (!this.isActionAllowed('REQUEST_DELAY')) return
      const pending = this.isDelayPending() ? '&pending=1' : ''
      uni.navigateTo({
        url: `/pages/order/delay/index?id=${encodeURIComponent(this.orderId)}${pending}`,
        fail: () => this.$u.toast('延期申请页面打开失败，请稍后重试')
      })
    },

    confirmAccept() {
      if (this.submittingAction) return
      uni.showModal({
        title: '确认接单',
        content: '确定要接取此工单吗？',
        cancelText: '取消',
        confirmText: '确定',
        success: (result) => {
          if (result.confirm) this.submitAction('ACCEPT')
        }
      })
    },

    actionFiles(stage) {
      const action = Object.keys(ACTION_STAGES).find(key => ACTION_STAGES[key] === stage)
      return action && this.forms[action] ? this.forms[action].attachments : []
    },

    actionForStage(stage) {
      return Object.keys(ACTION_STAGES).find(key => ACTION_STAGES[key] === stage) || ''
    },

    actionAttachmentIds(action) {
      const stage = ACTION_STAGES[action]
      if (!stage) return []
      return this.actionFiles(stage)
        .filter(file => file && file.status === 'success' && file.attachmentId !== undefined && file.attachmentId !== null && file.attachmentId !== '')
        .map(file => file.attachmentId)
    },

    hasUploading(stage) {
      return this.actionFiles(stage).some(file => file && file.status === 'uploading')
    },

    hasFailed(stage) {
      return this.actionFiles(stage).some(file => file && file.status === 'failed')
    },

    async afterActionRead(stage, event) {
      const files = Array.isArray(event && event.file) ? event.file : [event && event.file]
      const action = this.actionForStage(stage)
      if (!action || !files[0]) return
      for (const file of files) {
        const index = this.actionFiles(stage).length
        this.actionFiles(stage).push({ ...file, status: 'uploading', message: '上传中' })
        await this.uploadActionFile(stage, index, true)
      }
    },

    async uploadActionFile(stage, index, force = false) {
      const files = this.actionFiles(stage)
      const file = files[index]
      if (!file || (file.status === 'uploading' && !force)) return
      this.$set(files, index, { ...file, status: 'uploading', message: '上传中' })
      try {
        const filePath = file.url || file.path
        if (!filePath) throw new Error('附件路径不能为空')
        const result = await orderApi.uploadAttachment(filePath, 'file', stage)
        const attachment = result && result.data !== undefined ? result.data : result
        const attachmentId = attachment && (attachment.id !== undefined ? attachment.id : attachment.attachmentId)
        if (attachmentId === undefined || attachmentId === null || attachmentId === '') {
          throw new Error('上传结果缺少附件标识')
        }
        this.$set(files, index, { ...file, status: 'success', message: '', attachmentId })
      } catch (uploadError) {
        console.error('上传处理附件失败:', uploadError)
        this.$set(files, index, { ...file, status: 'failed', message: '上传失败' })
      }
    },

    async retryActionUpload(stage, index) {
      if (this.hasUploading(stage)) return
      await this.uploadActionFile(stage, index)
    },

    async deleteActionFile(stage, event) {
      const index = event && event.index
      const files = this.actionFiles(stage)
      const file = index === undefined ? null : files[index]
      if (!file || file.deleting || file.status === 'uploading') return
      const attachmentId = file.attachmentId
      if (attachmentId === undefined || attachmentId === null || attachmentId === '') {
        files.splice(index, 1)
        return
      }
      this.$set(files, index, { ...file, deleting: true, deletable: false, message: '删除中' })
      try {
        await orderApi.deleteAttachment(attachmentId)
        files.splice(index, 1)
      } catch (deleteError) {
        console.error('删除处理附件失败:', deleteError)
        const current = files[index]
        if (current) this.$set(files, index, { ...current, deleting: false, deletable: true, message: '删除失败，请重试' })
        this.$u.toast('附件删除失败，文件仍保留')
      }
    },

    validateAction(action) {
      const form = action === 'ASSESS' ? this.forms.ASSESSMENT : this.forms[action]
      const content = String(form && form.content || '').trim()
      if (action === 'ARRIVE' && !content) return '请填写简要到场说明'
      if (action === 'ARRIVE' && this.hasUploading('ARRIVAL')) return '到场附件正在上传，请稍候'
      if (action === 'ARRIVE' && this.hasFailed('ARRIVAL')) return '存在上传失败的到场附件，请重试或删除'
      if (action === 'ARRIVE' && this.actionAttachmentIds('ARRIVE').length === 0) return '请至少上传1张到场照片'
      if (action === 'ASSESS' && !content) return '请填写故障评估说明'
      if (action === 'ASSESS' && form.requiresParts && !String(form.partsDescription || '').trim()) return '请填写零部件说明'
      if (action === 'ASSESS' && (!form.assessedHours || Number(form.assessedHours) <= 0)) return '请填写大于0的预计工时'
      if (action === 'ASSESS' && ![1, 2, 3].includes(Number(form.assessedUrgency))) return '请选择评估紧急程度'
      if (action === 'ASSESS' && ![1, 2, 3, 4].includes(Number(form.assessedScope))) return '请选择评估影响范围'
      if (action === 'PROGRESS' && !content) return '请填写处理进度'
      if (action === 'PROGRESS' && this.hasUploading('PROCESS')) return '过程附件正在上传，请稍候'
      if (action === 'PROGRESS' && this.hasFailed('PROCESS')) return '存在上传失败的过程附件，请重试或删除'
      if (action === 'FINISH' && !content) return '请填写处理结果'
      if (action === 'FINISH' && this.hasUploading('FINISH')) return '完工附件正在上传，请稍候'
      if (action === 'FINISH' && this.hasFailed('FINISH')) return '存在上传失败的完工附件，请重试或删除'
      if (action === 'FINISH' && this.actionAttachmentIds('FINISH').length === 0) return '请至少上传1张完工照片'
      return ''
    },

    actionPayload(action) {
      const form = action === 'ASSESS' ? this.forms.ASSESSMENT : this.forms[action]
      const payload = {
        content: String(form && form.content || '').trim(),
        attachmentIds: this.actionAttachmentIds(action),
        version: this.order.version
      }
      if (action === 'ASSESS') {
        payload.requiresParts = Boolean(form.requiresParts)
        payload.partsDescription = String(form.partsDescription || '').trim()
        payload.assessedHours = form.assessedHours === '' ? null : form.assessedHours
        payload.assessedUrgency = form.assessedUrgency === '' ? null : form.assessedUrgency
        payload.assessedScope = form.assessedScope === '' ? null : form.assessedScope
        payload.requiresExtension = Boolean(form.requiresExtension)
      }
      return payload
    },

    getActionKey(action) {
      if (!this.actionKeys[action]) this.$set(this.actionKeys, action, orderApi.createIdempotencyKey(action))
      return this.actionKeys[action]
    },

    resetAction(action) {
      this.$set(this.actionKeys, action, '')
      if (action === 'ARRIVE') this.$set(this.forms, 'ARRIVE', { content: '', attachments: [] })
      if (action === 'ASSESS') this.$set(this.forms, 'ASSESSMENT', {
        content: '',
        requiresParts: false,
        partsDescription: '',
        assessedHours: '',
        assessedUrgency: '',
        assessedScope: '',
        requiresExtension: false,
        attachments: []
      })
      if (action === 'PROGRESS') this.$set(this.forms, 'PROGRESS', { content: '', attachments: [] })
      if (action === 'FINISH') this.$set(this.forms, 'FINISH', { content: '', attachments: [] })
    },

    cancelAction() {
      if (this.submittingAction) return
      if (this.activeAction) this.$set(this.actionKeys, this.activeAction, '')
      this.activeAction = ''
    },

    isConflictError(error) {
      const code = error && (error.code || error.statusCode || error.status)
      return Number(error) === 409 || Number(code) === 409 || String(error || '').indexOf('409') !== -1
    },

    async submitAction(action) {
      const normalized = String(action || '').toUpperCase()
      if (this.submittingAction) return
      if (normalized === 'REQUEST_DELAY') {
        this.openDelayRequest()
        return
      }
      if (!this.isActionAllowed(normalized)) {
        this.$u.toast('工单状态已更新，请刷新后重试')
        await this.loadOrderDetail()
        return
      }
      const validationMessage = this.validateAction(normalized)
      if (validationMessage) {
        this.$u.toast(validationMessage)
        return
      }

      this.submittingAction = normalized
      // Keep this key through a failed request so an explicit retry replays one command.
      const idempotencyKey = this.getActionKey(normalized)
      try {
        const method = ACTION_METHODS[normalized]
        await orderApi[method](this.orderId, {
          ...this.actionPayload(normalized),
          idempotencyKey
        })
        this.resetAction(normalized)
        this.activeAction = ''
        this.$u.toast(`${this.actionText(normalized)}成功`)
        uni.$emit('refreshOrderList')
        await this.loadOrderDetail()
      } catch (submitError) {
        console.error(`${this.actionText(normalized)}失败:`, submitError)
        if (this.isConflictError(submitError)) {
          // A stale version must be replaced before the user retries the preserved form.
          await this.loadOrderDetail()
          this.$u.toast('工单已被其他操作更新，请确认当前状态后重试')
        } else {
          // Keep the form and uploaded attachment ids intact for the same-key retry.
          this.$u.toast(`${this.actionText(normalized)}失败，已保留填写内容，请重试`)
        }
      } finally {
        this.submittingAction = ''
      }
    },

    openAttachment(item) {
      const downloadUrl = item && (item.downloadUrl || item.url)
      if (!downloadUrl) {
        this.$u.toast('附件暂不可下载')
        return
      }
      const baseUrl = String(config.baseUrl || '').replace(/\/$/, '')
      const url = /^https?:\/\//i.test(downloadUrl) ? downloadUrl : `${baseUrl}${downloadUrl}`
      if (!/^https?:\/\//i.test(url)) {
        this.$u.toast('未配置服务地址，无法下载附件')
        return
      }
      const token = getToken()
      uni.showLoading({ title: '正在下载', mask: true })
      uni.downloadFile({
        url,
        header: token ? { Authorization: `Bearer ${token}` } : {},
        success: (response) => {
          if (response.statusCode !== 200 || !response.tempFilePath) {
            this.$u.toast('附件下载失败，请稍后重试')
            return
          }
          if (/\.(png|jpe?g|gif|bmp|webp)$/i.test(item.fileName || item.name || '')) {
            uni.previewImage({ current: response.tempFilePath, urls: [response.tempFilePath] })
            return
          }
          uni.openDocument({
            filePath: response.tempFilePath,
            showMenu: true,
            fail: () => this.$u.toast('附件已下载，请使用本机应用打开')
          })
        },
        fail: () => this.$u.toast('附件下载失败，请检查网络后重试'),
        complete: () => uni.hideLoading()
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.page-container {
  background-color: #F5F7FA;
  min-height: 100vh;
}

/* 状态标签 */
.status-tag {
  height: 60rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  font-size: 28rpx;
  font-weight: 500;
}

.status-pending, .status-normal {
  background-color: #36CFC9;
}

.status-processing, .status-urgent {
  background-color: #FF9C07;
}

.status-completed {
  background-color: #00B42A;
}

.status-emergency {
  background-color: #F53F3F;
}

.status-text {
  padding: 0 20rpx;
}

/* 内容滚动区域 */
.content-scroll {
  height: calc(100vh - 140rpx);
}

/* 骨架屏样式 */
.detail-skeleton {
  padding: 20rpx;
}

.skeleton-loading {
  width: 100%;
}

.loading-indicator {
  margin-top: 200rpx;
}

.empty-tip {
  margin-top: 200rpx;
}

.error-state {
  margin-top: 180rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.error-text {
  color: #606266;
  font-size: 26rpx;
  margin: 20rpx 0;
}

.detail-container {
  padding: 20rpx;
}

/* 信息卡片 */
.info-card, .action-form-card {
  background-color: #ffffff;
  border-radius: 16rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
  margin-bottom: 20rpx;
  overflow: hidden;
}

.card-title {
  display: flex;
  align-items: center;
  padding: 20rpx 24rpx;
  border-bottom: 1rpx solid #F2F3F5;
}

.title-text {
  font-size: 30rpx;
  color: #303133;
  margin-left: 12rpx;
  font-weight: 500;
}

.info-content {
  padding: 20rpx 24rpx;
}

.info-row {
  display: flex;
  margin-bottom: 20rpx;
  align-items: flex-start;
}

.info-row:last-child {
  margin-bottom: 0;
}

.full-width {
  flex-direction: column;
}

.info-label {
  font-size: 26rpx;
  color: #606266;
  width: 160rpx;
  flex-shrink: 0;
}

.full-width .info-label {
  margin-bottom: 8rpx;
}

.info-value {
  font-size: 26rpx;
  color: #303133;
  word-break: break-all;
}

.full-content {
  width: 100%;
}

.urgency-text {
  font-weight: 500;
}

.urgency-normal {
  color: #36CFC9;
}

.urgency-urgent {
  color: #FF9C07;
}

.urgency-emergency {
  color: #F53F3F;
}

.solution-text {
  color: #00B42A;
  background-color: #F7FFF9;
  padding: 15rpx;
  border-radius: 8rpx;
  border: 1rpx solid #E1F3E8;
}

/* 时间线样式 */
.timeline {
  padding: 10rpx 24rpx 20rpx;
  position: relative;
}

.timeline::before {
  content: '';
  position: absolute;
  top: 0;
  left: 24rpx;
  width: 2rpx;
  height: 100%;
  background-color: #E5E6EB;
}

.timeline-item {
  display: flex;
  position: relative;
  padding-bottom: 30rpx;
}

.timeline-item:last-child {
  padding-bottom: 0;
}

.timeline-node {
  width: 20rpx;
  height: 20rpx;
  border-radius: 50%;
  background-color: #36CFC9;
  margin-right: 20rpx;
  margin-top: 5rpx;
  position: relative;
  z-index: 1;
}

.timeline-node.last {
  background-color: #00B42A;
}

.timeline-content {
  flex: 1;
}

.timeline-time {
  font-size: 24rpx;
  color: #909399;
  margin-bottom: 5rpx;
}

.timeline-text {
  font-size: 26rpx;
  color: #303133;
  margin-bottom: 5rpx;
  background-color: #F7F8FA;
  padding: 15rpx;
  border-radius: 8rpx;
}

.timeline-operator {
  font-size: 24rpx;
  color: #909399;
}

/* 附件列表 */
.attachment-list {
  padding: 10rpx 24rpx 20rpx;
}

.attachment-item {
  display: flex;
  align-items: center;
  padding: 15rpx 0;
  border-bottom: 1rpx solid #F2F3F5;
}

.attachment-item:last-child {
  border-bottom: none;
}

.attachment-copy {
  flex: 1;
  min-width: 0;
  margin: 0 15rpx;
}

.attachment-name {
  display: block;
  font-size: 26rpx;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.attachment-meta {
  display: block;
  font-size: 22rpx;
  color: #909399;
  margin-top: 5rpx;
}

/* 按钮区域 */
.button-container {
  padding: 20rpx;
  background-color: #ffffff;
  margin: 20rpx;
  border-radius: 16rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
}

/* 操作表单沿用详情卡片的圆角、阴影和色彩。 */
.action-form-content {
  padding: 20rpx 24rpx;
}

.action-label {
  display: block;
  color: #606266;
  font-size: 26rpx;
  margin-bottom: 12rpx;
}

.attachment-label {
  margin-top: 24rpx;
}

.switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 0;
  border-top: 1rpx solid #F2F3F5;
  margin-top: 20rpx;
}

.conditional-field {
  padding-top: 18rpx;
}

.assessment-row {
  padding: 18rpx 0;
}

.assessment-field {
  width: 55%;
}

.radio-group-horizontal {
  display: flex;
  flex-wrap: wrap;
  padding: 8rpx 0 18rpx;
}

.radio-item {
  margin-right: 26rpx;
  margin-bottom: 10rpx;
}

.radio-label {
  font-size: 25rpx;
  color: #303133;
}

.action-form-actions {
  display: flex;
  gap: 20rpx;
  margin-top: 24rpx;
}

.action-form-actions .u-button {
  flex: 1;
}

.upload-component {
  padding: 10rpx 0;
}

.add-btn {
  width: 160rpx;
  height: 160rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 1rpx dashed #C9CDD4;
  border-radius: 8rpx;
}

.add-text {
  color: #86909C;
  font-size: 22rpx;
  margin-top: 8rpx;
}

.upload-retry-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10rpx;
  margin-top: 10rpx;
  background-color: #FFF2F0;
  border-radius: 8rpx;
}

.upload-retry-text {
  flex: 1;
  margin-right: 16rpx;
  color: #F53F3F;
  font-size: 24rpx;
}
</style>

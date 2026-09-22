<template>
  <view class="page-container">
    <scroll-view class="page-scroll" :class="{ 'page-scroll-with-actions': primaryActions.length }" scroll-y>
      <view v-if="loading" class="state-card">
        <u-loading-icon mode="circle" color="#36CFC9" size="36"></u-loading-icon>
        <text class="state-text">工单详情加载中</text>
      </view>

      <view v-else-if="error" class="state-card">
        <u-icon name="info-circle" color="#F53F3F" size="44"></u-icon>
        <text class="state-text error-text">{{ error }}</text>
        <u-button type="primary" size="mini" plain @click="loadDetail">重试</u-button>
      </view>

      <view v-else class="detail-content">
        <view class="detail-card summary-card">
          <view class="summary-header">
            <view class="summary-title-wrap">
              <text class="summary-title">{{ order.title || order.categoryName || '工单详情' }}</text>
              <text class="summary-no">{{ order.orderNo || `工单 #${order.id}` }}</text>
            </view>
            <text class="status-tag" :class="statusClass(order.status)">{{ statusText(order.status) }}</text>
          </view>
          <view class="summary-grid">
            <view class="summary-item">
              <text class="summary-label">故障分类</text>
              <text class="summary-value">{{ order.categoryName || '未分类' }}</text>
            </view>
            <view class="summary-item">
              <text class="summary-label">紧急程度</text>
              <text class="summary-value">{{ urgencyText(order.urgencyLevel) }}</text>
            </view>
            <view class="summary-item">
              <text class="summary-label">影响范围</text>
              <text class="summary-value">{{ scopeText(order.impactScope) }}</text>
            </view>
            <view class="summary-item">
              <text class="summary-label">提交时间</text>
              <text class="summary-value">{{ formatTime(order.submittedAt || order.createTime) }}</text>
            </view>
          </view>
        </view>

        <view class="detail-card">
          <view class="card-title-row">
            <text class="card-title">核心信息</text>
            <view class="card-line"></view>
          </view>
          <view class="field-row">
            <text class="field-label">报修人</text>
            <text class="field-value">{{ order.applicantName || '未知' }}</text>
          </view>
          <view class="field-row">
            <text class="field-label">所属科室</text>
            <text class="field-value">{{ order.applicantDeptName || '未填写' }}</text>
          </view>
          <view class="field-row">
            <text class="field-label">联系电话</text>
            <text class="field-value">{{ order.applicantPhone || '未填写' }}</text>
          </view>
          <view class="field-row">
            <text class="field-label">故障位置</text>
            <text class="field-value">{{ order.location || '未填写' }}</text>
          </view>
          <view class="field-block">
            <text class="field-label">故障描述</text>
            <text class="field-content">{{ order.description || '暂无描述' }}</text>
          </view>
          <view v-if="order.possibleCause" class="field-block">
            <text class="field-label">补充说明</text>
            <text class="field-content">{{ order.possibleCause }}</text>
          </view>
        </view>

        <view class="detail-card">
          <view class="card-title-row">
            <text class="card-title">附件</text>
            <text class="card-count">{{ attachments.length }} 个</text>
          </view>
          <view v-if="attachments.length" class="attachment-list">
            <view v-for="item in attachments" :key="item.id" class="attachment-row attachment-row-clickable" @click="openAttachment(item)">
              <u-icon name="attach" color="#36CFC9" size="34"></u-icon>
              <view class="attachment-info">
                <text class="attachment-name">{{ item.fileName || '未命名附件' }}</text>
                <text class="attachment-meta">{{ stageText(item.bizStage) }} · {{ formatFileSize(item.fileSize) }}</text>
              </view>
              <u-icon name="arrow-right" color="#C9CDD4" size="26"></u-icon>
            </view>
          </view>
          <text v-else class="empty-text">暂无附件</text>
        </view>

        <view class="detail-card">
          <view class="card-title-row">
            <text class="card-title">处理时间线</text>
            <text class="card-count">{{ timeline.length }} 条</text>
          </view>
          <view v-if="timeline.length" class="timeline-list">
            <view v-for="(item, index) in timeline" :key="item.id || index" class="timeline-item">
              <view class="timeline-node" :class="{ 'timeline-node-last': index === timeline.length - 1 }"></view>
              <view class="timeline-body">
                <view class="timeline-header">
                  <text class="timeline-action">{{ actionText(item.actionType) }}</text>
                  <text class="timeline-time">{{ formatTime(item.actionTime) }}</text>
                </view>
                <text class="timeline-operator">{{ item.operatorName || '系统' }}</text>
                <text v-if="item.actionContent" class="timeline-content">{{ item.actionContent }}</text>
                <text v-else-if="item.fromStatus || item.toStatus" class="timeline-content">
                  {{ statusText(item.fromStatus) }} → {{ statusText(item.toStatus) }}
                </text>
              </view>
            </view>
          </view>
          <text v-else class="empty-text">暂无处理记录</text>
        </view>

        <view class="detail-card">
          <view class="card-title-row">
            <text class="card-title">当前允许动作</text>
          </view>
          <view v-if="allowedActions.length" class="action-list">
            <text v-for="action in allowedActions" :key="action" class="action-tag">
              {{ actionText(action) }}
            </text>
          </view>
          <text v-else class="empty-text">当前无可执行动作</text>
        </view>
      </view>
    </scroll-view>

    <view v-if="primaryActions.length && !loading && !error" class="primary-action-bar">
      <button
        v-for="action in primaryActions"
        :key="action"
        class="primary-action-button"
        :class="actionButtonClass(action)"
        :disabled="Boolean(submittingAction)"
        @click="handlePrimaryAction(action)"
      >
        {{ submittingAction === action ? '提交中...' : actionText(action) }}
      </button>
    </view>
  </view>
</template>

<script>
import config from '@/config'
import { cancelOrder, createOrderIdempotencyKey, getOrderDetail } from '@/api/order/core'
import { getToken } from '@/utils/auth'

export default {
  data() {
    return {
      orderId: '',
      order: {},
      attachments: [],
      timeline: [],
      allowedActions: [],
      loading: true,
      error: '',
      submittingAction: '',
      cancelIdempotencyKey: ''
    }
  },

  computed: {
    primaryActions() {
      const visible = ['CANCEL', 'RETURN', 'CONFIRM', 'EVALUATE']
      return visible.filter(action => this.allowedActions.includes(action))
    }
  },

  onLoad(options) {
    this.orderId = options && options.id ? options.id : ''
  },

  onShow() {
    this.loadDetail()
  },

  methods: {
    async loadDetail() {
      if (!this.orderId) {
        this.loading = false
        this.error = '缺少工单编号，无法加载详情'
        return
      }
      this.loading = true
      this.error = ''
      try {
        const response = await getOrderDetail(this.orderId)
        // AjaxResult.data 才是 WorkOrder；附件、时间线和允许动作均以服务端当前返回为准，不补造按钮或数据。
        const order = response && response.data !== undefined ? response.data : response
        if (!order || order.id === undefined || order.id === null) {
          throw new Error('工单详情响应缺少 id')
        }
        const previousVersion = this.order && this.order.version
        this.order = order
        this.attachments = Array.isArray(order.attachments) ? order.attachments : []
        this.timeline = Array.isArray(order.timeline) ? order.timeline : []
        this.allowedActions = (Array.isArray(order.allowedActions) ? order.allowedActions : [])
          .map(action => String(action).toUpperCase())
        if (previousVersion !== undefined && previousVersion !== order.version) {
          this.cancelIdempotencyKey = ''
        }
      } catch (requestError) {
        this.error = '工单详情加载失败，请重试'
      } finally {
        this.loading = false
      }
    },

    actionButtonClass(action) {
      if (action === 'CANCEL' || action === 'RETURN') return 'danger-action-button'
      if (action === 'EVALUATE') return 'evaluate-action-button'
      return 'confirm-action-button'
    },

    handlePrimaryAction(action) {
      if (this.submittingAction) return
      if (action === 'CANCEL') {
        this.confirmCancel()
        return
      }
      if (action === 'CONFIRM' || action === 'RETURN') {
        uni.navigateTo({
          url: `/pages/order/order-detail/order-confirm?id=${encodeURIComponent(this.orderId)}`,
          fail: () => this.$u.toast('完工确认页面打开失败，请重试')
        })
        return
      }
      if (action === 'EVALUATE') {
        uni.navigateTo({
          url: `/pages/order/order-detail/order-evaluate?id=${encodeURIComponent(this.orderId)}`,
          fail: () => this.$u.toast('评价页面打开失败，请重试')
        })
      }
    },

    isConflictError(error) {
      const code = error && (error.code || error.statusCode || error.status)
      return Number(error) === 409 || Number(code) === 409 || String(error || '').indexOf('409') !== -1
    },

    confirmCancel() {
      if (!this.allowedActions.includes('CANCEL') || this.submittingAction) return
      uni.showModal({
        title: '取消工单',
        content: '取消后工单将终止且无法恢复，确定继续吗？',
        confirmText: '确认取消',
        confirmColor: '#F53F3F',
        success: result => {
          if (result.confirm) this.submitCancel()
        }
      })
    },

    async submitCancel() {
      if (this.submittingAction) return
      if (!this.allowedActions.includes('CANCEL')) {
        this.$u.toast('工单状态已变化，正在刷新')
        await this.loadDetail()
        return
      }

      this.submittingAction = 'CANCEL'
      if (!this.cancelIdempotencyKey) {
        this.cancelIdempotencyKey = createOrderIdempotencyKey('cancel')
      }
      try {
        await cancelOrder(this.orderId, {
          version: this.order.version,
          idempotencyKey: this.cancelIdempotencyKey
        })
        this.cancelIdempotencyKey = ''
        uni.$emit('refreshOrderList')
        this.$u.toast('工单已取消')
        await this.loadDetail()
      } catch (error) {
        if (this.isConflictError(error)) {
          this.cancelIdempotencyKey = ''
          await this.loadDetail()
          this.$u.toast('工单状态已变化，已刷新最新信息')
        } else {
          // 网络失败时保留 key，用户重试仍对应同一次取消操作。
          this.$u.toast('取消失败，请检查网络后重试')
        }
      } finally {
        this.submittingAction = ''
      }
    },

    // 服务端仅返回受鉴权保护的相对下载地址；下载时附带当前登录令牌，避免暴露存储对象地址。
    openAttachment(item) {
      if (!item || !item.downloadUrl) {
        this.$u.toast('附件暂不可下载')
        return
      }

      const baseUrl = String(config.baseUrl || '').replace(/\/$/, '')
      const downloadUrl = String(item.downloadUrl)
      const url = /^https?:\/\//i.test(downloadUrl) ? downloadUrl : `${baseUrl}${downloadUrl}`
      if (!url || !/^https?:\/\//i.test(url)) {
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
          const filePath = response.tempFilePath
          const isImage = /\.(png|jpe?g|gif|bmp|webp)$/i.test(item.fileName || '')
          if (isImage) {
            uni.previewImage({ current: filePath, urls: [filePath] })
            return
          }
          uni.openDocument({
            filePath,
            showMenu: true,
            fail: () => this.$u.toast('附件已下载，请使用本机应用打开')
          })
        },
        fail: () => this.$u.toast('附件下载失败，请检查网络后重试'),
        complete: () => uni.hideLoading()
      })
    },

    statusText(status) {
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
      return labels[status] || status || '未知状态'
    },

    statusClass(status) {
      const classes = {
        WAIT_ASSIGN: 'status-wait',
        WAIT_ACCEPT: 'status-wait',
        ACCEPTED: 'status-processing',
        PROCESSING: 'status-processing',
        WAIT_CONFIRM: 'status-warning',
        COMPLETED: 'status-success',
        CLOSED: 'status-muted',
        CANCELLED: 'status-danger'
      }
      return classes[status] || 'status-muted'
    },

    urgencyText(level) {
      return { 1: '一般', 2: '紧急', 3: '特急' }[level] || '未填写'
    },

    scopeText(scope) {
      return { 1: '个人事件', 2: '科室事件', 3: '多科室事件', 4: '全院事件' }[scope] || '未填写'
    },

    actionText(action) {
      const labels = {
        SUBMIT: '提交工单',
        CANCEL: '取消工单',
        ASSIGN: '派单',
        REASSIGN: '改派',
        ACCEPT: '接单',
        ARRIVE: '到场确认',
        ASSESS: '故障评估',
        PROGRESS: '更新进度',
        FINISH: '提交完工',
        CONFIRM: '确认完工',
        RETURN: '退回处理',
        EVALUATE: '提交评价'
      }
      return labels[action] || action || '系统记录'
    },

    stageText(stage) {
      return {
        SUBMIT: '报修提交',
        ARRIVAL: '到场确认',
        PROCESS: '处理过程',
        FINISH: '完工提交',
        EVALUATION: '服务评价'
      }[stage] || stage || '工单附件'
    },

    formatFileSize(size) {
      const value = Number(size)
      if (!Number.isFinite(value) || value <= 0) return '大小未知'
      if (value < 1024) return `${value} B`
      if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`
      return `${(value / 1024 / 1024).toFixed(1)} MB`
    },

    formatTime(value) {
      if (!value) return '时间未知'
      return String(value).replace('T', ' ').slice(0, 16)
    }
  }
}
</script>

<style lang="scss" scoped>
page {
  background: #F5F7FA;
}

.page-container {
  min-height: 100vh;
  padding: 24rpx;
  box-sizing: border-box;
  background: #F5F7FA;
}

.page-scroll {
  height: calc(100vh - 48rpx);
}

.page-scroll-with-actions {
  height: calc(100vh - 160rpx);
}

.state-card,
.detail-card {
  border-radius: 20rpx;
  background: #FFFFFF;
  box-shadow: 0 8rpx 24rpx rgba(29, 33, 41, 0.05);
}

.state-card {
  display: flex;
  min-height: 320rpx;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  padding: 40rpx;
}

.state-text {
  margin: 20rpx 0;
  color: #4E5969;
  font-size: 28rpx;
}

.error-text {
  color: #F53F3F;
}

.detail-content {
  padding-bottom: 30rpx;
}

.detail-card {
  margin-bottom: 20rpx;
  padding: 26rpx;
}

.summary-card {
  background: linear-gradient(135deg, #FFFFFF 0%, #F3FFFE 100%);
}

.summary-header,
.card-title-row,
.field-row,
.attachment-row,
.timeline-header {
  display: flex;
  align-items: center;
}

.summary-header,
.card-title-row,
.field-row,
.timeline-header {
  justify-content: space-between;
}

.summary-title-wrap {
  min-width: 0;
  margin-right: 20rpx;
}

.summary-title {
  display: block;
  overflow: hidden;
  color: #1D2129;
  font-size: 34rpx;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.summary-no {
  display: block;
  margin-top: 10rpx;
  color: #86909C;
  font-size: 23rpx;
}

.status-tag {
  flex-shrink: 0;
  padding: 8rpx 16rpx;
  border-radius: 20rpx;
  font-size: 22rpx;
}

.status-wait {
  color: #1677FF;
  background: #E8F3FF;
}

.status-processing {
  color: #D46B08;
  background: #FFF7E6;
}

.status-warning {
  color: #D4380D;
  background: #FFF2E8;
}

.status-success {
  color: #08979C;
  background: #E6FFFB;
}

.status-muted {
  color: #86909C;
  background: #F2F3F5;
}

.status-danger {
  color: #F53F3F;
  background: #FFF1F0;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24rpx 16rpx;
  margin-top: 30rpx;
}

.summary-label,
.field-label {
  color: #86909C;
  font-size: 23rpx;
}

.summary-value {
  display: block;
  margin-top: 8rpx;
  color: #1D2129;
  font-size: 26rpx;
}

.card-title-row {
  position: relative;
  margin-bottom: 22rpx;
}

.card-title {
  padding-left: 20rpx;
  color: #1D2129;
  font-size: 30rpx;
  font-weight: 500;
}

.card-line {
  position: absolute;
  top: 50%;
  left: 0;
  width: 8rpx;
  height: 28rpx;
  border-radius: 4rpx;
  background: #36CFC9;
  transform: translateY(-50%);
}

.card-count {
  color: #86909C;
  font-size: 23rpx;
}

.field-row {
  min-height: 64rpx;
  border-bottom: 1rpx solid #F2F3F5;
}

.field-value {
  max-width: 68%;
  color: #1D2129;
  font-size: 26rpx;
  text-align: right;
}

.field-block {
  padding-top: 20rpx;
}

.field-content {
  display: block;
  margin-top: 12rpx;
  color: #4E5969;
  font-size: 27rpx;
  line-height: 1.6;
}

.attachment-list {
  border-top: 1rpx solid #F2F3F5;
}

.attachment-row {
  padding: 18rpx 0;
  border-bottom: 1rpx solid #F2F3F5;
}

.attachment-row-clickable {
  cursor: pointer;
}

.attachment-info {
  flex: 1;
  min-width: 0;
  margin-left: 16rpx;
}

.attachment-name,
.attachment-meta {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.attachment-name {
  color: #1D2129;
  font-size: 26rpx;
}

.attachment-meta {
  margin-top: 8rpx;
  color: #86909C;
  font-size: 22rpx;
}

.empty-text {
  display: block;
  padding: 12rpx 0 4rpx;
  color: #86909C;
  font-size: 24rpx;
  text-align: center;
}

.timeline-item {
  display: flex;
  position: relative;
  padding-bottom: 26rpx;
}

.timeline-item:last-child {
  padding-bottom: 0;
}

.timeline-node {
  position: relative;
  z-index: 1;
  width: 16rpx;
  height: 16rpx;
  margin: 6rpx 22rpx 0 4rpx;
  border: 4rpx solid #36CFC9;
  border-radius: 50%;
  background: #FFFFFF;
  box-sizing: border-box;
}

.timeline-node:not(.timeline-node-last)::after {
  position: absolute;
  top: 12rpx;
  left: 2rpx;
  width: 4rpx;
  height: 90rpx;
  background: #D9F7F5;
  content: '';
}

.timeline-body {
  flex: 1;
  min-width: 0;
}

.timeline-action {
  color: #1D2129;
  font-size: 26rpx;
  font-weight: 500;
}

.timeline-time,
.timeline-operator {
  color: #86909C;
  font-size: 22rpx;
}

.timeline-operator,
.timeline-content {
  display: block;
  margin-top: 8rpx;
}

.timeline-content {
  color: #4E5969;
  font-size: 24rpx;
  line-height: 1.5;
}

.action-list {
  display: flex;
  flex-wrap: wrap;
  gap: 14rpx;
}

.action-tag {
  padding: 10rpx 16rpx;
  border: 1rpx solid #B5F5EC;
  border-radius: 18rpx;
  color: #08979C;
  background: #E6FFFB;
  font-size: 23rpx;
}

.primary-action-bar {
  display: flex;
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 10;
  gap: 16rpx;
  padding: 18rpx 24rpx;
  border-top: 1rpx solid #F2F3F5;
  background: #FFFFFF;
  box-sizing: border-box;
}

.primary-action-button {
  flex: 1;
  height: 82rpx;
  margin: 0;
  border-radius: 12rpx;
  font-size: 28rpx;
  line-height: 82rpx;
}

.danger-action-button {
  border: 1rpx solid #F53F3F;
  color: #F53F3F;
  background: #FFFFFF;
}

.confirm-action-button {
  border: 1rpx solid #36CFC9;
  color: #FFFFFF;
  background: #36CFC9;
}

.evaluate-action-button {
  border: 1rpx solid #FF9C07;
  color: #FFFFFF;
  background: #FF9C07;
}

.primary-action-button[disabled] {
  opacity: 0.55;
}
</style>

<template>
  <view class="page-container">
    <scroll-view class="page-scroll" scroll-y>
      <view v-if="loading" class="state-card">
        <u-loading-icon mode="circle" color="#36CFC9" size="36"></u-loading-icon>
        <text class="state-text">完工信息加载中</text>
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
              <text class="summary-title">{{ order.title || order.categoryName || '完工确认' }}</text>
              <text class="summary-no">{{ order.orderNo || `工单 #${order.id}` }}</text>
            </view>
            <text class="status-tag">{{ statusText(order.status) }}</text>
          </view>
          <view class="summary-grid">
            <view class="summary-item">
              <text class="summary-label">处理工程师</text>
              <text class="summary-value">{{ order.currentAssigneeName || '未填写' }}</text>
            </view>
            <view class="summary-item">
              <text class="summary-label">完工时间</text>
              <text class="summary-value">{{ formatTime(order.finishedAt || (finishRecord && finishRecord.actionTime)) }}</text>
            </view>
          </view>
        </view>

        <view class="detail-card">
          <view class="card-title-row">
            <text class="card-title">完工结果</text>
            <view class="card-line"></view>
          </view>
          <text v-if="finishRecord && finishRecord.actionContent" class="result-content">
            {{ finishRecord.actionContent }}
          </text>
          <text v-else class="empty-text">暂无完工说明</text>
          <view v-if="finishRecord" class="result-meta">
            <text>{{ finishRecord.operatorName || order.currentAssigneeName || '维修人员' }}</text>
            <text>{{ formatTime(finishRecord.actionTime || order.finishedAt) }}</text>
          </view>
        </view>

        <view class="detail-card">
          <view class="card-title-row">
            <text class="card-title">完工附件</text>
            <text class="card-count">{{ finishAttachments.length }} 个</text>
          </view>
          <view v-if="finishAttachments.length" class="attachment-list">
            <view
              v-for="item in finishAttachments"
              :key="item.id"
              class="attachment-row"
              @click="openAttachment(item)"
            >
              <u-icon name="attach" color="#36CFC9" size="34"></u-icon>
              <view class="attachment-info">
                <text class="attachment-name">{{ item.fileName || '未命名附件' }}</text>
                <text class="attachment-meta">{{ formatFileSize(item.fileSize) }}</text>
              </view>
              <u-icon name="arrow-right" color="#C9CDD4" size="26"></u-icon>
            </view>
          </view>
          <text v-else class="empty-text">暂无完工附件</text>
        </view>

        <view v-if="canReturn" class="detail-card">
          <view class="card-title-row">
            <text class="card-title">退回处理</text>
            <view class="card-line danger-line"></view>
          </view>
          <textarea
            v-model="returnReason"
            class="reason-input"
            maxlength="500"
            placeholder="请填写退回原因（必填）"
            :disabled="Boolean(submittingAction)"
          ></textarea>
          <text class="word-count">{{ returnReason.length }}/500</text>
        </view>

        <view v-if="!canConfirm && !canReturn" class="detail-card">
          <text class="empty-text">当前工单状态已变化，请返回详情查看</text>
        </view>
      </view>
    </scroll-view>

    <view v-if="!loading && !error && (canConfirm || canReturn)" class="action-bar">
      <button
        v-if="canReturn"
        class="action-button return-button"
        :disabled="Boolean(submittingAction)"
        @click="submitAction('RETURN')"
      >{{ submittingAction === 'RETURN' ? '提交中...' : '退回处理' }}</button>
      <button
        v-if="canConfirm"
        class="action-button confirm-button"
        :disabled="Boolean(submittingAction)"
        @click="submitAction('CONFIRM')"
      >{{ submittingAction === 'CONFIRM' ? '提交中...' : '确认完工' }}</button>
    </view>
  </view>
</template>

<script>
import config from '@/config'
import evaluationApi from '@/api/order/evaluate.js'
import { createOrderIdempotencyKey } from '@/api/order/core'
import { getToken } from '@/utils/auth'

export default {
  data() {
    return {
      orderId: '',
      order: {},
      finishRecord: null,
      finishAttachments: [],
      allowedActions: [],
      returnReason: '',
      actionKeys: {
        CONFIRM: '',
        RETURN: ''
      },
      submittingAction: '',
      loading: true,
      error: ''
    }
  },

  computed: {
    canConfirm() {
      return this.allowedActions.includes('CONFIRM')
    },
    canReturn() {
      return this.allowedActions.includes('RETURN')
    }
  },

  watch: {
    returnReason() {
      // 用户修改退回原因后，请求指纹变化，下一次提交必须使用新 key。
      if (this.submittingAction !== 'RETURN') this.$set(this.actionKeys, 'RETURN', '')
    }
  },

  onLoad(options) {
    this.orderId = options && options.id ? options.id : ''
    this.loadDetail()
  },

  methods: {
    extractData(response) {
      return response && response.data !== undefined ? response.data : response
    },

    async loadDetail() {
      if (!this.orderId) {
        this.loading = false
        this.error = '缺少工单编号，无法加载完工信息'
        return
      }
      this.loading = true
      this.error = ''
      try {
        const response = await evaluationApi.getOrderForEvaluation(this.orderId)
        const order = this.extractData(response)
        if (!order || order.id === undefined || order.id === null) {
          throw new Error('工单详情响应缺少 id')
        }
        this.order = order
        this.allowedActions = (Array.isArray(order.allowedActions) ? order.allowedActions : [])
          .map(action => String(action).toUpperCase())

        const finishRecords = (Array.isArray(order.timeline) ? order.timeline : [])
          .filter(item => String(item.actionType).toUpperCase() === 'FINISH')
        this.finishRecord = finishRecords.length ? finishRecords[finishRecords.length - 1] : null
        // 退回返工会保留历史完工证据，因此展示详情接口返回的全部 FINISH 附件。
        this.finishAttachments = (Array.isArray(order.attachments) ? order.attachments : [])
          .filter(item => String(item.bizStage).toUpperCase() === 'FINISH')
      } catch (error) {
        this.error = '完工信息加载失败，请重试'
      } finally {
        this.loading = false
      }
    },

    getActionKey(action) {
      if (!this.actionKeys[action]) {
        this.$set(this.actionKeys, action, createOrderIdempotencyKey(action))
      }
      return this.actionKeys[action]
    },

    resetActionKey(action) {
      this.$set(this.actionKeys, action, '')
    },

    showActionConfirm(action) {
      const isReturn = action === 'RETURN'
      return new Promise(resolve => {
        uni.showModal({
          title: isReturn ? '确认退回' : '确认完工',
          content: isReturn
            ? '退回后工单将重新进入处理中，确定继续吗？'
            : '确认处理结果无误并完成本次工单吗？',
          confirmText: isReturn ? '确认退回' : '确认完成',
          confirmColor: isReturn ? '#F53F3F' : '#36CFC9',
          success: result => resolve(Boolean(result.confirm)),
          fail: () => resolve(false)
        })
      })
    },

    isConflictError(error) {
      const code = error && (error.code || error.statusCode || error.status)
      return Number(error) === 409 || Number(code) === 409 || String(error || '').indexOf('409') !== -1
    },

    async submitAction(action) {
      if (this.submittingAction) return
      if (action === 'RETURN' && !String(this.returnReason || '').trim()) {
        this.$u.toast('请填写退回原因')
        return
      }
      if (!this.allowedActions.includes(action)) {
        this.$u.toast('工单状态已变化，正在刷新')
        await this.loadDetail()
        return
      }
      if (!await this.showActionConfirm(action)) return

      this.submittingAction = action
      const idempotencyKey = this.getActionKey(action)
      try {
        if (action === 'CONFIRM') {
          await evaluationApi.confirmOrder(this.orderId, {
            version: this.order.version,
            idempotencyKey
          })
        } else {
          await evaluationApi.returnOrder(this.orderId, {
            reason: this.returnReason,
            version: this.order.version,
            idempotencyKey
          })
        }

        this.resetActionKey(action)
        uni.$emit('refreshOrderList')
        if (action === 'CONFIRM') {
          this.$u.toast('完工确认成功，请进行评价')
          uni.redirectTo({
            url: `/pages/order/order-detail/order-evaluate?id=${encodeURIComponent(this.orderId)}`,
            fail: () => this.$u.toast('评价页面打开失败，请从工单详情进入')
          })
        } else {
          this.$u.toast('工单已退回处理')
          uni.redirectTo({
            url: `/pages/order/detail/index?id=${encodeURIComponent(this.orderId)}`,
            fail: () => this.loadDetail()
          })
        }
      } catch (error) {
        if (this.isConflictError(error)) {
          this.resetActionKey(action)
          await this.loadDetail()
          this.$u.toast('工单状态已变化，已刷新最新信息')
        } else {
          this.$u.toast(`${action === 'RETURN' ? '退回' : '确认'}失败，已保留当前内容，请重试`)
        }
      } finally {
        this.submittingAction = ''
      }
    },

    // 下载地址来自详情接口，下载请求始终携带当前登录令牌。
    openAttachment(item) {
      if (!item || !item.downloadUrl) {
        this.$u.toast('附件暂不可下载')
        return
      }
      const baseUrl = String(config.baseUrl || '').replace(/\/$/, '')
      const downloadUrl = String(item.downloadUrl)
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
        success: response => {
          if (response.statusCode !== 200 || !response.tempFilePath) {
            this.$u.toast('附件下载失败，请稍后重试')
            return
          }
          const filePath = response.tempFilePath
          if (/\.(png|jpe?g|gif|bmp|webp)$/i.test(item.fileName || '')) {
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
      return {
        WAIT_CONFIRM: '待确认',
        PROCESSING: '处理中',
        COMPLETED: '已完成',
        CLOSED: '已关闭'
      }[status] || status || '未知状态'
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
page,
.page-container { min-height: 100vh; background: #F5F7FA; }
.page-container { padding: 24rpx; box-sizing: border-box; }
.page-scroll { height: calc(100vh - 164rpx); }

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

.state-text { margin: 20rpx 0; color: #4E5969; font-size: 28rpx; }
.error-text { color: #F53F3F; }
.detail-content { padding-bottom: 30rpx; }
.detail-card { margin-bottom: 20rpx; padding: 26rpx; }
.summary-card { background: linear-gradient(135deg, #FFFFFF 0%, #F3FFFE 100%); }

.summary-header,
.card-title-row,
.attachment-row,
.result-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.summary-title-wrap { min-width: 0; margin-right: 20rpx; }

.summary-title {
  display: block;
  overflow: hidden;
  color: #1D2129;
  font-size: 34rpx;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.summary-no { display: block; margin-top: 10rpx; color: #86909C; font-size: 23rpx; }

.status-tag {
  flex-shrink: 0;
  padding: 8rpx 16rpx;
  border-radius: 20rpx;
  color: #D4380D;
  background: #FFF2E8;
  font-size: 22rpx;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24rpx 16rpx;
  margin-top: 30rpx;
}

.summary-label { color: #86909C; font-size: 23rpx; }
.summary-value { display: block; margin-top: 8rpx; color: #1D2129; font-size: 26rpx; }
.card-title-row { position: relative; margin-bottom: 22rpx; }
.card-title { padding-left: 20rpx; color: #1D2129; font-size: 30rpx; font-weight: 500; }

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

.danger-line { background: #F53F3F; }
.card-count { color: #86909C; font-size: 23rpx; }
.result-content { display: block; color: #4E5969; font-size: 27rpx; line-height: 1.7; }

.result-meta {
  margin-top: 20rpx;
  padding-top: 18rpx;
  border-top: 1rpx solid #F2F3F5;
  color: #86909C;
  font-size: 22rpx;
}

.attachment-list { border-top: 1rpx solid #F2F3F5; }
.attachment-row { padding: 18rpx 0; border-bottom: 1rpx solid #F2F3F5; }
.attachment-info { flex: 1; min-width: 0; margin-left: 16rpx; }

.attachment-name,
.attachment-meta {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.attachment-name { color: #1D2129; font-size: 26rpx; }
.attachment-meta { margin-top: 8rpx; color: #86909C; font-size: 22rpx; }
.empty-text { display: block; padding: 12rpx 0 4rpx; color: #86909C; font-size: 24rpx; text-align: center; }

.reason-input {
  width: 100%;
  min-height: 180rpx;
  padding: 18rpx;
  border: 1rpx solid #E5E6EB;
  border-radius: 10rpx;
  color: #1D2129;
  background: #FAFAFA;
  font-size: 26rpx;
  box-sizing: border-box;
}

.word-count { display: block; margin-top: 10rpx; color: #86909C; font-size: 22rpx; text-align: right; }

.action-bar {
  display: flex;
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 10;
  gap: 20rpx;
  padding: 20rpx 24rpx;
  border-top: 1rpx solid #F2F3F5;
  background: #FFFFFF;
  box-sizing: border-box;
}

.action-button {
  flex: 1;
  height: 84rpx;
  border-radius: 12rpx;
  font-size: 30rpx;
  line-height: 84rpx;
}

.return-button { border: 1rpx solid #F53F3F; color: #F53F3F; background: #FFFFFF; }
.confirm-button { border: 1rpx solid #36CFC9; color: #FFFFFF; background: #36CFC9; }
.action-button[disabled] { opacity: 0.55; }
</style>

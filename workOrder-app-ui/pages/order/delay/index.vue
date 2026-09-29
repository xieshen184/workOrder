<template>
  <view class="page-container">
    <u-navbar
      title="延期申请"
      :is-back="true"
      background="#FFFFFF"
      title-color="#303133"
      left-icon-color="#606266"
      :border-bottom="false"
    ></u-navbar>

    <scroll-view class="content-scroll" scroll-y :show-scrollbar="false">
      <view v-if="loading" class="state-card">
        <u-loading-icon mode="circle" color="#FF9800" size="42"></u-loading-icon>
        <text class="state-text">正在加载延期信息...</text>
      </view>

      <view v-else-if="error" class="state-card error-state">
        <u-icon name="info-circle" color="#F53F3F" size="44"></u-icon>
        <text class="state-text">{{ error }}</text>
        <u-button type="primary" size="mini" plain @click="loadPageData()">重试</u-button>
      </view>

      <view v-else class="content-container">
        <view class="deadline-card">
          <view class="deadline-accent"></view>
          <view class="deadline-content">
            <text class="deadline-title">当前时限</text>
            <view class="deadline-row">
              <text class="deadline-label">原截止时间</text>
              <text class="deadline-value">{{ formatDeadline(originalDeadline) }}</text>
            </view>
            <view class="deadline-row">
              <text class="deadline-label">当前有效截止时间</text>
              <text class="deadline-value">{{ formatDeadline(effectiveDeadline) }}</text>
            </view>
            <view class="deadline-row">
              <text class="deadline-label">剩余时长</text>
              <text class="deadline-value" :class="{ overdue: remainingDuration === '已超时' }">{{ remainingDuration }}</text>
            </view>
          </view>
        </view>

        <view v-if="isPending" class="notice notice-warning">
          <u-icon name="clock" color="#FF9800" size="30"></u-icon>
          <text>等待审批，仍按原时限计时</text>
        </view>
        <view v-else-if="isRejected" class="notice notice-error">
          <u-icon name="close-circle" color="#F53F3F" size="30"></u-icon>
          <text>上次申请已拒绝{{ rejectionReason ? '：' + rejectionReason : '' }}，可补充材料后重新申请</text>
        </view>
        <view v-else-if="isApproved" class="notice notice-success">
          <u-icon name="checkmark-circle" color="#00B42A" size="30"></u-icon>
          <text>延期申请已审批通过，当前有效截止时间已更新</text>
        </view>

        <view class="form-item">
          <text class="form-label">申请延期至 *</text>
          <view
            class="picker-field"
            :class="{ disabled: isReadOnly }"
            @click="openDeadlinePicker"
          >
            <text :class="{ placeholder: !form.requestedDeadline }">
              {{ form.requestedDeadline ? formatDeadline(form.requestedDeadline) : '选择日期与时间' }}
            </text>
            <u-icon v-if="!isReadOnly" name="arrow-right" color="#C9CDD4" size="28"></u-icon>
          </view>
        </view>

        <view class="form-item">
          <text class="form-label">延期原因 *</text>
          <u-textarea
            v-model="form.reason"
            placeholder="说明无法按时完成的客观原因"
            :height="220"
            :maxlength="500"
            :disabled="isReadOnly"
            border="surround"
          ></u-textarea>
        </view>

        <view class="form-item attachment-form-item">
          <view class="attachment-label-row">
            <text class="form-label">佐证照片（选填）</text>
            <text class="attachment-limit">最多 10 张</text>
          </view>
          <u-upload
            :file-list="form.attachments"
            :max-count="10"
            :multiple="true"
            :disabled="isReadOnly || submitting"
            :deletable="!isReadOnly"
            @afterRead="afterRead"
            @delete="deleteFile"
            name="file"
            accept="image"
            class="upload-component"
          >
            <template #add>
              <view class="add-btn">
                <u-icon name="plus" color="#9AA5B1" size="34"></u-icon>
                <text class="add-text">上传照片</text>
              </view>
            </template>
          </u-upload>
          <view
            v-for="(file, index) in form.attachments"
            v-if="file.status === 'failed'"
            :key="`delay-retry-${index}`"
            class="upload-retry-item"
          >
            <text class="upload-retry-text">{{ file.name || '图片' }}上传失败</text>
            <u-button type="primary" size="mini" plain @click.stop="retryUpload(index)">重试</u-button>
          </view>
        </view>

        <view class="rule-notice">
          提交后工单继续计时；审批通过才更新截止时间，审批记录会进入处理日志。
        </view>
      </view>
    </scroll-view>

    <view v-if="!loading && !error" class="submit-bar">
      <u-button
        v-if="!isReadOnly"
        type="primary"
        size="large"
        :loading="submitting"
        :disabled="submitting"
        :custom-style="submitButtonStyle"
        @click="submitForm"
      >提交延期申请</u-button>
      <u-button
        v-else
        type="primary"
        size="large"
        disabled
        :custom-style="lockedButtonStyle"
      >{{ isPending ? '等待审批' : '已审批' }}</u-button>
    </view>

    <u-datetime-picker
      :show="showDatePicker"
      v-model="pickerValue"
      mode="datetime"
      title="选择延期时间"
      confirm-text="确定"
      cancel-text="取消"
      confirm-color="#FF9800"
      :min-date="pickerMinDate"
      :max-date="pickerMaxDate"
      @confirm="onDeadlineConfirm"
      @cancel="closeDatePicker"
    ></u-datetime-picker>
  </view>
</template>

<script>
import orderApi from '@/api/order/handle.js'
import delayApi from '@/api/order/delay.js'

const MINUTE = 60 * 1000
const HOUR = 60 * MINUTE
const DAY = 24 * HOUR

export default {
  data() {
    const now = Date.now()
    return {
      orderId: '',
      order: {},
      originalDeadline: '',
      effectiveDeadline: '',
      currentVersion: null,
      latestRequest: null,
      pendingHint: false,
      loading: true,
      error: '',
      submitting: false,
      idempotencyKey: '',
      form: {
        requestedDeadline: '',
        reason: '',
        attachments: []
      },
      showDatePicker: false,
      pickerValue: now,
      pickerMinDate: now + MINUTE,
      pickerMaxDate: now + 10 * 365 * DAY,
      now,
      clockTimer: null,
      navigateTimer: null,
      submitButtonStyle: {
        height: '96rpx',
        borderRadius: '12rpx',
        backgroundColor: '#FF9800',
        borderColor: '#FF9800',
        fontSize: '30rpx'
      },
      lockedButtonStyle: {
        height: '96rpx',
        borderRadius: '12rpx',
        backgroundColor: '#C9CDD4',
        borderColor: '#C9CDD4',
        fontSize: '30rpx'
      }
    }
  },

  computed: {
    latestStatus() {
      return String(this.latestRequest && this.latestRequest.requestStatus || '').toUpperCase()
    },

    isPending() {
      return this.latestStatus === 'PENDING' || this.pendingHint
    },

    isRejected() {
      return this.latestStatus === 'REJECTED'
    },

    isApproved() {
      return this.latestStatus === 'APPROVED'
    },

    isReadOnly() {
      return this.isPending
    },

    rejectionReason() {
      return String(this.latestRequest && this.latestRequest.approvalComment || '').trim()
    },

    remainingDuration() {
      const deadline = this.parseDate(this.effectiveDeadline)
      if (!deadline) return '—'
      const remaining = deadline.getTime() - this.now
      if (remaining <= 0) return '已超时'
      const totalSeconds = Math.floor(remaining / 1000)
      const hours = Math.floor(totalSeconds / 3600)
      const minutes = Math.floor((totalSeconds % 3600) / 60)
      const seconds = totalSeconds % 60
      return `${this.pad(hours)}:${this.pad(minutes)}:${this.pad(seconds)}`
    }
  },

  onLoad(options) {
    this.orderId = options && options.id ? options.id : ''
    this.pendingHint = this.isTruthy(options && options.pending)
    if (!this.orderId) {
      this.loading = false
      this.error = '缺少工单编号，无法加载延期申请'
      return
    }
    this.startClock()
    this.loadPageData()
  },

  onShow() {
    this.startClock()
  },

  onHide() {
    this.stopClock()
  },

  onUnload() {
    this.stopClock()
    if (this.navigateTimer) clearTimeout(this.navigateTimer)
  },

  methods: {
    async loadPageData(preserveForm = false) {
      if (!this.orderId) {
        this.loading = false
        this.error = '缺少工单编号，无法加载延期申请'
        return
      }
      this.loading = true
      this.error = ''
      try {
        const detailResponse = await orderApi.getOrderDetail(this.orderId)
        const detail = this.unwrap(detailResponse)
        if (!detail || detail.id === undefined || detail.id === null) {
          throw new Error('工单详情响应缺少 id')
        }
        this.applyOrderDetail(detail)

        let latest = null
        try {
          const latestResponse = await delayApi.getLatestDelayRequest(this.orderId)
          latest = this.normalizeLatest(this.unwrap(latestResponse))
        } catch (latestError) {
          if (!this.isNotFoundError(latestError)) throw latestError
        }
        this.applyLatestRequest(latest, preserveForm)
      } catch (loadError) {
        console.error('获取延期申请信息失败:', loadError)
        this.error = '延期申请信息加载失败，请重试'
      } finally {
        this.loading = false
      }
    },

    applyOrderDetail(detail) {
      this.order = detail
      this.currentVersion = detail.version !== undefined && detail.version !== null
        ? detail.version
        : this.currentVersion
      // 详情接口返回该字段时以服务端为准；仅旧接口缺字段时保留路由提示。
      if (detail.delayPendingFlag !== undefined && detail.delayPendingFlag !== null) {
        this.pendingHint = this.isTruthy(detail.delayPendingFlag)
      }

      const baseDeadline = this.firstValue(
        detail.originalDeadline,
        detail.finishDeadline,
        detail.currentDeadline
      )
      const effectiveDeadline = this.firstValue(
        detail.currentEffectiveDeadline,
        detail.effectiveDeadline,
        detail.extensionDeadline,
        detail.finishDeadline,
        baseDeadline
      )
      this.originalDeadline = baseDeadline
      this.effectiveDeadline = effectiveDeadline
    },

    applyLatestRequest(latest, preserveForm) {
      this.latestRequest = latest
      if (!latest) return

      this.originalDeadline = this.firstValue(latest.originalDeadline, this.originalDeadline)
      this.effectiveDeadline = this.firstValue(
        latest.currentEffectiveDeadline,
        latest.effectiveDeadline,
        this.effectiveDeadline
      )
      if (this.latestStatus === 'PENDING') this.pendingHint = true
      if (this.latestStatus === 'REJECTED' || this.latestStatus === 'APPROVED') this.pendingHint = false

      if (this.latestStatus === 'APPROVED') {
        // An approved request is historical. If the current order still exposes
        // REQUEST_DELAY, start the next application with an empty form/key.
        this.form = {
          requestedDeadline: '',
          reason: '',
          attachments: []
        }
        this.idempotencyKey = ''
        return
      }
      if (this.latestStatus === 'REJECTED') this.idempotencyKey = ''

      // A pending request is server-owned and must be shown exactly as returned.
      // A rejected request starts a new editable attempt with its previous values
      // as a convenient draft; its old attachments are not reused or deleted.
      if (!preserveForm || this.latestStatus === 'PENDING') {
        this.form.requestedDeadline = latest.requestedDeadline || ''
        this.form.reason = latest.reason || ''
        this.form.attachments = this.latestStatus === 'PENDING'
          ? this.normalizeAttachments(latest)
          : []
      }
    },

    normalizeLatest(raw) {
      if (!raw) return null
      const source = raw.latest !== undefined
        ? raw.latest
        : raw.delayRequest !== undefined
          ? raw.delayRequest
          : raw.request !== undefined
            ? raw.request
            : (Array.isArray(raw.rows) ? raw.rows[0] : raw)
      const candidate = Array.isArray(source) ? source[0] : source
      if (!candidate) return null
      return {
        id: candidate.id,
        requestStatus: this.firstValue(candidate.requestStatus, candidate.status, candidate.approvalStatus),
        originalDeadline: this.firstValue(candidate.originalDeadline, candidate.original_deadline),
        currentEffectiveDeadline: this.firstValue(
          candidate.currentEffectiveDeadline,
          candidate.current_effective_deadline,
          candidate.effectiveDeadline,
          candidate.effective_deadline,
          candidate.currentDeadline,
          candidate.current_deadline
        ),
        requestedDeadline: this.firstValue(
          candidate.requestedDeadline,
          candidate.requested_deadline,
          candidate.newDeadline,
          candidate.new_deadline
        ),
        reason: this.firstValue(candidate.reason),
        approvalComment: this.firstValue(
          candidate.approvalComment,
          candidate.approval_comment,
          candidate.approvalReason,
          candidate.rejectionReason,
          candidate.rejectReason
        ),
        attachments: Array.isArray(candidate.attachments) ? candidate.attachments : [],
        attachmentIds: Array.isArray(candidate.attachmentIds)
          ? candidate.attachmentIds
          : (Array.isArray(candidate.attachment_ids) ? candidate.attachment_ids : [])
      }
    },

    normalizeAttachments(latest) {
      if (Array.isArray(latest.attachments) && latest.attachments.length) {
        return latest.attachments.map(item => ({
          ...item,
          status: item.status || 'success',
          attachmentId: item.attachmentId !== undefined ? item.attachmentId : item.id,
          deletable: false
        }))
      }
      return latest.attachmentIds.map(id => ({
        id,
        attachmentId: id,
        name: `附件 ${id}`,
        status: 'success',
        deletable: false
      }))
    },

    unwrap(response) {
      return response && response.data !== undefined ? response.data : response
    },

    firstValue(...values) {
      for (const value of values) {
        if (value !== undefined && value !== null && value !== '') return value
      }
      return ''
    },

    isTruthy(value) {
      return value === true || value === 1 || ['1', 'true', 'y', 'yes'].indexOf(String(value || '').toLowerCase()) !== -1
    },

    isNotFoundError(error) {
      const code = error && (error.code || error.statusCode || error.status)
      return Number(error) === 404 || Number(code) === 404 || String(error || '').indexOf('404') !== -1
    },

    isConflictError(error) {
      const code = error && (error.code || error.statusCode || error.status)
      const text = String(error || '')
      return Number(error) === 409 || Number(code) === 409 || text.indexOf('409') !== -1 || text.indexOf('版本') !== -1
    },

    openDeadlinePicker() {
      if (this.isReadOnly || this.submitting) return
      const current = this.parseDate(this.effectiveDeadline)
      const minimum = Math.max(Date.now() + MINUTE, current ? current.getTime() + MINUTE : 0)
      this.pickerMinDate = minimum
      this.pickerMaxDate = Math.max(minimum + DAY, Date.now() + 10 * 365 * DAY)
      const selected = this.parseDate(this.form.requestedDeadline)
      this.pickerValue = selected && selected.getTime() > minimum ? selected.getTime() : minimum
      this.showDatePicker = true
    },

    closeDatePicker() {
      this.showDatePicker = false
    },

    onDeadlineConfirm(event) {
      const value = event && event.value !== undefined ? event.value : event
      const timestamp = Number(value)
      if (!Number.isFinite(timestamp)) {
        this.$u.toast('新截止时间无效，请重新选择')
        return
      }
      this.pickerValue = timestamp
      this.form.requestedDeadline = timestamp
      this.showDatePicker = false
    },

    validateForm() {
      if (this.isReadOnly) return '已有待审批延期申请，不能重复提交'
      if (this.currentVersion === undefined || this.currentVersion === null || this.currentVersion === '') {
        return '工单版本缺失，请刷新后重试'
      }
      const requested = this.parseDate(this.form.requestedDeadline)
      if (!requested) return '请选择新截止时间'
      const current = this.parseDate(this.effectiveDeadline)
      if (current && requested.getTime() <= current.getTime()) {
        return '新截止时间必须晚于当前有效截止时间'
      }
      if (!String(this.form.reason || '').trim()) return '请填写延期原因'
      if (String(this.form.reason || '').trim().length > 500) return '延期原因不能超过500个字符'
      if (this.hasUploading()) return '佐证附件正在上传，请稍候'
      if (this.hasFailed()) return '存在上传失败的附件，请重试或删除'
      return ''
    },

    getIdempotencyKey() {
      if (!this.idempotencyKey) {
        this.idempotencyKey = delayApi.createDelayIdempotencyKey(this.orderId)
      }
      return this.idempotencyKey
    },

    attachmentIds() {
      return this.form.attachments
        .filter(file => file && file.status === 'success')
        .map(file => file.attachmentId !== undefined ? file.attachmentId : file.id)
        .filter(id => id !== undefined && id !== null && id !== '')
    },

    async submitForm() {
      if (this.submitting) return
      const validationMessage = this.validateForm()
      if (validationMessage) {
        this.$u.toast(validationMessage)
        return
      }

      this.submitting = true
      try {
        await delayApi.submitDelayRequest(this.orderId, {
          requestedDeadline: this.formatApiDeadline(this.form.requestedDeadline),
          reason: String(this.form.reason || '').trim(),
          attachmentIds: this.attachmentIds(),
          version: this.currentVersion,
          idempotencyKey: this.getIdempotencyKey()
        })
        // Lock the page immediately in case navigation is delayed or fails;
        // the detail page will reconcile the flag from the next GET.
        this.pendingHint = true
        uni.$emit('refreshOrderList')
        this.$u.toast('延期申请已提交，等待审批')
        this.navigateTimer = setTimeout(() => {
          uni.navigateBack()
        }, 500)
      } catch (submitError) {
        console.error('提交延期申请失败:', submitError)
        if (this.isConflictError(submitError)) {
          await this.loadPageData(true)
          this.$u.toast('工单状态已变化，已刷新最新信息，请确认后重试')
        } else {
          // Preserve the form and key so retrying the same submit is replay-safe.
          this.$u.toast('提交失败，已保留填写内容，请重试')
        }
      } finally {
        this.submitting = false
      }
    },

    hasUploading() {
      return this.form.attachments.some(file => file && file.status === 'uploading')
    },

    hasFailed() {
      return this.form.attachments.some(file => file && file.status === 'failed')
    },

    async afterRead(event) {
      if (this.isReadOnly || this.submitting) return
      const files = Array.isArray(event && event.file) ? event.file : [event && event.file]
      for (const file of files) {
        if (!file || this.form.attachments.length >= 10) break
        const index = this.form.attachments.length
        this.form.attachments.push({ ...file, status: 'uploading', message: '上传中' })
        await this.uploadFileAtIndex(index, true)
      }
    },

    async uploadFileAtIndex(index, force = false) {
      const file = this.form.attachments[index]
      if (!file || (file.status === 'uploading' && !force)) return
      this.$set(this.form.attachments, index, { ...file, status: 'uploading', message: '上传中' })
      try {
        const filePath = file.url || file.path
        if (!filePath) throw new Error('附件路径不能为空')
        const result = await delayApi.uploadAttachment(filePath, 'file')
        const attachment = result && result.data !== undefined ? result.data : result
        const attachmentId = attachment && (
          attachment.id !== undefined ? attachment.id : attachment.attachmentId
        )
        if (attachmentId === undefined || attachmentId === null || attachmentId === '') {
          throw new Error('上传结果缺少附件标识')
        }
        this.$set(this.form.attachments, index, {
          ...file,
          status: 'success',
          message: '',
          attachmentId
        })
      } catch (uploadError) {
        console.error('上传延期附件失败:', uploadError)
        this.$set(this.form.attachments, index, { ...file, status: 'failed', message: '上传失败' })
      }
    },

    async retryUpload(index) {
      if (this.isReadOnly || this.hasUploading()) return
      await this.uploadFileAtIndex(index)
    },

    async deleteFile(event) {
      if (this.isReadOnly || this.submitting) return
      const index = event && event.index
      const file = index === undefined ? null : this.form.attachments[index]
      if (!file || file.deleting || file.status === 'uploading') return
      const attachmentId = file.attachmentId
      if (attachmentId === undefined || attachmentId === null || attachmentId === '') {
        this.form.attachments.splice(index, 1)
        return
      }

      this.$set(this.form.attachments, index, {
        ...file,
        deleting: true,
        deletable: false,
        message: '删除中'
      })
      try {
        await delayApi.deleteAttachment(attachmentId)
        this.form.attachments.splice(index, 1)
      } catch (deleteError) {
        console.error('删除延期附件失败:', deleteError)
        const current = this.form.attachments[index]
        if (current) {
          this.$set(this.form.attachments, index, {
            ...current,
            deleting: false,
            deletable: true,
            message: '删除失败，请重试'
          })
        }
        this.$u.toast('附件删除失败，文件仍保留')
      }
    },

    startClock() {
      if (this.clockTimer) return
      this.now = Date.now()
      this.clockTimer = setInterval(() => {
        this.now = Date.now()
      }, 1000)
    },

    stopClock() {
      if (!this.clockTimer) return
      clearInterval(this.clockTimer)
      this.clockTimer = null
    },

    parseDate(value) {
      if (value === undefined || value === null || value === '') return null
      if (value instanceof Date) return Number.isNaN(value.getTime()) ? null : value
      if (typeof value === 'number') {
        const date = new Date(value)
        return Number.isNaN(date.getTime()) ? null : date
      }
      const text = String(value).trim()
      const normalized = text.indexOf('T') === -1 ? text.replace(' ', 'T') : text
      let date = new Date(normalized)
      if (Number.isNaN(date.getTime())) {
        const parts = text.match(/^(\d{4})[-/]?(\d{2})[-/]?(\d{2})(?:[ T](\d{2}):?(\d{2})(?::?(\d{2}))?)?$/)
        if (parts) {
          date = new Date(
            Number(parts[1]),
            Number(parts[2]) - 1,
            Number(parts[3]),
            Number(parts[4] || 0),
            Number(parts[5] || 0),
            Number(parts[6] || 0)
          )
        }
      }
      if (!Number.isNaN(date.getTime())) return date
      const fallback = new Date(text)
      return Number.isNaN(fallback.getTime()) ? null : fallback
    },

    formatDeadline(value) {
      const date = this.parseDate(value)
      if (!date) return value ? String(value).replace('T', ' ').replace(/\.000Z$/, '') : '—'
      return this.formatDateTime(date, false)
    },

    formatApiDeadline(value) {
      const date = this.parseDate(value)
      return date ? this.formatDateTime(date, true) : value
    },

    formatDateTime(date, includeSeconds) {
      const result = `${date.getFullYear()}-${this.pad(date.getMonth() + 1)}-${this.pad(date.getDate())} ${this.pad(date.getHours())}:${this.pad(date.getMinutes())}`
      return includeSeconds ? `${result}:${this.pad(date.getSeconds())}` : result
    },

    pad(value) {
      return String(value).padStart(2, '0')
    }
  }
}
</script>

<style lang="scss" scoped>
.page-container {
  min-height: 100vh;
  background-color: #F3F6F9;
}

.content-scroll {
  height: calc(100vh - 128rpx);
}

.content-container {
  padding: 14rpx 24rpx 190rpx;
}

.state-card {
  margin: 160rpx 24rpx 0;
  min-height: 260rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background-color: #FFFFFF;
  border-radius: 16rpx;
  color: #86909C;
}

.state-text {
  margin: 20rpx 0;
  color: #606266;
  font-size: 26rpx;
}

.deadline-card {
  display: flex;
  overflow: hidden;
  margin-bottom: 20rpx;
  background-color: #FFFFFF;
  border-radius: 16rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
}

.deadline-accent {
  width: 6rpx;
  flex-shrink: 0;
  background-color: #FF9800;
}

.deadline-content {
  flex: 1;
  padding: 24rpx 26rpx;
}

.deadline-title {
  display: block;
  margin-bottom: 12rpx;
  color: #273444;
  font-size: 30rpx;
  font-weight: 600;
}

.deadline-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12rpx;
}

.deadline-label {
  color: #8995A5;
  font-size: 24rpx;
}

.deadline-value {
  color: #445466;
  font-size: 24rpx;
  text-align: right;
}

.deadline-value.overdue {
  color: #F53F3F;
}

.notice,
.rule-notice {
  display: flex;
  align-items: flex-start;
  padding: 20rpx 24rpx;
  margin-bottom: 22rpx;
  border-radius: 12rpx;
  font-size: 24rpx;
  line-height: 1.6;
}

.notice text {
  flex: 1;
  margin-left: 12rpx;
}

.notice-warning {
  color: #9A6700;
  background-color: #FFF7E8;
}

.notice-error {
  color: #B42318;
  background-color: #FFF2F0;
}

.notice-success {
  color: #087443;
  background-color: #E8FFEA;
}

.form-item {
  margin-bottom: 24rpx;
}

.form-label {
  display: block;
  margin: 0 6rpx 12rpx;
  color: #657384;
  font-size: 24rpx;
  font-weight: 500;
}

.picker-field {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 88rpx;
  padding: 0 24rpx;
  background-color: #FFFFFF;
  border: 1rpx solid #E5E9EF;
  border-radius: 12rpx;
  color: #445466;
  font-size: 26rpx;
}

.picker-field.disabled {
  background-color: #F7F8FA;
}

.placeholder {
  color: #9AA5B1;
}

.attachment-form-item {
  margin-top: 8rpx;
}

.attachment-label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.attachment-limit {
  margin-right: 6rpx;
  color: #9AA5B1;
  font-size: 22rpx;
}

.upload-component {
  padding: 8rpx 0;
}

.add-btn {
  width: 160rpx;
  height: 160rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 1rpx dashed #CFD8E3;
  border-radius: 12rpx;
  background-color: #FFFFFF;
}

.add-text {
  margin-top: 8rpx;
  color: #9AA5B1;
  font-size: 22rpx;
}

.upload-retry-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12rpx;
  margin-top: 8rpx;
  background-color: #FFF2F0;
  border-radius: 8rpx;
}

.upload-retry-text {
  flex: 1;
  margin-right: 16rpx;
  color: #F53F3F;
  font-size: 24rpx;
}

.rule-notice {
  display: block;
  color: #7C8795;
  background-color: #FFFFFF;
}

.submit-bar {
  position: fixed;
  z-index: 10;
  right: 0;
  bottom: 0;
  left: 0;
  padding: 16rpx 24rpx 24rpx;
  background-color: rgba(243, 246, 249, 0.96);
}
</style>

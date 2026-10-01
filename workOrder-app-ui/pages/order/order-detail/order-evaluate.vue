<template>
  <view class="page-container">
    <u-navbar
      :title="isEvaluated ? '评价详情' : '评价工单'"
      :auto-back="true"
      background="#36CFC9"
      title-color="#ffffff"
      left-icon-color="#ffffff"
      :border-bottom="false"
    ></u-navbar>

    <scroll-view class="content-scroll" scroll-y :show-scrollbar="false">
      <view v-if="loading" class="loading-container">
        <view class="custom-loading"></view>
        <text class="loading-text">加载中...</text>
      </view>

      <view v-else-if="error" class="error-container">
        <u-empty :text="error" mode="error" color="#F53F3F" class="empty-tip"></u-empty>
        <u-button type="primary" size="mini" plain @click="loadOrderInfo">重试</u-button>
      </view>

      <view v-else class="detail-container">
        <view class="info-card">
          <view class="card-title">
            <u-icon name="clipboard" color="#36CFC9" size="28"></u-icon>
            <text class="title-text">工单信息</text>
          </view>
          <view class="info-content">
            <view class="info-row">
              <text class="info-label">工单编号：</text>
              <text class="info-value">{{ order.orderNo || order.id }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">工单标题：</text>
              <text class="info-value">{{ order.title || order.categoryName || '工单详情' }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">故障类型：</text>
              <text class="info-value">{{ order.categoryName || '未分类' }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">处理工程师：</text>
              <text class="info-value">{{ order.currentAssigneeName || '未填写' }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">完成时间：</text>
              <text class="info-value">{{ formatTime(order.finishedAt) }}</text>
            </view>
          </view>
        </view>

        <view v-if="isEvaluated" class="evaluation-card">
          <view class="card-title">
            <u-icon name="star" color="#FF9C07" size="28"></u-icon>
            <text class="title-text">评价信息</text>
          </view>
          <view class="evaluation-content">
            <view class="score-container">
              <text class="score-label">您的满意度评价：</text>
              <view class="rating-stars">
                <u-icon
                  v-for="star in ratingOptions"
                  :key="star"
                  :name="star <= evaluationDetail.score ? 'star-fill' : 'star'"
                  color="#FF9C07"
                  size="40rpx"
                ></u-icon>
              </view>
              <text class="score-text">{{ getRatingText(evaluationDetail.score) }}</text>
            </view>
            <view class="comment-container">
              <text class="comment-label">评价内容：</text>
              <view class="comment-content">
                <text>{{ evaluationDetail.content || '未填写评价内容' }}</text>
              </view>
            </view>
            <view class="info-row">
              <text class="info-label">评价时间：</text>
              <text class="info-value">{{ formatTime(evaluationDetail.createTime) }}</text>
            </view>
          </view>
        </view>

        <view v-else class="evaluation-form-card">
          <view class="card-title">
            <u-icon name="star" color="#FF9C07" size="28"></u-icon>
            <text class="title-text">满意度评价</text>
          </view>
          <view class="evaluation-content">
            <view class="score-container">
              <text class="score-label">请评价您对本次服务的满意度：</text>
              <view class="star-container">
                <view
                  v-for="star in ratingOptions"
                  :key="star"
                  class="star-item"
                  :class="{ active: star <= evaluation.score }"
                  @click="selectScore(star)"
                >
                  <u-icon
                    :name="star <= evaluation.score ? 'star-fill' : 'star'"
                    color="#FF9C07"
                    size="50rpx"
                  ></u-icon>
                </view>
              </view>
              <text class="score-text">{{ scoreText }}</text>
              <text v-if="showScoreError" class="validation-error">请选择满意度评分</text>
            </view>

            <view class="comment-container">
              <text class="comment-label">评价内容（选填）：</text>
              <textarea
                v-model="evaluation.content"
                placeholder="请输入您的评价（最多500字）"
                maxlength="500"
                class="comment-input"
              ></textarea>
              <text class="word-count">{{ evaluation.content.length }}/500</text>
            </view>
          </view>
        </view>
      </view>
    </scroll-view>

    <view v-if="!isEvaluated && canEvaluate && !loading && !error" class="submit-button-container">
      <button
        class="submit-button"
        :disabled="!evaluation.score || submitting"
        @click="submitEvaluation"
      >
        <view v-if="submitting" class="button-loading"></view>
        <text v-else class="button-text">提交评价</text>
      </button>
    </view>
  </view>
</template>

<script>
import evaluationApi from '@/api/order/evaluate.js'
import { createOrderIdempotencyKey } from '@/api/order/core'

export default {
  data() {
    return {
      orderId: '',
      order: {},
      evaluationDetail: null,
      // 小程序端不能使用 `v-for="star in 5"` 表示 1~5。
      // 微信编译产物会按 0~4 生成循环项，导致第五颗星实际提交 4 分。
      ratingOptions: [1, 2, 3, 4, 5],
      evaluation: {
        score: 0,
        content: ''
      },
      idempotencyKey: '',
      loading: true,
      submitting: false,
      error: '',
      showScoreError: false
    }
  },

  computed: {
    isEvaluated() {
      return Boolean(this.evaluationDetail)
    },
    canEvaluate() {
      const actions = Array.isArray(this.order.allowedActions) ? this.order.allowedActions : []
      return actions.some(action => String(action).toUpperCase() === 'EVALUATE')
    },
    scoreText() {
      return {
        0: '请选择评分',
        1: '非常不满意',
        2: '不满意',
        3: '一般',
        4: '满意',
        5: '非常满意'
      }[this.evaluation.score]
    }
  },

  watch: {
    'evaluation.score'() {
      if (!this.submitting) this.idempotencyKey = ''
    },
    'evaluation.content'() {
      if (!this.submitting) this.idempotencyKey = ''
    }
  },

  onLoad(options) {
    this.orderId = options && options.id ? options.id : ''
    this.loadOrderInfo()
  },

  methods: {
    extractData(response) {
      return response && response.data !== undefined ? response.data : response
    },

    normalizeEvaluation(raw) {
      if (!raw) return null
      return {
        ...raw,
        score: Number(raw.overallScore === undefined ? raw.score : raw.overallScore) || 0,
        content: raw.evaluationContent === undefined ? (raw.content || '') : raw.evaluationContent,
        createTime: raw.evaluatedAt || raw.createTime || ''
      }
    },

    async loadOrderInfo() {
      if (!this.orderId) {
        this.loading = false
        this.error = '缺少工单编号，无法加载评价'
        return
      }

      this.loading = true
      this.error = ''
      this.evaluationDetail = null
      try {
        const orderResponse = await evaluationApi.getOrderForEvaluation(this.orderId)
        const order = this.extractData(orderResponse)
        if (!order || order.id === undefined || order.id === null) {
          throw new Error('工单详情响应缺少 id')
        }
        this.order = order

        // CLOSED 为已评价终态，评价内容必须通过只读接口重新获取，不能沿用列表缓存。
        if (String(order.status).toUpperCase() === 'CLOSED' || order.evaluation) {
          const evaluationResponse = await evaluationApi.getEvaluation(this.orderId)
          this.evaluationDetail = this.normalizeEvaluation(this.extractData(evaluationResponse))
          if (!this.evaluationDetail) throw new Error('评价详情不存在')
        } else if (!this.canEvaluate) {
          this.error = '当前工单不可评价，请返回查看最新状态'
        }
      } catch (error) {
        this.error = this.error || '获取工单或评价信息失败，请重试'
      } finally {
        this.loading = false
      }
    },

    selectScore(score) {
      if (this.submitting || this.isEvaluated) return
      this.evaluation.score = score
      this.showScoreError = false
    },

    getRatingText(score) {
      return {
        1: '非常不满意',
        2: '不满意',
        3: '一般',
        4: '满意',
        5: '非常满意'
      }[score] || ''
    },

    confirmSubmit(score) {
      return new Promise(resolve => {
        uni.showModal({
          title: '提交评价',
          content: `当前选择 ${score} 分，评价提交后不可修改，确定提交吗？`,
          confirmText: '确定提交',
          success: result => resolve(Boolean(result.confirm)),
          fail: () => resolve(false)
        })
      })
    },

    isConflictError(error) {
      const code = error && (error.code || error.statusCode || error.status)
      return Number(error) === 409 || Number(code) === 409 || String(error || '').indexOf('409') !== -1
    },

    async submitEvaluation() {
      if (this.submitting || this.isEvaluated) return
      if (!this.evaluation.score) {
        this.showScoreError = true
        return
      }
      if (this.evaluation.content.length > 500) {
        this.$u.toast('评价内容最多500字')
        return
      }
      // 在确认框打开前固定本次提交分数，避免异步确认期间界面状态变化影响请求体。
      const selectedScore = Number(this.evaluation.score)
      if (!await this.confirmSubmit(selectedScore)) return

      this.submitting = true
      if (!this.idempotencyKey) {
        this.idempotencyKey = createOrderIdempotencyKey('evaluation')
      }
      try {
        await evaluationApi.submitEvaluation(this.orderId, {
          overallScore: selectedScore,
          evaluationContent: this.evaluation.content,
          version: this.order.version,
          idempotencyKey: this.idempotencyKey
        })
        this.idempotencyKey = ''
        uni.$emit('refreshEvaluationList')
        uni.$emit('refreshOrderList')
        await this.loadOrderInfo()
        const persistedScore = this.evaluationDetail ? Number(this.evaluationDetail.score) : 0
        if (persistedScore !== selectedScore) {
          this.$u.toast('评价已提交，但服务端评分与所选分数不一致')
          return
        }
        this.$u.toast(`已提交 ${persistedScore} 分评价`)
      } catch (error) {
        if (this.isConflictError(error)) {
          // 刷新后 version 已变化，下一次提交必须生成与新请求体对应的新 key。
          this.idempotencyKey = ''
          await this.loadOrderInfo()
          this.$u.toast('工单状态已变化，已刷新最新信息')
        } else {
          this.$u.toast('评价提交失败，已保留填写内容，请重试')
        }
      } finally {
        this.submitting = false
      }
    },

    formatTime(value) {
      if (!value) return '时间未知'
      return String(value).replace('T', ' ').slice(0, 16)
    }
  }
}
</script>

<style lang="scss" scoped>
.page-container {
  min-height: 100vh;
  background-color: #F5F7FA;
}

.content-scroll {
  height: calc(100vh - 120rpx);
  padding: 20rpx;
  box-sizing: border-box;
}

.loading-container,
.error-container {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  margin-top: 180rpx;
}

.custom-loading,
.button-loading {
  width: 40rpx;
  height: 40rpx;
  border: 4rpx solid rgba(54, 207, 201, 0.3);
  border-top-color: #36CFC9;
  border-radius: 50%;
  animation: spin 1s ease-in-out infinite;
}

.custom-loading { margin-bottom: 20rpx; }
.button-loading { border-color: rgba(255, 255, 255, 0.4); border-top-color: #FFFFFF; }
.loading-text { color: #86909C; font-size: 28rpx; }
@keyframes spin { to { transform: rotate(360deg); } }
.empty-tip { margin-bottom: 24rpx; }
.detail-container { padding-bottom: 120rpx; }

.info-card,
.evaluation-card,
.evaluation-form-card {
  margin-bottom: 20rpx;
  overflow: hidden;
  border-radius: 16rpx;
  background-color: #FFFFFF;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
}

.card-title {
  display: flex;
  align-items: center;
  padding: 20rpx 24rpx;
  border-bottom: 1rpx solid #F2F3F5;
}

.title-text {
  margin-left: 12rpx;
  color: #303133;
  font-size: 30rpx;
  font-weight: 500;
}

.info-content,
.evaluation-content { padding: 20rpx 24rpx; }

.info-row {
  display: flex;
  align-items: center;
  margin-bottom: 20rpx;
}

.info-row:last-child { margin-bottom: 0; }
.info-label { width: 180rpx; color: #606266; font-size: 26rpx; }
.info-value { flex: 1; color: #303133; font-size: 26rpx; }

.score-container {
  display: flex;
  flex-direction: column;
  margin-bottom: 30rpx;
  padding: 10rpx 0;
}

.score-label,
.comment-label {
  margin-bottom: 15rpx;
  color: #303133;
  font-size: 28rpx;
}

.star-container,
.rating-stars {
  display: flex;
  width: 100%;
  justify-content: center;
  margin-bottom: 10rpx;
  overflow: hidden;
  white-space: nowrap;
}

.star-item { margin: 0 8rpx; transform-origin: center; }
.star-item.active { transform: scale(1.1); }
.star-item:active { transform: scale(0.95); }

.score-text {
  color: #FF9C07;
  font-size: 26rpx;
  font-weight: 500;
  text-align: center;
}

.validation-error {
  margin-top: 6rpx;
  color: #F53F3F;
  font-size: 24rpx;
  text-align: center;
}

.comment-container { margin-top: 20rpx; }
.comment-label { display: block; }

.comment-content {
  min-height: 100rpx;
  padding: 18rpx;
  border-radius: 8rpx;
  color: #303133;
  background: #F7F8FA;
  font-size: 26rpx;
  line-height: 1.6;
}

.comment-input {
  width: 100%;
  min-height: 180rpx;
  margin-bottom: 10rpx;
  padding: 15rpx;
  border: 1rpx solid #E5E6EB;
  border-radius: 8rpx;
  color: #303133;
  font-size: 26rpx;
  box-sizing: border-box;
}

.word-count {
  display: block;
  color: #909399;
  font-size: 24rpx;
  text-align: right;
}

.submit-button-container {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 10;
  padding: 20rpx;
  border-top: 1rpx solid #F2F3F5;
  background-color: #FFFFFF;
  box-sizing: border-box;
}

.submit-button {
  display: flex;
  width: 100%;
  height: 90rpx;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 10rpx;
  color: #FFFFFF;
  background-color: #36CFC9;
  font-size: 32rpx;
}

.submit-button:disabled { background-color: #C9CDD4; }
.button-text { color: #FFFFFF; }
</style>

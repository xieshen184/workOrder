<template>
  <view class="page-container">
    <u-navbar
      title="满意度评价"
      :is-back="true"
      background="#36CFC9"
      title-color="#ffffff"
      left-icon-color="#ffffff"
      :border-bottom="false"
    ></u-navbar>

    <view class="status-tabs">
      <view
        v-for="(tab, index) in tabList"
        :key="tab.status"
        class="status-tab"
        :class="{ active: activeTab === index }"
        :style="getTabStyle(index)"
        @click="switchTab(index)"
      >
        <text class="tab-text">{{ tab.name }}</text>
        <view v-if="tabStates[index].total > 0" class="tab-badge">{{ tabStates[index].total }}</view>
      </view>
    </view>

    <scroll-view
      class="content-scroll"
      scroll-y
      :scroll-top="scrollTop"
      scroll-with-animation
      :show-scrollbar="false"
      :refresher-enabled="true"
      :refresher-triggered="currentState.refreshing"
      @refresherrefresh="refreshCurrentTab"
      @scrolltolower="loadMore"
    >
      <view v-if="currentState.loading && !currentOrders.length" class="loading-container">
        <view class="custom-loading"></view>
        <text class="loading-text">加载中...</text>
      </view>

      <view v-else class="list-container">
        <view v-if="currentState.error && !currentOrders.length" class="error-card">
          <u-icon name="info-circle" color="#F53F3F" size="40"></u-icon>
          <text class="error-text">{{ currentState.error }}</text>
          <u-button size="mini" plain type="primary" @click="refreshCurrentTab">重试</u-button>
        </view>

        <u-empty
          v-else-if="!currentOrders.length"
          :text="activeTab === 0 ? '暂无待评价工单' : '暂无已评价工单'"
          :mode="activeTab === 0 ? 'comment' : 'success'"
          color="#C9CDD4"
          class="empty-tip"
        ></u-empty>

        <view v-else class="order-list">
          <view
            v-for="order in currentOrders"
            :key="orderKey(order)"
            class="order-card"
            :class="activeTab === 0 ? 'pending-card' : 'evaluated-card'"
            @click="openEvaluation(order)"
          >
            <view class="status-bar" :class="activeTab === 0 ? 'pending-bar' : 'evaluated-bar'"></view>
            <view class="order-content">
              <view class="order-header">
                <view class="order-left">
                  <text class="order-no">{{ order.orderNo || `工单#${order.id}` }}</text>
                  <u-badge
                    :text="activeTab === 0 ? '待评价' : '已评价'"
                    :type="activeTab === 0 ? 'warning' : 'success'"
                    class="status-badge"
                  ></u-badge>
                </view>
                <text class="order-time">{{ orderTime(order) }}</text>
              </view>

              <view class="order-info">
                <text class="info-label">工单标题：</text>
                <text class="info-value">{{ order.title || order.categoryName || '工单详情' }}</text>
              </view>
              <view class="order-info">
                <text class="info-label">处理工程师：</text>
                <text class="info-value">{{ order.currentAssigneeName || '未填写' }}</text>
              </view>
              <view v-if="activeTab === 0" class="order-info">
                <text class="info-label">故障类型：</text>
                <text class="info-value">{{ order.categoryName || '未分类' }}</text>
              </view>

              <view v-if="activeTab === 0" class="evaluate-btn-container">
                <u-button
                  type="primary"
                  size="mini"
                  :custom-style="{ backgroundColor: '#FF9C07', borderColor: '#FF9C07' }"
                  @click.stop="openEvaluation(order)"
                >去评价</u-button>
              </view>

              <view v-else class="order-rating">
                <view class="rating-stars">
                  <u-icon
                    v-for="star in 5"
                    :key="star"
                    :name="star <= evaluationScore(order.evaluation) ? 'star-fill' : 'star'"
                    color="#FF9C07"
                    size="28rpx"
                  ></u-icon>
                </view>
                <text class="rating-text">{{ getRatingText(evaluationScore(order.evaluation)) }}</text>
              </view>
            </view>
          </view>
        </view>

        <view v-if="currentState.error && currentOrders.length" class="load-more-error">
          <text class="error-text">{{ currentState.error }}</text>
          <text class="retry-text" @click="retryLoadMore">点击重试</text>
        </view>
        <view v-else-if="currentState.loading && currentOrders.length" class="load-more-state">
          <text>加载中...</text>
        </view>
        <view v-else-if="currentOrders.length && !currentState.hasMore" class="load-more-state">
          <text>没有更多工单</text>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script>
import evaluationApi from '@/api/order/evaluate.js'

export default {
  data() {
    return {
      activeTab: 0,
      tabList: [
        { name: '待评价', status: 'COMPLETED' },
        { name: '已评价', status: 'CLOSED' }
      ],
      tabStates: [this.createTabState(), this.createTabState()],
      pageSize: 10,
      scrollTop: 0,
      hasShown: false
    }
  },

  computed: {
    currentState() {
      return this.tabStates[this.activeTab]
    },
    currentOrders() {
      return this.currentState.orders
    }
  },

  onShow() {
    // 从评价详情返回时重新读取两组真实状态，评价成功后卡片会自动换组。
    this.refreshAllTabs()
    this.hasShown = true
  },

  onPullDownRefresh() {
    this.refreshCurrentTab().finally(() => uni.stopPullDownRefresh())
  },

  methods: {
    createTabState() {
      return {
        orders: [],
        pageNum: 0,
        total: 0,
        loading: false,
        refreshing: false,
        error: '',
        hasMore: true
      }
    },

    patchTab(index, patch) {
      this.$set(this.tabStates, index, Object.assign({}, this.tabStates[index], patch))
    },

    getTabStyle(index) {
      const styles = [
        {
          backgroundColor: this.activeTab === 0 ? '#FFF7E8' : 'transparent',
          color: this.activeTab === 0 ? '#FF9C07' : '#86909C'
        },
        {
          backgroundColor: this.activeTab === 1 ? '#E8FFF3' : 'transparent',
          color: this.activeTab === 1 ? '#00B42A' : '#86909C'
        }
      ]
      return styles[index]
    },

    switchTab(index) {
      if (this.activeTab === index) return
      this.activeTab = index
      this.scrollTop = this.scrollTop === 0 ? 1 : 0
      if (!this.tabStates[index].orders.length && this.tabStates[index].hasMore) {
        this.loadTabPage(index, true)
      }
    },

    extractPage(response) {
      const payload = response && response.data && Array.isArray(response.data.rows)
        ? response.data
        : response
      if (!payload || !Array.isArray(payload.rows) || !Number.isFinite(Number(payload.total))) {
        throw new Error('评价工单分页响应缺少 rows/total')
      }
      return { rows: payload.rows, total: Number(payload.total) }
    },

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

    async attachEvaluations(rows) {
      return Promise.all(rows.map(async order => {
        const response = await evaluationApi.getEvaluation(order.id)
        return {
          ...order,
          evaluation: this.normalizeEvaluation(this.extractData(response))
        }
      }))
    },

    orderKey(order) {
      if (!order) return ''
      if (order.id !== undefined && order.id !== null) return `id:${order.id}`
      return `no:${order.orderNo || ''}`
    },

    mergeOrders(existing, incoming) {
      const merged = []
      const seen = new Set()
      existing.concat(incoming).forEach(order => {
        const key = this.orderKey(order)
        if (!key || seen.has(key)) return
        seen.add(key)
        merged.push(order)
      })
      return merged
    },

    async loadTabPage(index, reset = false) {
      const state = this.tabStates[index]
      if (!state || state.loading || (!reset && !state.hasMore)) return

      const pageNum = reset ? 1 : state.pageNum + 1
      this.patchTab(index, {
        loading: true,
        refreshing: reset && state.orders.length > 0,
        error: ''
      })

      try {
        const requestMethod = index === 0 ? 'getPendingEvaluationOrders' : 'getEvaluatedOrders'
        const response = await evaluationApi[requestMethod]({ pageNum, pageSize: this.pageSize })
        const page = this.extractPage(response)
        const rows = index === 1 ? await this.attachEvaluations(page.rows) : page.rows
        const orders = this.mergeOrders(reset ? [] : state.orders, rows)
        this.patchTab(index, {
          orders,
          pageNum,
          total: page.total,
          hasMore: orders.length < page.total,
          error: ''
        })
      } catch (error) {
        this.patchTab(index, {
          error: reset ? '评价工单加载失败，请重试' : '加载更多失败，请重试',
          hasMore: true
        })
      } finally {
        this.patchTab(index, { loading: false, refreshing: false })
      }
    },

    refreshAllTabs() {
      return Promise.all([this.loadTabPage(0, true), this.loadTabPage(1, true)])
    },

    refreshCurrentTab() {
      return this.loadTabPage(this.activeTab, true)
    },

    loadMore() {
      return this.loadTabPage(this.activeTab, false)
    },

    retryLoadMore() {
      return this.currentOrders.length ? this.loadMore() : this.refreshCurrentTab()
    },

    evaluationScore(evaluation) {
      return evaluation ? Number(evaluation.score || evaluation.overallScore) || 0 : 0
    },

    getRatingText(score) {
      return {
        1: '非常不满意',
        2: '不满意',
        3: '一般',
        4: '满意',
        5: '非常满意'
      }[score] || '查看评价'
    },

    orderTime(order) {
      if (this.activeTab === 1 && order.evaluation && order.evaluation.createTime) {
        return this.formatTime(order.evaluation.createTime)
      }
      return this.formatTime(order.finishedAt || order.updateTime)
    },

    formatTime(value) {
      if (!value) return '时间未知'
      return String(value).replace('T', ' ').slice(0, 16)
    },

    openEvaluation(order) {
      if (!order || order.id === undefined || order.id === null || order.id === '') {
        this.$u.toast('工单编号不存在')
        return
      }
      uni.navigateTo({
        url: `/pages/order/order-detail/order-evaluate?id=${encodeURIComponent(order.id)}`,
        fail: () => this.$u.toast('评价页面打开失败，请重试')
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.page-container {
  min-height: 100vh;
  background-color: #F5F7FA;
}

.status-tabs {
  display: flex;
  padding: 16rpx;
  border-bottom: 1rpx solid #F2F3F5;
  background-color: #FFFFFF;
}

.status-tab {
  display: flex;
  position: relative;
  flex: 1;
  height: 70rpx;
  align-items: center;
  justify-content: center;
  margin: 0 8rpx;
  border-radius: 35rpx;
  font-size: 28rpx;
  font-weight: 500;
}

.tab-text { margin-right: 8rpx; }

.tab-badge {
  min-width: 30rpx;
  height: 30rpx;
  padding: 0 6rpx;
  border-radius: 15rpx;
  color: #FFFFFF;
  background-color: #F53F3F;
  font-size: 20rpx;
  line-height: 30rpx;
  text-align: center;
  box-sizing: border-box;
}

.content-scroll { height: calc(100vh - 140rpx); }

.loading-container,
.error-card {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  margin: 200rpx 20rpx 0;
}

.custom-loading {
  width: 40rpx;
  height: 40rpx;
  margin-bottom: 20rpx;
  border: 4rpx solid rgba(54, 207, 201, 0.3);
  border-top-color: #36CFC9;
  border-radius: 50%;
  animation: spin 1s ease-in-out infinite;
}

.loading-text,
.load-more-state {
  color: #86909C;
  font-size: 26rpx;
}

@keyframes spin { to { transform: rotate(360deg); } }

.list-container { padding: 20rpx; }
.empty-tip { margin-top: 180rpx; }

.error-card {
  margin-top: 160rpx;
  padding: 34rpx;
  border-radius: 16rpx;
  background: #FFFFFF;
}

.error-text {
  margin: 18rpx 0;
  color: #F53F3F;
  font-size: 24rpx;
}

.order-card {
  position: relative;
  width: 100%;
  margin-bottom: 20rpx;
  overflow: hidden;
  border-radius: 16rpx;
  background-color: #FFFFFF;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
  box-sizing: border-box;
}

.order-card:active { transform: scale(0.99); }
.pending-card { border-left: 8rpx solid #FF9C07; }
.evaluated-card { border-left: 8rpx solid #00B42A; }

.status-bar {
  position: absolute;
  top: 0;
  left: 0;
  width: 8rpx;
  height: 100%;
}

.pending-bar { background-color: #FF9C07; }
.evaluated-bar { background-color: #00B42A; }
.order-content { padding: 20rpx 24rpx; }

.order-header,
.order-left,
.order-info,
.order-rating,
.rating-stars,
.evaluate-btn-container,
.load-more-error {
  display: flex;
  align-items: center;
}

.order-header { justify-content: space-between; margin-bottom: 20rpx; }
.order-left { min-width: 0; }

.order-no {
  max-width: 320rpx;
  margin-right: 15rpx;
  overflow: hidden;
  color: #303133;
  font-size: 28rpx;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-badge { flex-shrink: 0; }
.order-time { color: #909399; font-size: 24rpx; }

.order-info {
  padding: 15rpx 0;
  border-top: 1rpx solid #F2F3F5;
}

.info-label { width: 180rpx; color: #606266; font-size: 26rpx; }
.info-value { flex: 1; color: #303133; font-size: 26rpx; }

.evaluate-btn-container {
  justify-content: flex-end;
  margin-top: 10rpx;
  padding-top: 15rpx;
  border-top: 1rpx dashed #F2F3F5;
}

.order-rating {
  padding: 15rpx 0;
  border-top: 1rpx solid #F2F3F5;
}

.rating-stars { margin-right: 15rpx; }
.rating-text { color: #FF9C07; font-size: 26rpx; }

.load-more-state,
.load-more-error {
  justify-content: center;
  padding: 24rpx 0;
  text-align: center;
}

.retry-text { margin-left: 20rpx; color: #36CFC9; font-size: 24rpx; }
</style>

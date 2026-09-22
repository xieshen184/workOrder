<template>
  <view class="page-container">
    <!-- 顶部导航栏 -->
    <u-navbar
      title="工单管理"
      :is-back="true"
      background="#36CFC9"
      title-color="#ffffff"
      left-icon-color="#ffffff"
      :border-bottom="false"
    ></u-navbar>

    <!-- 状态标签切换 -->
    <view class="status-tabs">
      <view
        v-for="(tab, index) in tabList"
        :key="tab.name"
        class="status-tab"
        :class="{ active: activeTab === index }"
        :style="getTabStyle(index)"
        @click="switchTab(index)"
      >
        <text class="tab-text">{{ tab.name }}</text>
        <view class="tab-badge" v-if="tab.count > 0">{{ tab.count }}</view>
      </view>
    </view>

    <!-- 工单列表区域 -->
    <scroll-view
      class="content-scroll"
      scroll-y
      :scroll-top="scrollTop"
      scroll-with-animation
      :show-scrollbar="false"
      @scrolltolower="handleScrollToLower"
      ref="contentScroll"
    >
      <!-- 骨架屏加载状态 -->
      <view v-if="loading || (currentOrders.length === 0 && currentPage.loading)" class="skeleton-container">
        <view class="skeleton-item" v-for="i in 3" :key="i">
          <u-skeleton
            :rows="4"
            :title="true"
            :loading="true"
            row-height="40"
            title-width="60%"
            radius="16rpx"
            bg-color="#f5f5f5"
            active-color="#eeeeee"
            class="skeleton-loading"
          ></u-skeleton>
        </view>
      </view>

      <view v-else class="list-container">
        <!-- 网络错误不清空上一份成功数据，用户可主动重试。 -->
        <view v-if="currentError && currentOrders.length === 0" class="error-state">
          <u-icon name="info-circle" color="#F53F3F" size="38"></u-icon>
          <text class="error-text">{{ currentError }}</text>
          <u-button type="primary" size="mini" plain @click="retryCurrentTab">重试</u-button>
        </view>

        <u-empty
          v-else-if="currentOrders.length === 0"
          :text="emptyText"
          :mode="emptyMode"
          color="#C9CDD4"
          class="empty-tip"
        ></u-empty>

        <view v-else class="order-list">
          <view
            v-for="order in currentOrders"
            :key="order.id"
            class="order-card"
            :class="cardClass(order)"
            @click="toOrderDetail(order.id)"
          >
            <view class="status-bar" :class="barClass(order)"></view>
            <view class="order-content">
              <view class="order-header">
                <view class="order-left">
                  <text class="order-no">工单#{{ order.orderNo || order.id }}</text>
                  <u-badge
                    :text="urgencyText(order.urgencyLevel)"
                    :type="getUrgencyType(order.urgencyLevel)"
                    class="urgency-badge"
                  ></u-badge>
                </view>
                <text class="order-time">{{ order.submittedAt || order.createTime || '—' }}</text>
              </view>

              <view class="order-info">
                <text class="info-label">申请人：</text>
                <text class="info-value">{{ order.applicantName || order.applicant || '—' }}</text>
              </view>

              <view class="order-info">
                <text class="info-label">科室：</text>
                <text class="info-value">{{ order.applicantDeptName || order.department || '—' }}</text>
              </view>

              <view class="order-info">
                <text class="info-label">故障类型：</text>
                <text class="info-value">{{ order.categoryName || order.category || '—' }}</text>
              </view>
            </view>
          </view>
        </view>

        <view v-if="currentOrders.length > 0 && currentPage.loading" class="load-more-state">
          <u-loading-icon mode="circle" color="#86909C" size="28"></u-loading-icon>
          <text class="load-more-text">加载更多工单...</text>
        </view>

        <view v-else-if="currentOrders.length > 0 && currentPage.error" class="load-more-state">
          <text class="error-text">{{ currentPage.error }}</text>
          <u-button type="primary" size="mini" plain @click="retryCurrentTab">重试</u-button>
        </view>

        <view v-else-if="currentOrders.length > 0 && !currentPage.hasMore" class="load-more-end">
          <text>已是全部工单</text>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script>
import orderApi from '@/api/order/handle.js'

const TAB_STATUSES = [
  ['WAIT_ACCEPT'],
  ['ACCEPTED', 'PROCESSING'],
  ['WAIT_CONFIRM', 'COMPLETED', 'CLOSED']
]

export default {
  data() {
    return {
      activeTab: 0,
      tabList: [
        { name: '待接单', count: 0 },
        { name: '执行中', count: 0 },
        { name: '已完成', count: 0 }
      ],
      pendingOrders: [],
      processingOrders: [],
      completedOrders: [],
      pagination: [
        { pageNum: 0, total: 0, hasMore: true, loading: false, error: '', initialized: false },
        { pageNum: 0, total: 0, hasMore: true, loading: false, error: '', initialized: false },
        { pageNum: 0, total: 0, hasMore: true, loading: false, error: '', initialized: false }
      ],
      loading: true,
      hasLoadedOnce: false,
      scrollTop: 0,
      pageSize: 10,
      refreshHandler: null
    }
  },

  computed: {
    currentOrders() {
      return [this.pendingOrders, this.processingOrders, this.completedOrders][this.activeTab] || []
    },

    currentPage() {
      return this.pagination[this.activeTab] || {
        pageNum: 0,
        total: 0,
        hasMore: false,
        loading: false,
        error: '',
        initialized: false
      }
    },

    currentError() {
      return this.currentPage.error || ''
    },

    emptyText() {
      return ['暂无待接工单', '暂无执行中工单', '暂无已完成工单'][this.activeTab]
    },

    emptyMode() {
      return ['order', 'search', 'success'][this.activeTab]
    }
  },

  async created() {
    await this.loadOrders()
    this.refreshHandler = () => this.loadOrders()
    uni.$on('refreshOrderList', this.refreshHandler)
  },

  onUnload() {
    if (this.refreshHandler) uni.$off('refreshOrderList', this.refreshHandler)
  },

  async onPullDownRefresh() {
    try {
      await this.loadOrders()
    } finally {
      uni.stopPullDownRefresh()
    }
  },

  methods: {
    getTabStyle(index) {
      const styles = [
        {
          backgroundColor: this.activeTab === 0 ? '#E8F3FF' : 'transparent',
          color: this.activeTab === 0 ? '#36CFC9' : '#86909C'
        },
        {
          backgroundColor: this.activeTab === 1 ? '#FFF7E8' : 'transparent',
          color: this.activeTab === 1 ? '#FF9C07' : '#86909C'
        },
        {
          backgroundColor: this.activeTab === 2 ? '#E8FFF3' : 'transparent',
          color: this.activeTab === 2 ? '#00B42A' : '#86909C'
        }
      ]
      return styles[index]
    },

    switchTab(index) {
      if (this.activeTab !== index) {
        this.activeTab = index
        this.scrollTop = this.scrollTop === 0 ? 1 : 0
      }
    },

    urgencyText(level) {
      return { 1: '一般', 2: '紧急', 3: '特急' }[level] || '—'
    },

    getUrgencyType(level) {
      return { 1: 'info', 2: 'warning', 3: 'error' }[level] || 'info'
    },

    cardClass(order) {
      if (this.activeTab === 0) return 'pending-card'
      if (this.activeTab === 1) return 'processing-card'
      return 'completed-card'
    },

    barClass(order) {
      if (this.activeTab === 0) return 'pending-bar'
      if (this.activeTab === 1) return 'processing-bar'
      return 'completed-bar'
    },

    extractRows(response) {
      const payload = response && response.data !== undefined && !Array.isArray(response.data)
        ? response.data
        : response
      if (!payload) return { rows: [], total: 0 }
      const rows = Array.isArray(payload.rows)
        ? payload.rows
        : Array.isArray(payload.list)
          ? payload.list
          : Array.isArray(payload.records)
            ? payload.records
            : Array.isArray(payload.data)
              ? payload.data
              : []
      return {
        rows,
        total: Number(payload.total === undefined ? rows.length : payload.total)
      }
    },

    async loadStatusGroup(statuses, pageNum) {
      const results = await Promise.all(statuses.map(status => orderApi.getEngineerOrders({
        pageNum,
        pageSize: this.pageSize,
        status
      }).then(response => ({ ok: true, response })).catch(error => ({ ok: false, error }))))

      return results.reduce((result, item) => {
        if (!item.ok) {
          result.failedCount += 1
          return result
        }
        const page = this.extractRows(item.response)
        result.rows = result.rows.concat(page.rows)
        result.total += page.total
        return result
      }, { rows: [], total: 0, failedCount: 0 })
    },

    ordersForTab(index) {
      return [this.pendingOrders, this.processingOrders, this.completedOrders][index] || []
    },

    setTabOrders(index, orders) {
      const keys = ['pendingOrders', 'processingOrders', 'completedOrders']
      this.$set(this, keys[index], orders)
    },

    setPaginationState(index, patch) {
      this.$set(this.pagination, index, Object.assign({}, this.pagination[index], patch))
    },

    orderKey(order) {
      if (!order) return ''
      if (order.id !== undefined && order.id !== null) return `id:${order.id}`
      if (order.orderId !== undefined && order.orderId !== null) return `orderId:${order.orderId}`
      if (order.orderNo !== undefined && order.orderNo !== null) return `orderNo:${order.orderNo}`
      try {
        return JSON.stringify(order) || String(order)
      } catch (error) {
        return String(order)
      }
    },

    mergeOrders(existing, incoming) {
      // Keep pagination appends idempotent when status pages overlap or a failed page is retried.
      const merged = []
      const seen = new Set()
      existing.concat(incoming).forEach(order => {
        const key = this.orderKey(order)
        if (seen.has(key)) return
        seen.add(key)
        merged.push(order)
      })
      return merged
    },

    async loadTabPage(index, reset = false) {
      const state = this.pagination[index]
      if (!state || state.loading) return

      const targetPage = reset ? 1 : state.pageNum + 1
      const existingRows = this.ordersForTab(index)
      if (reset) {
        // Keep old rows visible during refresh; replace them only after a complete page succeeds.
        this.setPaginationState(index, { pageNum: 0, hasMore: true, error: '', loading: true })
      } else {
        this.setPaginationState(index, { error: '', loading: true })
      }

      try {
        const group = await this.loadStatusGroup(TAB_STATUSES[index], targetPage)
        const complete = group.failedCount === 0
        const rows = this.mergeOrders(complete && reset ? [] : existingRows, group.rows)
        const currentState = this.pagination[index]
        const total = complete
          ? group.total
          : Math.max(currentState.total, group.total, rows.length)

        this.setTabOrders(index, rows)
        this.setPaginationState(index, {
          pageNum: complete ? targetPage : (reset ? 0 : currentState.pageNum),
          total,
          hasMore: !complete || rows.length < total,
          error: complete ? '' : '工单列表加载失败，请重试',
          initialized: true
        })
        this.$set(this.tabList[index], 'count', total)
      } catch (error) {
        // An unexpected parser/state error must leave the tab rows intact and retryable.
        console.error('加载工单分页失败:', error)
        this.setPaginationState(index, {
          error: '工单列表加载失败，请重试',
          hasMore: true,
          initialized: true
        })
      } finally {
        this.setPaginationState(index, { loading: false })
      }
    },

    async loadOrders() {
      const firstLoad = !this.hasLoadedOnce
      if (firstLoad) this.loading = true
      try {
        // Each tab owns its request/error state, so one rejected status cannot clear other groups.
        await Promise.all(TAB_STATUSES.map((statuses, index) => this.loadTabPage(index, true)))
      } finally {
        this.hasLoadedOnce = true
        this.loading = false
      }
    },

    handleScrollToLower() {
      this.loadMore(this.activeTab)
    },

    loadMore(index = this.activeTab) {
      const state = this.pagination[index]
      if (!state || state.loading || !state.hasMore) return
      return this.loadTabPage(index)
    },

    retryCurrentTab() {
      return this.loadMore(this.activeTab)
    },

    toOrderDetail(orderId) {
      if (orderId === undefined || orderId === null || orderId === '') {
        this.$u.toast('工单编号不存在')
        return
      }
      uni.navigateTo({
        url: `/pages/order/order-detail/order-handel?id=${encodeURIComponent(orderId)}`,
        fail: () => this.$u.toast('详情页打开失败，请重试')
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

/* 状态标签样式 */
.status-tabs {
  display: flex;
  padding: 16rpx;
  background-color: #ffffff;
  border-bottom: 1rpx solid #F2F3F5;
}

.status-tab {
  flex: 1;
  height: 70rpx;
  border-radius: 35rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 8rpx;
  font-size: 28rpx;
  font-weight: 500;
  position: relative;
  transition: all 0.3s ease;
}

.tab-text {
  margin-right: 8rpx;
}

.tab-badge {
  position: absolute;
  right: 25%;
  top: 10rpx;
  background-color: #F53F3F;
  color: #ffffff;
  font-size: 20rpx;
  width: 30rpx;
  height: 30rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 内容滚动区域 */
.content-scroll {
  height: calc(100vh - 180rpx);
}

/* 骨架屏样式 */
.skeleton-container {
  padding: 20rpx;
}

.skeleton-item {
  margin-bottom: 20rpx;
}

.skeleton-loading {
  width: 100%;
}

.list-container {
  padding: 20rpx;
}

/* 工单列表 */
.order-list {
  width: 100%;
}

/* 工单卡片样式 - 优化点击区域 */
.order-card {
  margin-bottom: 20rpx;
  border-radius: 16rpx;
  background-color: #ffffff;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
  overflow: hidden;
  position: relative;
  width: 100%;
  cursor: pointer;
  /* 添加点击反馈 */
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.order-card:active {
  transform: scale(0.99);
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.03);
}

/* 状态标识条 */
.status-bar {
  position: absolute;
  top: 0;
  left: 0;
  width: 8rpx;
  height: 100%;
}

.pending-bar {
  background-color: #36CFC9;
}

.processing-bar {
  background-color: #FF9C07;
}

.completed-bar {
  background-color: #00B42A;
}

/* 不同状态卡片样式 */
.pending-card {
  border-left: 8rpx solid #36CFC9;
}

.processing-card {
  border-left: 8rpx solid #FF9C07;
}

.completed-card {
  border-left: 8rpx solid #00B42A;
}

.order-content {
  padding: 20rpx 24rpx;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}

.order-left {
  display: flex;
  align-items: center;
}

.order-no {
  font-size: 28rpx;
  color: #303133;
  margin-right: 15rpx;
  font-weight: 500;
}

.urgency-badge {
  height: 36rpx;
  line-height: 36rpx;
}

.order-time {
  font-size: 24rpx;
  color: #909399;
}

.order-info {
  display: flex;
  padding: 15rpx 0;
  border-top: 1rpx solid #F2F3F5;
}

.order-info:first-child {
  border-top: none;
}

.info-label {
  font-size: 26rpx;
  color: #606266;
  width: 140rpx;
}

.info-value {
  font-size: 26rpx;
  color: #303133;
  flex: 1;
}

.empty-tip {
  margin-top: 200rpx;
}

.error-state {
  margin-top: 160rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.error-text {
  color: #606266;
  font-size: 26rpx;
  margin: 20rpx 0;
}

.load-more-state,
.load-more-end {
  min-height: 72rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #86909C;
  font-size: 24rpx;
}

.load-more-state .error-text {
  margin: 0 20rpx 0 0;
}

.load-more-text {
  margin-left: 12rpx;
}
</style>

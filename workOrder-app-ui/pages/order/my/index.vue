<template>
  <view class="page-container">
    <view class="page-header">
      <view>
        <text class="page-title">我的工单</text>
        <text class="page-subtitle">查看本人提交的工单进度</text>
      </view>
      <u-icon name="reload" color="#36CFC9" size="34" @click="refreshOrders"></u-icon>
    </view>

    <scroll-view
      class="page-scroll"
      scroll-y
      :refresher-enabled="true"
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresherRefresh"
      @scrolltolower="loadMore"
    >
      <view v-if="loading && !orders.length" class="state-card">
        <u-loading-icon mode="circle" color="#36CFC9" size="36"></u-loading-icon>
        <text class="state-text">工单加载中</text>
      </view>

      <view v-else-if="error && !orders.length" class="state-card">
        <u-icon name="info-circle" color="#F53F3F" size="44"></u-icon>
        <text class="state-text error-text">{{ error }}</text>
        <u-button type="primary" size="mini" plain @click="retryLoad">重试</u-button>
      </view>

      <view v-else-if="!orders.length" class="state-card">
        <u-icon name="order" color="#C9CDD4" size="56"></u-icon>
        <text class="state-text">暂无工单</text>
        <text class="state-hint">提交工单后，记录会显示在这里</text>
      </view>

      <view v-else class="list-content">
        <view v-if="error" class="inline-error">
          <text class="inline-error-text">{{ error }}</text>
          <u-button type="primary" size="mini" plain @click="retryLoad">重试</u-button>
        </view>

        <view
          v-for="item in orders"
          :key="item.id"
          class="order-card"
          @click="openDetail(item)"
        >
          <view class="order-card-header">
            <text class="order-no">{{ item.orderNo || `工单 #${item.id}` }}</text>
            <text class="status-tag" :class="statusClass(item.status)">{{ statusText(item.status) }}</text>
          </view>
          <text class="order-title">{{ item.title || item.categoryName || '未命名工单' }}</text>
          <view class="order-meta">
            <text class="meta-item">{{ item.categoryName || '未分类' }}</text>
            <text class="meta-item">{{ item.location || '未填写位置' }}</text>
          </view>
          <view class="order-card-footer">
            <text class="order-time">提交于 {{ formatTime(item.submittedAt || item.createTime) }}</text>
            <u-icon name="arrow-right" color="#C9CDD4" size="26"></u-icon>
          </view>
        </view>

        <view v-if="loadMoreError" class="load-more-error">
          <text class="load-more-error-text">{{ loadMoreError }}</text>
          <u-button type="primary" size="mini" plain @click="retryLoadMore">重试</u-button>
        </view>
        <u-loadmore :status="loadMoreStatus" :load-text="loadText"></u-loadmore>
      </view>
    </scroll-view>
  </view>
</template>

<script>
import { getMyOrders } from '@/api/order/core'

export default {
  data() {
    return {
      pageSize: 10,
      pageNum: 1,
      total: 0,
      filterStatus: '',
      orders: [],
      loading: false,
      loadingMore: false,
      refreshing: false,
      error: '',
      loadMoreError: '',
      loadText: {
        loadmore: '上拉加载更多',
        loading: '加载中',
        nomore: '没有更多工单'
      }
    }
  },

  computed: {
    hasMore() {
      return this.orders.length < this.total
    },

    loadMoreStatus() {
      if (this.loadingMore) return 'loading'
      return this.hasMore ? 'loadmore' : 'nomore'
    }
  },

  onLoad(options) {
    this.filterStatus = options && options.status ? options.status : ''
    this.loadOrders(true)
  },

  onPullDownRefresh() {
    this.refreshOrders()
  },

  methods: {
    async refreshOrders() {
      if (this.refreshing) return
      // 刷新不先清空旧列表；请求失败时继续保留旧数据，并给出可重试入口。
      this.refreshing = true
      await this.loadOrders(true)
    },

    onRefresherRefresh() {
      this.refreshOrders()
    },

    loadMore() {
      this.loadOrders(false)
    },

    async loadOrders(reset = false) {
      if (reset) {
        if (this.loading && !this.orders.length) {
          this.refreshing = false
          return
        }
        this.pageNum = 1
        this.error = ''
        this.loadMoreError = ''
        this.loading = !this.orders.length
      } else {
        if (this.loading || this.loadingMore || this.refreshing || !this.hasMore) return
        this.loadingMore = true
        this.loadMoreError = ''
      }

      const requestPage = reset ? 1 : this.pageNum + 1
      try {
        const response = await getMyOrders({
          pageNum: requestPage,
          pageSize: this.pageSize,
          status: this.filterStatus
        })
        // TableDataInfo 直接返回 rows/total；兼容请求层仍包一层 data 的环境，但不制造本地数据。
        const result = response && response.data && Array.isArray(response.data.rows)
          ? response.data
          : response
        if (!result || !Array.isArray(result.rows) || result.total === undefined
          || !Number.isFinite(Number(result.total))) {
          throw new Error('我的工单响应缺少 rows/total')
        }
        const rows = result.rows
        this.orders = reset ? rows : this.orders.concat(rows)
        this.total = Number(result.total)
        this.pageNum = requestPage
      } catch (requestError) {
        if (reset) {
          // 首次加载失败展示错误态；刷新失败不清空已有列表，避免假装刷新成功。
          this.error = '我的工单加载失败，请重试'
        } else {
          this.loadMoreError = '加载更多失败，请重试'
        }
      } finally {
        if (reset) {
          this.loading = false
          this.refreshing = false
          uni.stopPullDownRefresh()
        } else {
          this.loadingMore = false
        }
      }
    },

    retryLoad() {
      this.refreshOrders()
    },

    retryLoadMore() {
      this.loadOrders(false)
    },

    openDetail(item) {
      if (!item || item.id === undefined || item.id === null || item.id === '') {
        this.$u.toast('工单缺少有效编号')
        return
      }
      uni.navigateTo({
        url: `/pages/order/detail/index?id=${encodeURIComponent(item.id)}`
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

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10rpx 8rpx 28rpx;
}

.page-title {
  display: block;
  color: #1D2129;
  font-size: 38rpx;
  font-weight: 600;
}

.page-subtitle {
  display: block;
  margin-top: 8rpx;
  color: #86909C;
  font-size: 24rpx;
}

.page-scroll {
  height: calc(100vh - 130rpx);
}

.state-card,
.order-card,
.inline-error,
.load-more-error {
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
  margin-top: 20rpx;
  color: #4E5969;
  font-size: 28rpx;
}

.state-hint {
  margin-top: 12rpx;
  color: #86909C;
  font-size: 24rpx;
}

.error-text,
.inline-error-text,
.load-more-error-text {
  color: #F53F3F;
}

.list-content {
  padding-bottom: 32rpx;
}

.order-card {
  margin-bottom: 20rpx;
  padding: 26rpx;
}

.order-card-header,
.order-card-footer,
.order-meta,
.inline-error,
.load-more-error {
  display: flex;
  align-items: center;
}

.order-card-header,
.order-card-footer,
.inline-error,
.load-more-error {
  justify-content: space-between;
}

.order-no {
  color: #4E5969;
  font-size: 24rpx;
}

.status-tag {
  padding: 6rpx 14rpx;
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

.order-title {
  display: block;
  margin-top: 22rpx;
  color: #1D2129;
  font-size: 32rpx;
  font-weight: 600;
}

.order-meta {
  margin-top: 18rpx;
}

.meta-item {
  max-width: 48%;
  margin-right: 22rpx;
  overflow: hidden;
  color: #86909C;
  font-size: 24rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-card-footer {
  margin-top: 24rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid #F2F3F5;
}

.order-time {
  color: #86909C;
  font-size: 22rpx;
}

.inline-error,
.load-more-error {
  margin-bottom: 20rpx;
  padding: 18rpx 22rpx;
}

.inline-error-text,
.load-more-error-text {
  margin-right: 18rpx;
  font-size: 24rpx;
}
</style>

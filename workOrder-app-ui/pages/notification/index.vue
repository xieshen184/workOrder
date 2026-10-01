<template>
  <view class="page-container">
    <u-navbar
      title="消息中心"
      :auto-back="false"
      background="#FFFFFF"
      title-color="#273444"
      left-icon-color="#445466"
      :border-bottom="false"
      @leftClick="backToPreviousPage"
    >
      <view slot="right" class="mark-all-action" :class="{ disabled: markAllLoading }" @click="markAllRead">
        {{ markAllLoading ? '处理中' : '全部已读' }}
      </view>
    </u-navbar>

    <view class="category-tabs">
      <view
        v-for="(tab, index) in tabs"
        :key="tab.value || 'all'"
        class="category-tab"
        :class="{ active: activeTab === index }"
        @click="switchTab(index)"
      >
        <text class="category-tab-label">{{ tab.label }}</text>
        <text class="category-tab-count" :class="{ empty: tabUnreadCount(tab) === 0 }">
          {{ tabUnreadCount(tab) }}
        </text>
      </view>
    </view>

    <scroll-view
      class="content-scroll"
      scroll-y
      :refresher-enabled="true"
      :refresher-triggered="currentState.refreshing"
      :show-scrollbar="false"
      @refresherrefresh="onRefresherRefresh"
      @scrolltolower="loadMore"
    >
      <view v-if="currentState.loading && !currentNotifications.length" class="state-card">
        <u-loading-icon mode="circle" color="#36CFC9" size="36"></u-loading-icon>
        <text class="state-text">消息加载中</text>
      </view>

      <view v-else-if="currentState.error && !currentNotifications.length" class="state-card">
        <u-icon name="info-circle" color="#F53F3F" size="44"></u-icon>
        <text class="state-text error-text">{{ currentState.error }}</text>
        <u-button type="primary" size="mini" plain @click="retryLoad">重试</u-button>
      </view>

      <view v-else-if="!currentNotifications.length" class="state-card">
        <u-icon name="chat" color="#C9CDD4" size="56"></u-icon>
        <text class="state-text">暂无消息</text>
        <text class="state-hint">当前分段没有可查看的消息</text>
      </view>

      <view v-else class="list-content">
        <view v-if="currentState.error" class="inline-error">
          <text class="inline-error-text">{{ currentState.error }}</text>
          <u-button type="primary" size="mini" plain @click="retryLoad">重试</u-button>
        </view>

        <u-swipe-action>
        <u-swipe-action-item
          v-for="item in currentNotifications"
          :key="item.id"
          :options="swipeReadOptions"
          :disabled="!isUnread(item)"
          @click="markFromSwipe($event, item)"
        >
        <view
          class="notification-card"
          :class="{ unread: isUnread(item) }"
          @click="openNotification(item)"
        >
          <view class="notification-dot" :class="categoryClass(item.category)"></view>
          <view class="notification-body">
            <view class="notification-title-row">
              <text class="notification-title">{{ item.title || '未命名消息' }}</text>
              <text class="notification-category">{{ categoryText(item.category) }}</text>
            </view>
            <text class="notification-content">{{ item.content || '暂无内容' }}</text>
            <text class="notification-time">{{ formatTime(item.createTime) }}</text>
          </view>
          <u-icon name="arrow-right" color="#C9CDD4" size="26"></u-icon>
        </view>
        </u-swipe-action-item>
        </u-swipe-action>

        <view v-if="currentState.loadMoreError" class="load-more-error">
          <text class="load-more-error-text">{{ currentState.loadMoreError }}</text>
          <u-button type="primary" size="mini" plain @click="retryLoadMore">重试</u-button>
        </view>
        <u-loadmore :status="loadMoreStatus" :load-text="loadText"></u-loadmore>
      </view>
    </scroll-view>
  </view>
</template>

<script>
import {
  getNotifications,
  getUnreadNotificationCount,
  markAllNotificationsRead,
  markNotificationRead
} from '@/api/notification'

const TAB_LIST = [
  { label: '全部', value: '', countKey: 'total' },
  { label: '工单', value: 'WORK_ORDER', countKey: 'workOrder' },
  { label: '审批', value: 'APPROVAL', countKey: 'approval' },
  { label: '系统', value: 'SYSTEM', countKey: 'system' }
]

function createTabState(category) {
  return {
    category,
    pageNum: 0,
    total: 0,
    notifications: [],
    loading: false,
    loadingMore: false,
    refreshing: false,
    error: '',
    loadMoreError: ''
  }
}

export default {
  data() {
    return {
      tabs: TAB_LIST,
      activeTab: 0,
      pageSize: 10,
      tabStates: TAB_LIST.map(tab => createTabState(tab.value)),
      unreadCounts: {
        total: 0,
        workOrder: 0,
        approval: 0,
        system: 0
      },
      unreadLoading: false,
      markAllLoading: false,
      openingKey: '',
      swipeReadOptions: [{
        text: '标为已读',
        style: { backgroundColor: '#27BDB5', color: '#FFFFFF', fontSize: '13px' }
      }],
      loadText: {
        loadmore: '上拉加载更多',
        loading: '加载中',
        nomore: '没有更多消息'
      }
    }
  },

  computed: {
    currentState() {
      return this.tabStates[this.activeTab] || this.tabStates[0]
    },

    currentNotifications() {
      return this.currentState.notifications
    },

    hasMore() {
      return this.currentState.notifications.length < this.currentState.total
    },

    loadMoreStatus() {
      if (this.currentState.loadingMore) return 'loading'
      return this.hasMore ? 'loadmore' : 'nomore'
    },

    activeCategory() {
      return this.tabs[this.activeTab] ? this.tabs[this.activeTab].value : ''
    }
  },

  onLoad() {
    this.loadUnreadCounts()
    this.loadTab(this.activeTab, true)
  },

  onPullDownRefresh() {
    this.refreshCurrentTab().finally(() => uni.stopPullDownRefresh())
  },

  methods: {
    // 调试器可能将消息中心作为页面栈唯一页面打开；此时 navigateBack 无效，
    // 统一退回首页，正常从上一页进入时仍按页面栈返回。
    backToPreviousPage() {
      if (getCurrentPages().length > 1) {
        uni.navigateBack()
        return
      }
      uni.switchTab({ url: '/pages/index' })
    },

    switchTab(index) {
      if (index === this.activeTab) return
      this.activeTab = index
      const state = this.tabStates[index]
      if (state && state.pageNum === 0 && !state.loading) this.loadTab(index, true, false)
    },

    onRefresherRefresh() {
      this.refreshCurrentTab()
    },

    refreshCurrentTab() {
      return Promise.all([
        this.loadTab(this.activeTab, true, true),
        this.loadUnreadCounts()
      ])
    },

    loadMore() {
      this.loadTab(this.activeTab, false)
    },

    retryLoad() {
      this.refreshCurrentTab()
    },

    retryLoadMore() {
      this.loadTab(this.activeTab, false)
    },

    tabUnreadCount(tab) {
      const count = Number(this.unreadCounts[tab.countKey])
      return Number.isFinite(count) && count >= 0 ? count : 0
    },

    async loadUnreadCounts() {
      if (this.unreadLoading) return
      this.unreadLoading = true
      try {
        const response = await getUnreadNotificationCount()
        const result = response && response.data !== undefined ? response.data : response
        if (!result || result.total === undefined || !Number.isFinite(Number(result.total))) {
          throw new Error('未读消息数量响应缺少 total')
        }
        this.unreadCounts = {
          total: Math.max(0, Number(result.total)),
          workOrder: Math.max(0, Number(result.workOrder || 0)),
          approval: Math.max(0, Number(result.approval || 0)),
          system: Math.max(0, Number(result.system || 0))
        }
      } catch (requestError) {
        // 未读数失败时保留上次成功值，列表仍可继续使用。
      } finally {
        this.unreadLoading = false
      }
    },

    async loadTab(index, reset, manualRefresh) {
      const state = this.tabStates[index]
      if (!state) return

      if (reset) {
        if (state.loading || state.loadingMore || state.refreshing) return
        state.error = ''
        state.loadMoreError = ''
        state.refreshing = Boolean(manualRefresh) || state.notifications.length > 0
        state.loading = state.notifications.length === 0
      } else {
        if (state.loading || state.loadingMore || state.refreshing
          || (!state.notifications.length && state.pageNum > 0)) return
        if (state.pageNum > 0 && state.notifications.length >= state.total) return
        state.loadMoreError = ''
        state.loadingMore = true
      }

      const requestPage = reset ? 1 : state.pageNum + 1
      try {
        const response = await getNotifications({
          pageNum: requestPage,
          pageSize: this.pageSize,
          category: state.category
        })
        const result = this.normalizePage(response)
        // 分页只合并当前成功页，并按消息 id 去重，避免触底重复触发或边界数据变化造成重复卡片。
        state.notifications = this.mergePageRows(state.notifications, result.rows, reset)
        state.total = result.total
        state.pageNum = requestPage
      } catch (requestError) {
        if (reset) {
          // 刷新失败不清空已有消息；只有没有旧数据时才展示整页失败态。
          state.error = '消息加载失败，请重试'
        } else {
          state.loadMoreError = '加载更多失败，请重试'
        }
      } finally {
        state.loading = false
        state.loadingMore = false
        state.refreshing = false
      }
    },

    normalizePage(response) {
      const result = response && response.data && Array.isArray(response.data.rows)
        ? response.data
        : response
      if (!result || !Array.isArray(result.rows) || result.total === undefined
        || !Number.isFinite(Number(result.total))) {
        throw new Error('消息分页响应缺少 rows/total')
      }
      return {
        rows: result.rows,
        total: Number(result.total)
      }
    },

    mergePageRows(existingRows, incomingRows, reset) {
      if (reset) return incomingRows
      const seen = {}
      existingRows.forEach(item => {
        if (item && item.id !== undefined && item.id !== null) seen[String(item.id)] = true
      })
      return existingRows.concat(incomingRows.filter(item => {
        if (!item || item.id === undefined || item.id === null) return true
        const key = String(item.id)
        if (seen[key]) return false
        seen[key] = true
        return true
      }))
    },

    async markAllRead() {
      if (this.markAllLoading) return
      this.markAllLoading = true
      const category = this.activeCategory
      try {
        // “全部”才省略 category；其它分段只读当前分类，避免误清理其它类型的未读消息。
        await markAllNotificationsRead(category || undefined)
        this.tabStates.forEach(state => {
          state.notifications.forEach(item => {
            if (!category || state.category === category || String(item.category || '').toUpperCase() === category) {
              this.setReadLocally(item)
            }
          })
        })
        await this.loadUnreadCounts()
        this.$u.toast('已全部标记为已读')
      } catch (requestError) {
        this.$u.toast('全部已读失败，请重试')
      } finally {
        this.markAllLoading = false
      }
    },

    async markAsRead(item) {
      let marked = false
      try {
        await markNotificationRead(item.id)
        this.setReadLocally(item)
        marked = true
      } catch (requestError) {
        this.$u.toast('消息已打开，但标记已读失败，请稍后重试')
      } finally {
        this.openingKey = ''
      }
      if (marked) await this.loadUnreadCounts()
    },

    markFromSwipe(event, item) {
      if (!event || event.index !== 0 || !this.isUnread(item)) return
      this.markAsRead(item)
    },

    setReadLocally(item) {
      if (!item) return
      const readAt = new Date().toISOString()
      const notificationId = item.id
      this.tabStates.forEach(state => {
        state.notifications.forEach(loadedItem => {
          const sameItem = loadedItem === item
            || (notificationId !== undefined && notificationId !== null
              && String(loadedItem.id) === String(notificationId))
          if (sameItem) {
            this.$set(loadedItem, 'readFlag', '1')
            this.$set(loadedItem, 'readAt', readAt)
          }
        })
      })
    },

    openNotification(item) {
      if (!item || this.openingKey || this.markAllLoading) return
      const url = this.resolveNotificationUrl(item)
      if (!url) {
        this.$u.toast('消息关联页面暂不可用')
        return
      }

      const openingKey = item.id === undefined || item.id === null ? url : String(item.id)
      this.openingKey = openingKey
      try {
        uni.navigateTo({
          url,
          success: () => {
            // 只有导航成功后才调用已读接口；导航失败留在列表且不改变未读状态。
            if (this.isUnread(item)) this.markAsRead(item)
            else this.openingKey = ''
          },
          fail: () => {
            this.openingKey = ''
            this.$u.toast('消息详情打开失败，请稍后重试')
          }
        })
      } catch (error) {
        this.openingKey = ''
        this.$u.toast('消息详情打开失败，请稍后重试')
      }
    },

    resolveNotificationUrl(item) {
      const routeInfo = this.splitRoutePath(item.routePath)
      const params = Object.assign({}, routeInfo.params, this.parseRouteParams(item.routeParams))
      const businessId = this.firstValue(
        params.id,
        params.orderId,
        item.businessId,
        item.orderNo
      )
      const isWorkOrder = this.isWorkOrderNotification(item)
      let path = this.normalizeRoutePath(routeInfo.path)

      // 路由已失效时，工单消息仍回到只读工单详情；详情接口失败也由详情页展示失败态，不制造死链。
      if (!path && isWorkOrder) path = '/pages/order/detail/index'
      if (isWorkOrder && path && path.indexOf('/pages/order/') !== 0) path = '/pages/order/detail/index'
      if (!path) return ''
      if (path.indexOf('/pages/order/') === 0 && this.firstValue(params.id) === undefined && businessId !== undefined) {
        params.id = businessId
      }
      if (path === '/pages/order/detail/index' && this.firstValue(params.id) === undefined) return ''
      return this.appendQuery(path, params)
    },

    splitRoutePath(routePath) {
      const raw = String(routePath || '').trim()
      const queryIndex = raw.indexOf('?')
      if (queryIndex < 0) return { path: raw, params: {} }
      return {
        path: raw.slice(0, queryIndex),
        params: this.parseRouteParams(raw.slice(queryIndex + 1))
      }
    },

    parseRouteParams(value) {
      if (!value) return {}
      if (typeof value === 'object' && !Array.isArray(value)) return Object.assign({}, value)
      let raw = String(value).trim().replace(/^\?/, '')
      if (!raw) return {}
      try {
        const parsed = JSON.parse(raw)
        if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) return parsed
      } catch (error) {
        try {
          const decoded = decodeURIComponent(raw)
          const parsed = JSON.parse(decoded)
          if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) return parsed
        } catch (decodeError) {
          // 非 JSON 参数按普通 query 解析，保持旧消息的跳转兼容性。
        }
      }
      return raw.split('&').reduce((params, pair) => {
        if (!pair) return params
        const separator = pair.indexOf('=')
        const key = separator < 0 ? pair : pair.slice(0, separator)
        const valuePart = separator < 0 ? '' : pair.slice(separator + 1)
        if (!key) return params
        params[this.decodeQueryPart(key)] = this.decodeQueryPart(valuePart)
        return params
      }, {})
    },

    normalizeRoutePath(routePath) {
      const raw = String(routePath || '').trim()
      if (!raw || /^https?:\/\//i.test(raw)) return ''
      const path = `/${raw.replace(/^\/+/, '')}`
      if (path.indexOf('/pages/') !== 0) return ''
      return path
    },

    appendQuery(path, params) {
      const query = Object.keys(params || {}).filter(key => {
        const value = params[key]
        return value !== undefined && value !== null && value !== ''
      }).map(key => {
        const value = typeof params[key] === 'object' ? JSON.stringify(params[key]) : params[key]
        return `${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`
      })
      return query.length ? `${path}?${query.join('&')}` : path
    },

    decodeQueryPart(value) {
      try {
        return decodeURIComponent(value.replace(/\+/g, ' '))
      } catch (error) {
        return value
      }
    },

    firstValue() {
      const values = Array.prototype.slice.call(arguments)
      for (let i = 0; i < values.length; i += 1) {
        if (values[i] !== undefined && values[i] !== null && String(values[i]) !== '') return values[i]
      }
      return undefined
    },

    isWorkOrderNotification(item) {
      const category = String(item.category || '').toUpperCase()
      const businessType = String(item.businessType || '').toUpperCase()
      return category === 'WORK_ORDER'
        || businessType === 'ORDER'
        || businessType.indexOf('WORK_ORDER') >= 0
        || businessType.indexOf('ORDER') >= 0
        || this.firstValue(item.orderNo) !== undefined
    },

    isUnread(item) {
      return String(item && item.readFlag) === '0'
    },

    categoryText(category) {
      return {
        WORK_ORDER: '工单',
        APPROVAL: '审批',
        SYSTEM: '系统'
      }[String(category || '').toUpperCase()] || '消息'
    },

    categoryClass(category) {
      return `category-${String(category || '').toLowerCase()}`
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
  background: #F3F6F9;
}

.page-container {
  min-height: 100vh;
  background: #F3F6F9;
}

.mark-all-action {
  padding: 18rpx 24rpx;
  color: #27BDB5;
  font-size: 25rpx;
}

.mark-all-action.disabled {
  color: #C9CDD4;
}

.category-tabs {
  display: flex;
  height: 96rpx;
  align-items: stretch;
  background: #FFFFFF;
  border-top: 1rpx solid #F2F3F5;
  border-bottom: 1rpx solid #F2F3F5;
}

.category-tab {
  position: relative;
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: center;
  color: #86909C;
  font-size: 26rpx;
}

.category-tab.active {
  color: #27BDB5;
  font-weight: 600;
}

.category-tab-label {
  line-height: 34rpx;
}

.category-tab-count {
  min-width: 28rpx;
  margin-left: 8rpx;
  padding: 2rpx 6rpx;
  box-sizing: border-box;
  border-radius: 18rpx;
  background: #FFF1F0;
  color: #F53F3F;
  font-size: 19rpx;
  line-height: 26rpx;
  text-align: center;
}

.category-tab-count.empty {
  background: #F2F3F5;
  color: #A0A9B4;
}

.category-tab.active::after {
  position: absolute;
  right: 24rpx;
  bottom: 0;
  left: 24rpx;
  height: 6rpx;
  border-radius: 6rpx 6rpx 0 0;
  background: #27BDB5;
  content: '';
}

.content-scroll {
  height: calc(100vh - 184rpx);
}

.state-card,
.notification-card,
.inline-error,
.load-more-error {
  border-radius: 16rpx;
  background: #FFFFFF;
  box-shadow: 0 8rpx 24rpx rgba(29, 33, 41, 0.05);
}

.state-card {
  display: flex;
  min-height: 320rpx;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  margin: 20rpx 24rpx;
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
  padding: 4rpx 0 32rpx;
}

.notification-card {
  display: flex;
  align-items: flex-start;
  margin: 16rpx 24rpx 0;
  padding: 24rpx 20rpx;
}

.notification-card.unread .notification-title {
  color: #334155;
  font-weight: 600;
}

.notification-dot {
  width: 12rpx;
  height: 12rpx;
  flex: 0 0 12rpx;
  margin: 10rpx 14rpx 0 0;
  border-radius: 50%;
  background: transparent;
}

.notification-card.unread .notification-dot {
  background: #EF5656;
}

.notification-card.unread .category-approval {
  background: #FF9800;
}

.notification-card.unread .category-system {
  background: #2FC8C3;
}

.notification-body {
  min-width: 0;
  flex: 1;
}

.notification-title-row {
  display: flex;
  align-items: center;
}

.notification-title {
  overflow: hidden;
  flex: 1;
  color: #4E5969;
  font-size: 28rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notification-category {
  margin-left: 14rpx;
  color: #A0A9B4;
  font-size: 20rpx;
}

.notification-content {
  display: -webkit-box;
  margin-top: 10rpx;
  overflow: hidden;
  color: #788696;
  font-size: 24rpx;
  line-height: 36rpx;
  text-overflow: ellipsis;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.notification-time {
  display: block;
  margin-top: 10rpx;
  color: #A0A9B4;
  font-size: 21rpx;
  text-align: right;
}

.inline-error,
.load-more-error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 16rpx 24rpx 0;
  padding: 18rpx 22rpx;
}

.inline-error-text,
.load-more-error-text {
  margin-right: 18rpx;
  font-size: 24rpx;
}
</style>

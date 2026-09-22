<template>
  <view class="page-container">
    <!-- 顶部导航栏 -->
    <u-navbar 
      title="工单派发" 
      :is-back="true"
      background="#722ED1"
      title-color="#ffffff"
      left-icon-color="#ffffff"
      :border-bottom="false"
    ></u-navbar>

    <!-- 状态标签切换 -->
    <view class="status-tabs">
      <view 
        v-for="(tab, index) in tabList" 
        :key="index"
        class="status-tab"
        :class="{ active: activeTab === index }"
        :style="getTabStyle(index)"
        @click="switchTab(index)"
      >
        <text class="tab-text">{{ tab.name }}</text>
        <view class="tab-badge" v-if="tab.count > 0">{{ tab.count }}</view>
      </view>
    </view>

    <!-- 日期选择区域 - 上下排列布局 -->
    <view class="date-section">
      <view class="date-button-container">
        <button 
          class="date-button" 
          @click="showDateOptions"
        >
          <u-icon name="calendar" color="#ffffff" size="28" class="date-icon"></u-icon>
          <text class="date-text">{{ selectedDateText || '选择日期' }}</text>
        </button>
      </view>
      
      <view class="clear-button-container" v-if="selectedDate">
        <button 
          class="clear-button" 
          @click="clearDate"
        >
          <u-icon name="close" color="#F53F3F" size="24" class="clear-icon"></u-icon>
          <text class="clear-text">清除已选日期</text>
        </button>
      </view>
    </view>

    <!-- 工单列表区域 -->
    <scroll-view 
      class="content-scroll" 
      scroll-y 
      :scroll-top="scrollTop"
      scroll-with-animation
      :show-scrollbar="false"
      refresher-enabled
      :refresher-triggered="refreshing"
      @refresherrefresh="refreshOrders"
      @scrolltolower="loadMore"
      ref="contentScroll"
    >
      <!-- 骨架屏加载状态 -->
      <view v-if="loading" class="skeleton-container">
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
      
      <!-- 申请中列表 -->
      <view v-else-if="activeTab === 0" class="list-container">
        <u-empty 
          v-if="applyingOrders.length === 0" 
          text="暂无申请中工单" 
          mode="order"
          color="#C9CDD4"
          class="empty-tip"
        ></u-empty>
        
        <view class="order-list">
          <view 
            v-for="order in applyingOrders" 
            :key="order.id"
            class="order-card applying-card"
            @click="toDispatchDetail(order.id)"
          >
            <view class="status-bar applying-bar"></view>
            <view class="order-content">
              <view class="order-header">
                <view class="order-left">
                  <text class="order-no">工单#{{ order.orderNo }}</text>
                  <u-badge 
                    :text="order.urgency" 
                    :type="getUrgencyType(order.urgency)"
                    class="urgency-badge"
                  ></u-badge>
                </view>
                <text class="order-time">{{ order.createTime }}</text>
              </view>
              
              <view class="order-info">
                <text class="info-label">申请人：</text>
                <text class="info-value">{{ order.applicant }}</text>
              </view>
              
              <view class="order-info">
                <text class="info-label">科室：</text>
                <text class="info-value">{{ order.department }}</text>
              </view>
              
              <view class="order-info">
                <text class="info-label">故障类型：</text>
                <text class="info-value">{{ order.category }}</text>
              </view>
            </view>
          </view>
        </view>
        <u-loadmore v-if="applyingOrders.length" :status="loadMoreStatus" margin-top="20" margin-bottom="20"></u-loadmore>
      </view>

      <!-- 已派发列表 -->
      <view v-else-if="activeTab === 1" class="list-container">
        <u-empty 
          v-if="dispatchedOrders.length === 0" 
          text="暂无已派发工单" 
          mode="send"
          color="#C9CDD4"
          class="empty-tip"
        ></u-empty>
        
        <view class="order-list">
          <view 
            v-for="order in dispatchedOrders" 
            :key="order.id"
            class="order-card dispatched-card"
            @click="toDispatchDetail(order.id)"
          >
            <view class="status-bar dispatched-bar"></view>
            <view class="order-content">
              <view class="order-header">
                <view class="order-left">
                  <text class="order-no">工单#{{ order.orderNo }}</text>
                  <u-badge 
                    text="已派发" 
                    type="primary"
                    class="urgency-badge"
                  ></u-badge>
                </view>
                <text class="order-time">{{ order.dispatchTime }}</text>
              </view>
              
              <view class="order-info">
                <text class="info-label">申请人：</text>
                <text class="info-value">{{ order.applicant }}</text>
              </view>
              
              <view class="order-info">
                <text class="info-label">科室：</text>
                <text class="info-value">{{ order.department }}</text>
              </view>
              
              <view class="order-info">
                <text class="info-label">处理工程师：</text>
                <text class="info-value">{{ order.engineerName }}</text>
              </view>
            </view>
          </view>
        </view>
        <u-loadmore v-if="dispatchedOrders.length" :status="loadMoreStatus" margin-top="20" margin-bottom="20"></u-loadmore>
      </view>
    </scroll-view>
  </view>
</template>

<script>
import dispatchApi from '@/api/order/assign.js';

export default {
  data() {
    return {
      activeTab: 0,
      tabList: [
        { name: '申请中', count: 0 },
        { name: '已派发', count: 0 }
      ],
      applyingOrders: [],
      dispatchedOrders: [],
      loading: true,
      loadingMore: false,
      refreshing: false,
      scrollTop: 0,
      selectedDate: null,
      selectedDateText: '',
      dateOptions: [],
      paging: {
        applying: { pageNum: 1, pageSize: 10, total: 0 },
        dispatched: { pageNum: 1, pageSize: 10, total: 0 }
      }
    };
  },
  onLoad() {
    this.generateDateOptions();
    this.loadOrders(true);
    uni.$on('refreshDispatchList', this.handleExternalRefresh);
  },
  onUnload() {
    uni.$off('refreshDispatchList', this.handleExternalRefresh);
  },
  computed: {
    activeKey() {
      return this.activeTab === 0 ? 'applying' : 'dispatched';
    },
    activeOrders() {
      return this.activeTab === 0 ? this.applyingOrders : this.dispatchedOrders;
    },
    loadMoreStatus() {
      if (this.loadingMore) return 'loading';
      return this.activeOrders.length >= this.paging[this.activeKey].total ? 'nomore' : 'loadmore';
    }
  },
  methods: {
    // 生成最近7天日期选项
    generateDateOptions() {
      const options = [];
      const today = new Date();
      
      // 添加"今天"选项
      options.push({
        text: '今天',
        value: this.formatDate(today)
      });
      
      // 添加最近6天选项
      for (let i = 1; i <= 6; i++) {
        const date = new Date();
        date.setDate(today.getDate() - i);
        options.push({
          text: `${i}天前`,
          value: this.formatDate(date)
        });
      }
      
      this.dateOptions = options;
    },
    
    // 显示日期选择选项
    showDateOptions() {
      const itemList = this.dateOptions.map(option => option.text);
      
      uni.showActionSheet({
        itemList: itemList,
        success: (res) => {
          const selectedOption = this.dateOptions[res.tapIndex];
          this.selectedDate = selectedOption.value;
          this.selectedDateText = selectedOption.text;
          this.loadOrders(true);
        },
        fail: () => {}
      });
    },
    
    // 获取标签样式
    getTabStyle(index) {
      const styles = [
        // 申请中 - 紫色
        { 
          backgroundColor: this.activeTab === 0 ? '#F4EBFF' : 'transparent',
          color: this.activeTab === 0 ? '#722ED1' : '#86909C'
        },
        // 已派发 - 蓝色
        { 
          backgroundColor: this.activeTab === 1 ? '#E8F3FF' : 'transparent',
          color: this.activeTab === 1 ? '#36CFC9' : '#86909C'
        }
      ];
      return styles[index];
    },
    
    // 切换标签页
    switchTab(index) {
      if (this.activeTab !== index) {
        this.activeTab = index;
        this.scrollTop = this.scrollTop === 0 ? 1 : 0;
        this.loadOrders(true);
      }
    },
    
    // 格式化日期显示 (YYYY-MM-DD)
    formatDate(date) {
      const year = date.getFullYear();
      const month = (date.getMonth() + 1).toString().padStart(2, '0');
      const day = date.getDate().toString().padStart(2, '0');
      return `${year}-${month}-${day}`;
    },
    
    // 清除日期选择
    clearDate() {
      this.selectedDate = null;
      this.selectedDateText = '';
      this.loadOrders(true);
    },
    
    // 根据紧急程度获取徽章类型
    getUrgencyType(urgency) {
      switch(urgency) {
        case '一般':
          return 'info';
        case '紧急':
          return 'warning';
        case '特急':
          return 'error';
        default:
          return 'info';
      }
    },
    
    // 两个标签各自保存分页游标，避免切换标签时相互覆盖页码和总数。
    loadOrders: async function(reset = false) {
      const key = this.activeKey;
      const page = this.paging[key];
      if (this.loadingMore || (!reset && this.activeOrders.length >= page.total)) return;
      try {
        if (reset) {
          page.pageNum = 1;
          this.loading = true;
        } else {
          this.loadingMore = true;
        }
        const response = await dispatchApi.getOrders(key, this.selectedDate, page);
        const rows = response.rows || [];
        const currentRows = key === 'applying' ? this.applyingOrders : this.dispatchedOrders;
        const merged = reset ? rows : currentRows.concat(rows);
        if (key === 'applying') this.applyingOrders = merged;
        else this.dispatchedOrders = merged;
        page.total = response.total;
        this.tabList[key === 'applying' ? 0 : 1].count = response.total;
        if (merged.length < page.total) page.pageNum += 1;
      } catch (error) {
        console.error('加载工单数据失败:', error);
        this.$u.toast('数据加载失败，请稍后重试');
      } finally {
        this.loading = false;
        this.loadingMore = false;
        this.refreshing = false;
      }
    },

    loadMore() {
      this.loadOrders(false);
    },

    refreshOrders() {
      this.refreshing = true;
      this.loadOrders(true);
    },

    handleExternalRefresh() {
      this.loadOrders(true);
    },
    
    // 跳转到派发详情页
    toDispatchDetail: function(orderId) {
      if (!orderId) {
        this.$u.toast('工单ID不存在');
        return;
      }
      
      uni.navigateTo({
        url: `/pages/order/order-detail/order-assign?id=${orderId}`,
        fail: () => this.$u.toast('跳转失败，请检查页面路径')
      });
    }
  }
};
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

/* 日期选择区域 - 上下排列布局 */
.date-section {
  background-color: #ffffff;
  padding: 16rpx 20rpx;
}

.date-button-container {
  margin-bottom: 15rpx;
}

/* 日期按钮 - 优化样式 */
.date-button {
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #722ED1;
  color: #ffffff;
  font-size: 30rpx;
  border-radius: 12rpx;
  padding: 0 30rpx;
  height: 100rpx;
  width: 100%;
  box-shadow: 0 6rpx 16rpx rgba(114, 46, 209, 0.2);
  transition: all 0.2s ease;
}

.date-button:active {
  transform: scale(0.98);
  box-shadow: 0 4rpx 12rpx rgba(114, 46, 209, 0.15);
}

.date-icon {
  margin-right: 15rpx;
}

/* 清除按钮 - 优化样式 */
.clear-button {
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #FFF1F0;
  color: #F53F3F;
  font-size: 28rpx;
  border-radius: 12rpx;
  padding: 0 20rpx;
  height: 80rpx;
  width: 100%;
  transition: all 0.2s ease;
}

.clear-button:active {
  background-color: #FEF0F0;
  transform: scale(0.98);
}

.clear-icon {
  margin-right: 12rpx;
}

/* 内容滚动区域 */
.content-scroll {
  height: calc(100vh - 300rpx);
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

/* 工单卡片样式 */
.order-card {
  margin-bottom: 20rpx;
  border-radius: 16rpx;
  background-color: #ffffff;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
  overflow: hidden;
  position: relative;
  width: 100%;
  cursor: pointer;
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

.applying-bar {
  background-color: #722ED1;
}

.dispatched-bar {
  background-color: #36CFC9;
}

/* 不同状态卡片样式 */
.applying-card {
  border-left: 8rpx solid #722ED1;
}

.dispatched-card {
  border-left: 8rpx solid #36CFC9;
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
  width: 160rpx;
}

.info-value {
  font-size: 26rpx;
  color: #303133;
  flex: 1;
}

.empty-tip {
  margin-top: 200rpx;
}
</style>

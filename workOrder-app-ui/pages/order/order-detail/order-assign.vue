<template>
  <view class="page-container">
    <!-- 顶部导航栏 -->
    <u-navbar 
      title="工单派发详情" 
      :auto-back="true"
      background="#722ED1"
      title-color="#ffffff"
      left-icon-color="#ffffff"
      :border-bottom="false"
    ></u-navbar>

    <!-- 工单状态标签 -->
    <view class="status-tag" :class="getStatusClass(order.status || 'applying')" v-if="!loading && !error">
      <text class="status-text">{{ getStatusText(order.status || 'applying') }}</text>
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
      
      <!-- 错误提示 -->
      <view v-if="error" class="empty-tip">
        <u-empty text="获取工单详情失败" mode="error" color="#F53F3F"></u-empty>
        <u-button size="mini" @click="loadPage">重新加载</u-button>
      </view>
      
      <!-- 工单详情内容 -->
      <view v-else class="detail-container">
        <!-- 工单基本信息 -->
        <view class="info-card">
          <view class="card-title">
            <u-icon name="clipboard" color="#722ED1" size="28"></u-icon>
            <text class="title-text">基本信息</text>
          </view>
          <view class="info-content">
            <view class="info-row">
              <text class="info-label">工单编号：</text>
              <text class="info-value">{{ order.orderNo || order.id }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">申请人：</text>
              <text class="info-value">{{ order.applicant }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">联系电话：</text>
              <text class="info-value">{{ order.phone }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">所属科室：</text>
              <text class="info-value">{{ order.department }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">提交时间：</text>
              <text class="info-value">{{ order.createTime }}</text>
            </view>
            <view class="info-row" v-if="order.dispatchTime">
              <text class="info-label">派发时间：</text>
              <text class="info-value">{{ order.dispatchTime }}</text>
            </view>
            <view class="info-row" v-if="order.engineerName">
              <text class="info-label">处理工程师：</text>
              <text class="info-value">{{ order.engineerName }}</text>
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
              <text class="info-value">{{ order.category }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">紧急程度：</text>
              <text class="info-value urgency-text" :class="getUrgencyClass(order.urgency)">{{ order.urgency }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">影响范围：</text>
              <text class="info-value">{{ order.scope }}</text>
            </view>
            <view class="info-row full-width">
              <text class="info-label">故障描述：</text>
              <text class="info-value full-content">{{ order.description }}</text>
            </view>
            <view class="info-row full-width" v-if="order.explanation">
              <text class="info-label">可能原因：</text>
              <text class="info-value full-content">{{ order.explanation }}</text>
            </view>
          </view>
        </view>
        
        <!-- 工程师选择 (仅申请中工单显示) -->
        <view class="info-card" v-if="canDispatch">
          <view class="card-title">
            <u-icon name="user" color="#722ED1" size="28"></u-icon>
            <text class="title-text">选择工程师</text>
          </view>
          <view class="info-content">
            <!-- 使用uni-app原生picker组件 -->
            <view class="native-picker-container">
              <button 
                class="picker-button" 
                :disabled="!engineerOptions.length"
              >
                <text class="button-text">{{ selectedEngineer || '请选择处理工程师' }}</text>
                <u-icon name="arrow-down" color="#86909C" size="24"></u-icon>
              </button>
              
              <!-- 原生picker组件 - 显示姓名和部门 -->
              <picker 
                v-if="engineerOptions.length > 0"
                mode="selector"
                :range="engineerOptions"
                :value="selectedIndex"
                @change="onPickerChange"
                class="native-picker"
              ></picker>
            </view>
            
            <!-- 自定义错误提示 -->
            <view v-if="!selectedEngineerId && submitFailed" class="error-tip">
              <u-icon name="error-circle" color="#F53F3F" size="24" class="error-icon"></u-icon>
              <text class="error-text">请选择处理工程师</text>
            </view>
            
            <view class="engineer-info" v-if="selectedEngineerId">
              <view class="info-row">
                <text class="info-label">所属科室：</text>
                <text class="info-value">{{ selectedEngineerDept }}</text>
              </view>
              <view class="info-row">
                <text class="info-label">擅长领域：</text>
                <text class="info-value">{{ selectedEngineerSpec }}</text>
              </view>
            </view>

            <view v-if="dispatchAction === 'REASSIGN'" class="reassign-reason">
              <u-input
                v-model="dispatchReason"
                type="textarea"
                :maxlength="500"
                placeholder="请填写改派原因"
                @input="onReasonInput"
              ></u-input>
            </view>
          </view>
        </view>
        
        <!-- 派发日志 -->
        <view class="info-card" v-if="order.dispatchLog && order.dispatchLog.length > 0">
          <view class="card-title">
            <u-icon name="clock" color="#86909C" size="28"></u-icon>
            <text class="title-text">派发日志</text>
          </view>
          <view class="timeline">
            <view class="timeline-item" v-for="(log, index) in order.dispatchLog" :key="index">
              <view class="timeline-node" :class="{ last: index === order.dispatchLog.length - 1 }"></view>
              <view class="timeline-content">
                <view class="timeline-time">{{ log.time }}</view>
                <view class="timeline-text">{{ log.content }}</view>
                <view class="timeline-operator" v-if="log.operator">操作人：{{ log.operator }}</view>
              </view>
            </view>
          </view>
        </view>
        
        <!-- 附件上传 -->
        <view class="info-card" v-if="order.attachments && order.attachments.length > 0">
          <view class="card-title">
            <u-icon name="paperclip" color="#86909C" size="28"></u-icon>
            <text class="title-text">附件信息</text>
          </view>
          <view class="attachment-list">
            <view class="attachment-item" v-for="(item, index) in order.attachments" :key="item.id || index" @click="openAttachment(item)">
              <u-icon name="image" color="#722ED1" size="36"></u-icon>
              <text class="attachment-name">{{ item.fileName || '未命名附件' }}</text>
              <u-icon name="arrow-right" color="#C9CDD4" size="24"></u-icon>
            </view>
          </view>
        </view>
        
        <!-- 操作按钮区域 (仅申请中工单显示) -->
        <view class="button-container" v-if="canDispatch">
          <u-button 
            type="primary" 
            size="large"
            :disabled="!selectedEngineerId"
            :loading="submitting"
            :custom-style="{
              backgroundColor: '#722ED1',
              borderColor: '#722ED1'
            }"
            @click="dispatchOrder"
          >
            {{ dispatchAction === 'REASSIGN' ? '确认改派' : '确认派发' }}
          </u-button>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script>
import dispatchApi from '@/api/order/assign.js';
import config from '@/config';
import { getToken } from '@/utils/auth';

export default {
  data() {
    return {
      orderId: '',
      order: {},
      engineers: [],
      engineerOptions: [],
      selectedIndex: 0,
      selectedEngineerId: '',
      selectedEngineer: '',
      selectedEngineerDept: '',
      selectedEngineerSpec: '',
      loading: true,
      submitting: false,
      error: false,
      submitFailed: false,
      dispatchReason: '',
      idempotencyKey: ''
    };
  },
  onLoad(options) {
    if (options.id) {
      this.orderId = options.id;
      this.loadPage();
    } else {
      this.error = true;
      this.loading = false;
    }
  },
  computed: {
    dispatchAction() {
      const actions = this.order.allowedActions || [];
      if (actions.includes('ASSIGN')) return 'ASSIGN';
      if (actions.includes('REASSIGN')) return 'REASSIGN';
      return '';
    },
    canDispatch() {
      return Boolean(this.dispatchAction);
    }
  },
  methods: {
    async loadPage() {
      this.loading = true;
      this.error = false;
      try {
        await Promise.all([this.loadOrderDetail(), this.loadEngineers()]);
        this.syncSelectedEngineer();
      } catch (error) {
        console.error('派单详情加载失败:', error);
        this.error = true;
      } finally {
        this.loading = false;
      }
    },

    // 加载工单详情
    loadOrderDetail: async function() {
      const detail = await dispatchApi.getOrderDetail(this.orderId);
      if (!detail || !detail.id) throw new Error('工单不存在或无权访问');
      this.order = detail;
      if (detail.engineerId) {
        this.selectedEngineerId = detail.engineerId;
        this.selectedEngineer = detail.engineerName;
      }
    },
    
    // 加载工程师列表
    loadEngineers: async function() {
      this.engineers = await dispatchApi.getEngineers();
      this.engineerOptions = this.engineers.map(eng => `${eng.name} - ${eng.department}`);
    },

    syncSelectedEngineer() {
      if (!this.selectedEngineerId || !this.engineers.length) return;
      const index = this.engineers.findIndex(eng => String(eng.id) === String(this.selectedEngineerId));
      if (index !== -1) {
        this.selectedIndex = index;
        this.updateSelectedEngineer(index, false);
      }
    },
    
    // 选择器变化事件
    onPickerChange(e) {
      const index = Number(e.detail.value);
      this.selectedIndex = index;
      this.updateSelectedEngineer(index);
      this.submitFailed = false;
    },
    
    // 更新选中的工程师信息
    updateSelectedEngineer(index, resetKey = true) {
      if (this.engineers[index]) {
        const selected = this.engineers[index];
        this.selectedEngineerId = selected.id;
        this.selectedEngineer = selected.name;
        this.selectedEngineerDept = selected.department;
        this.selectedEngineerSpec = selected.specialty;
        if (resetKey) this.idempotencyKey = '';
      }
    },

    onReasonInput() {
      this.idempotencyKey = '';
    },
    
    // 获取状态文本
    getStatusText(status) {
      const statusMap = {
        WAIT_ASSIGN: '申请中',
        WAIT_ACCEPT: '待接单',
        ACCEPTED: '已接单',
        PROCESSING: '处理中',
        WAIT_CONFIRM: '待确认',
        COMPLETED: '已完成',
        CLOSED: '已关闭',
        CANCELLED: '已取消'
      };
      return statusMap[status] || status;
    },
    
    // 获取状态样式类
    getStatusClass(status) {
      const classMap = {
        WAIT_ASSIGN: 'status-applying',
        WAIT_ACCEPT: 'status-dispatched',
        ACCEPTED: 'status-dispatched',
        PROCESSING: 'status-dispatched'
      };
      return classMap[status] || 'status-dispatched';
    },
    
    // 获取紧急程度样式
    getUrgencyClass(urgency) {
      const classMap = {
        '一般': 'urgency-normal',
        '紧急': 'urgency-urgent',
        '特急': 'urgency-emergency'
      };
      return classMap[urgency] || 'urgency-normal';
    },
    
    // 幂等键只在提交成功、请求内容变化或并发冲突后清除；网络失败重试仍对应同一次操作。
    dispatchOrder: async function() {
      if (!this.selectedEngineerId || !this.dispatchAction || this.submitting) {
        this.submitFailed = true;
        this.$u.toast('请选择处理工程师');
        return;
      }
      if (this.dispatchAction === 'REASSIGN' && !this.dispatchReason.trim()) {
        this.$u.toast('请填写改派原因');
        return;
      }

      const actionText = this.dispatchAction === 'REASSIGN' ? '改派' : '派发';
      this.$u.confirm(`确定将工单${actionText}给${this.selectedEngineer}吗？`).then(async res => {
        if (res) {
          try {
            this.submitting = true;
            if (!this.idempotencyKey) {
              this.idempotencyKey = dispatchApi.createDispatchIdempotencyKey(this.dispatchAction);
            }
            const payload = {
              engineerId: this.selectedEngineerId,
              reason: this.dispatchReason,
              version: this.order.version,
              idempotencyKey: this.idempotencyKey
            };
            if (this.dispatchAction === 'REASSIGN') {
              await dispatchApi.reassignOrder(this.orderId, payload);
            } else {
              await dispatchApi.assignOrder(this.orderId, payload);
            }
            this.idempotencyKey = '';
            this.$u.toast(`${actionText}成功`);
            uni.navigateBack({
              delta: 1,
              success: () => uni.$emit('refreshDispatchList')
            });
          } catch (error) {
            if (this.isConflictError(error)) {
              const selectedEngineerId = this.selectedEngineerId;
              this.idempotencyKey = '';
              await this.loadPage();
              const selectedIndex = this.engineers.findIndex(item => String(item.id) === String(selectedEngineerId));
              if (selectedIndex !== -1) {
                this.selectedIndex = selectedIndex;
                this.updateSelectedEngineer(selectedIndex, false);
              }
              this.$u.toast('工单状态已变化，已刷新最新信息');
            } else {
              this.$u.toast(`${actionText}失败，请检查网络后重试`);
            }
          } finally {
            this.submitting = false;
          }
        }
      });
    },

    isConflictError(error) {
      return Number(error) === 409 || Number(error && error.code) === 409;
    },

    // 私有附件先携带当前令牌下载到临时目录，再交给微信预览或系统应用打开。
    openAttachment(item) {
      if (!item || !item.downloadUrl) {
        this.$u.toast('附件暂不可下载');
        return;
      }
      const baseUrl = String(config.baseUrl || '').replace(/\/$/, '');
      const path = String(item.downloadUrl);
      const url = /^https?:\/\//i.test(path) ? path : `${baseUrl}${path}`;
      if (!/^https?:\/\//i.test(url)) {
        this.$u.toast('未配置服务地址，无法下载附件');
        return;
      }
      const token = getToken();
      uni.showLoading({ title: '正在下载', mask: true });
      uni.downloadFile({
        url,
        header: token ? { Authorization: `Bearer ${token}` } : {},
        success: response => {
          if (response.statusCode !== 200 || !response.tempFilePath) {
            this.$u.toast('附件下载失败，请稍后重试');
            return;
          }
          const filePath = response.tempFilePath;
          if (/\.(png|jpe?g|gif|bmp|webp)$/i.test(item.fileName || '')) {
            uni.previewImage({ current: filePath, urls: [filePath] });
          } else {
            uni.openDocument({
              filePath,
              showMenu: true,
              fail: () => this.$u.toast('附件已下载，请使用本机应用打开')
            });
          }
        },
        fail: () => this.$u.toast('附件下载失败，请检查网络后重试'),
        complete: () => uni.hideLoading()
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

.status-applying {
  background-color: #722ED1;
}

.status-dispatched {
  background-color: #36CFC9;
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

.detail-container {
  padding: 20rpx;
}

/* 信息卡片 */
.info-card {
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

/* 原生选择器样式 */
.native-picker-container {
  width: 100%;
  position: relative;
}

/* 选择器按钮 - 明确的可点击样式 */
.picker-button {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  height: 90rpx;
  padding: 0 20rpx;
  border: 2rpx solid #722ED1;
  border-radius: 10rpx;
  background-color: #ffffff;
  box-sizing: border-box;
  font-size: 28rpx;
  color: #303133;
}

.picker-button:active {
  background-color: #F4EBFF;
}

.button-text {
  flex: 1;
  text-align: left;
}

/* 隐藏原生picker但保持功能 */
.native-picker {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  z-index: 1;
}

/* 工程师信息样式 */
.engineer-info {
  padding: 15rpx;
  background-color: #F4EBFF;
  border-radius: 8rpx;
  margin-top: 15rpx;
}

.reassign-reason {
  margin-top: 20rpx;
  padding: 12rpx;
  background-color: #F7F8FA;
  border-radius: 8rpx;
}

/* 错误提示样式 */
.error-tip {
  margin-top: 15rpx;
  color: #F53F3F;
  font-size: 24rpx;
  display: flex;
  align-items: center;
  padding-left: 10rpx;
}

.error-icon {
  margin-right: 8rpx;
}

.error-text {
  flex: 1;
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
  background-color: #722ED1;
  margin-right: 20rpx;
  margin-top: 5rpx;
  position: relative;
  z-index: 1;
}

.timeline-node.last {
  background-color: #36CFC9;
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

.attachment-name {
  flex: 1;
  font-size: 26rpx;
  color: #303133;
  margin: 0 15rpx;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 按钮区域 */
.button-container {
  padding: 20rpx;
  background-color: #ffffff;
  margin: 20rpx;
  border-radius: 16rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
}
</style>

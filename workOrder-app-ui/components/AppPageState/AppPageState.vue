<template>
  <view class="app-page-state" :class="`app-page-state--${resolvedType}`">
    <view class="state-icon" aria-hidden="true">
      <view v-if="resolvedType === 'loading'" class="loading-ring"></view>
      <text v-else class="iconfont" :class="stateConfig.icon"></text>
    </view>
    <text class="state-title">{{ displayTitle }}</text>
    <text v-if="displayDescription" class="state-description">{{ displayDescription }}</text>
    <button
      v-if="shouldShowAction"
      class="state-action"
      :disabled="actionLoading"
      @click="$emit('action')"
    >
      {{ actionLoading ? '处理中...' : displayActionText }}
    </button>
  </view>
</template>

<script>
  // 页面只传入有限状态码；图标和兜底文案集中在组件内部，避免各页面出现互相矛盾的异常提示。
  const STATE_CONFIG = Object.freeze({
    loading: {
      title: '正在加载',
      description: '请稍候，不要重复操作',
      actionText: '',
      icon: ''
    },
    empty: {
      title: '暂无内容',
      description: '当前还没有可展示的数据',
      actionText: '',
      icon: 'icon-empty'
    },
    searchEmpty: {
      title: '没有匹配结果',
      description: '请尝试更换关键词或清除筛选条件',
      actionText: '清除搜索',
      icon: 'icon-search'
    },
    error: {
      title: '加载失败',
      description: '网络可能开小差了，请稍后重试',
      actionText: '重新加载',
      icon: 'icon-warning'
    },
    offline: {
      title: '网络不可用',
      description: '请检查网络连接后重试',
      actionText: '重新连接',
      icon: 'icon-wifi'
    },
    forbidden: {
      title: '暂无访问权限',
      description: '当前账号不能查看此内容',
      actionText: '返回上一页',
      icon: 'icon-lock'
    },
    disabled: {
      title: '功能暂不可用',
      description: '该功能尚未开放，请稍后再试',
      actionText: '返回上一页',
      icon: 'icon-info'
    }
  })

  export default {
    name: 'AppPageState',
    props: {
      type: {
        type: String,
        default: 'empty'
      },
      title: {
        type: String,
        default: ''
      },
      description: {
        type: String,
        default: ''
      },
      actionText: {
        type: String,
        default: ''
      },
      actionLoading: {
        type: Boolean,
        default: false
      },
      showAction: {
        type: Boolean,
        default: undefined
      }
    },
    computed: {
      resolvedType() {
        return STATE_CONFIG[this.type] ? this.type : 'error'
      },
      stateConfig() {
        return STATE_CONFIG[this.resolvedType]
      },
      displayTitle() {
        return this.title || this.stateConfig.title
      },
      displayDescription() {
        return this.description || this.stateConfig.description
      },
      displayActionText() {
        return this.actionText || this.stateConfig.actionText
      },
      shouldShowAction() {
        if (typeof this.showAction === 'boolean') return this.showAction
        return Boolean(this.displayActionText)
      }
    }
  }
</script>

<style lang="scss" scoped>
  .app-page-state {
    min-height: 420rpx;
    padding: 96rpx 48rpx 48rpx;
    box-sizing: border-box;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    text-align: center;
  }

  .state-icon {
    width: 112rpx;
    height: 112rpx;
    margin-bottom: 28rpx;
    border-radius: 56rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #eef6ff;
    color: #3c96f3;

    .iconfont {
      font-size: 52rpx;
    }
  }

  .state-title {
    color: #303133;
    font-size: 32rpx;
    font-weight: 600;
    line-height: 48rpx;
  }

  .state-description {
    max-width: 560rpx;
    margin-top: 12rpx;
    color: #909399;
    font-size: 26rpx;
    line-height: 40rpx;
  }

  .state-action {
    min-width: 240rpx;
    height: 76rpx;
    margin-top: 36rpx;
    padding: 0 36rpx;
    border: 0;
    border-radius: 38rpx;
    background: #3c96f3;
    color: #ffffff;
    font-size: 28rpx;
    line-height: 76rpx;
  }

  .state-action::after {
    border: 0;
  }

  .state-action[disabled] {
    background: #a8cff7;
    color: #ffffff;
  }

  .loading-ring {
    width: 44rpx;
    height: 44rpx;
    border: 5rpx solid #cfe5fb;
    border-top-color: #3c96f3;
    border-radius: 50%;
    animation: state-loading 0.9s linear infinite;
  }

  @keyframes state-loading {
    to { transform: rotate(360deg); }
  }
</style>

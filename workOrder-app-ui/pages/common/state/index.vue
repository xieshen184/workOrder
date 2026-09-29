<template>
  <view class="state-page">
    <app-page-state
      :type="stateType"
      :title="stateTitle"
      :description="stateDescription"
      :action-text="actionText"
      @action="handleAction"
    />
  </view>
</template>

<script>
  import AppPageState from '@/components/AppPageState/AppPageState.vue'

  // 独立状态页只接收白名单状态码和动作码，不接受任意文案或任意跳转地址。
  const STATE_ROUTE_CONFIG = Object.freeze({
    empty: { title: '暂无内容', description: '当前还没有可展示的数据', action: 'back' },
    searchEmpty: { title: '没有匹配结果', description: '请调整搜索条件后再试', action: 'back' },
    error: { title: '页面加载失败', description: '请返回后重新进入', action: 'back' },
    offline: { title: '网络不可用', description: '请检查网络连接后重试', action: 'retry' },
    forbidden: { title: '暂无访问权限', description: '当前账号不能查看此页面', action: 'home' },
    disabled: { title: '功能暂不可用', description: '该功能尚未开放，请稍后再试', action: 'back' }
  })

  export default {
    components: { AppPageState },
    data() {
      return {
        stateType: 'error',
        stateTitle: '',
        stateDescription: '',
        action: 'back'
      }
    },
    computed: {
      actionText() {
        return this.action === 'home' ? '返回首页' : this.action === 'retry' ? '重试' : '返回上一页'
      }
    },
    onLoad(options) {
      const requestedType = options && options.type
      const config = STATE_ROUTE_CONFIG[requestedType] || STATE_ROUTE_CONFIG.error
      this.stateType = STATE_ROUTE_CONFIG[requestedType] ? requestedType : 'error'
      this.stateTitle = config.title
      this.stateDescription = config.description
      this.action = config.action
    },
    methods: {
      handleAction() {
        if (this.action === 'home') {
          this.$tab.switchTab('/pages/index')
          return
        }
        if (this.action === 'retry') {
          uni.getNetworkType({
            success: ({ networkType }) => {
              if (networkType === 'none') {
                this.$modal.msg('网络仍未连接')
                return
              }
              this.$tab.navigateBack()
            },
            fail: () => this.$modal.msg('暂时无法检测网络状态')
          })
          return
        }
        this.$tab.navigateBack()
      }
    }
  }
</script>

<style lang="scss" scoped>
  page,
  .state-page {
    min-height: 100%;
    background: #f8f8f8;
  }
</style>

<template>
  <view class="webview-container">
    <web-view
      v-if="params.url && !errorMessage"
      :webview-styles="webviewStyles"
      :src="params.url"
      @error="handleError"
    ></web-view>

    <app-page-state
      v-else
      type="error"
      title="外部内容不可用"
      :description="errorMessage || '外部内容入口未登记'"
      :action-text="params.fallbackContentId ? '查看内置内容' : '返回'"
      @action="handleErrorAction"
    />
  </view>
</template>

<script>
  import { getContentById, getResourceByKey, isTrustedHttpsUrl } from '@/utils/contentRegistry'
  import AppPageState from '@/components/AppPageState/AppPageState.vue'

  export default {
    components: { AppPageState },
    data() {
      return {
        params: {
          url: '',
          title: '',
          fallbackContentId: ''
        },
        errorMessage: '',
        webviewStyles: {
          progress: {
            color: '#FF3333'
          }
        }
      }
    },
    onLoad(options) {
      const resourceKey = this.decodeOption(options && options.key)
      const resource = getResourceByKey(resourceKey)
      if (!resource) {
        this.rejectNavigation('外部内容入口未登记')
        return
      }

      this.params = {
        url: resource.url || '',
        title: resource.title || '浏览网页',
        fallbackContentId: resource.fallbackContentId || ''
      }
      if (!resource.url) {
        this.rejectNavigation('在线内容尚未配置，可查看内置内容')
        return
      }
      if (!isTrustedHttpsUrl(resource.url)) {
        this.rejectNavigation('外部内容地址未通过 HTTPS 或可信主机校验')
        return
      }

      this.errorMessage = ''
      uni.setNavigationBarTitle({ title: this.params.title })
    },
    methods: {
      decodeOption(value) {
        if (typeof value !== 'string' || !value) return ''
        try {
          return decodeURIComponent(value)
        } catch (error) {
          return ''
        }
      },
      rejectNavigation(message) {
        this.errorMessage = message
        uni.setNavigationBarTitle({ title: this.params.title || '浏览网页' })
      },
      handleError() {
        this.errorMessage = '外部内容加载失败，可查看内置内容或返回重试'
      },
      openFallback() {
        const contentId = this.params.fallbackContentId
        if (!contentId || !getContentById(contentId)) {
          this.errorMessage = '内置回退内容不可用，请联系系统管理员'
          return
        }
        this.$tab.redirectTo(`/pages/common/textview/index?id=${encodeURIComponent(contentId)}`)
      },
      handleErrorAction() {
        if (this.params.fallbackContentId) {
          this.openFallback()
          return
        }
        this.handleBack()
      },
      handleBack() {
        uni.navigateBack({ delta: 1 })
      }
    }
  }
</script>

<style lang="scss" scoped>
  page {
    background-color: #ffffff;
  }

  .webview-container {
    min-height: 100%;
    background-color: #ffffff;
  }

</style>

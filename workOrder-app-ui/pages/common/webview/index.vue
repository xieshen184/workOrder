<template>
  <view v-if="params.url">
    <web-view :webview-styles="webviewStyles" :src="params.url" @error="handleError"></web-view>
  </view>
</template>

<script>
  import config from '@/config'

  export default {
    data() {
      return {
        params: {},
        webviewStyles: {
          progress: {
            color: "#FF3333"
          }
        }
      }
    },
    props: {
      src: {
        type: [String],
        default: null
      }
    },
    onLoad(event) {
      let title = ''
      let url = ''
      try {
        title = decodeURIComponent(event.title || '')
        url = decodeURIComponent(event.url || '')
      } catch (error) {
        this.rejectNavigation()
        return
      }
      const allowedUrls = (config.appInfo.agreements || []).map(item => item.url).filter(Boolean)
      if (!url || !allowedUrls.includes(url)) {
        this.rejectNavigation()
        return
      }
      this.params = { title, url }
      if (title) {
        uni.setNavigationBarTitle({
          title
        })
      }
    },
    methods: {
      rejectNavigation() {
        uni.showToast({ title: '该内容地址未获授权', icon: 'none' })
        setTimeout(() => uni.navigateBack(), 800)
      },
      handleError() {
        uni.showToast({ title: '内容加载失败，请稍后重试', icon: 'none' })
      }
    }
  }
</script>

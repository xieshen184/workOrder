<template>
  <view class="about-container">
    <view class="header-section text-center">
      <image class="app-logo" :src="logo" mode="aspectFit"></image>
      <view class="app-name">{{ appName }}</view>
      <view class="app-version">版本 {{ version }}</view>
    </view>

    <view class="content-section">
      <view class="menu-list">
        <view class="list-cell list-cell-arrow">
          <view class="menu-item-box">
            <view>应用名称</view>
            <view class="text-right">{{ appName }}</view>
          </view>
        </view>
        <view class="list-cell list-cell-arrow">
          <view class="menu-item-box">
            <view>版本信息</view>
            <view class="text-right">v{{ version }}</view>
          </view>
        </view>
        <view v-if="!serviceEmail && !servicePhone" class="list-cell list-cell-arrow">
          <view class="menu-item-box">
            <view>联系信息</view>
            <view class="text-right">请联系系统管理员</view>
          </view>
        </view>
        <view v-if="serviceEmail" class="list-cell list-cell-arrow" @click="copyEmail">
          <view class="menu-item-box">
            <view>客服邮箱</view>
            <view class="text-right link-text">{{ serviceEmail }}</view>
          </view>
        </view>
        <view v-if="servicePhone" class="list-cell list-cell-arrow" @click="callService">
          <view class="menu-item-box">
            <view>客服电话</view>
            <view class="text-right link-text">{{ servicePhone }}</view>
          </view>
        </view>
        <view v-if="serviceSiteAvailable" class="list-cell list-cell-arrow" @click="openLegal('serviceSite')">
          <view class="menu-item-box">
            <view>服务网站</view>
            <view class="text-right link-text">查看</view>
          </view>
        </view>
        <view class="list-cell list-cell-arrow" @click="openLegal('serviceAgreement')">
          <view class="menu-item-box">
            <view>用户服务协议</view>
            <view class="text-right link-text">查看</view>
          </view>
        </view>
        <view class="list-cell list-cell-arrow" @click="openLegal('privacyPolicy')">
          <view class="menu-item-box">
            <view>隐私政策</view>
            <view class="text-right link-text">查看</view>
          </view>
        </view>
      </view>
    </view>

    <view class="copyright">本应用由系统管理员维护</view>
  </view>
</template>

<script>
  import config from '@/config'
  import { getContentById, getResourceByKey, isTrustedHttpsUrl } from '@/utils/contentRegistry'

  const appInfo = config && config.appInfo ? config.appInfo : {}

  export default {
    data() {
      return {
        logo: appInfo.logo || '/static/logo.png',
        appName: appInfo.name || '应用',
        version: appInfo.version || '未知',
        serviceEmail: appInfo.contact ? String(appInfo.contact.email || '').trim() : '',
        servicePhone: appInfo.contact ? String(appInfo.contact.phone || '').trim() : ''
      }
    },
    computed: {
      serviceSiteAvailable() {
        const resource = getResourceByKey('serviceSite')
        return Boolean(resource && resource.url && isTrustedHttpsUrl(resource.url))
      }
    },
    methods: {
      copyEmail() {
        if (!this.serviceEmail) return
        uni.setClipboardData({
          data: this.serviceEmail,
          success: () => this.$modal.msgSuccess('邮箱已复制'),
          fail: () => this.$modal.msgError('复制失败，请手动记录')
        })
      },
      callService() {
        if (!this.servicePhone) return
        this.$modal.confirm(`是否拨打 ${this.servicePhone}？`).then(() => {
          uni.makePhoneCall({
            phoneNumber: this.servicePhone,
            fail: () => this.$modal.msgError('无法调用拨号功能，请手动拨打')
          })
        })
      },
      openLegal(resourceKey) {
        const resource = getResourceByKey(resourceKey)
        if (!resource) {
          this.$modal.msgError('协议内容未登记，请联系系统管理员')
          return
        }

        // 外部内容只把登记 key 交给 webview，绝不把 URL、标题或正文拼到跳转参数中。
        if (resource.url && isTrustedHttpsUrl(resource.url)) {
          this.$tab.navigateTo(`/pages/common/webview/index?key=${encodeURIComponent(resource.key)}`)
          return
        }

        // 没有合法受信 HTTPS 地址时，使用注册表提供的内置回退文本。
        if (resource.fallbackContentId && getContentById(resource.fallbackContentId)) {
          this.$tab.navigateTo(`/pages/common/textview/index?id=${encodeURIComponent(resource.fallbackContentId)}`)
          return
        }
        this.$modal.msgError('协议内容暂不可用，请联系系统管理员')
      }
    }
  }
</script>

<style lang="scss" scoped>
  page {
    background-color: #f8f8f8;
  }

  .about-container {
    min-height: 100%;
    padding-bottom: 40rpx;
    background-color: #f8f8f8;
  }

  .header-section {
    display: flex;
    padding: 40rpx 0 30rpx;
    flex-direction: column;
    align-items: center;
  }

  .app-logo {
    width: 150rpx;
    height: 150rpx;
    margin-bottom: 18rpx;
  }

  .app-name {
    color: #303133;
    font-size: 36rpx;
    font-weight: bold;
  }

  .app-version {
    margin-top: 10rpx;
    color: #909399;
    font-size: 24rpx;
  }

  .content-section {
    padding: 0 30rpx;
  }

  .menu-item-box {
    width: 100%;
  }

  .text-right {
    max-width: 62%;
    color: #909399;
    text-align: right;
    word-break: break-all;
  }

  .link-text {
    color: #3c96f3;
  }

  .copyright {
    margin-top: 50rpx;
    text-align: center;
    line-height: 60rpx;
    color: #999;
    font-size: 24rpx;
  }
</style>

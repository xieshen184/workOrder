<template>
  <view class="setting-container" :style="{height: `${windowHeight}px`}">
    <view class="info-card account-card">
      <view class="card-title">当前账号</view>
      <view class="info-row">
        <text class="info-label">账号</text>
        <text class="info-value">{{ accountName }}</text>
      </view>
    </view>

    <view class="menu-list">
      <view class="list-cell list-cell-arrow" @click="handleToPwd">
        <view class="menu-item-box">
          <view class="iconfont icon-password menu-icon"></view>
          <view>修改密码</view>
        </view>
      </view>
      <view class="list-cell list-cell-arrow" @click="handleToUpgrade">
        <view class="menu-item-box">
          <view class="iconfont icon-refresh menu-icon"></view>
          <view>检查更新</view>
        </view>
      </view>
      <view class="list-cell list-cell-arrow" @click="handleCleanTmp">
        <view class="menu-item-box">
          <view class="iconfont icon-clean menu-icon"></view>
          <view>清理缓存</view>
        </view>
      </view>
    </view>

    <view class="info-card device-card">
      <view class="card-title">当前设备</view>
      <view class="info-row">
        <text class="info-label">平台</text>
        <text class="info-value">{{ deviceInfo.platform }}</text>
      </view>
      <view class="info-row">
        <text class="info-label">系统</text>
        <text class="info-value">{{ deviceInfo.system }}</text>
      </view>
      <view class="info-row">
        <text class="info-label">设备</text>
        <text class="info-value">{{ deviceInfo.model }}</text>
      </view>
      <view class="info-row">
        <text class="info-label">应用版本</text>
        <text class="info-value">{{ appName }} v{{ appVersion }}</text>
      </view>
    </view>

    <view class="info-card cache-card">
      <view class="cache-header">
        <view class="card-title">当前缓存</view>
        <text class="cache-total">{{ cacheActiveCount }} 项 · {{ cacheTotalSizeText }}</text>
      </view>
      <view v-for="item in cacheItems" :key="item.key" class="cache-row">
        <text class="info-label">{{ item.label }}</text>
        <text class="info-value">{{ item.exists ? item.sizeText : '无' }}</text>
      </view>
      <view v-if="cacheActiveCount === 0" class="cache-empty">
        当前没有可清理的非业务缓存
      </view>
      <view class="cache-note">仅处理登记的非业务缓存，不会清除登录状态、用户资料或工单草稿。</view>
    </view>

    <view class="cu-list menu">
      <view class="cu-item item-box">
        <view class="content text-center" @click="handleLogout">
          <text class="text-black">退出登录</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
  import config from '@/config'
  import { clearRegisteredCache, getCacheSummary } from '@/utils/cacheManager'
  import { applyReadyUpdate, getUpdateStatus, initUpdateManager } from '@/utils/updateManager'

  const appInfo = config && config.appInfo ? config.appInfo : {}
  const platformNames = {
    android: 'Android',
    ios: 'iOS',
    windows: 'Windows',
    mac: 'macOS',
    h5: 'H5',
    'mp-weixin': '微信小程序',
    devtools: '微信开发者工具'
  }

  function getSystemInfo() {
    try {
      return uni.getSystemInfoSync() || {}
    } catch (error) {
      return {}
    }
  }

  export default {
    data() {
      const systemInfo = getSystemInfo()
      const platform = systemInfo.uniPlatform || systemInfo.platform || ''
      return {
        windowHeight: systemInfo.windowHeight || 0,
        appName: appInfo.name || '应用',
        appVersion: appInfo.version || '未知',
        accountName: '未读取到账号信息',
        deviceInfo: {
          platform: platformNames[platform] || platform || '未知平台',
          system: systemInfo.system || '未知系统',
          model: systemInfo.model || '未知设备'
        },
        cacheItems: [],
        cacheActiveCount: 0,
        cacheTotalSizeText: '0 B'
      }
    },
    onShow() {
      this.refreshAccount()
      this.refreshDevice()
      this.refreshCache()
    },
    methods: {
      refreshAccount() {
        const user = this.$store && this.$store.state && this.$store.state.user
          ? this.$store.state.user
          : {}
        this.accountName = user.name || '未读取到账号信息'
      },
      refreshDevice() {
        const systemInfo = getSystemInfo()
        const platform = systemInfo.uniPlatform || systemInfo.platform || ''
        this.deviceInfo = {
          platform: platformNames[platform] || platform || '未知平台',
          system: systemInfo.system || '未知系统',
          model: systemInfo.model || '未知设备'
        }
      },
      refreshCache() {
        const summary = getCacheSummary()
        this.cacheItems = summary.items
        this.cacheActiveCount = summary.activeCount
        this.cacheTotalSizeText = summary.totalSizeText
      },
      handleToPwd() {
        this.$tab.navigateTo('/pages/mine/pwd/index')
      },
      handleToUpgrade() {
        // #ifdef MP-WEIXIN
        initUpdateManager()
        const status = getUpdateStatus()
        if (!status.supported) {
          this.$modal.showToast('当前微信环境暂不支持在线检查更新')
          return
        }
        if (status.ready) {
          this.$modal.confirm('新版本已准备好，是否立即重启应用？').then(() => {
            if (!applyReadyUpdate()) this.$modal.msgError('应用更新失败，请重新打开小程序')
          })
          return
        }
        if (status.failed) {
          this.$modal.msgError('新版本下载失败，请重新打开小程序后重试')
          return
        }
        if (status.hasUpdate) {
          this.$modal.showToast('发现新版本，微信正在后台下载')
          return
        }
        if (status.checked) {
          this.$modal.showToast(`当前已是最新版本（v${this.appVersion}）`)
          return
        }
        this.$modal.showToast('微信将在启动时自动检查更新，当前结果尚未返回')
        // #endif
        // #ifndef MP-WEIXIN
        this.$modal.showToast('当前平台不支持在线检查更新，请通过应用分发渠道或联系系统管理员获取最新版本')
        // #endif
      },
      handleCleanTmp() {
        const summary = getCacheSummary()
        if (summary.activeCount === 0) {
          this.$modal.showToast('当前没有可清理的非业务缓存')
          return
        }
        this.$modal.confirm(`将清理 ${summary.activeCount} 项登记的非业务缓存，不会影响登录状态和业务数据，是否继续？`).then(() => {
          const result = clearRegisteredCache()
          this.refreshCache()
          if (result.cleared.length > 0) {
            this.$modal.msgSuccess('缓存清理完成')
          } else {
            this.$modal.showToast('没有可清理的缓存')
          }
        })
      },
      handleLogout() {
        this.$modal.confirm('确定注销并退出系统吗？').then(() => {
          // 退出登录必须沿用真实 store action，由 store 负责服务端注销和登录态清理。
          this.$store.dispatch('LogOut').then(() => {}).finally(() => {
            this.$tab.reLaunch('/pages/login')
          })
        })
      }
    }
  }
</script>

<style lang="scss" scoped>
  page {
    background-color: #f8f8f8;
  }

  .setting-container {
    padding-bottom: 30rpx;
    background-color: #f8f8f8;
  }

  .info-card {
    margin: 24rpx 30rpx 0;
    padding: 26rpx 30rpx;
    background-color: #FFFFFF;
    border-radius: 12rpx;
    box-shadow: 0 0 10rpx rgba(193, 193, 193, 0.16);
  }

  .card-title {
    color: #303133;
    font-size: 30rpx;
    font-weight: bold;
    margin-bottom: 16rpx;
  }

  .info-row,
  .cache-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    min-height: 54rpx;
    font-size: 26rpx;
  }

  .info-label {
    color: #909399;
    flex-shrink: 0;
  }

  .info-value {
    color: #303133;
    max-width: 70%;
    text-align: right;
    word-break: break-all;
  }

  .menu-list {
    margin-top: 24rpx;
  }

  .cache-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .cache-total {
    color: #909399;
    font-size: 24rpx;
  }

  .cache-empty,
  .cache-note {
    color: #909399;
    font-size: 24rpx;
    line-height: 38rpx;
  }

  .cache-empty {
    padding: 8rpx 0;
  }

  .cache-note {
    margin-top: 10rpx;
    padding-top: 16rpx;
    border-top: 1rpx solid #F5F5F5;
  }

  .item-box {
    background-color: #FFFFFF;
    margin: 30rpx;
    display: flex;
    flex-direction: row;
    justify-content: center;
    align-items: center;
    padding: 10rpx;
    border-radius: 8rpx;
    color: #303133;
    font-size: 32rpx;
  }
</style>

<template>
  <view class="mine-container" :style="{height: `${windowHeight}px`}">
    <!--顶部个人信息栏-->
    <view class="header-section">
      <view class="flex padding justify-between">
        <view class="flex align-center">
          <view v-if="!avatar" class="cu-avatar xl round bg-white">
            <view class="iconfont icon-people text-gray icon"></view>
          </view>
          <image v-if="avatar" @click="handleToAvatar" :src="avatar" class="cu-avatar xl round" mode="widthFix">
          </image>
          <view v-if="!profileName" @click="handleToLogin" class="login-tip">
            点击登录
          </view>
          <view v-if="profileName" @click="handleToInfo" class="user-info">
            <view class="u_title">
              {{ profileName }}
            </view>
            <view v-if="organization" class="u_subtitle">{{ organization }}</view>
          </view>
        </view>
        <view @click="handleToInfo" class="flex align-center">
          <text>个人信息</text>
          <view class="iconfont icon-right"></view>
        </view>
      </view>
    </view>

    <view class="content-section">
      <view class="menu-list">
        <view class="list-cell list-cell-arrow" @click="handleToInfo">
          <view class="menu-item-box">
            <view class="iconfont icon-user menu-icon"></view>
            <view>个人信息</view>
          </view>
        </view>
        <view class="list-cell list-cell-arrow" @click="handleToPwd">
          <view class="menu-item-box">
            <view class="iconfont icon-password menu-icon"></view>
            <view>修改密码</view>
          </view>
        </view>
        <view class="list-cell list-cell-arrow" @click="handleNotifications">
          <view class="menu-item-box">
            <view class="iconfont icon-service menu-icon"></view>
            <view>消息通知</view>
          </view>
          <text v-if="unreadCount > 0" class="notification-badge">
            {{ unreadCount > 99 ? '99+' : unreadCount }}
          </text>
        </view>
        <view class="list-cell list-cell-arrow" @click="handleHelp">
          <view class="menu-item-box">
            <view class="iconfont icon-help menu-icon"></view>
            <view>常见问题</view>
          </view>
        </view>
        <view class="list-cell list-cell-arrow" @click="handleAbout">
          <view class="menu-item-box">
            <view class="iconfont icon-aixin menu-icon"></view>
            <view>关于我们</view>
          </view>
        </view>
        <view class="list-cell list-cell-arrow" @click="handleToSetting">
          <view class="menu-item-box">
            <view class="iconfont icon-setting menu-icon"></view>
            <view>应用设置</view>
          </view>
        </view>
        <view class="list-cell" @click="handleLogout">
          <view class="menu-item-box">
            <view class="iconfont icon-logout menu-icon"></view>
            <view>退出登录</view>
          </view>
        </view>
      </view>

    </view>
  </view>
</template>

<script>
  import { getUnreadNotificationCount } from '@/api/notification'
  import { getUserProfile } from '@/api/system/user'
  import { getToken } from '@/utils/auth'

  export default {
    data() {
      return {
        profile: {},
        unreadCount: 0,
        unreadLoading: false,
        profileLoading: false
      }
    },
    computed: {
      avatar() {
        return this.$store.state.user.avatar
      },
      windowHeight() {
        return uni.getSystemInfoSync().windowHeight - 50
      },
      profileName() {
        return this.profile.nickName || this.$store.state.user.name
      },
      organization() {
        const deptName = this.profile.dept && this.profile.dept.deptName
        return deptName || this.profile.postGroup || ''
      }
    },
    onShow() {
      this.loadUnreadCount()
      this.loadProfileSummary()
    },
    methods: {
      async loadProfileSummary() {
        if (!getToken() || this.profileLoading) return
        this.profileLoading = true
        try {
          const response = await getUserProfile()
          const user = response && response.data ? response.data : {}
          this.profile = Object.assign({}, user, { postGroup: response.postGroup || '' })
        } catch (error) {
          // 资料摘要失败不阻塞个人中心入口，继续使用登录时缓存的用户名和头像。
        } finally {
          this.profileLoading = false
        }
      },
      async loadUnreadCount() {
        if (!getToken()) {
          this.unreadCount = 0
          return
        }
        if (this.unreadLoading) return
        this.unreadLoading = true
        try {
          const response = await getUnreadNotificationCount()
          const result = response && response.data !== undefined ? response.data : response
          if (!result || result.total === undefined || !Number.isFinite(Number(result.total))) {
            throw new Error('未读消息数量响应缺少 total')
          }
          this.unreadCount = Math.max(0, Number(result.total))
        } catch (error) {
          // 未读数失败时保留上次成功值，不阻塞“我的”页面其它入口。
        } finally {
          this.unreadLoading = false
        }
      },
      handleNotifications() {
        this.$tab.navigateTo('/pages/notification/index')
      },
      handleToInfo() {
        this.$tab.navigateTo('/pages/mine/info/index')
      },
      handleToPwd() {
        this.$tab.navigateTo('/pages/mine/pwd/index')
      },
      handleToSetting() {
        this.$tab.navigateTo('/pages/mine/setting/index')
      },
      handleToLogin() {
        this.$tab.reLaunch('/pages/login')
      },
      handleToAvatar() {
        this.$tab.navigateTo('/pages/mine/avatar/index')
      },
      handleHelp() {
        this.$tab.navigateTo('/pages/mine/help/index')
      },
      handleAbout() {
        this.$tab.navigateTo('/pages/mine/about/index')
      },
      handleLogout() {
        this.$modal.confirm('确定退出当前账号吗？').then(() => {
          this.$store.dispatch('LogOut').finally(() => {
            this.$tab.reLaunch('/pages/login')
          })
        })
      }
    }
  }
</script>

<style lang="scss" scoped>
  page {
    background-color: #f5f6f7;
  }

  .mine-container {
    width: 100%;
    height: 100%;


    .header-section {
      padding: 15px 15px 45px 15px;
      background-color: #3c96f3;
      color: white;

      .login-tip {
        font-size: 18px;
        margin-left: 10px;
      }

      .cu-avatar {
        border: 2px solid #eaeaea;

        .icon {
          font-size: 40px;
        }
      }

      .user-info {
        margin-left: 15px;

        .u_title {
          font-size: 18px;
          line-height: 30px;
        }

        .u_subtitle {
          font-size: 13px;
          line-height: 24px;
          opacity: 0.9;
        }
      }
    }

    .content-section {
      position: relative;
      top: -50px;

      .menu-list {
        margin: 15px;
        overflow: hidden;
        border-radius: 8px;
        background: #ffffff;
      }
    }

    .notification-badge {
      position: absolute;
      top: 50%;
      right: 62rpx;
      min-width: 32rpx;
      padding: 2rpx 8rpx;
      box-sizing: border-box;
      border-radius: 20rpx;
      background: #F53F3F;
      color: #FFFFFF;
      font-size: 20rpx;
      line-height: 28rpx;
      text-align: center;
      transform: translateY(-50%);
    }
  }
</style>

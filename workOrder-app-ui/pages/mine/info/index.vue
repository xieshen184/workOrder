<template>
  <view class="container">
    <app-page-state
      v-if="loading && !hasLoaded"
      type="loading"
      title="个人信息加载中"
      description="正在读取最新账户资料"
      :show-action="false"
    />

    <app-page-state
      v-else-if="loadError && !hasLoaded"
      type="error"
      :description="loadError"
      @action="retryLoad"
    />

    <view v-else>
      <view v-if="loading" class="refresh-tip">正在刷新个人信息...</view>
      <view v-if="loadError" class="inline-error">
        <text>{{ loadError }}</text>
        <button class="retry-button" size="mini" type="primary" @click="retryLoad">重试</button>
      </view>

      <uni-list>
        <uni-list-item
          title="头像"
          :thumb="avatarUrl"
          thumbSize="lg"
          :showExtraIcon="!avatarUrl"
          :extraIcon="{ type: 'person-filled' }"
          :rightText="avatarUrl ? '点击修改' : '未设置'"
          showArrow
          clickable
          @click="toAvatar"
        />
        <uni-list-item showExtraIcon="true" :extraIcon="{type: 'person-filled'}" title="昵称" :rightText="displayText(user.nickName)" />
        <uni-list-item showExtraIcon="true" :extraIcon="{type: 'phone-filled'}" title="手机号码" :rightText="maskedPhone" />
        <uni-list-item showExtraIcon="true" :extraIcon="{type: 'email-filled'}" title="邮箱" :rightText="displayText(user.email)" />
        <uni-list-item showExtraIcon="true" :extraIcon="{type: 'auth-filled'}" title="岗位" :rightText="postGroup" />
        <uni-list-item showExtraIcon="true" :extraIcon="{type: 'staff-filled'}" title="角色" :rightText="roleGroup" />
        <uni-list-item showExtraIcon="true" :extraIcon="{type: 'calendar-filled'}" title="创建日期" :rightText="displayText(user.createTime)" />
        <uni-list-item
          showExtraIcon="true"
          :extraIcon="{type: 'compose'}"
          title="编辑资料"
          rightText=""
          showArrow
          clickable
          @click="toEdit"
        />
      </uni-list>
    </view>
  </view>
</template>

<script>
  import config from '@/config'
  import { getUserProfile } from "@/api/system/user"
  import AppPageState from '@/components/AppPageState/AppPageState.vue'

  const baseUrl = config.baseUrl

  export default {
    components: { AppPageState },
    data() {
      return {
        user: {},
        roleGroup: "未设置",
        postGroup: "未设置",
        loading: false,
        hasLoaded: false,
        loadError: ""
      }
    },
    computed: {
      avatarUrl() {
        const avatar = this.user.avatar || (this.$store.state.user && this.$store.state.user.avatar)
        return this.normalizeAvatarUrl(avatar)
      },
      maskedPhone() {
        const phone = String(this.user.phonenumber || '').trim()
        if (!phone) return '未设置'
        if (/^\d{11}$/.test(phone)) return phone.slice(0, 3) + '****' + phone.slice(-4)
        if (phone.length > 7) return phone.slice(0, 3) + '****' + phone.slice(-4)
        return phone.length > 4 ? phone.slice(0, 2) + '***' + phone.slice(-2) : '****'
      }
    },
    onShow() {
      // 页面从头像/编辑页返回时重新读取，避免展示过期的个人资料。
      this.getUser()
    },
    methods: {
      async getUser() {
        if (this.loading) return
        this.loading = true
        this.loadError = ''
        try {
          const response = await getUserProfile()
          const profile = response && response.data
          if (!profile || typeof profile !== 'object') throw new Error('个人信息响应无效')
          this.user = profile
          this.roleGroup = this.formatGroup(response.roleGroup)
          this.postGroup = this.formatGroup(response.postGroup)
          this.hasLoaded = true
        } catch (error) {
          // 刷新失败不清空上一份成功资料；首次失败保留重试入口。
          this.loadError = '个人信息加载失败，请重试'
        } finally {
          this.loading = false
        }
      },
      retryLoad() {
        this.getUser()
      },
      displayText(value) {
        const text = String(value == null ? '' : value).trim()
        return text || '未设置'
      },
      formatGroup(value) {
        if (Array.isArray(value)) {
          const text = value.filter(item => item != null && String(item).trim() !== '').join('、')
          return text || '未设置'
        }
        return this.displayText(value)
      },
      normalizeAvatarUrl(value) {
        const avatar = String(value || '').trim()
        if (!avatar) return ''
        if (/^(https?:|data:|blob:)/i.test(avatar)) return avatar
        const prefix = String(baseUrl || '').replace(/\/+$/, '')
        return prefix + '/' + avatar.replace(/^\/+/, '')
      },
      toAvatar() {
        this.$tab.navigateTo('/pages/mine/avatar/index')
      },
      toEdit() {
        this.$tab.navigateTo('/pages/mine/info/edit')
      }
    }
  }
</script>

<style lang="scss">
  page {
    background-color: #ffffff;
  }

  .retry-button {
    display: inline-block;
    margin: 0;
    padding: 0 24rpx;
    line-height: 64rpx;
  }

  .refresh-tip {
    padding: 18rpx 30rpx;
    color: #999999;
    font-size: 24rpx;
    text-align: center;
  }

  .inline-error {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16rpx 30rpx;
    color: #dd524d;
    font-size: 24rpx;
  }

  .inline-error text {
    flex: 1;
    margin-right: 20rpx;
  }
</style>

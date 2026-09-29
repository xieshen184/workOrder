<template>
  <view class="pwd-retrieve-container">
    <uni-forms ref="form" :model="user" :rules="rules" labelWidth="80px">
      <uni-forms-item name="oldPassword" label="旧密码">
        <uni-easyinput type="password" passwordIcon v-model="user.oldPassword" maxlength="50" placeholder="请输入旧密码" />
      </uni-forms-item>
      <uni-forms-item name="newPassword" label="新密码">
        <uni-easyinput type="password" passwordIcon v-model="user.newPassword" maxlength="20" placeholder="请输入新密码" />
      </uni-forms-item>
      <uni-forms-item name="confirmPassword" label="确认密码">
        <uni-easyinput type="password" passwordIcon v-model="user.confirmPassword" maxlength="20" placeholder="请确认新密码" />
      </uni-forms-item>
      <view class="password-tip">密码长度为 8-20 位，且必须同时包含字母和数字</view>
      <button type="primary" :loading="submitting" :disabled="submitting" @click="submit">
        {{ submitting ? '提交中...' : '提交' }}
      </button>
    </uni-forms>
  </view>
</template>

<script>
  import { updateUserPwd } from "@/api/system/user"
  import { removeToken } from "@/utils/auth"

  export default {
    data() {
      return {
        user: {
          oldPassword: "",
          newPassword: "",
          confirmPassword: ""
        },
        submitting: false,
        rules: {
          oldPassword: {
            rules: [{
              required: true,
              errorMessage: '旧密码不能为空'
            }]
          },
          newPassword: {
            rules: [{
              required: true,
              errorMessage: '新密码不能为空'
            }, {
              minLength: 8,
              maxLength: 20,
              errorMessage: '新密码长度必须为8-20位'
            }, {
              pattern: /^(?=.*[A-Za-z])(?=.*\d)\S{8,20}$/,
              errorMessage: '新密码必须同时包含字母和数字'
            }, {
              validateFunction: (rule, value, data) => {
                if (value && data && value === data.oldPassword) return '新密码不能与旧密码相同'
                return null
              },
              errorMessage: '新密码不能与旧密码相同'
            }]
          },
          confirmPassword: {
            rules: [{
              required: true,
              errorMessage: '确认密码不能为空'
            }, {
              validateFunction: (rule, value, data) => {
                if (value !== (data && data.newPassword)) return '两次输入的密码不一致'
                return null
              },
              errorMessage: '两次输入的密码不一致'
            }]
          }
        }
      }
    },
    onReady() {
      if (this.$refs.form) this.$refs.form.setRules(this.rules)
    },
    methods: {
      async submit() {
        if (this.submitting) return
        this.submitting = true

        // 校验错误由表单组件展示；这里只把网络请求失败作为提交失败处理。
        try {
          await this.$refs.form.validate()
        } catch (validationError) {
          this.submitting = false
          return
        }

        try {
          await updateUserPwd(this.user.oldPassword, this.user.newPassword)
        } catch (error) {
          // 请求层已展示服务端的旧密码剩余次数或网络错误；此处只保留输入并解除提交锁。
          this.submitting = false
          return
        }

        // 服务端已经让当前令牌失效，这里只清理本机身份，不再用失效令牌调用注销接口。
        this.clearLocalAuth()
        this.user.oldPassword = ''
        this.user.newPassword = ''
        this.user.confirmPassword = ''
        this.submitting = false
        uni.showToast({ title: '密码修改成功，请重新登录', icon: 'success', duration: 1500 })
        uni.reLaunch({ url: '/pages/login' })
      },
      clearLocalAuth() {
        removeToken()
        this.$store.commit('SET_TOKEN', '')
        this.$store.commit('SET_ID', '')
        this.$store.commit('SET_NAME', '')
        this.$store.commit('SET_AVATAR', '')
        this.$store.commit('SET_ROLES', [])
        this.$store.commit('SET_PERMISSIONS', [])
      }
    }
  }
</script>

<style lang="scss" scoped>
  page {
    background-color: #ffffff;
  }

  .pwd-retrieve-container {
    padding: 36rpx 15px 15px;
  }

  .password-tip {
    margin: -8rpx 0 24rpx 80px;
    color: #999999;
    font-size: 24rpx;
    line-height: 36rpx;
  }
</style>

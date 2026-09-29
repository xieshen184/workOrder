<template>
  <view class="container">
    <app-page-state
      v-if="loading && !profileLoaded"
      type="loading"
      title="个人信息加载中"
      description="正在读取可编辑资料"
      :show-action="false"
    />

    <app-page-state
      v-else-if="loadError && !profileLoaded"
      type="error"
      :description="loadError"
      @action="getUser"
    />

    <view v-else class="example">
      <view v-if="loadError" class="inline-error">
        <text>{{ loadError }}</text>
        <button class="retry-button" size="mini" type="primary" @click="getUser">重试</button>
      </view>
      <uni-forms ref="form" :model="user" :rules="rules" labelWidth="80px">
        <uni-forms-item label="用户昵称" name="nickName">
          <uni-easyinput v-model="user.nickName" trim maxlength="30" placeholder="请输入昵称" />
        </uni-forms-item>
        <uni-forms-item label="手机号码" name="phonenumber">
          <uni-easyinput v-model="user.phonenumber" trim maxlength="11" placeholder="请输入手机号码" />
        </uni-forms-item>
        <uni-forms-item label="邮箱" name="email">
          <uni-easyinput v-model="user.email" trim maxlength="50" placeholder="请输入邮箱" />
        </uni-forms-item>
        <uni-forms-item label="性别" name="sex" required>
          <uni-data-checkbox v-model="user.sex" :localdata="sexs" />
        </uni-forms-item>
      </uni-forms>
      <button
        type="primary"
        :loading="submitting"
        :disabled="submitting || loading || !hasChanges"
        @click="submit"
      >{{ submitting ? '保存中...' : '保存' }}</button>
    </view>
  </view>
</template>

<script>
  import { getUserProfile, updateUserProfile } from "@/api/system/user"
  import AppPageState from '@/components/AppPageState/AppPageState.vue'

  const EDITABLE_FIELDS = ['nickName', 'phonenumber', 'email', 'sex']

  export default {
    components: { AppPageState },
    data() {
      return {
        user: {
          nickName: "",
          phonenumber: "",
          email: "",
          sex: ""
        },
        originalUser: {
          nickName: "",
          phonenumber: "",
          email: "",
          sex: ""
        },
        sexs: [{
          text: '男',
          value: "0"
        }, {
          text: '女',
          value: "1"
        }, {
          text: '未知',
          value: "2"
        }],
        loading: false,
        profileLoaded: false,
        loadError: "",
        submitting: false,
        allowLeave: false,
        confirmingLeave: false,
        rules: {
          nickName: {
            rules: [{
              required: true,
              errorMessage: '用户昵称不能为空'
            }, {
              minLength: 1,
              maxLength: 30,
              errorMessage: '用户昵称长度不能超过30个字符'
            }]
          },
          phonenumber: {
            rules: [{
              required: true,
              errorMessage: '手机号码不能为空'
            }, {
              pattern: /^1[3-9]\d{9}$/,
              errorMessage: '请输入正确的手机号码'
            }]
          },
          email: {
            rules: [{
              validateFunction: (rule, value) => {
                if (!value) return null
                return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value) ? null : '请输入正确的邮箱地址'
              },
              errorMessage: '请输入正确的邮箱地址'
            }]
          },
          sex: {
            rules: [{
              required: true,
              errorMessage: '请选择性别'
            }]
          }
        }
      }
    },
    computed: {
      hasChanges() {
        if (!this.profileLoaded) return false
        return EDITABLE_FIELDS.some(field => this.normalizeValue(this.user[field]) !== this.originalUser[field])
      }
    },
    onLoad() {
      this.getUser()
    },
    onReady() {
      if (this.$refs.form) this.$refs.form.setRules(this.rules)
    },
    onBackPress() {
      if (this.allowLeave) return false
      if (this.submitting) {
        this.$u.toast('正在保存，请稍候')
        return true
      }
      if (!this.hasChanges) return false
      this.confirmLeave()
      return true
    },
    methods: {
      async getUser() {
        if (this.loading || this.submitting) return
        if (this.hasChanges) {
          this.$u.toast('当前有未保存修改，请先保存')
          return
        }
        this.loading = true
        this.loadError = ''
        try {
          const response = await getUserProfile()
          const profile = response && response.data
          if (!profile || typeof profile !== 'object') throw new Error('个人信息响应无效')
          const editable = this.getEditableValues(profile)
          this.user = Object.assign({}, profile, editable)
          this.originalUser = editable
          this.profileLoaded = true
          this.$nextTick(() => {
            if (this.$refs.form && this.$refs.form.clearValidate) this.$refs.form.clearValidate()
          })
        } catch (error) {
          this.loadError = '个人信息加载失败，请重试'
        } finally {
          this.loading = false
        }
      },
      getEditableValues(source) {
        const data = source || {}
        return {
          nickName: this.normalizeValue(data.nickName),
          phonenumber: this.normalizeValue(data.phonenumber),
          email: this.normalizeValue(data.email),
          sex: this.normalizeValue(data.sex)
        }
      },
      normalizeValue(value) {
        return value == null ? '' : String(value).trim()
      },
      async submit() {
        if (this.submitting || this.loading || !this.hasChanges) return
        this.submitting = true

        // 校验失败由 uni-forms 展示字段错误，不能被当成网络失败提示。
        try {
          await this.$refs.form.validate()
        } catch (validationError) {
          this.submitting = false
          return
        }

        const editable = this.getEditableValues(this.user)
        // 接口只提交允许本人修改的字段，账号、岗位、角色等只读字段不回传。
        const payload = editable
        try {
          await updateUserProfile(payload)
        } catch (error) {
          // 请求失败不回填服务端数据，保留用户输入以便直接重试。
          this.$u.toast('保存失败，已保留填写内容，请重试')
          this.submitting = false
          return
        }

        this.user = Object.assign({}, this.user, editable)
        this.originalUser = editable
        // 更新成功后尽量刷新全局用户信息；刷新失败不回滚已成功的资料修改。
        try {
          await this.$store.dispatch('GetInfo')
        } catch (refreshError) {
          // 个人信息页返回时还会重新请求真实资料，避免用本地猜测覆盖 store。
        }
        this.allowLeave = true
        this.$modal.msgSuccess('修改成功')
        this.submitting = false
        this.$tab.navigateBack()
      },
      confirmLeave() {
        if (this.confirmingLeave) return
        this.confirmingLeave = true
        uni.showModal({
          title: '提示',
          content: '当前内容尚未保存，确定离开吗？',
          cancelText: '继续编辑',
          confirmText: '离开',
          success: result => {
            this.confirmingLeave = false
            if (!result.confirm) return
            this.allowLeave = true
            uni.navigateBack({
              delta: 1,
              fail: () => {
                this.allowLeave = false
                this.$u.toast('返回失败，请重试')
              }
            })
          },
          fail: () => {
            this.confirmingLeave = false
          }
        })
      }
    }
  }
</script>

<style lang="scss" scoped>
  page {
    background-color: #ffffff;
  }

  .example {
    padding: 15px;
    background-color: #fff;
  }

  .retry-button {
    display: inline-block;
    margin: 0;
    padding: 0 24rpx;
    line-height: 64rpx;
  }

  .inline-error {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 0 20rpx;
    color: #dd524d;
    font-size: 24rpx;
  }

  .inline-error text {
    flex: 1;
    margin-right: 20rpx;
  }
</style>

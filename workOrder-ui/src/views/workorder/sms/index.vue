<template>
  <div class="app-container sms-page">
    <div class="page-header">
      <div>
        <h3>短信账户</h3>
        <p>查看短信供应商账户状态、余额和发送策略。</p>
      </div>
      <el-button
        v-hasPermi="['workorder:sms:refresh']"
        type="primary"
        plain
        icon="el-icon-refresh"
        :loading="accountLoading"
        @click="refreshAccount"
      >刷新账户</el-button>
    </div>

    <el-alert
      v-if="accountError && account"
      :title="accountError"
      type="error"
      show-icon
      :closable="false"
      class="page-alert"
    >
      <el-button type="text" size="mini" @click="refreshAccount">重试</el-button>
    </el-alert>

    <div v-if="accountLoading && !account" class="state-panel">
      <i class="el-icon-loading state-icon" />
      <p>正在加载短信账户信息…</p>
    </div>
    <div v-else-if="accountError && !account" class="state-panel state-panel-error">
      <i class="el-icon-warning-outline state-icon" />
      <p>{{ accountError }}</p>
      <el-button type="primary" size="small" @click="loadAccount">重新加载</el-button>
    </div>
    <el-empty v-else-if="!account" description="暂无短信账户数据" :image-size="110" />

    <template v-else>
      <el-alert
        v-if="!account.configured"
        title="短信账户尚未配置，余额和发送统计暂不可用。请完成供应商配置后再启用短信发送。"
        type="warning"
        show-icon
        :closable="false"
        class="page-alert"
      />
      <el-alert
        v-else-if="lowBalance"
        title="短信账户余额已达到预警阈值，请及时充值，避免发送中断。"
        type="warning"
        show-icon
        :closable="false"
        class="page-alert"
      />

      <el-row :gutter="16" class="summary-row">
        <el-col :xs="24" :sm="12" :md="6">
          <div class="summary-card">
            <div class="summary-label">当前可用短信</div>
            <div class="summary-value primary">{{ metricValue(account.availableBalance) }}</div>
            <div class="summary-meta">余额单位以供应商返回为准</div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <div class="summary-card">
            <div class="summary-label">今日发送 / 失败</div>
            <div class="summary-value">{{ metricPair(account.todaySent, account.todayFailed) }}</div>
            <div class="summary-meta">今日累计发送统计</div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <div class="summary-card">
            <div class="summary-label">本月已发送</div>
            <div class="summary-value success">{{ metricValue(account.monthSent) }}</div>
            <div class="summary-meta">本月累计发送条数</div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <div class="summary-card">
            <div class="summary-label">账户状态</div>
            <div class="summary-status">
              <el-tag :type="statusTag(account.status)" size="small">{{ statusText(account.status, account.configured) }}</el-tag>
            </div>
            <div class="summary-meta">最近成功：{{ formatTime(account.lastSuccessTime) }}</div>
          </div>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :xs="24" :md="12">
          <el-card shadow="never" class="info-card">
            <div slot="header" class="card-header">账户信息</div>
            <el-descriptions :column="1" size="small" border>
              <el-descriptions-item label="供应商">{{ account.providerName || '未配置' }}</el-descriptions-item>
              <el-descriptions-item label="账户别名">{{ account.accountAlias || '未配置' }}</el-descriptions-item>
              <el-descriptions-item label="凭据（脱敏）">
                <span class="masked-credential">{{ safeMaskedCredential(account.maskedCredential) }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="最近成功时间">{{ formatTime(account.lastSuccessTime) }}</el-descriptions-item>
              <el-descriptions-item label="最近错误">
                <span :class="{ 'error-text': account.lastError }">{{ account.lastError || '暂无' }}</span>
              </el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-card shadow="never" class="info-card">
            <div slot="header" class="card-header">发送策略</div>
            <el-alert
              v-if="settingsError"
              :title="settingsError"
              type="error"
              show-icon
              :closable="false"
              class="settings-alert"
            />
            <el-form ref="settingsForm" :model="settings" :rules="settingsRules" label-width="120px">
              <el-form-item label="余额预警阈值" prop="warningThreshold">
                <el-input-number
                  v-model="settings.warningThreshold"
                  :min="0"
                  :max="999999999"
                  controls-position="right"
                  style="width: 220px"
                />
                <span class="form-suffix">条</span>
              </el-form-item>
              <el-form-item label="失败自动重试" prop="retryEnabled">
                <el-switch
                  v-model="settings.retryEnabled"
                  :disabled="!account.configured"
                  active-text="开启"
                  inactive-text="关闭"
                />
              </el-form-item>
              <el-form-item>
                <el-button
                  v-hasPermi="['workorder:sms:edit']"
                  type="primary"
                  :loading="settingsSaving"
                  @click="saveSettings"
                >保存设置</el-button>
              </el-form-item>
            </el-form>
          </el-card>
        </el-col>
      </el-row>

      <el-card shadow="never" class="last-refresh-card">
        <span>账户数据由短信供应商返回；刷新失败时保留上次成功数据。</span>
        <span v-if="account.lastSuccessTime" class="last-refresh-time">最近成功同步：{{ formatTime(account.lastSuccessTime) }}</span>
      </el-card>
    </template>
  </div>
</template>

<script>
import { getSmsAccount, refreshSmsAccount, updateSmsAccountSettings } from '@/api/workorder/sms'

function hasOwn(value, key) {
  return value && Object.prototype.hasOwnProperty.call(value, key)
}

function responsePayload(response) {
  let payload = response
  if (hasOwn(payload, 'data')) payload = payload.data
  if (payload && payload.code !== undefined && hasOwn(payload, 'data')) payload = payload.data
  return payload
}

function messageFrom(value) {
  if (!value) return ''
  if (typeof value === 'string') return value
  if (typeof value.msg === 'string') return value.msg
  if (typeof value.message === 'string') return value.message
  if (typeof value.error === 'string') return value.error
  return ''
}

function requestErrorMessage(error, fallback) {
  const response = error && error.response
  const responseData = response && response.data
  return messageFrom(error && error.data) || messageFrom(responseData) || messageFrom(error) || fallback
}

function booleanValue(value) {
  return value === true || value === 1 || String(value).toUpperCase() === '1' || String(value).toUpperCase() === 'TRUE'
}

function numberValue(value) {
  if (value === undefined || value === null || value === '') return null
  const number = Number(value)
  return Number.isNaN(number) ? value : number
}

function normalizeAccount(source) {
  const item = source || {}
  return {
    configured: booleanValue(item.configured),
    providerName: item.providerName || '',
    accountAlias: item.accountAlias || '',
    maskedCredential: item.maskedCredential || '',
    status: item.status || '',
    availableBalance: numberValue(item.availableBalance),
    todaySent: numberValue(item.todaySent),
    todayFailed: numberValue(item.todayFailed),
    monthSent: numberValue(item.monthSent),
    warningThreshold: numberValue(item.warningThreshold),
    retryEnabled: booleanValue(item.retryEnabled),
    lastSuccessTime: item.lastSuccessTime || '',
    lastError: item.lastError || ''
  }
}

export default {
  name: 'WorkorderSmsAccount',
  data() {
    return {
      account: null,
      accountLoading: false,
      accountError: '',
      settingsSaving: false,
      settingsError: '',
      settings: {
        warningThreshold: null,
        retryEnabled: false
      },
      settingsRules: {
        warningThreshold: [
          { required: true, message: '请输入余额预警阈值', trigger: 'change' },
          { type: 'number', min: 0, max: 999999999, message: '预警阈值必须在 0-999999999 之间', trigger: 'change' }
        ],
        retryEnabled: [{ required: true, message: '请选择失败自动重试策略', trigger: 'change' }]
      }
    }
  },
  created() {
    this.loadAccount()
  },
  computed: {
    lowBalance() {
      if (!this.account || !this.account.configured) return false
      const balance = Number(this.account.availableBalance)
      const threshold = Number(this.account.warningThreshold)
      return Number.isFinite(balance) && Number.isFinite(threshold) && balance <= threshold
    }
  },
  methods: {
    loadAccount() {
      return this.fetchAccount(getSmsAccount)
    },
    refreshAccount() {
      if (this.accountLoading) return
      return this.fetchAccount(refreshSmsAccount)
    },
    fetchAccount(fetcher) {
      this.accountLoading = true
      this.accountError = ''
      return fetcher().then(response => {
        const payload = responsePayload(response)
        if (!payload || typeof payload !== 'object' || Array.isArray(payload)) {
          throw new Error('短信账户接口未返回有效数据')
        }
        const account = normalizeAccount(payload)
        this.account = account
        this.settings.warningThreshold = account.warningThreshold
        this.settings.retryEnabled = account.retryEnabled
        this.settingsError = ''
      }).catch(error => {
        // 刷新失败时不清理 account，页面继续展示上次成功返回的数据。
        this.accountError = requestErrorMessage(error, '短信账户加载失败，请稍后重试')
      }).then(() => {
        this.accountLoading = false
      })
    },
    saveSettings() {
      if (this.settingsSaving) return
      this.$refs.settingsForm.validate(valid => {
        if (!valid) return
        this.settingsSaving = true
        this.settingsError = ''
        const payload = {
          warningThreshold: Number(this.settings.warningThreshold),
          retryEnabled: Boolean(this.settings.retryEnabled)
        }
        const retryText = payload.retryEnabled ? '开启' : '关闭'
        this.$modal.confirm(`确认保存余额阈值 ${payload.warningThreshold}，并${retryText}失败自动重试吗？`).then(() => {
          return updateSmsAccountSettings(payload)
        }).then(response => {
          const result = responsePayload(response)
          if (result && typeof result === 'object') this.account = normalizeAccount(result)
          this.settings.warningThreshold = this.account ? this.account.warningThreshold : payload.warningThreshold
          this.settings.retryEnabled = this.account ? this.account.retryEnabled : payload.retryEnabled
          this.$modal.msgSuccess('短信发送设置保存成功')
        }).catch(error => {
          if (error !== 'cancel' && error !== 'close') {
            this.settingsError = requestErrorMessage(error, '短信发送设置保存失败，请稍后重试')
          }
        }).then(() => {
          this.settingsSaving = false
        })
      })
    },
    metricValue(value) {
      if (!this.account || !this.account.configured) return '未配置'
      return value === undefined || value === null || value === '' ? '—' : value
    },
    metricPair(sent, failed) {
      if (!this.account || !this.account.configured) return '未配置'
      return this.displayNumber(sent) + ' / ' + this.displayNumber(failed)
    },
    displayNumber(value) {
      return value === undefined || value === null || value === '' ? '—' : value
    },
    statusText(status, configured) {
      if (!configured) return '未配置'
      return {
        ACTIVE: '正常',
        OK: '正常',
        AVAILABLE: '可用',
        NORMAL: '正常',
        WARNING: '余额预警',
        ERROR: '异常',
        DISABLED: '已停用',
        UNAVAILABLE: '不可用'
      }[String(status || '').toUpperCase()] || status || '未知'
    },
    statusTag(status) {
      const value = String(status || '').toUpperCase()
      if (['ACTIVE', 'OK', 'AVAILABLE', 'NORMAL'].includes(value)) return 'success'
      if (value === 'WARNING') return 'warning'
      if (['ERROR', 'DISABLED', 'UNAVAILABLE'].includes(value)) return 'danger'
      return 'info'
    },
    safeMaskedCredential(value) {
      if (!value) return '未配置'
      const text = String(value)
      // 后端只应返回掩码；即使异常返回非掩码文本，前端也不回显原值。
      return /[*＊•]/.test(text) ? text : '******'
    },
    formatTime(value) {
      if (!value) return '—'
      return this.parseTime ? this.parseTime(value) : value
    }
  }
}
</script>

<style lang="scss" scoped>
.page-header { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 18px; }
.page-header h3 { margin: 0; color: #303133; font-size: 20px; font-weight: 500; }
.page-header p { margin: 8px 0 0; color: #909399; font-size: 13px; }
.page-alert { margin-bottom: 16px; }
.summary-row { margin-bottom: 16px; }
.summary-card { min-height: 122px; padding: 18px 20px; background: #fff; border: 1px solid #ebeef5; border-radius: 6px; }
.summary-label { color: #909399; font-size: 13px; }
.summary-value { margin-top: 10px; color: #303133; font-size: 24px; font-weight: 600; }
.summary-value.primary { color: #409eff; }
.summary-value.success { color: #67c23a; }
.summary-status { margin-top: 15px; min-height: 28px; }
.summary-meta { margin-top: 8px; color: #909399; font-size: 12px; }
.info-card { min-height: 270px; margin-bottom: 16px; }
.card-header { color: #303133; font-weight: 500; }
.masked-credential { color: #606266; letter-spacing: 1px; }
.error-text { color: #f56c6c; word-break: break-word; }
.settings-alert { margin-bottom: 16px; }
.form-suffix { margin-left: 8px; color: #909399; }
.last-refresh-card { color: #909399; font-size: 12px; }
.last-refresh-time { margin-left: 20px; }
.state-panel { padding: 120px 0; color: #909399; text-align: center; }
.state-panel p { margin: 12px 0 16px; }
.state-icon { font-size: 34px; }
.state-panel-error .state-icon { color: #f56c6c; }
@media screen and (max-width: 768px) {
  .page-header { display: block; }
  .page-header .el-button { margin-top: 12px; }
  .last-refresh-time { display: block; margin-top: 8px; margin-left: 0; }
}
</style>

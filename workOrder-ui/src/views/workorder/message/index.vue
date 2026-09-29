<template>
  <div class="app-container message-page">
    <el-tabs v-model="activeTab" type="card" @tab-click="handleTabChange">
      <el-tab-pane label="模板配置" name="templates">
        <el-form
          v-show="showTemplateSearch"
          ref="templateQueryForm"
          :model="templateQuery"
          :inline="true"
          size="small"
          label-width="72px"
          @submit.native.prevent
        >
          <el-form-item label="事件编码" prop="eventCode">
            <el-input
              v-model.trim="templateQuery.eventCode"
              clearable
              placeholder="请输入事件编码"
              style="width: 190px"
              @keyup.enter.native="handleTemplateQuery"
            />
          </el-form-item>
          <el-form-item label="分类" prop="category">
            <el-select v-model="templateQuery.category" clearable placeholder="全部分类" style="width: 140px">
              <el-option label="工单" value="WORK_ORDER" />
              <el-option label="审批" value="APPROVAL" />
              <el-option label="系统" value="SYSTEM" />
            </el-select>
          </el-form-item>
          <el-form-item label="渠道" prop="channel">
            <el-select v-model="templateQuery.channel" clearable placeholder="全部渠道" style="width: 140px">
              <el-option label="应用内" value="IN_APP" />
              <el-option label="短信" value="SMS" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态" prop="enabledFlag">
            <el-select v-model="templateQuery.enabledFlag" clearable placeholder="全部状态" style="width: 120px">
              <el-option label="启用" value="1" />
              <el-option label="停用" value="0" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" @click="handleTemplateQuery">查询</el-button>
            <el-button icon="el-icon-refresh" @click="resetTemplateQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-alert
          v-if="templateListError"
          :title="templateListError"
          type="error"
          show-icon
          :closable="false"
          class="list-alert"
        >
          <el-button type="text" size="mini" @click="getTemplates">重试</el-button>
        </el-alert>

        <el-table v-loading="templateLoading" :data="templates" border stripe row-key="id">
          <el-table-column label="事件编码" prop="eventCode" min-width="170" show-overflow-tooltip />
          <el-table-column label="模板名称" prop="templateName" min-width="160" show-overflow-tooltip>
            <template slot-scope="scope">{{ scope.row.templateName || '未命名模板' }}</template>
          </el-table-column>
          <el-table-column label="分类" width="90" align="center">
            <template slot-scope="scope">{{ categoryText(scope.row.category) }}</template>
          </el-table-column>
          <el-table-column label="渠道" width="100" align="center">
            <template slot-scope="scope">{{ channelText(scope.row.channel) }}</template>
          </el-table-column>
          <el-table-column label="标题模板" min-width="200" show-overflow-tooltip>
            <template slot-scope="scope">{{ scope.row.titleTemplate || '-' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="82" align="center">
            <template slot-scope="scope">
              <el-tag :type="enabledValue(scope.row.enabledFlag) === '1' ? 'success' : 'info'" size="small">
                {{ enabledValue(scope.row.enabledFlag) === '1' ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="最大重试" prop="maxRetry" width="90" align="center" />
          <el-table-column label="更新时间" min-width="160">
            <template slot-scope="scope">{{ formatTime(scope.row.updateTime || scope.row.updateAt) }}</template>
          </el-table-column>
          <el-table-column
            label="操作"
            width="90"
            fixed="right"
            align="center"
            class-name="small-padding fixed-width"
          >
            <template slot-scope="scope">
              <el-button
                v-hasPermi="['workorder:message:template']"
                type="text"
                size="mini"
                icon="el-icon-edit"
                @click="handleEditTemplate(scope.row)"
              >编辑</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-empty v-if="!templateLoading && !templateListError && !templates.length" description="暂无消息模板" :image-size="100" />

        <pagination
          v-show="!templateListError && templateTotal > 0"
          :total="templateTotal"
          :page.sync="templateQuery.pageNum"
          :limit.sync="templateQuery.pageSize"
          @pagination="handleTemplatePagination"
        />
      </el-tab-pane>

      <el-tab-pane label="发送记录" name="records">
        <el-form
          v-show="showRecordSearch"
          ref="recordQueryForm"
          :model="recordQuery"
          :inline="true"
          size="small"
          label-width="72px"
          @submit.native.prevent
        >
          <el-form-item label="状态" prop="status">
            <el-select v-model="recordQuery.status" clearable placeholder="全部状态" style="width: 125px">
              <el-option label="待发送" value="PENDING" />
              <el-option label="处理中" value="PROCESSING" />
              <el-option label="重试中" value="RETRY" />
              <el-option label="成功" value="SUCCESS" />
              <el-option label="已终止" value="DEAD" />
            </el-select>
          </el-form-item>
          <el-form-item label="渠道" prop="channel">
            <el-select v-model="recordQuery.channel" clearable placeholder="全部渠道" style="width: 125px">
              <el-option label="应用内" value="IN_APP" />
              <el-option label="短信" value="SMS" />
            </el-select>
          </el-form-item>
          <el-form-item label="分类" prop="category">
            <el-select v-model="recordQuery.category" clearable placeholder="全部分类" style="width: 125px">
              <el-option label="工单" value="WORK_ORDER" />
              <el-option label="审批" value="APPROVAL" />
              <el-option label="系统" value="SYSTEM" />
            </el-select>
          </el-form-item>
          <el-form-item label="事件编码" prop="eventCode">
            <el-input
              v-model.trim="recordQuery.eventCode"
              clearable
              placeholder="请输入事件编码"
              style="width: 175px"
              @keyup.enter.native="handleRecordQuery"
            />
          </el-form-item>
          <el-form-item label="订单号" prop="orderNo">
            <el-input
              v-model.trim="recordQuery.orderNo"
              clearable
              placeholder="请输入订单号"
              style="width: 175px"
              @keyup.enter.native="handleRecordQuery"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" @click="handleRecordQuery">查询</el-button>
            <el-button icon="el-icon-refresh" @click="resetRecordQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-alert
          v-if="recordListError"
          :title="recordListError"
          type="error"
          show-icon
          :closable="false"
          class="list-alert"
        >
          <el-button type="text" size="mini" @click="getTasks">重试</el-button>
        </el-alert>
        <el-alert
          v-if="recordActionError"
          :title="recordActionError"
          type="error"
          show-icon
          :closable="false"
          class="list-alert"
        />

        <el-table v-loading="recordLoading" :data="tasks" border stripe row-key="id">
          <el-table-column label="发送时间" min-width="160">
            <template slot-scope="scope">{{ formatTime(scope.row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="事件编码" prop="eventCode" min-width="160" show-overflow-tooltip />
          <el-table-column label="分类" width="88" align="center">
            <template slot-scope="scope">{{ categoryText(scope.row.category) }}</template>
          </el-table-column>
          <el-table-column label="渠道" width="95" align="center">
            <template slot-scope="scope">{{ channelText(scope.row.channel) }}</template>
          </el-table-column>
          <el-table-column label="订单号" min-width="155" show-overflow-tooltip>
            <template slot-scope="scope">
              <el-button
                v-if="scope.row.orderNo"
                type="text"
                class="order-link"
                @click="openOrder(scope.row.orderNo)"
              >{{ scope.row.orderNo }}</el-button>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="接收人" min-width="150" show-overflow-tooltip>
            <template slot-scope="scope">
              <div>{{ scope.row.recipientName || '-' }}</div>
              <div class="muted-text">{{ scope.row.recipientAddress || '地址未返回' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="92" align="center">
            <template slot-scope="scope">
              <el-tag :type="taskStatusTag(scope.row.status)" size="small">{{ taskStatusText(scope.row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="重试次数" width="98" align="center">
            <template slot-scope="scope">{{ retryText(scope.row) }}</template>
          </el-table-column>
          <el-table-column label="失败原因" min-width="200" show-overflow-tooltip>
            <template slot-scope="scope">{{ scope.row.lastError || '-' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="92" fixed="right" align="center">
            <template slot-scope="scope">
              <el-button
                v-if="isRetryable(scope.row.status)"
                v-hasPermi="['workorder:message:retry']"
                type="text"
                size="mini"
                :loading="retryingId === scope.row.id"
                :disabled="Boolean(retryingId)"
                @click="retryTask(scope.row)"
              >重试</el-button>
              <span v-else class="muted-text">-</span>
            </template>
          </el-table-column>
        </el-table>

        <el-empty v-if="!recordLoading && !recordListError && !tasks.length" description="暂无发送记录" :image-size="100" />

        <pagination
          v-show="!recordListError && recordTotal > 0"
          :total="recordTotal"
          :page.sync="recordQuery.pageNum"
          :limit.sync="recordQuery.pageSize"
          @pagination="handleRecordPagination"
        />
      </el-tab-pane>
    </el-tabs>

    <el-dialog
      :title="dialogTitle"
      :visible.sync="templateOpen"
      width="900px"
      append-to-body
      :close-on-click-modal="false"
      :show-close="!submitting && !previewLoading"
      @closed="resetTemplateDialog"
    >
      <div v-loading="templateDetailLoading" class="template-dialog-body">
        <el-alert
          v-if="templateDetailError"
          :title="templateDetailError"
          type="error"
          show-icon
          :closable="false"
          class="dialog-alert"
        >
          <el-button type="text" size="mini" @click="loadTemplateDetail(editingTemplateId)">重试</el-button>
        </el-alert>

        <template v-if="!templateDetailLoading && !templateDetailError">
          <el-alert
            v-if="templateSaveError"
            :title="templateSaveError"
            type="error"
            show-icon
            :closable="false"
            class="dialog-alert"
          />
          <el-form ref="templateForm" :model="templateForm" :rules="templateRules" label-width="100px">
            <el-row :gutter="18">
              <el-col :span="12">
                <el-form-item label="事件编码" prop="eventCode">
                  <el-input v-model="templateForm.eventCode" disabled />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="发送渠道" prop="channel">
                  <el-input :value="channelText(templateForm.channel)" disabled />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="18">
              <el-col :span="12">
                <el-form-item label="消息分类" prop="category">
                  <el-input :value="categoryText(templateForm.category)" disabled />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="模板名称" prop="templateName">
                  <el-input
                    v-model.trim="templateForm.templateName"
                    maxlength="64"
                    show-word-limit
                    @input="invalidateTemplatePreview"
                  />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="18">
              <el-col :span="12">
                <el-form-item label="状态" prop="enabledFlag">
                  <el-radio-group v-model="templateForm.enabledFlag" @change="invalidateTemplatePreview">
                    <el-radio label="1">启用</el-radio>
                    <el-radio label="0">停用</el-radio>
                  </el-radio-group>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="最大重试" prop="maxRetry">
                  <el-input-number
                    v-model="templateForm.maxRetry"
                    :min="0"
                    :max="10"
                    controls-position="right"
                    style="width: 180px"
                    @change="invalidateTemplatePreview"
                  />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="标题模板" prop="titleTemplate">
              <el-input
                v-model="templateForm.titleTemplate"
                maxlength="200"
                show-word-limit
                placeholder="请输入标题模板，可使用后端返回的变量"
                @input="invalidateTemplatePreview"
              />
            </el-form-item>
            <el-form-item label="正文模板" prop="contentTemplate">
              <el-input
                v-model="templateForm.contentTemplate"
                type="textarea"
                :rows="6"
                maxlength="2000"
                show-word-limit
                placeholder="请输入正文模板，可使用后端返回的变量"
                @input="invalidateTemplatePreview"
              />
            </el-form-item>
            <el-form-item label="备注" prop="remark">
              <el-input
                v-model="templateForm.remark"
                type="textarea"
                :rows="2"
                maxlength="500"
                show-word-limit
                @input="invalidateTemplatePreview"
              />
            </el-form-item>
          </el-form>

          <el-alert
            v-if="previewError"
            :title="previewError"
            type="error"
            show-icon
            :closable="false"
            class="dialog-alert"
          />
          <el-card shadow="never" class="preview-card">
            <div slot="header" class="preview-header">
              <span>保存前预览</span>
              <span class="preview-hint">修改任一字段后需要重新预览</span>
            </div>
            <div v-if="previewResult" class="preview-content">
              <div class="preview-title">{{ previewResult.title || '-' }}</div>
              <div class="preview-body">{{ previewResult.content || '-' }}</div>
              <div class="preview-variable-title">变量示例</div>
              <el-table
                v-if="previewResult.variables && previewResult.variables.length"
                :data="previewResult.variables"
                border
                size="mini"
              >
                <el-table-column label="变量" prop="name" min-width="160" />
                <el-table-column label="说明" prop="label" min-width="140" />
                <el-table-column label="示例值" prop="sample" min-width="160" />
              </el-table>
              <el-empty v-else description="后端未返回变量说明" :image-size="60" />
            </div>
            <div v-else class="preview-empty">
              <i class="el-icon-view" />
              <span>请先点击“预览”，确认最终文本后再保存</span>
            </div>
          </el-card>
        </template>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button
          v-if="!templateDetailError"
          :loading="previewLoading"
          :disabled="templateDetailLoading || submitting"
          @click="previewTemplate"
        >预览</el-button>
        <el-button
          v-if="!templateDetailError"
          type="primary"
          :loading="submitting"
          :disabled="templateDetailLoading || previewLoading || !previewReady"
          @click="submitTemplate"
        >保存模板</el-button>
        <el-button :disabled="submitting || previewLoading" @click="templateOpen = false">取消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  getNotificationTemplate,
  listNotificationTasks,
  listNotificationTemplates,
  previewNotificationTemplate,
  retryNotificationTask,
  updateNotificationTemplate
} from '@/api/workorder/message'

function hasOwn(value, key) {
  return value && Object.prototype.hasOwnProperty.call(value, key)
}

function responsePayload(response) {
  let payload = response
  if (hasOwn(payload, 'data')) payload = payload.data
  if (payload && payload.code !== undefined && hasOwn(payload, 'data')) payload = payload.data
  return payload
}

function extractRows(response) {
  const payload = responsePayload(response)
  if (Array.isArray(payload)) return payload
  if (payload && Array.isArray(payload.rows)) return payload.rows
  if (payload && Array.isArray(payload.list)) return payload.list
  if (response && Array.isArray(response.rows)) return response.rows
  return []
}

function extractTotal(response, rows) {
  const payload = responsePayload(response)
  const candidates = [
    response && response.total,
    payload && payload.total,
    response && response.data && response.data.total
  ]
  const total = candidates.find(value => typeof value === 'number')
  return total === undefined ? rows.length : total
}

function extractEntity(response) {
  const payload = responsePayload(response)
  if (payload && payload.data && typeof payload.data === 'object' && !Array.isArray(payload.data)) return payload.data
  return payload || {}
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

function enabledFlag(value) {
  return value === true || value === 1 || String(value).toUpperCase() === '1' || String(value).toUpperCase() === 'TRUE'
    ? '1'
    : '0'
}

function defaultTemplate(source) {
  const item = source || {}
  return {
    id: item.id === undefined || item.id === null ? null : item.id,
    eventCode: item.eventCode || '',
    category: item.category || '',
    channel: item.channel || '',
    templateName: item.templateName || '',
    titleTemplate: item.titleTemplate || '',
    contentTemplate: item.contentTemplate || '',
    enabledFlag: enabledFlag(item.enabledFlag === undefined ? item.status : item.enabledFlag),
    maxRetry: item.maxRetry === undefined || item.maxRetry === null || item.maxRetry === '' ? 3 : Number(item.maxRetry),
    remark: item.remark || ''
  }
}

export default {
  name: 'WorkorderMessages',
  data() {
    return {
      activeTab: 'templates',
      showTemplateSearch: true,
      showRecordSearch: true,
      templateLoading: false,
      recordLoading: false,
      templateListError: '',
      recordListError: '',
      recordActionError: '',
      templates: [],
      tasks: [],
      templateTotal: 0,
      recordTotal: 0,
      recordsLoaded: false,
      templateQuery: {
        eventCode: '',
        category: '',
        channel: '',
        enabledFlag: '',
        pageNum: 1,
        pageSize: 10
      },
      recordQuery: {
        status: '',
        channel: '',
        category: '',
        eventCode: '',
        orderNo: '',
        pageNum: 1,
        pageSize: 10
      },
      templateOpen: false,
      dialogTitle: '编辑消息模板',
      templateDetailLoading: false,
      templateDetailError: '',
      templateSaveError: '',
      editingTemplateId: null,
      submitting: false,
      previewLoading: false,
      previewError: '',
      previewResult: null,
      previewReady: false,
      previewFingerprint: '',
      originalEnabledFlag: '1',
      retryingId: null,
      templateForm: defaultTemplate(),
      templateRules: {
        templateName: [{ required: true, message: '模板名称不能为空', trigger: 'blur' }],
        titleTemplate: [{ required: true, message: '标题模板不能为空', trigger: 'blur' }],
        contentTemplate: [{ required: true, message: '正文模板不能为空', trigger: 'blur' }],
        enabledFlag: [{ required: true, message: '请选择模板状态', trigger: 'change' }],
        maxRetry: [
          { required: true, message: '最大重试次数不能为空', trigger: 'change' },
          { type: 'number', min: 0, max: 10, message: '最大重试次数必须在 0-10 之间', trigger: 'change' }
        ]
      }
    }
  },
  created() {
    this.getTemplates()
  },
  methods: {
    handleTabChange(tab) {
      if (tab && tab.name === 'records' && !this.recordsLoaded) this.getTasks()
    },
    buildTemplateQuery() {
      return this.buildPageQuery(this.templateQuery, ['eventCode', 'category', 'channel', 'enabledFlag'])
    },
    buildRecordQuery() {
      return this.buildPageQuery(this.recordQuery, ['status', 'channel', 'category', 'eventCode', 'orderNo'])
    },
    buildPageQuery(source, keys) {
      const query = { pageNum: source.pageNum, pageSize: source.pageSize }
      keys.forEach(key => {
        if (source[key] !== undefined && source[key] !== null && source[key] !== '') query[key] = source[key]
      })
      return query
    },
    getTemplates() {
      this.templateLoading = true
      this.templateListError = ''
      return listNotificationTemplates(this.buildTemplateQuery()).then(response => {
        const rows = extractRows(response)
        this.templates = rows
        this.templateTotal = extractTotal(response, rows)
      }).catch(error => {
        this.templateListError = requestErrorMessage(error, '消息模板加载失败，请稍后重试')
      }).then(() => {
        this.templateLoading = false
      })
    },
    getTasks() {
      this.recordLoading = true
      this.recordListError = ''
      this.recordActionError = ''
      return listNotificationTasks(this.buildRecordQuery()).then(response => {
        const rows = extractRows(response)
        this.tasks = rows
        this.recordTotal = extractTotal(response, rows)
        this.recordsLoaded = true
      }).catch(error => {
        this.recordListError = requestErrorMessage(error, '发送记录加载失败，请稍后重试')
      }).then(() => {
        this.recordLoading = false
      })
    },
    handleTemplateQuery() {
      this.templateQuery.pageNum = 1
      this.getTemplates()
    },
    handleTemplatePagination() {
      this.getTemplates()
    },
    resetTemplateQuery() {
      this.templateQuery.eventCode = ''
      this.templateQuery.category = ''
      this.templateQuery.channel = ''
      this.templateQuery.enabledFlag = ''
      this.templateQuery.pageNum = 1
      this.getTemplates()
    },
    handleRecordQuery() {
      this.recordQuery.pageNum = 1
      this.getTasks()
    },
    handleRecordPagination() {
      this.getTasks()
    },
    resetRecordQuery() {
      this.recordQuery.status = ''
      this.recordQuery.channel = ''
      this.recordQuery.category = ''
      this.recordQuery.eventCode = ''
      this.recordQuery.orderNo = ''
      this.recordQuery.pageNum = 1
      this.getTasks()
    },
    handleEditTemplate(row) {
      const id = row && row.id
      if (id === undefined || id === null || id === '') {
        this.templateListError = '模板缺少有效编号，无法编辑'
        return
      }
      this.templateOpen = true
      this.dialogTitle = '编辑消息模板'
      this.editingTemplateId = id
      this.templateDetailError = ''
      this.templateSaveError = ''
      this.previewError = ''
      this.previewResult = null
      this.previewReady = false
      this.previewFingerprint = ''
      this.originalEnabledFlag = enabledFlag(row.enabledFlag === undefined ? row.status : row.enabledFlag)
      this.templateForm = defaultTemplate(row)
      this.loadTemplateDetail(id)
    },
    loadTemplateDetail(id) {
      if (id === undefined || id === null || id === '') return Promise.resolve()
      this.templateDetailLoading = true
      this.templateDetailError = ''
      return getNotificationTemplate(id).then(response => {
        const entity = extractEntity(response)
        this.templateForm = {
          ...defaultTemplate(),
          ...entity,
          id,
          enabledFlag: enabledFlag(entity.enabledFlag === undefined ? entity.status : entity.enabledFlag),
          maxRetry: entity.maxRetry === undefined || entity.maxRetry === null || entity.maxRetry === '' ? 3 : Number(entity.maxRetry)
        }
        this.originalEnabledFlag = this.templateForm.enabledFlag
        this.previewResult = null
        this.previewReady = false
        this.previewFingerprint = ''
        this.$nextTick(() => {
          if (this.$refs.templateForm) this.$refs.templateForm.clearValidate()
        })
      }).catch(error => {
        this.templateDetailError = requestErrorMessage(error, '模板详情加载失败，请稍后重试')
      }).then(() => {
        this.templateDetailLoading = false
      })
    },
    previewPayload() {
      return {
        eventCode: String(this.templateForm.eventCode || '').trim(),
        channel: String(this.templateForm.channel || '').trim(),
        templateName: String(this.templateForm.templateName || '').trim(),
        titleTemplate: String(this.templateForm.titleTemplate || '').trim(),
        contentTemplate: String(this.templateForm.contentTemplate || '').trim(),
        enabledFlag: enabledFlag(this.templateForm.enabledFlag),
        maxRetry: Number(this.templateForm.maxRetry),
        remark: String(this.templateForm.remark || '').trim()
      }
    },
    templatePayload() {
      const payload = this.previewPayload()
      return {
        templateName: payload.templateName,
        titleTemplate: payload.titleTemplate,
        contentTemplate: payload.contentTemplate,
        enabledFlag: payload.enabledFlag,
        maxRetry: payload.maxRetry,
        remark: payload.remark
      }
    },
    templateFingerprint() {
      return JSON.stringify(this.previewPayload())
    },
    invalidateTemplatePreview() {
      this.previewReady = false
      this.previewFingerprint = ''
      this.previewResult = null
    },
    previewTemplate() {
      if (this.previewLoading || this.templateDetailLoading) return
      this.$refs.templateForm.validate(valid => {
        if (!valid) return
        this.previewLoading = true
        this.previewError = ''
        previewNotificationTemplate(this.previewPayload()).then(response => {
          const result = extractEntity(response)
          if (!result || typeof result !== 'object' || (!hasOwn(result, 'title') && !hasOwn(result, 'content'))) {
            this.previewResult = null
            this.previewReady = false
            this.previewFingerprint = ''
            this.previewError = '预览接口未返回有效内容，请检查后端响应'
            return
          }
          this.previewResult = {
            title: result.title || '',
            content: result.content || '',
            variables: Array.isArray(result.variables) ? result.variables : []
          }
          this.previewReady = true
          this.previewFingerprint = this.templateFingerprint()
        }).catch(error => {
          this.previewResult = null
          this.previewReady = false
          this.previewFingerprint = ''
          this.previewError = requestErrorMessage(error, '模板预览失败，请稍后重试')
        }).then(() => {
          this.previewLoading = false
        })
      })
    },
    submitTemplate() {
      if (this.submitting || !this.previewReady) {
        if (!this.previewReady) this.previewError = '请先预览模板，确认内容后再保存'
        return
      }
      if (this.previewFingerprint !== this.templateFingerprint()) {
        this.invalidateTemplatePreview()
        this.previewError = '模板内容已变化，请重新预览后再保存'
        return
      }
      this.$refs.templateForm.validate(valid => {
        if (!valid) return
        const save = () => {
          this.submitting = true
          this.templateSaveError = ''
          return updateNotificationTemplate(this.editingTemplateId, this.templatePayload()).then(() => {
            this.$modal.msgSuccess('模板保存成功')
            this.templateOpen = false
            return this.getTemplates()
          }).catch(error => {
            this.templateSaveError = requestErrorMessage(error, '模板保存失败，请稍后重试')
          }).then(() => {
            this.submitting = false
          })
        }
        if (this.originalEnabledFlag === '1' && enabledFlag(this.templateForm.enabledFlag) === '0') {
          this.$modal.confirm('模板停用后将不再创建新的通知任务，确认停用该模板吗？').then(save).catch(() => {})
        } else {
          save()
        }
      })
    },
    retryTask(row) {
      if (!row || !this.isRetryable(row.status) || this.retryingId !== null) return
      const id = row.id
      if (id === undefined || id === null || id === '') {
        this.recordActionError = '发送记录缺少有效编号，无法重试'
        return
      }
      this.$modal.confirm('确认重新发送该通知任务吗？').then(() => {
        this.retryingId = id
        this.recordActionError = ''
        return retryNotificationTask(id).then(() => {
          this.$modal.msgSuccess('通知任务已重新加入发送队列')
          return this.getTasks()
        })
      }).catch(error => {
        if (error && error !== 'cancel' && error !== 'close') {
          this.recordActionError = requestErrorMessage(error, '通知任务重试失败，请稍后重试')
        }
      }).then(() => {
        this.retryingId = null
      })
    },
    openOrder(orderNo) {
      if (!orderNo) return
      this.$router.push({ path: '/workorder/orders', query: { keyword: String(orderNo) } }).catch(() => {})
    },
    enabledValue(rowValue) {
      return enabledFlag(rowValue)
    },
    categoryText(value) {
      return { WORK_ORDER: '工单', APPROVAL: '审批', SYSTEM: '系统' }[value] || value || '-'
    },
    channelText(value) {
      return { IN_APP: '应用内', SMS: '短信' }[value] || value || '-'
    },
    taskStatusText(value) {
      return {
        PENDING: '待发送',
        PROCESSING: '处理中',
        RETRY: '重试中',
        SUCCESS: '成功',
        DEAD: '已终止',
        FAILED: '失败',
        TERMINATED: '已终止'
      }[String(value || '').toUpperCase()] || value || '-'
    },
    taskStatusTag(value) {
      const status = String(value || '').toUpperCase()
      if (status === 'SUCCESS') return 'success'
      if (['RETRY', 'PENDING', 'PROCESSING'].includes(status)) return 'warning'
      if (['DEAD', 'FAILED', 'TERMINATED', 'FAILURE', 'ERROR'].includes(status)) return 'danger'
      return 'info'
    },
    isRetryable(value) {
      return ['RETRY', 'DEAD', 'FAILED', 'TERMINATED', 'FAILURE', 'ERROR'].includes(String(value || '').toUpperCase())
    },
    retryText(row) {
      const retryCount = row && row.retryCount !== undefined && row.retryCount !== null ? row.retryCount : 0
      const maxRetry = row && row.maxRetry !== undefined && row.maxRetry !== null ? row.maxRetry : '-'
      return retryCount + ' / ' + maxRetry
    },
    formatTime(value) {
      if (!value) return '-'
      return this.parseTime ? this.parseTime(value) : value
    },
    resetTemplateDialog() {
      if (this.submitting || this.previewLoading) return
      this.templateDetailLoading = false
      this.templateDetailError = ''
      this.templateSaveError = ''
      this.previewError = ''
      this.previewResult = null
      this.previewReady = false
      this.previewFingerprint = ''
      this.editingTemplateId = null
      this.templateForm = defaultTemplate()
    }
  }
}
</script>

<style lang="scss" scoped>
.message-page { min-height: 460px; }
.list-alert,
.dialog-alert { margin-bottom: 12px; }
.template-dialog-body { min-height: 280px; }
.preview-card { margin-top: 18px; background: #fafafa; border-color: #ebeef5; }
.preview-header { display: flex; align-items: center; justify-content: space-between; color: #303133; font-weight: 500; }
.preview-hint { color: #909399; font-size: 12px; font-weight: 400; }
.preview-content { padding: 4px; }
.preview-title { color: #303133; font-size: 16px; font-weight: 600; }
.preview-body { min-height: 62px; margin-top: 12px; padding: 12px; color: #606266; line-height: 1.7; white-space: pre-wrap; word-break: break-word; background: #fff; border: 1px solid #ebeef5; border-radius: 4px; }
.preview-variable-title { margin: 16px 0 8px; color: #606266; font-size: 13px; }
.preview-empty { display: flex; align-items: center; justify-content: center; min-height: 100px; color: #909399; font-size: 13px; }
.preview-empty i { margin-right: 8px; font-size: 20px; }
.order-link { padding: 0; }
.muted-text { margin-top: 3px; color: #909399; font-size: 12px; }
</style>

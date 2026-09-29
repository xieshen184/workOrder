<template>
  <div class="app-container sla-page">
    <el-form
      v-show="showSearch"
      ref="queryForm"
      :model="queryParams"
      :inline="true"
      size="small"
      label-width="72px"
      @submit.native.prevent
    >
      <el-form-item label="关键字" prop="keyword">
        <el-input
          v-model.trim="queryParams.keyword"
          clearable
          placeholder="规则名称或编码"
          style="width: 220px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="分类" prop="categoryId">
        <el-select v-model="queryParams.categoryId" clearable filterable placeholder="全部分类" style="width: 170px">
          <el-option
            v-for="category in categories"
            :key="category.id || category.categoryId"
            :label="categoryName(category)"
            :value="String(category.id || category.categoryId)"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="紧急度" prop="urgencyLevel">
        <el-select v-model="queryParams.urgencyLevel" clearable placeholder="全部" style="width: 120px">
          <el-option label="一般" :value="1" />
          <el-option label="紧急" :value="2" />
          <el-option label="特急" :value="3" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" clearable placeholder="全部" style="width: 110px">
          <el-option label="正常" value="0" />
          <el-option label="停用" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="handleQuery">查询</el-button>
        <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          v-hasPermi="['workorder:sla:add']"
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
        >新增规则</el-button>
      </el-col>
      <right-toolbar :show-search.sync="showSearch" @queryTable="getList" />
    </el-row>

    <el-alert
      title="规则修改只影响后续提交的工单；分类为空表示全局兜底规则。历史工单保留创建时的 SLA 快照。"
      type="info"
      show-icon
      :closable="false"
      class="sla-note"
    />
    <el-alert
      v-if="listError"
      :title="listError"
      type="error"
      show-icon
      :closable="false"
      class="list-alert"
    >
      <el-button type="text" size="mini" @click="getList">重试</el-button>
    </el-alert>

    <el-table v-loading="loading" :data="slaRules" border stripe row-key="id">
      <el-table-column label="规则编码" prop="ruleCode" min-width="155" show-overflow-tooltip />
      <el-table-column label="规则名称" prop="ruleName" min-width="180" show-overflow-tooltip />
      <el-table-column label="适用分类" min-width="135" show-overflow-tooltip>
        <template slot-scope="scope">
          {{ scope.row.categoryName || categoryLabel(scope.row.categoryId) || '全局' }}
        </template>
      </el-table-column>
      <el-table-column label="紧急度" width="85" align="center">
        <template slot-scope="scope">{{ urgencyText(scope.row.urgencyLevel) }}</template>
      </el-table-column>
      <el-table-column label="优先级" prop="priority" width="80" align="center" />
      <el-table-column label="生效期" min-width="205">
        <template slot-scope="scope">
          {{ formatTime(scope.row.effectiveFrom) }}
          <span class="period-separator">至</span>
          {{ scope.row.effectiveTo ? formatTime(scope.row.effectiveTo) : '长期' }}
        </template>
      </el-table-column>
      <el-table-column label="响应/到场/完成（分）" width="150" align="center">
        <template slot-scope="scope">
          {{ minuteText(scope.row.responseMinutes) }} /
          {{ minuteText(scope.row.arrivalMinutes) }} /
          {{ minuteText(scope.row.finishMinutes) }}
        </template>
      </el-table-column>
      <el-table-column label="预警（分）" prop="reminderBeforeMin" width="92" align="center" />
      <el-table-column label="允许延期" width="92" align="center">
        <template slot-scope="scope">{{ allowExtensionText(scope.row.allowExtension) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="82" align="center">
        <template slot-scope="scope">
          <el-tag :type="statusValue(scope.row) === '0' ? 'success' : 'info'" size="small">
            {{ statusValue(scope.row) === '0' ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="190" fixed="right" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            v-hasPermi="['workorder:sla:edit']"
            type="text"
            size="mini"
            icon="el-icon-edit"
            @click="handleEdit(scope.row)"
          >编辑</el-button>
          <el-button
            v-hasPermi="['workorder:sla:edit']"
            type="text"
            size="mini"
            :icon="statusValue(scope.row) === '0' ? 'el-icon-video-pause' : 'el-icon-video-play'"
            @click="handleToggle(scope.row)"
          >{{ statusValue(scope.row) === '0' ? '停用' : '启用' }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && !listError && !slaRules.length" description="暂无 SLA 规则" :image-size="100" />

    <pagination
      v-show="!listError && total > 0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="handlePagination"
    />

    <el-dialog
      :title="dialogTitle"
      :visible.sync="open"
      width="820px"
      append-to-body
      :close-on-click-modal="false"
      :show-close="!submitting"
      @closed="resetFormData"
    >
      <el-form ref="form" :model="form" :rules="rules" label-width="112px" class="sla-form">
        <el-row :gutter="18">
          <el-col :span="12">
            <el-form-item label="规则编码" prop="ruleCode">
              <el-input
                v-model.trim="form.ruleCode"
                :disabled="Boolean(form.id)"
                maxlength="32"
                placeholder="如 SLA_GLOBAL_NORMAL"
                @input="normalizeCode"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="规则名称" prop="ruleName">
              <el-input v-model.trim="form.ruleName" maxlength="64" show-word-limit />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="18">
          <el-col :span="12">
            <el-form-item label="适用分类" prop="categoryId">
              <el-select
                v-model="form.categoryId"
                clearable
                filterable
                placeholder="为空表示全局规则"
                style="width: 100%"
              >
                <el-option label="全局（分类为空）" value="" />
                <el-option
                  v-for="category in categories"
                  :key="category.id || category.categoryId"
                  :label="categoryName(category)"
                  :value="String(category.id || category.categoryId)"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="紧急度" prop="urgencyLevel">
              <el-select v-model="form.urgencyLevel" placeholder="请选择紧急度" style="width: 100%">
                <el-option label="一般" :value="1" />
                <el-option label="紧急" :value="2" />
                <el-option label="特急" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="18">
          <el-col :span="12">
            <el-form-item label="优先级" prop="priority">
              <el-input-number v-model="form.priority" :min="0" :max="999999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio label="0">正常</el-radio>
                <el-radio label="1">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="18">
          <el-col :span="12">
            <el-form-item label="生效时间" prop="effectiveFrom">
              <el-date-picker
                v-model="form.effectiveFrom"
                type="datetime"
                value-format="yyyy-MM-dd HH:mm:ss"
                placeholder="选择生效时间"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="失效时间" prop="effectiveTo">
              <el-date-picker
                v-model="form.effectiveTo"
                type="datetime"
                value-format="yyyy-MM-dd HH:mm:ss"
                placeholder="为空表示长期有效"
                clearable
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="18">
          <el-col :span="8">
            <el-form-item label="响应分钟" prop="responseMinutes">
              <el-input-number v-model="form.responseMinutes" :min="1" :max="999999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="到场分钟" prop="arrivalMinutes">
              <el-input-number v-model="form.arrivalMinutes" :min="1" :max="999999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="完成分钟" prop="finishMinutes">
              <el-input-number v-model="form.finishMinutes" :min="1" :max="999999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="18">
          <el-col :span="12">
            <el-form-item label="预警分钟" prop="reminderBeforeMin">
              <el-input-number v-model="form.reminderBeforeMin" :min="0" :max="999999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="允许延期" prop="allowExtension">
              <el-radio-group v-model="form.allowExtension">
                <el-radio label="1">允许</el-radio>
                <el-radio label="0">不允许</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>

      <el-card shadow="never" class="preview-card">
        <div slot="header" class="preview-header">
          <span>截止时间预览</span>
          <span class="preview-hint">按示例提交时间计算，不替代后端 SLA 结果</span>
        </div>
        <div class="preview-toolbar">
          <span class="preview-label">示例提交时间</span>
          <el-date-picker
            v-model="previewSubmittedAt"
            type="datetime"
            value-format="yyyy-MM-dd HH:mm:ss"
            size="small"
            style="width: 205px"
          />
        </div>
        <el-row :gutter="16" class="preview-deadlines">
          <el-col v-for="item in previewDeadlines" :key="item.label" :span="8">
            <div class="preview-item">
              <div class="preview-item-label">{{ item.label }}</div>
              <div class="preview-item-value">{{ item.value }}</div>
              <div class="preview-item-meta">{{ item.minutes }}</div>
            </div>
          </el-col>
        </el-row>
      </el-card>

      <div slot="footer" class="dialog-footer">
        <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
        <el-button :disabled="submitting" @click="open = false">取消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listCategories } from '@/api/workorder/order'
import { addSlaRule, getSlaRule, listSlaRules, updateSlaRule } from '@/api/workorder/sla'

function unwrapResponse(response) {
  if (response && Object.prototype.hasOwnProperty.call(response, 'data')) return response.data
  return response
}

function extractRows(response) {
  const payload = unwrapResponse(response)
  if (Array.isArray(payload)) return payload
  if (payload && Array.isArray(payload.rows)) return payload.rows
  if (payload && Array.isArray(payload.list)) return payload.list
  if (response && Array.isArray(response.rows)) return response.rows
  return []
}

function extractTotal(response, rows) {
  const payload = unwrapResponse(response)
  const candidates = [
    response && response.total,
    payload && payload.total,
    response && response.data && response.data.total
  ]
  const total = candidates.find(value => typeof value === 'number')
  return total === undefined ? rows.length : total
}

function extractEntity(response) {
  const payload = unwrapResponse(response)
  if (payload && payload.data && !payload.id && !payload.ruleCode) return payload.data
  return payload || {}
}

function messageFrom(value) {
  if (!value) return ''
  if (typeof value === 'string') return value
  if (typeof value.msg === 'string') return value.msg
  if (typeof value.message === 'string') return value.message
  return ''
}

function requestErrorMessage(error, fallback) {
  const response = error && error.response
  const data = response && response.data
  return messageFrom(error) || messageFrom(data) || messageFrom(data && data.data) || fallback
}

function defaultForm() {
  return {
    id: null,
    ruleCode: '',
    ruleName: '',
    categoryId: '',
    urgencyLevel: 1,
    priority: 100,
    effectiveFrom: formatDateTime(new Date()),
    effectiveTo: '',
    responseMinutes: 60,
    arrivalMinutes: 120,
    finishMinutes: 480,
    reminderBeforeMin: 15,
    allowExtension: '1',
    status: '0',
    remark: ''
  }
}

function formatDateTime(value) {
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const pad = number => String(number).padStart(2, '0')
  return date.getFullYear() + '-' + pad(date.getMonth() + 1) + '-' + pad(date.getDate()) + ' ' +
    pad(date.getHours()) + ':' + pad(date.getMinutes()) + ':' + pad(date.getSeconds())
}

function parseDateTime(value) {
  if (!value) return null
  if (value instanceof Date) return value
  const date = new Date(String(value).replace(' ', 'T'))
  return Number.isNaN(date.getTime()) ? null : date
}

export default {
  name: 'WorkorderSlaRules',
  data() {
    return {
      loading: false,
      listError: '',
      categoryError: '',
      categoryLoading: false,
      submitting: false,
      showSearch: true,
      open: false,
      dialogTitle: '',
      categories: [],
      slaRules: [],
      total: 0,
      queryParams: {
        keyword: '',
        categoryId: '',
        urgencyLevel: '',
        status: '',
        pageNum: 1,
        pageSize: 10
      },
      form: defaultForm(),
      previewSubmittedAt: '2026-01-01 09:00:00',
      rules: {
        ruleCode: [
          { required: true, message: '规则编码不能为空', trigger: 'blur' },
          { pattern: /^[A-Z][A-Z0-9_]{1,31}$/, message: '请输入 2-32 位大写字母、数字或下划线，且以字母开头', trigger: 'blur' }
        ],
        ruleName: [{ required: true, message: '规则名称不能为空', trigger: 'blur' }],
        urgencyLevel: [{ required: true, message: '请选择紧急度', trigger: 'change' }],
        priority: [{ required: true, message: '优先级不能为空', trigger: 'change' }, { type: 'number', min: 0, message: '优先级不能小于 0', trigger: 'change' }],
        effectiveFrom: [{ required: true, message: '生效时间不能为空', trigger: 'change' }],
        effectiveTo: [{ validator: this.validateEffectiveTo, trigger: 'change' }],
        responseMinutes: [{ required: true, message: '响应分钟不能为空', trigger: 'change' }, { type: 'number', min: 1, message: '响应分钟必须大于 0', trigger: 'change' }],
        arrivalMinutes: [{ type: 'number', min: 1, message: '到场分钟必须大于 0', trigger: 'change' }],
        finishMinutes: [{ type: 'number', min: 1, message: '完成分钟必须大于 0', trigger: 'change' }],
        reminderBeforeMin: [{ required: true, message: '预警分钟不能为空', trigger: 'change' }, { type: 'number', min: 0, message: '预警分钟不能小于 0', trigger: 'change' }],
        allowExtension: [{ required: true, message: '请选择是否允许延期', trigger: 'change' }],
        status: [{ required: true, message: '请选择状态', trigger: 'change' }]
      }
    }
  },
  computed: {
    previewDeadlines() {
      const submittedAt = parseDateTime(this.previewSubmittedAt)
      const values = [
        { label: '响应截止', minutes: this.form.responseMinutes },
        { label: '到场截止', minutes: this.form.arrivalMinutes },
        { label: '完成截止', minutes: this.form.finishMinutes }
      ]
      return values.map(item => {
        const minutes = this.toNullableNumber(item.minutes)
        const deadline = submittedAt && minutes !== null
          ? new Date(submittedAt.getTime() + minutes * 60 * 1000)
          : null
        return {
          label: item.label,
          value: deadline ? formatDateTime(deadline) : '-',
          minutes: minutes === null ? '未设置' : '提交时间 + ' + minutes + ' 分钟'
        }
      })
    }
  },
  created() {
    this.restoreRouteQuery()
    this.getCategories()
    this.getList()
  },
  methods: {
    restoreRouteQuery() {
      const query = this.$route.query || {}
      Object.keys(this.queryParams).forEach(key => {
        if (query[key] === undefined) return
        if (['pageNum', 'pageSize'].includes(key)) {
          if (!Number.isNaN(Number(query[key]))) this.queryParams[key] = Number(query[key])
        } else if (key === 'urgencyLevel') {
          this.queryParams[key] = query[key] === '' ? '' : Number(query[key])
        } else {
          this.queryParams[key] = query[key]
        }
      })
    },
    buildListQuery() {
      const query = { pageNum: this.queryParams.pageNum, pageSize: this.queryParams.pageSize }
      ;['keyword', 'categoryId', 'urgencyLevel', 'status'].forEach(key => {
        if (this.queryParams[key] !== undefined && this.queryParams[key] !== null && this.queryParams[key] !== '') query[key] = this.queryParams[key]
      })
      return query
    },
    syncRouteQuery() {
      const query = { ...this.$route.query }
      ;['keyword', 'categoryId', 'urgencyLevel', 'status', 'pageNum', 'pageSize'].forEach(key => { delete query[key] })
      const next = this.buildListQuery()
      Object.keys(next).forEach(key => { query[key] = String(next[key]) })
      this.$router.replace({ query }).catch(() => {})
    },
    getCategories() {
      this.categoryLoading = true
      this.categoryError = ''
      return listCategories().then(response => {
        this.categories = extractRows(response)
      }).catch(error => {
        this.categories = []
        this.categoryError = requestErrorMessage(error, '分类加载失败，可先保存全局规则')
      }).then(() => {
        this.categoryLoading = false
      })
    },
    getList() {
      this.loading = true
      this.listError = ''
      return listSlaRules(this.buildListQuery()).then(response => {
        const rows = extractRows(response)
        this.slaRules = rows
        this.total = extractTotal(response, rows)
      }).catch(error => {
        this.slaRules = []
        this.total = 0
        this.listError = requestErrorMessage(error, 'SLA 规则加载失败，请稍后重试')
      }).then(() => {
        this.loading = false
      })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.syncRouteQuery()
      this.getList()
    },
    handlePagination() {
      this.syncRouteQuery()
      this.getList()
    },
    resetQuery() {
      this.queryParams.keyword = ''
      this.queryParams.categoryId = ''
      this.queryParams.urgencyLevel = ''
      this.queryParams.status = ''
      this.queryParams.pageNum = 1
      this.syncRouteQuery()
      this.getList()
    },
    categoryName(category) {
      return category.categoryName || category.name || category.categoryCode || String(category.id || category.categoryId)
    },
    categoryLabel(categoryId) {
      if (categoryId === undefined || categoryId === null || categoryId === '') return ''
      const category = this.categories.find(item => String(item.id || item.categoryId) === String(categoryId))
      return category ? this.categoryName(category) : String(categoryId)
    },
    urgencyText(value) {
      return { 1: '一般', 2: '紧急', 3: '特急' }[Number(value)] || '-'
    },
    statusValue(row) {
      return String(row && row.status !== undefined ? row.status : '')
    },
    allowExtensionText(value) {
      return String(value) === '1' ? '允许' : '不允许'
    },
    minuteText(value) {
      return value === undefined || value === null || value === '' ? '-' : value
    },
    formatTime(value) {
      if (!value) return '-'
      return this.parseTime ? this.parseTime(value) : value
    },
    formatPreviewTime(value) {
      const date = parseDateTime(value)
      return date ? formatDateTime(date) : '-'
    },
    toNullableNumber(value) {
      if (value === undefined || value === null || value === '') return null
      const number = Number(value)
      return Number.isNaN(number) ? null : number
    },
    normalizeCode(value) {
      this.form.ruleCode = String(value || '').toUpperCase().replace(/[^A-Z0-9_]/g, '')
    },
    validateEffectiveTo(rule, value, callback) {
      if (!value || !this.form.effectiveFrom) {
        callback()
        return
      }
      const from = parseDateTime(this.form.effectiveFrom)
      const to = parseDateTime(value)
      if (from && to && to.getTime() <= from.getTime()) {
        callback(new Error('失效时间必须晚于生效时间'))
        return
      }
      callback()
    },
    handleAdd() {
      this.form = defaultForm()
      this.dialogTitle = '新增 SLA 规则'
      this.open = true
      this.$nextTick(() => {
        if (this.$refs.form) this.$refs.form.clearValidate()
      })
    },
    handleEdit(row) {
      const id = row && row.id
      if (id === undefined || id === null || id === '') {
        this.$modal.msgError('规则缺少有效编号，无法编辑')
        return
      }
      this.submitting = false
      return getSlaRule(id).then(response => {
        const entity = extractEntity(response)
        this.form = {
          ...defaultForm(),
          ...entity,
          id,
          categoryId: entity.categoryId === undefined || entity.categoryId === null ? '' : String(entity.categoryId),
          ruleCode: String(entity.ruleCode || row.ruleCode || '').toUpperCase(),
          status: String(entity.status === undefined || entity.status === null ? row.status || '0' : entity.status),
          allowExtension: String(entity.allowExtension === undefined || entity.allowExtension === null ? row.allowExtension || '1' : entity.allowExtension),
          effectiveTo: entity.effectiveTo || '',
          effectiveFrom: entity.effectiveFrom || row.effectiveFrom || defaultForm().effectiveFrom
        }
        this.dialogTitle = '编辑 SLA 规则'
        this.open = true
        this.$nextTick(() => {
          if (this.$refs.form) this.$refs.form.clearValidate()
        })
      }).catch(error => {
        this.$modal.msgError(requestErrorMessage(error, '规则详情加载失败，请稍后重试'))
      })
    },
    payload(source) {
      return {
        ruleCode: String(source.ruleCode || '').trim().toUpperCase(),
        ruleName: String(source.ruleName || '').trim(),
        categoryId: source.categoryId === undefined || source.categoryId === null || source.categoryId === '' ? null : Number(source.categoryId),
        urgencyLevel: this.toNullableNumber(source.urgencyLevel),
        priority: this.toNullableNumber(source.priority),
        effectiveFrom: source.effectiveFrom || null,
        effectiveTo: source.effectiveTo || null,
        responseMinutes: this.toNullableNumber(source.responseMinutes),
        arrivalMinutes: this.toNullableNumber(source.arrivalMinutes),
        finishMinutes: this.toNullableNumber(source.finishMinutes),
        reminderBeforeMin: this.toNullableNumber(source.reminderBeforeMin),
        allowExtension: String(source.allowExtension || '0'),
        status: String(source.status || '0'),
        remark: String(source.remark || '').trim()
      }
    },
    handleToggle(row) {
      const id = row && row.id
      if (id === undefined || id === null || id === '') return
      const nextStatus = this.statusValue(row) === '0' ? '1' : '0'
      const action = nextStatus === '1' ? '停用' : '启用'
      this.$modal.confirm('确认' + action + '规则“' + (row.ruleName || row.ruleCode || id) + '”吗？').then(() => {
        return updateSlaRule(id, this.payload({ ...row, status: nextStatus }))
      }).then(() => {
        this.$modal.msgSuccess(action + '成功')
        return this.getList()
      }).catch(() => {})
    },
    submitForm() {
      if (this.submitting) return
      this.$refs.form.validate(valid => {
        if (!valid) return
        this.submitting = true
        const body = this.payload(this.form)
        const request = this.form.id ? updateSlaRule(this.form.id, body) : addSlaRule(body)
        request.then(() => {
          this.$modal.msgSuccess(this.form.id ? '修改成功' : '新增成功')
          this.open = false
          return this.getList()
        }).catch(error => {
          this.$modal.msgError(requestErrorMessage(error, '保存失败，请稍后重试'))
        }).then(() => {
          this.submitting = false
        })
      })
    },
    resetFormData() {
      if (this.submitting) return
      this.form = defaultForm()
      if (this.$refs.form) this.$refs.form.clearValidate()
    }
  }
}
</script>

<style lang="scss" scoped>
.sla-note,
.list-alert { margin-bottom: 12px; }
.period-separator { margin: 0 4px; color: #c0c4cc; }
.sla-form { margin-bottom: 18px; }
.preview-card { background: #fafafa; border-color: #ebeef5; }
.preview-header { display: flex; align-items: center; justify-content: space-between; color: #303133; font-weight: 500; }
.preview-hint { color: #909399; font-size: 12px; font-weight: 400; }
.preview-toolbar { display: flex; align-items: center; margin-bottom: 14px; }
.preview-label { margin-right: 12px; color: #606266; font-size: 13px; }
.preview-deadlines { margin-bottom: -4px; }
.preview-item { padding: 12px; background: #fff; border: 1px solid #ebeef5; border-radius: 4px; text-align: center; }
.preview-item-label { color: #606266; font-size: 13px; }
.preview-item-value { margin-top: 8px; color: #303133; font-size: 15px; font-weight: 500; }
.preview-item-meta { margin-top: 5px; color: #909399; font-size: 12px; }
</style>

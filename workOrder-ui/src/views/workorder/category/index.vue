<template>
  <div class="app-container category-page">
    <el-form v-show="showSearch" ref="queryForm" :model="queryParams" :inline="true" size="small" label-width="72px">
      <el-form-item label="分类搜索" prop="keyword">
        <el-input
          v-model.trim="queryParams.keyword"
          placeholder="分类名称或编码"
          clearable
          style="width: 220px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 120px">
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
          v-hasPermi="['workorder:category:add']"
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
        >新增分类</el-button>
      </el-col>
      <right-toolbar :show-search.sync="showSearch" @queryTable="getList" />
    </el-row>

    <el-alert
      title="分类编码创建后不可修改；停用分类不会删除历史工单，但报修端将不能再选择该分类。"
      type="warning"
      :closable="false"
      show-icon
      class="category-note"
    />

    <el-table v-loading="loading" :data="filteredCategories" border stripe row-key="id">
      <el-table-column label="分类名称" prop="categoryName" min-width="170" show-overflow-tooltip />
      <el-table-column label="分类编码" prop="categoryCode" min-width="150" show-overflow-tooltip />
      <el-table-column label="上级分类" min-width="140">
        <template slot-scope="scope">{{ parentName(scope.row.parentId) }}</template>
      </el-table-column>
      <el-table-column label="负责部门" prop="managerDeptName" min-width="150">
        <template slot-scope="scope">{{ scope.row.managerDeptName || '未指定' }}</template>
      </el-table-column>
      <el-table-column label="历史引用" prop="referenceCount" width="100" align="center" sortable />
      <el-table-column label="排序" prop="orderNum" width="80" align="center" sortable />
      <el-table-column label="状态" width="90" align="center">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === '0' ? 'success' : 'info'" size="small">
            {{ scope.row.status === '0' ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="210" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            v-hasPermi="['workorder:category:edit']"
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleEdit(scope.row)"
          >编辑</el-button>
          <el-button
            v-hasPermi="['workorder:category:edit']"
            size="mini"
            type="text"
            :icon="scope.row.status === '0' ? 'el-icon-video-pause' : 'el-icon-video-play'"
            @click="handleToggle(scope.row)"
          >{{ scope.row.status === '0' ? '停用' : '启用' }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && !filteredCategories.length" description="暂无符合条件的分类" />

    <el-dialog :title="dialogTitle" :visible.sync="open" width="560px" append-to-body @closed="resetFormData">
      <el-form ref="form" :model="form" :rules="rules" label-width="96px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="分类名称" prop="categoryName">
              <el-input v-model.trim="form.categoryName" maxlength="64" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类编码" prop="categoryCode">
              <el-input
                v-model.trim="form.categoryCode"
                :disabled="Boolean(form.id)"
                maxlength="32"
                placeholder="如 EQUIPMENT"
                @input="normalizeCode"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="上级分类" prop="parentId">
          <treeselect v-model="form.parentId" :options="categoryOptions" :clearable="false" placeholder="请选择上级分类" />
        </el-form-item>
        <el-form-item label="负责部门" prop="managerDeptId">
          <treeselect v-model="form.managerDeptId" :options="deptOptions" :show-count="true" placeholder="请选择默认负责部门" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="显示顺序" prop="orderNum">
              <el-input-number v-model="form.orderNum" :min="0" :max="9999" controls-position="right" />
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
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
        <el-alert
          v-if="form.id && form.referenceCount > 0"
          :title="`该分类已被 ${form.referenceCount} 张历史工单引用，编辑不会修改历史工单快照。`"
          type="info"
          :closable="false"
          show-icon
        />
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
        <el-button :disabled="submitting" @click="open = false">取消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import Treeselect from '@riophae/vue-treeselect'
import '@riophae/vue-treeselect/dist/vue-treeselect.css'
import { deptTreeSelect } from '@/api/system/user'
import { listManagedCategories, getCategory, addCategory, updateCategory } from '@/api/workorder/category'

export default {
  name: 'WorkorderCategories',
  components: { Treeselect },
  data() {
    return {
      loading: false,
      submitting: false,
      showSearch: true,
      open: false,
      dialogTitle: '',
      categories: [],
      deptOptions: [],
      queryParams: { keyword: '', status: '' },
      form: {},
      rules: {
        categoryName: [{ required: true, message: '分类名称不能为空', trigger: 'blur' }],
        categoryCode: [
          { required: true, message: '分类编码不能为空', trigger: 'blur' },
          { pattern: /^[A-Z][A-Z0-9_]{1,31}$/, message: '请输入2到32位大写字母、数字或下划线', trigger: 'blur' }
        ],
        parentId: [{ required: true, message: '请选择上级分类', trigger: 'change' }],
        orderNum: [{ required: true, message: '请输入显示顺序', trigger: 'change' }],
        status: [{ required: true, message: '请选择状态', trigger: 'change' }]
      }
    }
  },
  computed: {
    filteredCategories() {
      const keyword = this.queryParams.keyword.toLowerCase()
      return this.categories.filter(item => {
        const matchesKeyword = !keyword || String(item.categoryName || '').toLowerCase().includes(keyword) ||
          String(item.categoryCode || '').toLowerCase().includes(keyword)
        const matchesStatus = !this.queryParams.status || item.status === this.queryParams.status
        return matchesKeyword && matchesStatus
      })
    },
    categoryOptions() {
      // 编辑时同时排除自身和全部后代，避免用户在页面上选择一个必然形成循环的父分类。
      const unavailableIds = this.collectDescendantIds(this.form.id)
      const available = this.categories
        .filter(item => item.status === '0' && !unavailableIds.has(String(item.id)))
        .map(item => ({ id: item.id, parentId: item.parentId, label: item.categoryName }))
      return [{ id: 0, label: '顶级分类', children: this.handleTree(available, 'id', 'parentId') }]
    }
  },
  created() {
    this.restoreRouteQuery()
    this.resetFormData()
    this.getList()
    this.getDeptOptions()
  },
  methods: {
    async getList() {
      this.loading = true
      try {
        const response = await listManagedCategories()
        this.categories = Array.isArray(response.data) ? response.data : []
      } finally {
        this.loading = false
      }
    },
    async getDeptOptions() {
      const response = await deptTreeSelect()
      this.deptOptions = response.data || []
    },
    handleQuery() {
      this.syncRouteQuery()
    },
    resetQuery() {
      this.$refs.queryForm.resetFields()
      this.syncRouteQuery()
    },
    restoreRouteQuery() {
      Object.keys(this.queryParams).forEach(key => {
        if (this.$route.query[key] !== undefined) this.queryParams[key] = this.$route.query[key]
      })
    },
    syncRouteQuery() {
      const query = {}
      Object.keys(this.queryParams).forEach(key => {
        if (this.queryParams[key]) query[key] = this.queryParams[key]
      })
      this.$router.replace({ query }).catch(() => {})
    },
    parentName(parentId) {
      if (!parentId) return '顶级分类'
      const parent = this.categories.find(item => item.id === parentId)
      return parent ? parent.categoryName : '上级分类已失效'
    },
    collectDescendantIds(rootId) {
      const result = new Set()
      if (!rootId) return result
      const pending = [rootId]
      while (pending.length) {
        const currentId = pending.pop()
        const currentKey = String(currentId)
        if (result.has(currentKey)) continue
        result.add(currentKey)
        this.categories.forEach(item => {
          if (String(item.parentId || 0) === currentKey) pending.push(item.id)
        })
      }
      return result
    },
    handleAdd() {
      this.resetFormData()
      this.dialogTitle = '新增分类'
      this.open = true
    },
    async handleEdit(row) {
      this.resetFormData()
      const response = await getCategory(row.id)
      this.form = { ...this.form, ...response.data }
      this.dialogTitle = '编辑分类'
      this.open = true
    },
    handleToggle(row) {
      const nextStatus = row.status === '0' ? '1' : '0'
      const action = nextStatus === '1' ? '停用' : '启用'
      const referenceHint = nextStatus === '1' && row.referenceCount > 0
        ? `该分类已被 ${row.referenceCount} 张历史工单引用，历史记录将保留。`
        : ''
      this.$modal.confirm(`${referenceHint}是否确认${action}“${row.categoryName}”？`).then(async () => {
        await updateCategory(row.id, this.payload({ ...row, status: nextStatus }))
        this.$modal.msgSuccess(`${action}成功`)
        await this.getList()
      }).catch(() => {})
    },
    submitForm() {
      this.$refs.form.validate(async valid => {
        if (!valid || this.submitting) return
        this.submitting = true
        try {
          if (this.form.id) await updateCategory(this.form.id, this.payload(this.form))
          else await addCategory(this.payload(this.form))
          this.$modal.msgSuccess(this.form.id ? '修改成功' : '新增成功')
          this.open = false
          await this.getList()
        } finally {
          this.submitting = false
        }
      })
    },
    payload(source) {
      return {
        parentId: source.parentId === null || source.parentId === undefined ? 0 : source.parentId,
        categoryCode: String(source.categoryCode || '').trim().toUpperCase(),
        categoryName: String(source.categoryName || '').trim(),
        managerDeptId: source.managerDeptId || null,
        orderNum: Number(source.orderNum || 0),
        status: source.status || '0',
        remark: String(source.remark || '').trim()
      }
    },
    normalizeCode(value) {
      this.form.categoryCode = String(value || '').toUpperCase().replace(/[^A-Z0-9_]/g, '')
    },
    resetFormData() {
      this.form = {
        id: null,
        parentId: 0,
        categoryCode: '',
        categoryName: '',
        managerDeptId: null,
        orderNum: 0,
        status: '0',
        remark: '',
        referenceCount: 0
      }
      this.resetForm('form')
    }
  }
}
</script>

<style lang="scss" scoped>
.category-note { margin-bottom: 16px; }
</style>

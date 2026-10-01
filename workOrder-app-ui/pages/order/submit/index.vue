<template>
  <view class="page-container">

    <!-- 主内容区 -->
    <scroll-view class="content-scroll" scroll-y ref="contentScroll">
      <view class="form-container">
        <!-- 步骤指示器 -->
        <u-steps 
          :current="currentStep" 
          :list="stepList" 
          active-color="#36CFC9"
          inactive-color="#C9CDD4"
          class="steps"
        ></u-steps>

        <!-- 登录人信息卡片：只读展示，创建接口不提交这些字段 -->
        <view class="custom-card shadow-lg radius-lg bg-white mb-30">
          <view class="card-header">
            <text class="card-title">登录人信息</text>
            <view class="card-line"></view>
          </view>

          <view v-if="profileLoading" class="profile-state">
            <u-loading-icon mode="circle" color="#36CFC9" size="28"></u-loading-icon>
            <text class="profile-state-text">登录人信息加载中</text>
          </view>
          <view v-else-if="profileLoadError" class="profile-state profile-load-error">
            <text class="profile-state-text">{{ profileLoadError }}</text>
            <u-button type="primary" size="mini" plain @click="loadProfile">重试</u-button>
          </view>
          <view v-else class="profile-info">
            <view class="profile-row">
              <text class="profile-label">姓名</text>
              <text class="profile-value">{{ baseForm.name || '未完善' }}</text>
            </view>
            <view class="profile-row">
              <text class="profile-label">科室</text>
              <text class="profile-value">{{ baseForm.department || '未完善' }}</text>
            </view>
            <view class="profile-row">
              <text class="profile-label">电话</text>
              <text class="profile-value" :class="{ 'profile-value-warning': !baseForm.phone }">
                {{ baseForm.phone || '未填写' }}
              </text>
            </view>
          </view>

          <view v-if="!profileLoading && !profileLoadError && profileMissingPhone" class="profile-warning">
            <u-icon name="info-circle" color="#FF9C07" size="30"></u-icon>
            <view class="profile-warning-content">
              <text class="profile-warning-text">未检测到手机号，请先完善个人资料后再提交工单。</text>
              <u-button type="warning" size="mini" plain @click="goProfile">去完善资料</u-button>
            </view>
          </view>
        </view>
        
        <!-- 故障信息卡片 -->
        <view class="custom-card shadow-lg radius-lg bg-white mb-30">
          <view class="card-header">
            <text class="card-title">故障信息</text>
            <view class="card-line"></view>
          </view>
          
          <u-form 
            ref="faultForm" 
            :model="faultForm" 
            :rules="faultRules" 
            label-position="left"
            label-width="140rpx"
            class="form-content"
          >
            <!-- 故障分类（单选框） -->
            <u-form-item label="故障分类" prop="categoryId" required>
              <view v-if="categoryLoading" class="category-loading">
                <u-loading-icon mode="circle" color="#36CFC9" size="28"></u-loading-icon>
                <text class="category-loading-text">分类加载中</text>
              </view>
              <view v-else-if="categoryLoadError" class="category-load-error">
                <text class="category-error-text">{{ categoryLoadError }}</text>
                <u-button type="primary" size="mini" plain @click="loadCategories">重试</u-button>
              </view>
              <u-radio-group
                v-else
                v-model="faultForm.categoryId"
                class="radio-group-horizontal"
                @change="handleSelectChange('categoryId', $event)"
              >
                <u-radio 
                  v-for="(item, index) in categoryList" 
                  :key="item.value" 
                  :label="item.value"
                  :name="item.value"
                  class="radio-item"
                >
                  <view class="radio-content">
                    <u-icon :type="item.icon" :color="item.color" size="24" class="radio-icon"></u-icon>
                    <text class="radio-label">{{ item.label }}</text>
                  </view>
                </u-radio>
              </u-radio-group>
            </u-form-item>

            <u-form-item label="故障位置" prop="location" required>
              <u-input 
                v-model="faultForm.location" 
                placeholder="请输入故障发生位置" 
                border="bottom"
                :focus-border-color="primaryColor"
                class="form-input"
              ></u-input>
            </u-form-item>
            
            <!-- 紧急程度（单选框） -->
            <u-form-item label="紧急程度" prop="urgencyLevel" required>
              <u-radio-group
                v-model="faultForm.urgencyLevel"
                class="radio-group-horizontal"
                @change="handleSelectChange('urgencyLevel', $event)"
              >
                <u-radio 
                  v-for="(item, index) in urgencyList" 
                  :key="index" 
                  :label="item.value"
                  :name="item.value"
                  class="radio-item"
                >
                  <view class="radio-content">
                    <view class="urgency-dot" :style="{ backgroundColor: item.color }"></view>
                    <text class="radio-label">{{ item.label }}</text>
                  </view>
                </u-radio>
              </u-radio-group>
            </u-form-item>
            
            <!-- 影响范围（单选框） -->
            <u-form-item label="影响范围" prop="impactScope" required>
              <u-radio-group
                v-model="faultForm.impactScope"
                class="radio-group-vertical"
                @change="handleSelectChange('impactScope', $event)"
              >
                <u-radio 
                  v-for="(item, index) in scopeList" 
                  :key="index" 
                  :label="item.value"
                  :name="item.value"
                  class="radio-item-vertical"
                >
                  <text class="radio-label-vertical">{{ item.label }}</text>
                </u-radio>
              </u-radio-group>
            </u-form-item>
            
            <u-form-item label="故障描述" prop="description" required>
              <u-textarea 
                v-model="faultForm.description" 
                placeholder="请详细描述故障现象"
                :height="150"
                border="bottom"
                :focus-border-color="primaryColor"
              ></u-textarea>
            </u-form-item>
            
            <u-form-item label="故障说明">
              <u-textarea 
                v-model="faultForm.possibleCause" 
                placeholder="请说明故障可能的原因和影响"
                :height="150"
                border="bottom"
                :focus-border-color="primaryColor"
              ></u-textarea>
            </u-form-item>
          </u-form>
        </view>
        
        <!-- 附件上传卡片 -->
        <view class="custom-card shadow-lg radius-lg bg-white mb-30">
          <view class="card-header">
            <text class="card-title">附件上传</text>
            <view class="card-line"></view>
          </view>
          
          <u-upload
            :file-list="fileList"
            :max-count="5"
            @afterRead="afterRead"
            @delete="deleteFile"
            name="file"
            accept="image"
            class="upload-component"
          >
            <template #add>
              <view class="add-btn">
                <u-icon name="camera" color="#36CFC9" size="36"></u-icon>
                <text class="add-text">上传/拍摄照片</text>
              </view>
            </template>
          </u-upload>

          <view
            v-for="(file, index) in fileList"
            v-if="file.status === 'failed'"
            :key="`upload-retry-${index}`"
            class="upload-retry-item"
          >
            <text class="upload-retry-text">{{ file.name || '图片' }}上传失败</text>
            <u-button type="primary" size="mini" plain @click.stop="retryUpload(index)">重试</u-button>
          </view>
          
          <view class="upload-tip" v-if="fileList.length === 0">
            最多上传5张图片，支持jpg、png格式
          </view>
        </view>
        
        <!-- 提交按钮 -->
        <view class="submit-btn-container">
          <u-button 
            type="primary" 
            size="large" 
            :loading="submitting"
            @click="submitForm"
            :custom-style="{
              height: '90rpx',
              fontSize: '32rpx',
              borderRadius: '45rpx',
              background: 'linear-gradient(to right, #36CFC9, #1E88E5)',
              border: 'none',
              boxShadow: '0 6rpx 16rpx rgba(54, 207, 201, 0.3)',
              marginTop: '20rpx'
            }"
          >
            <u-icon  color="#fff" size="28" class="btn-icon"></u-icon>
            提交工单
          </u-button>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script>
import { getInfo } from '@/api/login'
import { getCategories, submitRepair, uploadAttachment, deleteAttachment } from '@/api/order/submit'

export default {
  data() {
    return {
      // 主题颜色
      primaryColor: '#36CFC9',
      // 当前步骤
      currentStep: 0,
      // 步骤列表
      stepList: [
        { title: '基础信息' },
        { title: '故障信息' },
        { title: '附件上传' }
      ],
      // 基础信息表单数据
      baseForm: {
        name: '',
        department: '',
        phone: '',
        datetime: ''
      },
      profileLoading: true,
      profileLoadError: '',
      profileMissingPhone: false,
      // 故障信息表单数据
      faultForm: {
        categoryId: '',
        location: '',
        urgencyLevel: '',
        impactScope: '',
        description: '',
        possibleCause: ''
      },
      // 日期选择范围
      minDate: '',
      maxDate: '',
      // 故障分类单选框数据
      categoryList: [],
      categoryLoading: true,
      categoryLoadError: '',
      // 紧急程度单选框数据
      urgencyList: [
        { value: 1, label: '一般', color: '#86909C' },
        { value: 2, label: '紧急', color: '#FF9C07' },
        { value: 3, label: '特急', color: '#F53F3F' }
      ],
      // 影响范围单选框数据
      scopeList: [
        { value: 1, label: '个人事件' },
        { value: 2, label: '科室事件' },
        { value: 3, label: '多科室事件' },
        { value: 4, label: '全院事件' }
      ],
      // 文件上传
      fileList: [],
      faultRules: {
        categoryId: [
          // radio 在微信小程序端会于 change 后再次触发内部校验，
          // 此处由提交时的完整校验统一处理，避免选中后残留错误提示。
          { required: true, message: '请选择故障分类' }
        ],
        location: [
          { required: true, message: '请输入故障位置', trigger: ['blur', 'change'] }
        ],
        urgencyLevel: [
          { required: true, message: '请选择紧急程度' }
        ],
        impactScope: [
          { required: true, message: '请选择影响范围' }
        ],
        description: [
          { required: true, message: '请描述故障现象', trigger: ['blur', 'change'] }
        ]
      },
      // 提交状态
      submitting: false,
      idempotencyKey: ''
    };
  },

  onLoad() {
    this.loadProfile();
    this.loadCategories();
  },
  
  methods: {
    async loadProfile() {
      this.profileLoading = true;
      this.profileLoadError = '';
      try {
        const response = await getInfo();
        const user = response && response.user
          ? response.user
          : response && response.data && response.data.user
            ? response.data.user
            : null;
        if (!user) {
          throw new Error('登录人信息响应缺少 user');
        }
        // /getInfo 的用户姓名、科室和电话只用于展示；尤其不能把这些资料混入创建请求体。
        const department = user.dept && user.dept.deptName
          ? user.dept.deptName
          : (user.deptName || user.department || '');
        const phone = user.phonenumber || user.phone || '';
        this.baseForm = {
          ...this.baseForm,
          name: user.nickName || user.userName || '',
          department,
          phone
        };
        this.profileMissingPhone = !String(phone).trim();
        this.updateStepIndicator();
      } catch (error) {
        this.profileLoadError = '登录人信息加载失败，请重试';
        this.profileMissingPhone = false;
        this.$u.toast(this.profileLoadError);
      } finally {
        this.profileLoading = false;
      }
    },

    goProfile() {
      this.$tab.navigateTo('/pages/mine/info/edit');
    },

    // 初始化当前日期时间
    initDateTime() {
      const now = new Date();
      const year = now.getFullYear();
      const month = this.padZero(now.getMonth() + 1);
      const day = this.padZero(now.getDate());
      const hour = this.padZero(now.getHours());
      const minute = this.padZero(now.getMinutes());
      
      this.baseForm.datetime = `${year}-${month}-${day} ${hour}:${minute}`;
    },
    
    // 补零函数
    padZero(num) {
      return num < 10 ? '0' + num : num;
    },
    
    // 文件读取完成后触发
    async afterRead(event) {
      const files = Array.isArray(event.file) ? event.file : [event.file];
      for (const file of files) {
        const index = this.fileList.length;
        this.fileList.push({ ...file, status: 'uploading', message: '上传中' });
        await this.uploadFileAtIndex(index, true);
      }
      this.updateStepIndicator();
    },

    async uploadFileAtIndex(index, force = false) {
      const file = this.fileList[index];
      if (!file || (file.status === 'uploading' && !force)) return;

      this.$set(this.fileList, index, {
        ...file,
        status: 'uploading',
        message: '上传中'
      });

      try {
        const result = await uploadAttachment(file.url, 'file');
        const attachment = result && result.data !== undefined ? result.data : result;
        const attachmentId = attachment && (
          attachment.id !== undefined ? attachment.id : attachment.attachmentId
        );
        if (attachmentId === undefined || attachmentId === null || attachmentId === '') {
          throw new Error('上传结果缺少附件标识');
        }
        this.$set(this.fileList, index, {
          ...file,
          status: 'success',
          message: '',
          attachmentId
        });
      } catch (error) {
        this.$set(this.fileList, index, {
          ...file,
          status: 'failed',
          message: '上传失败'
        });
      }
    },

    async retryUpload(index) {
      if (!this.fileList[index] || this.fileList[index].status === 'uploading') return;
      await this.uploadFileAtIndex(index);
      this.updateStepIndicator();
    },
    
    // 删除文件
    async deleteFile(event) {
      const index = event && event.index;
      const file = index === undefined ? null : this.fileList[index];
      if (!file || file.deleting) return;

      const attachmentId = file.attachmentId;
      if (attachmentId === undefined || attachmentId === null || attachmentId === '') {
        // 尚未拿到附件 id 的本地项没有服务端记录，可以直接丢弃本地项。
        this.fileList.splice(index, 1);
        this.updateStepIndicator();
        return;
      }

      this.$set(this.fileList, index, {
        ...file,
        deleting: true,
        deletable: false,
        message: '删除中'
      });
      try {
        // 已上传但尚未绑定的附件必须先 DELETE 成功；失败时保留原项，避免界面与服务端状态不一致。
        await deleteAttachment(attachmentId);
        this.fileList.splice(index, 1);
        this.updateStepIndicator();
      } catch (error) {
        const currentFile = this.fileList[index];
        if (currentFile) {
          this.$set(this.fileList, index, {
            ...currentFile,
            deleting: false,
            deletable: true,
            message: '删除失败，请重试'
          });
        }
        this.$u.toast('附件删除失败，文件仍保留');
      }
    },
    
    // 更新步骤指示器
    updateStepIndicator() {
      // 根据表单填写情况更新步骤
      if (this.baseForm.name && this.baseForm.department && this.baseForm.phone) {
        this.currentStep = 1;
        
        if (this.faultForm.categoryId && this.faultForm.location && this.faultForm.urgencyLevel && this.faultForm.impactScope && this.faultForm.description) {
          this.currentStep = 2;
          
          if (this.fileList.length > 0) {
            this.currentStep = 3;
          }
        }
      } else {
        this.currentStep = 0;
      }
    },

    // 小程序端的 radio 视觉状态和表单校验状态分别更新；这里显式同步字段，
    // 并在数据写入完成后仅清除当前字段的旧校验结果。
    handleSelectChange(field, value) {
      this.faultForm[field] = value;
      this.$nextTick(() => {
        if (this.$refs.faultForm) {
          this.$refs.faultForm.clearValidate([field]);
        }
        this.updateStepIndicator();
      });
    },

    // 加载真实工单分类
    async loadCategories() {
      this.categoryLoading = true;
      this.categoryLoadError = '';
      this.categoryList = [];
      this.faultForm.categoryId = '';
      try {
        const result = await getCategories();
        const data = result && result.data !== undefined ? result.data : result;
        const categories = Array.isArray(data)
          ? data
          : data && (data.rows || data.list || data.records);
        const visuals = [
          { icon: 'tools', color: '#FF9F40' },
          { icon: 'wifi', color: '#00CFE8' },
          { icon: 'server', color: '#EA5455' }
        ];
        const list = Array.isArray(categories)
          ? categories.map((item, index) => {
            const id = item && item.id !== undefined ? item.id : item && item.categoryId;
            const label = item && (item.name || item.categoryName || item.label);
            if (id === undefined || id === null || id === '' || !label) return null;
            const visual = visuals[index % visuals.length];
            return {
              value: id,
              label,
              icon: item.icon || visual.icon,
              color: item.color || visual.color
            };
          }).filter(Boolean)
          : [];
        if (!list.length) {
          throw new Error('分类接口未返回可用分类');
        }
        this.categoryList = list;
      } catch (error) {
        this.categoryList = [];
        this.faultForm.categoryId = '';
        this.categoryLoadError = '分类加载失败，请点击重试';
        this.$u.toast(this.categoryLoadError);
      } finally {
        this.categoryLoading = false;
      }
    },
    
    // 提交表单
    async submitForm() {
      if (this.submitting) return;
      if (this.profileLoading) {
        this.$u.toast('登录人信息加载中，请稍候');
        return;
      }
      if (this.profileLoadError) {
        this.$u.toast('请先重试加载登录人信息');
        return;
      }
      if (!String(this.baseForm.phone || '').trim()) {
        this.$u.toast('未检测到手机号，请先去完善资料');
        return;
      }
      if (!String(this.baseForm.name || '').trim() || !String(this.baseForm.department || '').trim()) {
        this.$u.toast('登录人资料不完整，请先完善个人资料');
        return;
      }
      if (this.categoryLoading) {
        this.$u.toast('分类加载中，请稍候');
        return;
      }
      if (this.categoryLoadError || !this.categoryList.length) {
        this.$u.toast('分类加载失败，请点击重试');
        return;
      }

      this.submitting = true;
      try {
        await this.$refs.faultForm.validate();
      } catch (error) {
        this.$u.toast('请完善必填信息');
        this.submitting = false;
        return;
      }

      if (this.fileList.some(file => file.status === 'uploading')) {
        this.$u.toast('附件正在上传，请稍候');
        this.submitting = false;
        return;
      }
      if (this.fileList.some(file => file.status === 'failed')) {
        this.$u.toast('存在上传失败的附件，请点击重试或删除');
        this.submitting = false;
        return;
      }

      // 同一页面内的提交重试复用同一个幂等键，成功后由 resetForm 清空，下一次新建再生成新键。
      const idempotencyKey = this.getIdempotencyKey();
      const formData = {
        categoryId: Number(this.faultForm.categoryId),
        location: this.faultForm.location,
        urgencyLevel: Number(this.faultForm.urgencyLevel),
        impactScope: Number(this.faultForm.impactScope),
        description: this.faultForm.description,
        possibleCause: this.faultForm.possibleCause,
        attachmentIds: this.fileList.map(file => file.attachmentId),
        sourceType: 1,
        idempotencyKey
      };

      try {
        const response = await submitRepair(formData);
        const orderData = response && response.data !== undefined ? response.data : response;
        const orderId = orderData && orderData.id !== undefined
          ? orderData.id
          : orderData && orderData.orderId;
        if (orderId === undefined || orderId === null || orderId === '') {
          throw new Error('创建工单响应缺少 id');
        }
        this.resetForm();
        this.openOrderDetail(orderId);
      } catch (error) {
        this.$u.toast('提交失败，已保留当前填写内容');
      } finally {
        this.submitting = false;
      }
    },

    openOrderDetail(orderId) {
      uni.navigateTo({
        url: `/pages/order/detail/index?id=${encodeURIComponent(orderId)}`,
        success: () => {
          this.$u.toast('工单提交成功');
        },
        fail: () => {
          this.$u.toast('工单已提交，但详情页打开失败');
        }
      });
    },

    getIdempotencyKey() {
      if (!this.idempotencyKey) {
        this.idempotencyKey = this.createIdempotencyKey();
      }
      return this.idempotencyKey;
    },

    createIdempotencyKey() {
      return `wo-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 12)}`;
    },
    
    // 重置表单
    resetForm() {
      // 只清空本次工单字段；登录人只读资料保留，幂等键在创建成功后结束其生命周期。
      this.faultForm = {
        categoryId: '',
        location: '',
        urgencyLevel: '',
        impactScope: '',
        description: '',
        possibleCause: ''
      };
      
      this.fileList = [];
      this.currentStep = 0;
      this.idempotencyKey = '';
    },
    
    // 格式化日期时间
    formatDateTime(date) {
      const year = date.getFullYear();
      const month = this.padZero(date.getMonth() + 1);
      const day = this.padZero(date.getDate());
      const hour = this.padZero(date.getHours());
      const minute = this.padZero(date.getMinutes());
      return `${year}-${month}-${day} ${hour}:${minute}`;
    }
  }
};
</script>

<style lang="scss">
/* 全局样式 */
.page-container {
  min-height: 100vh;
  background-color: #F5F7FA;
  padding: 20rpx;
}

/* 页面标题 */
.page-title {
  padding: 30rpx 0;
  text-align: center;
  
  .title-text {
    font-size: 36rpx;
    font-weight: bold;
    color: #1D2129;
  }
}

/* 内容滚动区 */
.content-scroll {
  height: calc(100vh - 180rpx);
}

/* 步骤指示器 */
.steps {
  margin: 0 20rpx 40rpx;
}

/* 表单容器 */
.form-container {
  padding-bottom: 30rpx;
}

/* 自定义卡片样式 */
.custom-card {
  padding: 20rpx;
  transition: all 0.3s ease;
  
  &:hover {
    transform: translateY(-5rpx);
    box-shadow: 0 10rpx 20rpx rgba(0, 0, 0, 0.1);
  }
}

/* 卡片标题 */
.card-header {
  padding: 0 10rpx 20rpx;
  position: relative;
  
  .card-title {
    font-size: 30rpx;
    color: #1D2129;
    font-weight: 500;
    padding-left: 20rpx;
  }
  
  .card-line {
    height: 6rpx;
    width: 16rpx;
    background-color: #36CFC9;
    position: absolute;
    left: 10rpx;
    top: 20rpx;
    border-radius: 3rpx;
  }
}

/* 表单内容区 */
.form-content {
  padding: 0 10rpx;
}

/* 登录人资料只读展示与缺失电话提示 */
.profile-state,
.profile-info {
  padding: 0 10rpx;
}

.profile-state {
  display: flex;
  min-height: 90rpx;
  align-items: center;
  gap: 14rpx;
}

.profile-load-error {
  justify-content: space-between;
}

.profile-state-text,
.profile-label,
.profile-value,
.profile-warning-text {
  font-size: 26rpx;
}

.profile-state-text,
.profile-label {
  color: #86909C;
}

.profile-row {
  display: flex;
  min-height: 72rpx;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1rpx solid #F2F3F5;
}

.profile-value {
  color: #1D2129;
}

.profile-value-warning,
.profile-warning-text {
  color: #D46B08;
}

.profile-warning {
  display: flex;
  align-items: flex-start;
  margin: 20rpx 10rpx 0;
  padding: 18rpx;
  border-radius: 12rpx;
  background: #FFF7E6;
}

.profile-warning-content {
  flex: 1;
  margin-left: 12rpx;
}

.profile-warning-text {
  display: block;
  line-height: 1.5;
}

.profile-warning-content .u-button {
  margin: 14rpx 0 0;
}

/* 表单输入框样式 */
.form-input {
  font-size: 28rpx;
}

/* 分类加载状态 */
.category-loading,
.category-load-error {
  min-height: 70rpx;
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.category-loading-text,
.category-error-text {
  font-size: 26rpx;
  color: #86909C;
}

.category-error-text {
  color: #F53F3F;
}

/* 外边距类 */
.mb-30 {
  margin-bottom: 30rpx;
}

/* 水平排列单选框组 */
.radio-group-horizontal {
  display: flex;
  flex-wrap: wrap;
  gap: 20rpx;
  padding: 10rpx 0;
}

/* 单选框项（水平） */
.radio-item {
  flex: 1;
  min-width: 180rpx;
}

/* 单选框内容 */
.radio-content {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 15rpx;
  background-color: #F7FAFC;
  border-radius: 8rpx;
  transition: all 0.2s ease;
  
  .radio-icon {
    margin-right: 10rpx;
  }
  
  .radio-label {
    font-size: 28rpx;
  }
}

/* 单选框选中状态 */
.u-radio__icon--checked + .radio-content {
  background-color: #E6F7F5;
  border: 1rpx solid #36CFC9;
}

/* 紧急程度指示点 */
.urgency-dot {
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  margin-right: 10rpx;
}

/* 垂直排列单选框组 */
.radio-group-vertical {
  display: flex;
  flex-direction: column;
  gap: 10rpx;
  padding: 10rpx 0;
}

/* 单选框项（垂直） */
.radio-item-vertical {
  display: flex;
  align-items: center;
  padding: 15rpx;
  background-color: #F7FAFC;
  border-radius: 8rpx;
  transition: all 0.2s ease;
}

.radio-label-vertical {
  font-size: 28rpx;
  margin-left: 20rpx;
}

/* 垂直单选框选中状态 */
.radio-item-vertical .u-radio__icon--checked ~ .radio-label-vertical {
  color: #36CFC9;
  font-weight: 500;
}

/* 上传组件样式 */
.upload-component {
  padding: 10rpx;
}

/* 自定义上传按钮 */
.add-btn {
  width: 100%;
  height: 120rpx;
  background-color: #F2F7FF;
  border-radius: 10rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
}

.add-text {
  font-size: 28rpx;
  color: #36CFC9;
  margin-top: 10rpx;
}

/* 上传提示 */
.upload-tip {
  text-align: center;
  color: #86909C;
  font-size: 24rpx;
  padding: 20rpx 0;
}

/* 上传失败重试 */
.upload-retry-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10rpx;
  margin-top: 10rpx;
  background-color: #FFF2F0;
  border-radius: 8rpx;
}

.upload-retry-text {
  flex: 1;
  margin-right: 16rpx;
  color: #F53F3F;
  font-size: 24rpx;
}

/* 提交按钮容器 */
.submit-btn-container {
  padding: 20rpx 30rpx;
}

/* 按钮图标 */
.btn-icon {
  margin-right: 10rpx;
}

/* 图片预览样式 */
.u-upload__preview {
  margin-right: 20rpx;
  margin-bottom: 20rpx;
  border-radius: 8rpx;
  overflow: hidden;
}

.u-upload__preview-image {
  width: 160rpx;
  height: 160rpx;
}

.u-upload__preview-delete {
  background-color: rgba(245, 63, 63, 0.7);
}
</style>

<template>
  <view class="textview-container">
    <uni-card v-if="content" class="view-card" :title="content.title">
      <view v-for="section in content.sections" :key="section.id" class="content-section">
        <view v-if="section.heading" class="section-heading">{{ section.heading }}</view>
        <view v-for="(paragraph, index) in section.paragraphs" :key="`paragraph-${index}`" class="paragraph">
          <text>{{ paragraph }}</text>
        </view>
        <view v-for="(bullet, index) in section.bullets" :key="`bullet-${index}`" class="bullet-row">
          <text class="bullet-mark">•</text>
          <text class="bullet-text">{{ bullet }}</text>
        </view>
      </view>
    </uni-card>

    <app-page-state
      v-else
      type="error"
      title="内容不可用"
      :description="errorMessage"
      action-text="返回"
      @action="handleBack"
    />
  </view>
</template>

<script>
  import { getContentById } from '@/utils/contentRegistry'
  import AppPageState from '@/components/AppPageState/AppPageState.vue'

  export default {
    components: { AppPageState },
    data() {
      return {
        content: null,
        errorMessage: '内容未登记或访问参数无效'
      }
    },
    onLoad(options) {
      const contentId = this.decodeOption(options && options.id)
      const content = getContentById(contentId)
      if (!content) {
        uni.setNavigationBarTitle({ title: '浏览文本' })
        return
      }

      this.content = content
      uni.setNavigationBarTitle({ title: content.title })
    },
    methods: {
      decodeOption(value) {
        if (typeof value !== 'string' || !value) return ''
        try {
          return decodeURIComponent(value)
        } catch (error) {
          return ''
        }
      },
      handleBack() {
        uni.navigateBack({ delta: 1 })
      }
    }
  }
</script>

<style lang="scss" scoped>
  page {
    background-color: #ffffff;
  }

  .textview-container {
    min-height: 100%;
    padding: 1rpx 0 30rpx;
    background-color: #ffffff;
  }

  .view-card {
    margin-top: 24rpx;
  }

  .content-section {
    padding: 4rpx 5rpx 12rpx;
  }

  .section-heading {
    margin: 12rpx 0 16rpx;
    color: #303133;
    font-size: 28rpx;
    font-weight: bold;
  }

  .paragraph,
  .bullet-row {
    color: #333;
    font-size: 26rpx;
    line-height: 44rpx;
  }

  .paragraph {
    margin-bottom: 12rpx;
  }

  .bullet-row {
    display: flex;
    align-items: flex-start;
  }

  .bullet-mark {
    width: 28rpx;
    color: #3c96f3;
    flex-shrink: 0;
  }

  .bullet-text {
    flex: 1;
  }

</style>

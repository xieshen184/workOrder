<template>
  <view class="help-container">
    <view class="search-card">
      <view class="search-glyph"></view>
      <input
        v-model="keyword"
        class="search-input"
        confirm-type="search"
        placeholder="搜索问题关键词"
        maxlength="50"
      />
      <text v-if="keyword" class="clear-search" @click="clearSearch">清除</text>
    </view>

    <view v-for="group in filteredGroups" :key="group.key" class="list-title">
      <view class="text-title">
        <view :class="group.icon"></view>{{ group.title }}
      </view>
      <view class="child-list">
        <view
          v-for="(child, index) in group.childList"
          :key="child.id"
          class="question"
          hover-class="hover"
          @click="handleText(child.id)"
        >
          <view class="text-item">{{ child.title }}</view>
          <view class="line" v-if="index !== group.childList.length - 1"></view>
        </view>
      </view>
    </view>

    <app-page-state
      v-if="filteredGroups.length === 0"
      type="searchEmpty"
      description="没有匹配的帮助内容，请更换关键词或清除搜索"
      action-text="清除搜索"
      @action="clearSearch"
    />

    <view v-if="filteredGroups.length > 0" class="support-entry" @click="handleContact">
      以上内容未解决问题？查看联系提示
    </view>
  </view>
</template>

<script>
  import { getContentById } from '@/utils/contentRegistry'
  import AppPageState from '@/components/AppPageState/AppPageState.vue'

  // 页面只保留 FAQ 的分组关系和 content id，正文统一由 contentRegistry 提供。
  const FAQ_GROUP_DEFINITIONS = [{
    key: 'account',
    icon: 'iconfont icon-password',
    title: '账号与安全',
    ids: ['faq.account.login', 'faq.account.password', 'faq.account.logout']
  }, {
    key: 'order',
    icon: 'iconfont icon-service',
    title: '工单处理',
    ids: ['faq.order.submit', 'faq.order.status', 'faq.order.notification']
  }, {
    key: 'settings',
    icon: 'iconfont icon-setting',
    title: '设置与安全',
    ids: ['faq.settings.cache', 'faq.settings.update']
  }]

  function buildFaqGroups() {
    return FAQ_GROUP_DEFINITIONS.map(group => {
      const childList = group.ids.map(id => {
        const content = getContentById(id)
        if (!content) return null
        const searchParts = [content.title, content.category]
        ;(content.sections || []).forEach(section => {
          searchParts.push(section.heading || '')
          ;(section.paragraphs || []).forEach(paragraph => searchParts.push(paragraph))
          ;(section.bullets || []).forEach(bullet => searchParts.push(bullet))
        })
        return {
          id,
          title: content.title,
          searchText: searchParts.join(' ').toLowerCase()
        }
      }).filter(Boolean)

      return {
        key: group.key,
        icon: group.icon,
        title: group.title,
        childList
      }
    }).filter(group => group.childList.length > 0)
  }

  export default {
    components: { AppPageState },
    data() {
      return {
        keyword: '',
        groups: buildFaqGroups()
      }
    },
    computed: {
      filteredGroups() {
        const searchKeyword = this.keyword.trim().toLowerCase()
        if (!searchKeyword) return this.groups

        return this.groups.map(group => {
          return {
            key: group.key,
            icon: group.icon,
            title: group.title,
            childList: group.childList.filter(child => child.searchText.indexOf(searchKeyword) !== -1)
          }
        }).filter(group => group.childList.length > 0)
      }
    },
    methods: {
      clearSearch() {
        this.keyword = ''
      },
      handleText(contentId) {
        // 文本页只接收已登记的 id，不透传 FAQ 标题或正文。
        if (!getContentById(contentId)) return
        this.$tab.navigateTo(`/pages/common/textview/index?id=${encodeURIComponent(contentId)}`)
      },
      handleContact() {
        this.$tab.navigateTo('/pages/common/textview/index?id=faq.contact')
      }
    }
  }
</script>

<style lang="scss" scoped>
  page {
    background-color: #f8f8f8;
  }

  .help-container {
    min-height: 100%;
    margin-bottom: 100rpx;
    padding: 30rpx;
  }

  .search-card {
    display: flex;
    align-items: center;
    height: 76rpx;
    margin-bottom: 30rpx;
    padding: 0 24rpx;
    box-sizing: border-box;
    background: #ffffff;
    border-radius: 16rpx;
    box-shadow: 0 0 10rpx rgba(193, 193, 193, 0.16);
  }

  .search-glyph {
    width: 24rpx;
    height: 24rpx;
    margin: 0 18rpx 0 4rpx;
    border: 4rpx solid #909399;
    border-radius: 50%;
    box-sizing: border-box;
    position: relative;
    flex-shrink: 0;
  }

  .search-glyph::after {
    content: '';
    position: absolute;
    right: -12rpx;
    bottom: -8rpx;
    width: 12rpx;
    height: 4rpx;
    background-color: #909399;
    transform: rotate(45deg);
    transform-origin: left center;
  }

  .search-input {
    flex: 1;
    height: 76rpx;
    color: #303133;
    font-size: 28rpx;
  }

  .clear-search {
    padding-left: 16rpx;
    color: #3c96f3;
    font-size: 24rpx;
  }

  .list-title {
    margin-bottom: 30rpx;
  }

  .child-list {
    background: #ffffff;
    box-shadow: 0 0 10rpx rgba(193, 193, 193, 0.2);
    border-radius: 16rpx;
    margin-top: 10rpx;
  }

  .line {
    width: 100%;
    height: 1rpx;
    background-color: #F5F5F5;
  }

  .text-title {
    color: #303133;
    font-size: 32rpx;
    font-weight: bold;
    margin-left: 10rpx;

    .iconfont {
      display: inline-block;
      font-size: 16px;
      margin-right: 10rpx;
    }
  }

  .text-item {
    font-size: 28rpx;
    padding: 24rpx;
  }

  .question {
    color: #606266;
    font-size: 28rpx;
  }

  .support-entry {
    padding: 24rpx 0 8rpx;
    color: #3c96f3;
    font-size: 26rpx;
    text-align: center;
  }

</style>

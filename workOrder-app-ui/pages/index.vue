
<template>
  <div class="work-order-app" :class="{ dark: darkMode }">
    <!-- 顶部导航栏 -->
    <div class="app-header">
      <div class="header-content">
        <h1 class="app-title">工单管理中心</h1>
        <button class="theme-toggle" @click="toggleDarkMode">
          <i class="van-icon" :class="darkMode ? 'van-icon-sun-o' : 'van-icon-moon-o'"></i>
        </button>
      </div>
    </div>

    <!-- 顶部统计区域 -->
    <div class="stats-header">
      <div class="stats-title">当前工单概览</div>
      <div v-if="dashboardError" class="dashboard-message" @click="loadDashboard">{{ dashboardError }}，点击重试</div>
      <div class="stats-cards">
        <div class="stat-card" :class="cardHoverClass">
          <div class="stat-icon">
            <i class="van-icon van-icon-inbox"></i>
          </div>
          <div class="stat-content">
            <div class="stat-value" :class="{ countUp: animateStats }">{{ todayStats.received }}</div>
            <div class="stat-label">工单总数</div>
          </div>
          <div class="stat-trend">实时</div>
        </div>
        <div class="stat-card" :class="cardHoverClass">
          <div class="stat-icon">
            <i class="van-icon van-icon-check-circle"></i>
          </div>
          <div class="stat-content">
            <div class="stat-value" :class="{ countUp: animateStats }">{{ todayStats.completed }}</div>
            <div class="stat-label">已完成工单</div>
          </div>
          <div class="stat-trend">实时</div>
        </div>
      </div>
    </div>

    <!-- 工单趋势图表 -->
    <div class="chart-container" :class="cardHoverClass">
      <div class="chart-header">
        <h3 class="chart-title">工单状态分布</h3>
      </div>
      <div class="chart-content">
        <div class="chart-bars">
          <div v-for="(item, index) in chartData" :key="index" class="chart-bar">
            <div class="bar-value">{{ item.count }}</div>
            <div class="bar" :style="{ height: item.value + '%', backgroundColor: barColor }"></div>
            <div class="bar-label">{{ item.label }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 中部操作按钮区域 -->
    <div class="action-buttons">
      <button v-if="canSubmit" class="action-btn submit-btn" @click="handleSubmit" :class="btnHoverClass">
        <div class="btn-icon">
          <i class="van-icon van-icon-plus"></i>
        </div>
        <div class="btn-text">工单申请</div>
      </button>
      <button v-if="canAssign" class="action-btn assign-btn" @click="handleAssign" :class="btnHoverClass">
        <div class="btn-icon">
          <i class="van-icon van-icon-arrow-right"></i>
        </div>
        <div class="btn-text">工单派发</div>
      </button>
      <button v-if="canProcess" class="action-btn process-btn" @click="handleProcess" :class="btnHoverClass">
        <div class="btn-icon">
          <i class="van-icon van-icon-edit"></i>
        </div>
        <div class="btn-text">工单处理</div>
      </button>
    </div>

    <!-- 底部状态统计区域 -->
    <div class="status-stats">
      <div class="status-card" :class="[statusColors.waitingAssign, cardHoverClass]" @click="showStatusDetail('waitingAssign')">
        <div class="status-icon">
          <i class="van-icon van-icon-clock-o"></i>
        </div>
        <div class="status-info">
          <div class="status-value" :class="{ countUp: animateStats }">{{ statusStats.waitingAssign }}</div>
          <div class="status-label">待派单</div>
        </div>
        <div class="status-arrow">
          <i class="van-icon van-icon-arrow-right"></i>
        </div>
      </div>
      <div class="status-card" :class="[statusColors.waitingAccept, cardHoverClass]" @click="showStatusDetail('waitingAccept')">
        <div class="status-icon">
          <i class="van-icon van-icon-pause-circle-o"></i>
        </div>
        <div class="status-info">
          <div class="status-value" :class="{ countUp: animateStats }">{{ statusStats.waitingAccept }}</div>
          <div class="status-label">待接单</div>
        </div>
        <div class="status-arrow">
          <i class="van-icon van-icon-arrow-right"></i>
        </div>
      </div>
      <div class="status-card" :class="[statusColors.inProgress, cardHoverClass]" @click="showStatusDetail('inProgress')">
        <div class="status-icon">
          <i class="van-icon van-icon-loading"></i>
        </div>
        <div class="status-info">
          <div class="status-value" :class="{ countUp: animateStats }">{{ statusStats.inProgress }}</div>
          <div class="status-label">执行中</div>
        </div>
        <div class="status-arrow">
          <i class="van-icon van-icon-arrow-right"></i>
        </div>
      </div>
      <div class="status-card" :class="[statusColors.waitingConfirm, cardHoverClass]" @click="showStatusDetail('waitingConfirm')">
        <div class="status-icon">
          <i class="van-icon van-icon-exclamation-circle"></i>
        </div>
        <div class="status-info">
          <div class="status-value" :class="{ countUp: animateStats }">{{ statusStats.waitingConfirm }}</div>
          <div class="status-label">待确认</div>
        </div>
        <div class="status-arrow">
          <i class="van-icon van-icon-arrow-right"></i>
        </div>
      </div>
	  <div v-if="canEvaluate" class="status-card" :class="[statusColors.pendingEvaluation, cardHoverClass]" @click="handleevaluate">
	    <div class="status-icon">
	      <i class="van-icon van-icon-exclamation-circle"></i>
	    </div>
	    <div class="status-info">
	      <div class="status-value" :class="{ countUp: animateStats }">{{ statusStats.pendingEvaluation }}</div>
	      <div class="status-label">待评价</div>
	    </div>
	    <div class="status-arrow">
	      <i class="van-icon van-icon-arrow-right"></i>
	    </div>
	  </div>
    </div>

    <!-- 装饰元素 -->
    <div class="decorative-element top-left"></div>
    <div class="decorative-element bottom-right"></div>
  </div>
</template>

<script>
import { getDashboardSnapshot } from '@/api/order/dashboard'

export default {
  data() {
    return {
      darkMode: false,
      todayStats: {
        received: 0,
        completed: 0
      },
      statusStats: {
        waitingAssign: 0,
        waitingAccept: 0,
        inProgress: 0,
        waitingConfirm: 0,
        pendingEvaluation: 0,
        completed: 0
      },
      statusColors: {
        waitingAssign: 'status-waiting-assign',
        waitingAccept: 'status-waiting-accept',
        inProgress: 'status-in-progress',
        waitingConfirm: 'status-overdue',
        pendingEvaluation: 'status-overdue',
        completed: 'status-overdue'
      },
      animateStats: false,
      chartData: [],
      dashboardError: '',
      dashboardLoading: false,
      cardHoverClass: 'card-hover-effect',
      btnHoverClass: 'btn-hover-effect'
    };
  },
  computed: {
    barColor() {
      return this.darkMode ? 'rgba(255, 255, 255, 0.7)' : 'rgba(22, 93, 255, 0.7)';
    },
    permissions() {
      return this.$store.getters.permissions || [];
    },
    canSubmit() {
      return this.hasPermission('workorder:order:add');
    },
    canAssign() {
      return this.hasPermission('workorder:order:list') &&
        (this.hasPermission('workorder:order:assign') || this.hasPermission('workorder:order:reassign'));
    },
    canProcess() {
      return this.hasPermission('workorder:order:assigned:list');
    },
    canEvaluate() {
      return this.hasPermission('workorder:evaluation:add');
    }
  },
  mounted() {
    this.loadDashboard();
    // #ifdef H5
    window.addEventListener('resize', this.handleResize);
    // #endif
  },
  onShow() {
    if (this.animateStats) this.loadDashboard();
  },
  onPullDownRefresh() {
    this.loadDashboard().finally(() => uni.stopPullDownRefresh());
  },
  beforeDestroy() {
    // #ifdef H5
    window.removeEventListener('resize', this.handleResize);
    // #endif
  },
  methods: {
    hasPermission(permission) {
      return this.permissions.includes('*:*:*') || this.permissions.includes(permission);
    },

    // 按当前用户权限选择数据范围；各状态总数均来自真实分页接口，不生成本地趋势数据。
    async loadDashboard() {
      if (this.dashboardLoading) return;
      this.dashboardLoading = true;
      this.dashboardError = '';
      try {
        const snapshot = await getDashboardSnapshot(this.permissions);
        const value = snapshot.values;
        this.todayStats = {
          received: value.TOTAL || 0,
          completed: (value.COMPLETED || 0) + (value.CLOSED || 0)
        };
        this.statusStats = {
          waitingAssign: value.WAIT_ASSIGN || 0,
          waitingAccept: value.WAIT_ACCEPT || 0,
          inProgress: (value.ACCEPTED || 0) + (value.PROCESSING || 0),
          waitingConfirm: value.WAIT_CONFIRM || 0,
          // 待评价必须与评价页保持同一口径：当前报修人的 COMPLETED 工单，不包含已评价的 CLOSED。
          pendingEvaluation: value.PENDING_EVALUATION || 0,
          // 状态分布中的“已完成”表示业务已结束，因此包含已确认和已评价两种状态。
          completed: (value.COMPLETED || 0) + (value.CLOSED || 0)
        };
        this.updateChartData();
        this.animateStats = true;
        if (snapshot.failed.length) this.dashboardError = '部分统计暂时不可用';
      } catch (error) {
        this.dashboardError = '工单数据加载失败';
      } finally {
        this.dashboardLoading = false;
      }
    },

    toggleDarkMode() {
      this.darkMode = !this.darkMode;
      // #ifdef H5
      document.body.classList.toggle('dark-mode', this.darkMode);
      // #endif
    },
    updateChartData() {
      const source = [
        { label: '待派', count: this.statusStats.waitingAssign },
        { label: '待接', count: this.statusStats.waitingAccept },
        { label: '处理中', count: this.statusStats.inProgress },
        { label: '待确认', count: this.statusStats.waitingConfirm },
        { label: '已完成', count: this.statusStats.completed }
      ];
      const max = Math.max(1, ...source.map(item => item.count));
      this.chartData = source.map(item => ({
        label: item.label,
        count: item.count,
        value: item.count ? Math.max(8, Math.round(item.count / max * 100)) : 0
      }));
    },
    handleSubmit() {
       this.$tab.navigateTo('/pages/order/submit/index');
    },
    handleAssign() {
      this.$tab.navigateTo('/pages/order/assign/index');
    },
    handleProcess(status) {
      const tab = {
        waitingAccept: 0,
        inProgress: 1,
        waitingConfirm: 2
      }[status];
      const query = Number.isInteger(tab) ? `?tab=${tab}` : '';
      this.$tab.navigateTo(`/pages/order/handle/index${query}`);
    },
	handleevaluate() {
	  this.$tab.navigateTo('/pages/order/evaluate/index');
	},
    showStatusDetail(status) {
      const statusMap = {
        waitingAssign: 'WAIT_ASSIGN',
        waitingAccept: 'WAIT_ACCEPT',
        inProgress: 'PROCESSING',
        waitingConfirm: 'WAIT_CONFIRM'
      };
      if (this.canAssign && (status === 'waitingAssign' || status === 'waitingAccept')) {
        this.handleAssign();
        return;
      }
      if (this.canAssign) {
        this.$u.toast('该状态请在 PC 管理端查看');
        return;
      }
      if (this.canProcess) {
        this.handleProcess(status);
        return;
      }
      const statusQuery = statusMap[status] ? `?status=${statusMap[status]}` : '';
      this.$tab.navigateTo(`/pages/order/my/index${statusQuery}`);
    },
    getStatusName(status) {
      const names = {
        waitingAssign: '待派单',
        waitingAccept: '待接单',
        inProgress: '执行中',
        waitingConfirm: '待确认'
      };
      return names[status] || status;
    },
    handleResize() {
      this.updateChartData();
    }
  }
};
</script>

<style scoped lang="scss">
// 基础样式变量
$primary-color: #165DFF;
$success-color: #2DC853;
$warning-color: #FF9500;
$danger-color: #FF3B30;
$light-bg: #f5f7fa;
$dark-bg: #1E1E2E;
$card-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
$card-shadow-hover: 0 10px 30px rgba(0, 0, 0, 0.12);
$transition-default: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

.work-order-app {
  min-height: 100vh;
  background-color: $light-bg;
  background-image: 
    radial-gradient(circle at 10% 20%, rgba(22, 93, 255, 0.03) 0%, transparent 20%),
    radial-gradient(circle at 90% 80%, rgba(255, 149, 0, 0.03) 0%, transparent 20%);
  padding: 20px;
  box-sizing: border-box;
  color: #333;
  transition: background-color 0.5s ease;
  overflow-x: hidden;
  position: relative;
  
  &.dark {
    background-color: $dark-bg;
    color: #f0f0f0;
    background-image: 
      radial-gradient(circle at 10% 20%, rgba(22, 93, 255, 0.1) 0%, transparent 20%),
      radial-gradient(circle at 90% 80%, rgba(255, 149, 0, 0.1) 0%, transparent 20%);
  }
}

/* 装饰元素 */
.decorative-element {
  position: absolute;
  width: 200px;
  height: 200px;
  border-radius: 50%;
  z-index: 0;
  opacity: 0.1;
  filter: blur(50px);
  transition: opacity 0.5s ease;
  
  &.top-left {
    top: -100px;
    left: -100px;
    background: linear-gradient(135deg, $primary-color, $success-color);
  }
  
  &.bottom-right {
    bottom: -100px;
    right: -100px;
    background: linear-gradient(135deg, $warning-color, $danger-color);
  }
}

/* 顶部导航栏 */
.app-header {
  margin-bottom: 30px;
  position: relative;
  z-index: 1;
  
  .header-content {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  
  .app-title {
    font-size: 22px;
    font-weight: 700;
    background: linear-gradient(135deg, $primary-color, $success-color);
    -webkit-background-clip: text;
    background-clip: text;
    color: transparent;
    margin: 0;
  }
  
  .theme-toggle {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    background-color: white;
    border: none;
    box-shadow: $card-shadow;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 18px;
    color: $primary-color;
    cursor: pointer;
    transition: $transition-default;
    
    &:hover {
      transform: rotate(30deg);
      box-shadow: $card-shadow-hover;
    }
    
    .work-order-app.dark & {
      background-color: #2D2D44;
      color: #FFD700;
    }
  }
}

/* 顶部统计区域样式 */
.stats-header {
  margin-bottom: 30px;
  position: relative;
  z-index: 1;
  
  .stats-title {
    font-size: 18px;
    font-weight: 600;
    margin-bottom: 15px;
    display: flex;
    align-items: center;
    
    &:before {
      content: '';
      display: inline-block;
      width: 4px;
      height: 18px;
      background-color: $primary-color;
      border-radius: 2px;
      margin-right: 8px;
    }
  }

  .dashboard-message {
    margin: -6px 0 12px;
    color: #F53F3F;
    font-size: 13px;
  }
  
  .stats-cards {
    display: flex;
    gap: 15px;
    
    .stat-card {
      flex: 1;
      background-color: white;
      border-radius: 16px;
      padding: 20px;
      box-shadow: $card-shadow;
      transition: $transition-default;
      position: relative;
      overflow: hidden;
      
      &:before {
        content: '';
        position: absolute;
        top: 0;
        left: 0;
        width: 4px;
        height: 100%;
        background: linear-gradient(to bottom, $primary-color, rgba(22, 93, 255, 0.5));
      }
      
      .stat-icon {
        position: absolute;
        top: 20px;
        right: 20px;
        width: 36px;
        height: 36px;
        border-radius: 12px;
        background-color: rgba(22, 93, 255, 0.1);
        color: $primary-color;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 18px;
      }
      
      .stat-content {
        padding-right: 50px;
      }
      
      .stat-value {
        font-size: 32px;
        font-weight: 700;
        color: #333;
        margin-bottom: 5px;
        transition: all 1s cubic-bezier(0.34, 1.56, 0.64, 1);
        position: relative;
        
        .work-order-app.dark & {
          color: white;
        }
      }
      
      .stat-label {
        font-size: 14px;
        color: #666;
        
        .work-order-app.dark & {
          color: #bbb;
        }
      }
      
      .stat-trend {
        position: absolute;
        bottom: 20px;
        right: 20px;
        font-size: 13px;
        display: flex;
        align-items: center;
        
        &.trend-up {
          color: $success-color;
        }
        
        &.trend-down {
          color: $danger-color;
        }
        
        i {
          margin-right: 4px;
          font-size: 12px;
        }
      }
    }
  }
}

/* 图表容器样式 */
.chart-container {
  background-color: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: $card-shadow;
  margin-bottom: 30px;
  transition: $transition-default;
  position: relative;
  z-index: 1;
  
  .work-order-app.dark & {
    background-color: #2D2D44;
  }
  
  .chart-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
  }
  
  .chart-title {
    font-size: 16px;
    font-weight: 600;
    margin: 0;
  }
  
  .chart-period {
    display: flex;
    gap: 8px;
    
    .period-btn {
      padding: 4px 12px;
      border-radius: 12px;
      border: none;
      background-color: #f0f0f0;
      font-size: 13px;
      cursor: pointer;
      transition: $transition-default;
      
      &.active {
        background-color: $primary-color;
        color: white;
      }
      
      .work-order-app.dark & {
        background-color: #3D3D58;
        
        &.active {
          background-color: $primary-color;
          color: white;
        }
      }
    }
  }
  
  .chart-content {
    height: 180px;
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    padding-bottom: 30px;
    position: relative;
    
    &:before {
      content: '';
      position: absolute;
      bottom: 20px;
      left: 0;
      right: 0;
      height: 1px;
      background-color: #eee;
      
      .work-order-app.dark & {
        background-color: #444;
      }
    }
  }
  
  .chart-bars {
    display: flex;
    width: 100%;
    height: 100%;
    align-items: flex-end;
    justify-content: space-between;
    padding: 0 5px;
  }
  
  .chart-bar {
    display: flex;
    flex-direction: column;
    align-items: center;
    width: 12%;
    position: relative;

    .bar-value {
      min-height: 18px;
      margin-bottom: 6px;
      font-size: 13px;
      font-weight: 600;
      color: #445466;

      .work-order-app.dark & {
        color: #E5E6EB;
      }
    }
    
    .bar {
      width: 100%;
      border-radius: 6px 6px 0 0;
      transition: height 1s cubic-bezier(0.34, 1.56, 0.64, 1);
      position: relative;
      min-height: 10px;
      
      &:before {
        content: '';
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: linear-gradient(to top, rgba(0,0,0,0.1), transparent);
        border-radius: 6px 6px 0 0;
      }
    }
    
    .bar-label {
      position: absolute;
      bottom: -20px;
      font-size: 12px;
      color: #888;
      
      .work-order-app.dark & {
        color: #aaa;
      }
    }
  }
}

/* 中部操作按钮区域样式 */
.action-buttons {
  display: flex;
  gap: 15px;
  margin-bottom: 30px;
  position: relative;
  z-index: 1;
  
  .action-btn {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 25px 10px;
    border-radius: 16px;
    border: none;
    color: white;
    font-size: 16px;
    font-weight: 500;
    cursor: pointer;
    box-shadow: 0 6px 15px rgba(0, 0, 0, 0.1);
    transition: $transition-default;
    position: relative;
    overflow: hidden;
    
    &:before {
      content: '';
      position: absolute;
      top: 0;
      left: -100%;
      width: 100%;
      height: 100%;
      background: linear-gradient(90deg, transparent, rgba(255,255,255,0.2), transparent);
      transition: all 0.6s ease;
    }
    
    &:hover:before {
      left: 100%;
    }
    
    .btn-icon {
      width: 48px;
      height: 48px;
      border-radius: 14px;
      background-color: rgba(255, 255, 255, 0.2);
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: 12px;
      font-size: 24px;
      backdrop-filter: blur(5px);
    }
    
    .btn-text {
      position: relative;
      z-index: 1;
    }
  }
  
  .submit-btn {
    background: linear-gradient(135deg, #4CD964 0%, #2DC853 100%);
  }
  
  .assign-btn {
    background: linear-gradient(135deg, #165DFF 0%, #0E42D2 100%);
  }
  
  .process-btn {
    background: linear-gradient(135deg, #FF9500 0%, #FF6B00 100%);
  }
}

/* 底部状态统计区域样式 */
.status-stats {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
  position: relative;
  z-index: 1;
  
  .status-card {
    background-color: white;
    border-radius: 16px;
    padding: 20px;
    display: flex;
    align-items: center;
    box-shadow: $card-shadow;
    transition: $transition-default;
    cursor: pointer;
    position: relative;
    overflow: hidden;
    
    .work-order-app.dark & {
      background-color: #2D2D44;
    }
    
    .status-icon {
      width: 48px;
      height: 48px;
      border-radius: 14px;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-right: 15px;
      font-size: 22px;
      flex-shrink: 0;
    }
    
    .status-info {
      flex: 1;
      
      .status-value {
        font-size: 24px;
        font-weight: 600;
        margin-bottom: 3px;
        transition: all 1s cubic-bezier(0.34, 1.56, 0.64, 1);
        
        .work-order-app.dark & {
          color: white;
        }
      }
      
      .status-label {
        font-size: 14px;
        color: #666;
        
        .work-order-app.dark & {
          color: #bbb;
        }
      }
    }
    
    .status-arrow {
      color: #ccc;
      font-size: 16px;
      transition: $transition-default;
      
      .work-order-app.dark & {
        color: #666;
      }
    }
    
    &:hover .status-arrow {
      transform: translateX(5px);
      color: $primary-color;
    }
  }
  
  // 不同状态的颜色样式
  .status-waiting-assign {
    .status-icon {
      background-color: rgba(22, 93, 255, 0.1);
      color: $primary-color;
    }
    .status-value {
      color: $primary-color;
    }
  }
  
  .status-waiting-accept {
    .status-icon {
      background-color: rgba(76, 217, 100, 0.1);
      color: $success-color;
    }
    .status-value {
      color: $success-color;
    }
  }
  
  .status-in-progress {
    .status-icon {
      background-color: rgba(255, 149, 0, 0.1);
      color: $warning-color;
    }
    .status-value {
      color: $warning-color;
    }
  }
  
  .status-overdue {
    .status-icon {
      background-color: rgba(255, 59, 48, 0.1);
      color: $danger-color;
    }
    .status-value {
      color: $danger-color;
    }
  }
}

/* 动画效果 */
.countUp {
  animation: countUp 1.5s cubic-bezier(0.34, 1.56, 0.64, 1) forwards;
  opacity: 0;
  transform: translateY(10px);
}

@keyframes countUp {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 交互效果类 */
.card-hover-effect {
  &:hover {
    transform: translateY(-8px);
    box-shadow: $card-shadow-hover;
  }
}

.btn-hover-effect {
  &:hover {
    transform: translateY(-5px);
    box-shadow: 0 10px 20px rgba(0, 0, 0, 0.15);
  }
  
  &:active {
    transform: translateY(1px);
  }
}

/* 响应式调整 */
@media (max-width: 375px) {
  .work-order-app {
    padding: 15px 10px;
  }
  
  .stats-cards {
    gap: 10px;
  }
  
  .stat-card {
    padding: 15px !important;
  }
  
  .stat-value {
    font-size: 26px !important;
  }
  
  .action-btn {
    padding: 20px 5px !important;
    font-size: 14px !important;
    
    .btn-icon {
      width: 40px !important;
      height: 40px !important;
      font-size: 20px !important;
    }
  }
  
  .chart-container {
    padding: 15px !important;
  }
  
  .chart-content {
    height: 150px !important;
  }
}
</style>

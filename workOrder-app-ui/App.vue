<script>
  import config from './config'
  import { getToken } from '@/utils/auth'
  import { initUpdateManager } from '@/utils/updateManager'
  export default {
    onLaunch: function() {
      this.initApp()
    },
    methods: {
      // 初始化应用
      initApp() {
        // 微信只在小程序启动阶段检查更新，必须先订阅平台回调。
        initUpdateManager()
        // 初始化应用配置
        this.initConfig()
        // 检查用户登录状态
        this.checkLogin()
      },
      initConfig() {
        this.globalData.config = config
      },
      checkLogin() {
        if (!getToken()) {
          this.$tab.reLaunch('/pages/login') 
        }
      }
    }
  }
</script>

<style lang="scss">
  @import "@/node_modules/uview-ui/index.scss";
  @import '@/static/scss/index.scss';
</style>

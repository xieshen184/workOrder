<script>
  import config from './config'
  import { getToken } from '@/utils/auth'
  import { initUpdateManager } from '@/utils/updateManager'
  export default {
    globalData: {
      config: {}
    },
    onLaunch: function() {
      this.initApp()
    },
    methods: {
      initApp() {
        initUpdateManager()
        this.initConfig()
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
  @import "@/uview-ui/index.scss";
  @import '@/static/scss/index.scss';
</style>

// 应用全局配置。小程序构建必须通过环境变量注入 HTTPS 业务地址。
const baseUrl = process.env.VUE_APP_BASE_API || ''
const privacyUrl = process.env.VUE_APP_PRIVACY_URL || ''
const serviceAgreementUrl = process.env.VUE_APP_SERVICE_AGREEMENT_URL || ''

module.exports = {
  baseUrl,
  // 应用信息
  appInfo: {
    // 应用名称
    name: "医院后勤工单系统",
    // 应用版本
    version: "1.2.0",
    // 应用logo
    logo: "/static/logo.png",
    // 官方网站
    site_url: "",
    // 政策协议
    agreements: [{
        title: "隐私政策",
        url: privacyUrl
      },
      {
        title: "用户服务协议",
        url: serviceAgreementUrl
      }
    ]
  }
}

import config from '@/config'

// 内容注册表是应用内可信内容的唯一入口。
// 页面只能按 content id 或 resource key 查询，不能把任意标题、正文或 URL 透传到内容页。
const CONTENT_REGISTRY = {
  'faq.account.login': {
    type: 'article',
    category: '账号与安全',
    title: '如何登录系统？',
    sections: [{
      id: 'login-guide',
      heading: '操作提示',
      paragraphs: [
        '请使用管理员分配的账号和密码登录。',
        '如果看不到验证码或登录失败，请先检查网络连接；仍无法解决时，请联系系统管理员。'
      ],
      bullets: []
    }]
  },
  'faq.account.password': {
    type: 'article',
    category: '账号与安全',
    title: '如何修改登录密码？',
    sections: [{
      id: 'password-guide',
      heading: '操作路径',
      paragraphs: ['进入“我的”-“应用设置”，选择“修改密码”，填写旧密码和新密码后提交。'],
      bullets: [
        '新密码长度需符合页面提示。',
        '两次输入的新密码必须一致。'
      ]
    }]
  },
  'faq.account.logout': {
    type: 'article',
    category: '账号与安全',
    title: '如何退出登录？',
    sections: [{
      id: 'logout-guide',
      heading: '操作路径',
      paragraphs: ['进入“我的”-“应用设置”，选择“退出登录”并确认即可。'],
      bullets: []
    }]
  },
  'faq.order.submit': {
    type: 'article',
    category: '工单处理',
    title: '如何提交工单？',
    sections: [{
      id: 'submit-guide',
      heading: '操作提示',
      paragraphs: ['进入工单提交页面，填写主题、描述和必要的附件后提交。'],
      bullets: [
        '请尽量完整填写问题现象、发生时间和影响范围。',
        '提交前请确认联系人和附件内容准确。'
      ]
    }]
  },
  'faq.order.status': {
    type: 'article',
    category: '工单处理',
    title: '如何查看工单处理进度？',
    sections: [{
      id: 'status-guide',
      heading: '操作提示',
      paragraphs: ['进入“我的工单”查看工单状态、处理记录和最新更新时间。'],
      bullets: []
    }]
  },
  'faq.order.notification': {
    type: 'article',
    category: '工单处理',
    title: '为什么没有收到工单通知？',
    sections: [{
      id: 'notification-guide',
      heading: '排查建议',
      paragraphs: ['请先进入消息中心刷新列表，并确认当前账号仍处于登录状态。'],
      bullets: [
        '网络不稳定时，消息可能会延迟展示。',
        '如果长时间没有更新，请记录工单编号并联系系统管理员。'
      ]
    }]
  },
  'faq.settings.cache': {
    type: 'article',
    category: '设置与安全',
    title: '清理缓存会删除哪些内容？',
    sections: [{
      id: 'cache-guide',
      heading: '安全边界',
      paragraphs: ['清理缓存只处理应用登记的非业务缓存，用于清除页面组件的临时数据。'],
      bullets: [
        '不会删除登录状态或用户资料。',
        '不会删除工单数据、业务草稿或上传记录。'
      ]
    }]
  },
  'faq.settings.update': {
    type: 'article',
    category: '设置与安全',
    title: '如何检查应用更新？',
    sections: [{
      id: 'update-guide',
      heading: '操作提示',
      paragraphs: ['微信小程序可在应用设置中检查小程序更新；其他平台请通过应用分发渠道或联系系统管理员获取版本信息。'],
      bullets: []
    }]
  },
  'faq.contact': {
    type: 'article',
    category: '帮助与支持',
    title: '联系支持',
    sections: [{
      id: 'contact-guide',
      heading: '需要进一步帮助？',
      paragraphs: ['请准备账号、工单编号和问题现象，并联系系统管理员。请勿在公开渠道发送密码或登录凭证。'],
      bullets: []
    }]
  },
  'agreement.service': {
    type: 'article',
    category: '协议',
    title: '用户服务协议',
    sections: [{
      id: 'service-fallback',
      heading: '协议内容提示',
      paragraphs: [
        '当前环境尚未配置在线服务协议内容。',
        '如需查看完整协议，请联系系统管理员。'
      ],
      bullets: []
    }]
  },
  'agreement.privacy': {
    type: 'article',
    category: '协议',
    title: '隐私政策',
    sections: [{
      id: 'privacy-fallback',
      heading: '政策内容提示',
      paragraphs: [
        '当前环境尚未配置在线隐私政策内容。',
        '如需查看完整政策，请联系系统管理员。'
      ],
      bullets: []
    }]
  }
}

const agreementList = config && config.appInfo && Array.isArray(config.appInfo.agreements)
  ? config.appInfo.agreements
  : []

function getConfiguredAgreementUrl(index) {
  const agreement = agreementList[index]
  return agreement && typeof agreement.url === 'string' ? agreement.url.trim() : ''
}

// key 与具体 URL 的映射只能在此处登记，页面不得拼接或接收外部 URL。
const RESOURCE_REGISTRY = {
  serviceSite: {
    key: 'serviceSite',
    title: '服务网站',
    url: config && config.appInfo ? String(config.appInfo.site_url || '').trim() : '',
    fallbackContentId: 'faq.contact'
  },
  serviceAgreement: {
    key: 'serviceAgreement',
    title: '用户服务协议',
    url: getConfiguredAgreementUrl(1),
    fallbackContentId: 'agreement.service'
  },
  privacyPolicy: {
    key: 'privacyPolicy',
    title: '隐私政策',
    url: getConfiguredAgreementUrl(0),
    fallbackContentId: 'agreement.privacy'
  }
}

function clone(value) {
  return value ? JSON.parse(JSON.stringify(value)) : null
}

function getUrlHost(url) {
  if (typeof url !== 'string') return ''
  const value = url.trim()
  const match = /^https:\/\/([^/?#]+)(?:[/?#]|$)/i.exec(value)
  if (!match) return ''

  const authority = match[1]
  // 拒绝用户名密码、非法端口和不符合主机格式的地址，避免把类似 host@evil.example 的地址误判为可信。
  if (authority.indexOf('@') !== -1) return ''
  const parts = authority.split(':')
  const host = parts[0]
  const port = parts[1]
  if (!host || !/^[a-z0-9.-]+$/i.test(host)) return ''
  if (parts.length > 2) return ''
  if (parts.length === 2 && (!port || !/^\d+$/.test(port) || Number(port) < 1 || Number(port) > 65535)) return ''
  return host.toLowerCase().replace(/\.$/, '')
}

const trustedHosts = {}
Object.keys(RESOURCE_REGISTRY).forEach(key => {
  const host = getUrlHost(RESOURCE_REGISTRY[key].url)
  if (host) trustedHosts[host] = true
})

// 公开接口仅允许按 id 查询内置内容。
export function getContentById(id) {
  if (typeof id !== 'string') return null
  const normalizedId = id.trim()
  if (!Object.prototype.hasOwnProperty.call(CONTENT_REGISTRY, normalizedId)) return null
  const content = CONTENT_REGISTRY[normalizedId]
  return clone(content)
}

// 公开接口仅允许按 key 查询已登记的外部内容资源。
export function getResourceByKey(key) {
  if (typeof key !== 'string') return null
  const normalizedKey = key.trim()
  if (!Object.prototype.hasOwnProperty.call(RESOURCE_REGISTRY, normalizedKey)) return null
  const resource = RESOURCE_REGISTRY[normalizedKey]
  return clone(resource)
}

// 公开 URL 校验接口：必须是 HTTPS，且主机来自已登记资源的可信主机集合。
export function isTrustedHttpsUrl(url) {
  const host = getUrlHost(url)
  return !!(host && Object.prototype.hasOwnProperty.call(trustedHosts, host))
}

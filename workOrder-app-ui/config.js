// 应用全局配置。生产发行必须由环境变量注入真实 HTTPS 业务地址。
// VUE_APP_ENV 优先于 NODE_ENV；未设置时按开发环境处理，保证微信开发者工具本地自测可用。
const environmentVariables = typeof process !== 'undefined' && process.env ? process.env : {}

function readEnvironmentValue(name) {
  const value = environmentVariables[name]
  // 必填地址缺失时保留 undefined，交给校验接口明确报告，不静默拼接空地址。
  return value === undefined || value === null ? undefined : String(value).trim()
}

function normalizeEnvironment(value) {
  return typeof value === 'string' ? value.trim().toLowerCase() : ''
}

function resolveBuildEnvironment() {
  return normalizeEnvironment(readEnvironmentValue('VUE_APP_ENV'))
    || normalizeEnvironment(readEnvironmentValue('NODE_ENV'))
    || 'development'
}

function isDevelopmentEnvironment(value) {
  const normalized = normalizeEnvironment(value)
  return normalized === '' || /^(development|dev|local|test)$/.test(normalized)
}

function isValidIpv4(hostname) {
  const parts = hostname.split('.')
  if (parts.length !== 4) return false

  for (let index = 0; index < parts.length; index += 1) {
    if (!/^\d{1,3}$/.test(parts[index]) || Number(parts[index]) > 255) {
      return false
    }
  }
  return true
}

function isPrivateIpv4(hostname) {
  const parts = hostname.split('.').map(Number)
  const first = parts[0]
  const second = parts[1]
  const third = parts[2]

  return first === 0
    || first === 10
    || first === 127
    || (first === 100 && second >= 64 && second <= 127)
    || (first === 169 && second === 254)
    || (first === 172 && second >= 16 && second <= 31)
    || (first === 192 && second === 168)
    || (first === 192 && second === 0 && third === 0)
    || (first === 192 && second === 0 && third === 2)
    || (first === 198 && (second === 18 || second === 19))
    || (first === 198 && second === 51 && third === 100)
    || (first === 203 && second === 0 && third === 113)
    || first >= 224
}

function isPrivateIpv6(hostname) {
  const normalized = hostname.toLowerCase()
  const ipv4Mapped = normalized.match(/::ffff:(\d+(?:\.\d+){3})$/)

  return normalized === '::'
    || normalized === '::1'
    || /^f[cd]/.test(normalized)
    || /^fe[89ab]/.test(normalized)
    || (ipv4Mapped && isValidIpv4(ipv4Mapped[1]) && isPrivateIpv4(ipv4Mapped[1]))
}

function isPlaceholderHostname(hostname) {
  const normalized = hostname.toLowerCase()
  return normalized === 'your-domain.invalid'
    || normalized.slice(-(('.your-domain.invalid').length)) === '.your-domain.invalid'
    || normalized === 'invalid'
    || normalized.slice(-('.invalid'.length)) === '.invalid'
}

function isPrivateOrLocalHostname(hostname) {
  const normalized = hostname.toLowerCase()

  if (normalized === 'localhost'
      || normalized.slice(-('.localhost'.length)) === '.localhost'
      || normalized === 'local'
      || normalized.slice(-('.local'.length)) === '.local'
      || normalized === 'internal'
      || normalized.slice(-('.internal'.length)) === '.internal') {
    return true
  }

  if (isValidIpv4(normalized)) return isPrivateIpv4(normalized)
  if (normalized.indexOf(':') !== -1) return isPrivateIpv6(normalized)
  return false
}

function parseEndpoint(value) {
  if (typeof value !== 'string') return null

  const normalized = value.trim()
  const match = /^(https?):\/\/([^/?#]+)(?:[/?#].*)?$/i.exec(normalized)
  if (!match || /\s/.test(normalized)) return null

  const authority = match[2]
  if (!authority || authority.indexOf('@') !== -1) return null

  let hostname = authority
  let port = ''

  if (authority.charAt(0) === '[') {
    const closingBracket = authority.indexOf(']')
    if (closingBracket < 0) return null
    hostname = authority.slice(1, closingBracket)
    port = authority.slice(closingBracket + 1)
    if (port && !/^:\d+$/.test(port)) return null
    port = port.slice(1)
  } else {
    const lastColon = authority.lastIndexOf(':')
    if (lastColon !== -1) {
      if (authority.indexOf(':') !== lastColon) return null
      hostname = authority.slice(0, lastColon)
      port = authority.slice(lastColon + 1)
      if (!/^\d+$/.test(port)) return null
    }
  }

  if (!hostname || (port && (Number(port) < 1 || Number(port) > 65535))) return null

  hostname = hostname.toLowerCase().replace(/\.$/, '')
  if (!hostname) return null

  if (hostname.indexOf(':') !== -1) {
    if (!/^[0-9a-f:]+$/i.test(hostname)) return null
  } else if (/^\d+(?:\.\d+){3}$/.test(hostname)) {
    if (!isValidIpv4(hostname)) return null
  } else if (!/^(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)(?:\.(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?))*$/i.test(hostname)) {
    return null
  }

  return {
    protocol: match[1].toLowerCase(),
    hostname
  }
}

// 该接口只判断可用于生产的公开 HTTPS 地址，不做 DNS 解析。
function isValidHttpsUrl(value) {
  const endpoint = parseEndpoint(value)
  return Boolean(endpoint)
    && endpoint.protocol === 'https'
    && !isPlaceholderHostname(endpoint.hostname)
    && !isPrivateOrLocalHostname(endpoint.hostname)
}

function addUrlValidationError(errors, name, value, options) {
  const required = options.required !== false
  if (value === undefined || value === null || value === '') {
    if (required) errors.push(name + ' 未配置')
    return
  }

  const endpoint = parseEndpoint(value)
  if (!endpoint) {
    errors.push(name + ' 必须是完整的 HTTP(S) 地址')
    return
  }

  if (isPlaceholderHostname(endpoint.hostname)) {
    errors.push(name + ' 不能使用示例域名')
    return
  }

  if (options.requireHttps && endpoint.protocol !== 'https') {
    errors.push(name + ' 必须使用 HTTPS')
    return
  }

  if (options.rejectPrivateHost && isPrivateOrLocalHostname(endpoint.hostname)) {
    errors.push(name + ' 不能使用 localhost 或内网 IP')
  }
}

// 小而稳定的配置校验接口：开发环境允许显式本地地址，但生产/预发布要求公开 HTTPS 地址。
function validateConfig(values, environmentName) {
  const configValues = values || runtimeValues
  const buildEnvironment = normalizeEnvironment(environmentName) || environment
  const strict = !isDevelopmentEnvironment(buildEnvironment)
  const errors = []

  addUrlValidationError(errors, 'VUE_APP_BASE_API', configValues.baseUrl, {
    required: true,
    requireHttps: strict,
    rejectPrivateHost: strict
  })
  addUrlValidationError(errors, 'VUE_APP_PRIVACY_URL', configValues.privacyUrl, {
    required: strict,
    requireHttps: strict,
    rejectPrivateHost: strict
  })
  addUrlValidationError(errors, 'VUE_APP_SERVICE_AGREEMENT_URL', configValues.serviceAgreementUrl, {
    required: strict,
    requireHttps: strict,
    rejectPrivateHost: strict
  })
  addUrlValidationError(errors, 'VUE_APP_SERVICE_SITE_URL', configValues.serviceSiteUrl, {
    required: false,
    requireHttps: strict,
    rejectPrivateHost: strict
  })

  return {
    valid: errors.length === 0,
    environment: buildEnvironment,
    errors
  }
}

function assertValidConfig(values, environmentName) {
  const result = validateConfig(values, environmentName)
  if (!result.valid) {
    throw new Error('移动端生产配置校验失败：' + result.errors.join('；'))
  }
  return result
}

const environment = resolveBuildEnvironment()
const runtimeValues = {
  baseUrl: readEnvironmentValue('VUE_APP_BASE_API')
    || (isDevelopmentEnvironment(environment) ? 'http://192.168.0.116:8080' : undefined),
  privacyUrl: readEnvironmentValue('VUE_APP_PRIVACY_URL'),
  serviceAgreementUrl: readEnvironmentValue('VUE_APP_SERVICE_AGREEMENT_URL'),
  serviceSiteUrl: readEnvironmentValue('VUE_APP_SERVICE_SITE_URL'),
  serviceEmail: readEnvironmentValue('VUE_APP_SERVICE_EMAIL'),
  servicePhone: readEnvironmentValue('VUE_APP_SERVICE_PHONE')
}
const validation = validateConfig(runtimeValues, environment)

// 只有非开发环境在模块加载时阻断，避免影响微信开发者工具的本地联调。
if (!isDevelopmentEnvironment(environment)) {
  assertValidConfig(runtimeValues, environment)
}

module.exports = {
  environment,
  baseUrl: runtimeValues.baseUrl,
  validation,
  isValidHttpsUrl,
  validateConfig,
  assertValidConfig,
  // 应用信息
  appInfo: {
    // 应用名称
    name: "医院后勤工单系统",
    // 应用版本
    version: "1.2.0",
    // 应用logo
    logo: "/static/logo.png",
    // 官方网站：为空时不展示不可用入口，由部署环境按需注入。
    site_url: runtimeValues.serviceSiteUrl,
    // 联系方式可选，页面按需隐藏空值。
    contact: {
      email: runtimeValues.serviceEmail,
      phone: runtimeValues.servicePhone
    },
    // 政策协议地址在生产环境必须是公开 HTTPS 地址。
    agreements: [{
        title: "隐私政策",
        url: runtimeValues.privacyUrl
      },
      {
        title: "用户服务协议",
        url: runtimeValues.serviceAgreementUrl
      }
    ]
  }
}

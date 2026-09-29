// 只登记明确的、非业务用途缓存键。
// 这里的键来自组件自身的最近选择记录，不承载登录态、用户资料或工单业务数据。
const REGISTERED_CACHE_ITEMS = [{
  key: 'uni-data-select-lastSelectedValue',
  label: '选择器最近选项'
}]

// 防御性保护：即使以后有人误把敏感键加入登记表，也禁止清理。
const PROTECTED_CACHE_KEYS = [
  'App-Token',
  'storage_data',
  'user_avatar',
  'user_id',
  'user_name',
  'user_roles',
  'user_permissions'
]

function isProtectedCacheKey(key) {
  if (typeof key !== 'string') return true
  const normalizedKey = key.toLowerCase()
  return PROTECTED_CACHE_KEYS.indexOf(key) !== -1 ||
    normalizedKey.indexOf('token') !== -1 ||
    normalizedKey.indexOf('draft') !== -1 ||
    normalizedKey.indexOf('business') !== -1 ||
    normalizedKey.indexOf('user_') === 0
}

function readCache(key) {
  try {
    return uni.getStorageSync(key)
  } catch (error) {
    return undefined
  }
}

function hasCacheValue(value) {
  return value !== undefined && value !== null && value !== ''
}

function getCacheSize(value) {
  if (!hasCacheValue(value)) return 0
  try {
    return JSON.stringify(value).length
  } catch (error) {
    return String(value).length
  }
}

function formatSize(size) {
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / (1024 * 1024)).toFixed(1)} MB`
}

// 返回的只是登记键、存在状态和估算大小，不返回缓存值，避免在设置页泄露数据。
export function getCacheSummary() {
  const items = REGISTERED_CACHE_ITEMS.map(item => {
    const value = readCache(item.key)
    const size = getCacheSize(value)
    return {
      key: item.key,
      label: item.label,
      exists: hasCacheValue(value),
      size,
      sizeText: formatSize(size)
    }
  })
  const activeItems = items.filter(item => item.exists)
  const totalSize = activeItems.reduce((total, item) => total + item.size, 0)

  return {
    items,
    activeCount: activeItems.length,
    totalSize,
    totalSizeText: formatSize(totalSize)
  }
}

// 只遍历 REGISTERED_CACHE_ITEMS，绝不调用 clearStorageSync 或 storage.clean。
export function clearRegisteredCache() {
  const cleared = []
  const skipped = []

  REGISTERED_CACHE_ITEMS.forEach(item => {
    if (isProtectedCacheKey(item.key)) {
      skipped.push(item.key)
      return
    }
    try {
      uni.removeStorageSync(item.key)
      cleared.push(item.key)
    } catch (error) {
      skipped.push(item.key)
    }
  })

  return { cleared, skipped }
}

// 微信没有“点击后主动检查更新”的接口，平台只会在小程序启动阶段触发检查。
// 本模块在 App.onLaunch 尽早订阅事件，设置页只读取检查结果或应用已下载版本。
let updateManager = null
let initialized = false

const state = {
  supported: false,
  checked: false,
  hasUpdate: false,
  ready: false,
  failed: false
}

export function initUpdateManager() {
  if (initialized) return getUpdateStatus()
  initialized = true

  // #ifdef MP-WEIXIN
  if (typeof uni.getUpdateManager !== 'function') return getUpdateStatus()
  try {
    updateManager = uni.getUpdateManager()
    state.supported = true
    updateManager.onCheckForUpdate(result => {
      state.checked = true
      state.hasUpdate = Boolean(result && result.hasUpdate)
    })
    updateManager.onUpdateReady(() => {
      state.checked = true
      state.hasUpdate = true
      state.ready = true
      state.failed = false
    })
    updateManager.onUpdateFailed(() => {
      state.checked = true
      state.hasUpdate = true
      state.ready = false
      state.failed = true
    })
  } catch (error) {
    updateManager = null
    state.supported = false
  }
  // #endif

  return getUpdateStatus()
}

export function getUpdateStatus() {
  return Object.assign({}, state)
}

export function applyReadyUpdate() {
  if (!updateManager || !state.ready || typeof updateManager.applyUpdate !== 'function') return false
  updateManager.applyUpdate()
  return true
}

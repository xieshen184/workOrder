<template>
	<view class="container">
		<view class="page-body uni-content-info">
			<view class='cropper-content'>
				<view v-if="isShowImg" class="uni-corpper" :style="'width:'+cropperInitW+'px;height:'+cropperInitH+'px;background:#000'">
					<view class="uni-corpper-content" :style="'width:'+cropperW+'px;height:'+cropperH+'px;left:'+cropperL+'px;top:'+cropperT+'px'">
						<image :src="imageSrc" :style="'width:'+cropperW+'px;height:'+cropperH+'px'"></image>
						<view class="uni-corpper-crop-box" @touchstart.stop="contentStartMove" @touchmove.stop="contentMoveing" @touchend.stop="contentTouchEnd"
						    :style="'left:'+cutL+'px;top:'+cutT+'px;right:'+cutR+'px;bottom:'+cutB+'px'">
							<view class="uni-cropper-view-box">
								<view class="uni-cropper-dashed-h"></view>
								<view class="uni-cropper-dashed-v"></view>
								<view class="uni-cropper-line-t" data-drag="top" @touchstart.stop="dragStart" @touchmove.stop="dragMove"></view>
								<view class="uni-cropper-line-r" data-drag="right" @touchstart.stop="dragStart" @touchmove.stop="dragMove"></view>
								<view class="uni-cropper-line-b" data-drag="bottom" @touchstart.stop="dragStart" @touchmove.stop="dragMove"></view>
								<view class="uni-cropper-line-l" data-drag="left" @touchstart.stop="dragStart" @touchmove.stop="dragMove"></view>
								<view class="uni-cropper-point point-t" data-drag="top" @touchstart.stop="dragStart" @touchmove.stop="dragMove"></view>
								<view class="uni-cropper-point point-tr" data-drag="topTight"></view>
								<view class="uni-cropper-point point-r" data-drag="right" @touchstart.stop="dragStart" @touchmove.stop="dragMove"></view>
								<view class="uni-cropper-point point-rb" data-drag="rightBottom" @touchstart.stop="dragStart" @touchmove.stop="dragMove"></view>
								<view class="uni-cropper-point point-b" data-drag="bottom" @touchstart.stop="dragStart" @touchmove.stop="dragMove" @touchend.stop="dragEnd"></view>
								<view class="uni-cropper-point point-bl" data-drag="bottomLeft"></view>
								<view class="uni-cropper-point point-l" data-drag="left" @touchstart.stop="dragStart" @touchmove.stop="dragMove"></view>
								<view class="uni-cropper-point point-lt" data-drag="leftTop"></view>
							</view>
						</view>
					</view>
				</view>
				<view v-else class="empty-image-tip">
					{{ imageSrc ? '图片读取失败，请重新选择' : '请选择头像图片' }}
				</view>
			</view>
			<view class='cropper-config'>
				<button type="primary" :disabled="uploading" @click="getImage" style='margin-top: 30rpx;'>选择头像</button>
				<button type="warn" :disabled="uploading || !isShowImg" @click="getImageInfo" style='margin-top: 30rpx;'>提交</button>
				<view v-if="errorMessage" class="error-message">{{ errorMessage }}</view>
			</view>
			<canvas canvas-id="myCanvas" :style="'position:absolute;border: 1px solid red; width:'+imageW+'px;height:'+imageH+'px;top:-9999px;left:-9999px;'"></canvas>
		</view>
	</view>
</template>

<script>
  import config from '@/config'
  import store from "@/store"
  import { uploadAvatar } from "@/api/system/user"
  
  const baseUrl = config.baseUrl
	let sysInfo = uni.getSystemInfoSync()
	let SCREEN_WIDTH = sysInfo.screenWidth
	let PAGE_X, // 手按下的x位置
		PAGE_Y, // 手按下y的位置 
		PR = sysInfo.pixelRatio, // dpi
		T_PAGE_X, // 手移动的时候x的位置
		T_PAGE_Y, // 手移动的时候Y的位置
		CUT_L, // 初始化拖拽元素的left值
		CUT_T, // 初始化拖拽元素的top值
		CUT_R, // 初始化拖拽元素的
		CUT_B, // 初始化拖拽元素的
		CUT_W, // 初始化拖拽元素的宽度
		CUT_H, //  初始化拖拽元素的高度
		IMG_RATIO, // 图片比例
		IMG_REAL_W, // 图片实际的宽度
		IMG_REAL_H, // 图片实际的高度
		DRAFG_MOVE_RATIO = 1, //移动时候的比例,
		INIT_DRAG_POSITION = 100, // 初始化屏幕宽度和裁剪区域的宽度之差，用于设置初始化裁剪的宽度
		DRAW_IMAGE_W = sysInfo.screenWidth // 设置生成的图片宽度

	export default {
		/**
		 * 页面的初始数据
		 */
		data() {
			const avatar = store.getters.avatar || ''
			return {
				// 原头像只在上传成功后更新；失败时保留当前选择和原头像。
				originalAvatar: avatar,
				imageSrc: avatar,
				isShowImg: false,
				uploading: false,
				errorMessage: '',
				// 初始化的宽高
				cropperInitW: SCREEN_WIDTH,
				cropperInitH: SCREEN_WIDTH,
				// 动态的宽高
				cropperW: SCREEN_WIDTH,
				cropperH: SCREEN_WIDTH,
				// 动态的left top值
				cropperL: 0,
				cropperT: 0,

				transL: 0,
				transT: 0,

				// 图片缩放值
				scaleP: 0,
				imageW: 0,
				imageH: 0,

				// 裁剪框 宽高
				cutL: 0,
				cutT: 0,
				cutB: SCREEN_WIDTH,
				cutR: '100%',
				qualityWidth: DRAW_IMAGE_W,
				innerAspectRadio: DRAFG_MOVE_RATIO
			}
		},
		/**
		 * 生命周期函数--监听页面初次渲染完成
		 */
		onReady: function () {
			if (this.imageSrc) this.loadImage(false)
		},
		methods: {
			setData: function (obj) {
				let that = this
				Object.keys(obj).forEach(function (key) {
					that.$set(that.$data, key, obj[key])
				})
			},
			showMessage(message) {
				uni.showToast({ title: message, icon: 'none' })
			},
			getImage: function () {
				if (this.uploading) return
				const that = this
				uni.chooseImage({
					count: 1,
					sizeType: ['compressed'],
					success: function (res) {
						const imagePath = res && res.tempFilePaths && res.tempFilePaths[0]
						if (!imagePath) {
							that.errorMessage = '未获取到图片，请重新选择'
							that.showMessage(that.errorMessage)
							return
						}
						// 选择成功后保留本地路径；读取或上传失败时仍可继续重试。
						that.imageSrc = imagePath
						that.errorMessage = ''
						that.loadImage(true)
					},
					fail: function (error) {
						const message = error && error.errMsg && error.errMsg.indexOf('cancel') !== -1
							? '已取消选择头像'
							: '选择头像失败，请重试'
						that.errorMessage = message.indexOf('取消') !== -1 ? '' : message
						that.showMessage(message)
					}
				})
			},
			loadImage: function (showError) {
				const source = this.imageSrc
				return new Promise(resolve => {
					const fail = message => {
						this.isShowImg = false
						if (showError) {
							this.errorMessage = message
							this.showMessage(message)
						}
						resolve(false)
					}
					if (!source) {
						fail('请先选择头像')
						return
					}
					uni.getImageInfo({
						src: source,
						success: res => {
							const width = Number(res && res.width)
							const height = Number(res && res.height)
							if (!width || !height) {
								fail('图片读取失败，请重新选择')
								return
							}
							IMG_RATIO = width / height
							if (IMG_RATIO >= 1) {
								IMG_REAL_W = SCREEN_WIDTH
								IMG_REAL_H = SCREEN_WIDTH / IMG_RATIO
							} else {
								IMG_REAL_W = SCREEN_WIDTH * IMG_RATIO
								IMG_REAL_H = SCREEN_WIDTH
							}
							const minRange = Math.min(IMG_REAL_W, IMG_REAL_H)
							const initialDragPosition = Math.min(INIT_DRAG_POSITION, minRange)
							if (IMG_RATIO >= 1) {
								const cutT = Math.ceil((IMG_REAL_H - initialDragPosition) / 2)
								const cutL = Math.ceil((IMG_REAL_W - initialDragPosition) / 2)
								this.setData({
									cropperW: SCREEN_WIDTH,
									cropperH: IMG_REAL_H,
									cropperL: 0,
									cropperT: Math.ceil((SCREEN_WIDTH - IMG_REAL_H) / 2),
									cutL: cutL,
									cutT: cutT,
									cutR: cutL,
									cutB: cutT,
									imageW: IMG_REAL_W,
									imageH: IMG_REAL_H,
									scaleP: IMG_REAL_W / SCREEN_WIDTH,
									qualityWidth: DRAW_IMAGE_W,
									innerAspectRadio: IMG_RATIO,
									isShowImg: true
								})
							} else {
								const cutT = Math.ceil((IMG_REAL_H - initialDragPosition) / 2)
								this.setData({
									cropperW: IMG_REAL_W,
									cropperH: SCREEN_WIDTH,
									cropperL: Math.ceil((SCREEN_WIDTH - IMG_REAL_W) / 2),
									cropperT: 0,
									cutL: 0,
									cutT: cutT,
									cutR: 0,
									cutB: cutT,
									imageW: IMG_REAL_W,
									imageH: IMG_REAL_H,
									scaleP: IMG_REAL_W / SCREEN_WIDTH,
									qualityWidth: DRAW_IMAGE_W,
									innerAspectRadio: IMG_RATIO,
									isShowImg: true
								})
							}
							this.errorMessage = ''
							resolve(true)
						},
						fail: () => fail('图片读取失败，请重新选择')
					})
				})
			},
			// 拖动时候触发的touchStart事件
			contentStartMove(e) {
				PAGE_X = e.touches[0].pageX
				PAGE_Y = e.touches[0].pageY
			},

			// 拖动时候触发的touchMove事件
			contentMoveing(e) {
				var _this = this
				var dragLengthX = (PAGE_X - e.touches[0].pageX) * DRAFG_MOVE_RATIO
				var dragLengthY = (PAGE_Y - e.touches[0].pageY) * DRAFG_MOVE_RATIO
				// 左移
				if (dragLengthX > 0) {
					if (this.cutL - dragLengthX < 0) dragLengthX = this.cutL
				} else {
					if (this.cutR + dragLengthX < 0) dragLengthX = -this.cutR
				}

				if (dragLengthY > 0) {
					if (this.cutT - dragLengthY < 0) dragLengthY = this.cutT
				} else {
					if (this.cutB + dragLengthY < 0) dragLengthY = -this.cutB
				}
				this.setData({
					cutL: this.cutL - dragLengthX,
					cutT: this.cutT - dragLengthY,
					cutR: this.cutR + dragLengthX,
					cutB: this.cutB + dragLengthY
				})

				PAGE_X = e.touches[0].pageX
				PAGE_Y = e.touches[0].pageY
			},

			contentTouchEnd() {

			},

			// 裁剪、上传共用一个锁；失败只提示并保留当前选择，避免重复提交或清空原头像。
			async getImageInfo() {
				if (this.uploading) return
				if (!this.imageSrc || !this.isShowImg) {
					this.errorMessage = '请先选择可用的头像图片'
					this.showMessage(this.errorMessage)
					return
				}
				this.uploading = true
				let stage = '处理'
				uni.showLoading({ title: '正在处理头像...', mask: true })
				try {
					const tempFilePath = await this.createCroppedImage()
					stage = '上传'
					uni.showLoading({ title: '正在上传头像...', mask: true })
					const response = await uploadAvatar({ name: 'avatarfile', filePath: tempFilePath })
					if (!response || !response.imgUrl) throw new Error('上传响应缺少头像地址')
					const avatarUrl = this.normalizeAvatarUrl(response.imgUrl)
					store.commit('SET_AVATAR', avatarUrl)
					this.originalAvatar = avatarUrl
					this.imageSrc = avatarUrl
					this.errorMessage = ''
					uni.showToast({ title: '头像修改成功', icon: 'success' })
					try {
						this.$tab.navigateBack()
					} catch (navigationError) {
						this.showMessage('头像已修改，请手动返回')
					}
				} catch (error) {
					this.errorMessage = stage === '上传' ? '头像上传失败，请重试' : '头像处理失败，请重试'
					this.showMessage(this.errorMessage)
				} finally {
					uni.hideLoading()
					this.uploading = false
				}
			},
			createCroppedImage() {
				return new Promise((resolve, reject) => {
					try {
						const ctx = uni.createCanvasContext('myCanvas')
						ctx.drawImage(this.imageSrc, 0, 0, IMG_REAL_W, IMG_REAL_H)
						ctx.draw(false, () => {
							const canvasW = Math.max(1, Math.round(((this.cropperW - this.cutL - this.cutR) / this.cropperW) * IMG_REAL_W))
							const canvasH = Math.max(1, Math.round(((this.cropperH - this.cutT - this.cutB) / this.cropperH) * IMG_REAL_H))
							const canvasL = Math.max(0, (this.cutL / this.cropperW) * IMG_REAL_W)
							const canvasT = Math.max(0, (this.cutT / this.cropperH) * IMG_REAL_H)
							uni.canvasToTempFilePath({
								x: canvasL,
								y: canvasT,
								width: canvasW,
								height: canvasH,
								destWidth: canvasW,
								destHeight: canvasH,
								quality: 0.8,
								canvasId: 'myCanvas',
								success: result => {
									if (!result || !result.tempFilePath) {
										reject(new Error('裁剪结果为空'))
										return
									}
									resolve(result.tempFilePath)
								},
								fail: reject
							})
						})
					} catch (error) {
						reject(error)
					}
				})
			},
			normalizeAvatarUrl(url) {
				const value = String(url || '').trim()
				if (!value) return ''
				if (/^(https?:|data:|blob:)/i.test(value)) return value
				const prefix = String(baseUrl || '').replace(/\/+$/, '')
				return prefix + '/' + value.replace(/^\/+/, '')
			},
			// 设置大小的时候触发的touchStart事件
			dragStart(e) {
				T_PAGE_X = e.touches[0].pageX
				T_PAGE_Y = e.touches[0].pageY
				CUT_L = this.cutL
				CUT_R = this.cutR
				CUT_B = this.cutB
				CUT_T = this.cutT
			},

			// 设置大小的时候触发的touchMove事件
			dragMove(e) {
				var dragType = (e.currentTarget && e.currentTarget.dataset && e.currentTarget.dataset.drag)
					|| (e.target && e.target.dataset && e.target.dataset.drag)
				switch (dragType) {
					case 'right': {
						let dragLength = (T_PAGE_X - e.touches[0].pageX) * DRAFG_MOVE_RATIO
						if (CUT_R + dragLength < 0) dragLength = -CUT_R
						this.setData({
							cutR: CUT_R + dragLength
						})
						break
					}
					case 'left': {
						let dragLength = (T_PAGE_X - e.touches[0].pageX) * DRAFG_MOVE_RATIO
						if (CUT_L - dragLength < 0) dragLength = CUT_L
						if ((CUT_L - dragLength) > (this.cropperW - this.cutR)) dragLength = CUT_L - (this.cropperW - this.cutR)
						this.setData({
							cutL: CUT_L - dragLength
						})
						break
					}
					case 'top': {
						let dragLength = (T_PAGE_Y - e.touches[0].pageY) * DRAFG_MOVE_RATIO
						if (CUT_T - dragLength < 0) dragLength = CUT_T
						if ((CUT_T - dragLength) > (this.cropperH - this.cutB)) dragLength = CUT_T - (this.cropperH - this.cutB)
						this.setData({
							cutT: CUT_T - dragLength
						})
						break
					}
					case 'bottom': {
						let dragLength = (T_PAGE_Y - e.touches[0].pageY) * DRAFG_MOVE_RATIO
						if (CUT_B + dragLength < 0) dragLength = -CUT_B
						this.setData({
							cutB: CUT_B + dragLength
						})
						break
					}
					case 'rightBottom': {
						let dragLengthX = (T_PAGE_X - e.touches[0].pageX) * DRAFG_MOVE_RATIO
						let dragLengthY = (T_PAGE_Y - e.touches[0].pageY) * DRAFG_MOVE_RATIO

						if (CUT_B + dragLengthY < 0) dragLengthY = -CUT_B
						if (CUT_R + dragLengthX < 0) dragLengthX = -CUT_R
						let cutB = CUT_B + dragLengthY
						let cutR = CUT_R + dragLengthX

						this.setData({
							cutB: cutB,
							cutR: cutR
						})
						break
					}
					default:
						break
				}
			}
		}
	}
</script>

<style scoped>
	.cropper-config {
		padding: 20rpx 40rpx;
	}

	.cropper-content {
		min-height: 750rpx;
		width: 100%;
	}

	.empty-image-tip {
		padding-top: 300rpx;
		color: #999;
		font-size: 28rpx;
		text-align: center;
	}

	.error-message {
		padding-top: 20rpx;
		color: #dd524d;
		font-size: 26rpx;
		text-align: center;
	}

	.uni-corpper {
		position: relative;
		overflow: hidden;
		-webkit-user-select: none;
		-moz-user-select: none;
		-ms-user-select: none;
		user-select: none;
		-webkit-tap-highlight-color: transparent;
		-webkit-touch-callout: none;
		box-sizing: border-box;
	}

	.uni-corpper-content {
		position: relative;
	}

	.uni-corpper-content image {
		display: block;
		width: 100%;
		min-width: 0 !important;
		max-width: none !important;
		height: 100%;
		min-height: 0 !important;
		max-height: none !important;
		image-orientation: 0deg !important;
		margin: 0 auto;
	}

	/* 移动图片效果 */
	.uni-cropper-drag-box {
		position: absolute;
		top: 0;
		right: 0;
		bottom: 0;
		left: 0;
		cursor: move;
		background: rgba(0, 0, 0, 0.6);
		z-index: 1;
	}

	/* 内部的信息 */
	.uni-corpper-crop-box {
		position: absolute;
		background: rgba(255, 255, 255, 0.3);
		z-index: 2;
	}

	.uni-corpper-crop-box .uni-cropper-view-box {
		position: relative;
		display: block;
		width: 100%;
		height: 100%;
		overflow: visible;
		outline: 1rpx solid #69f;
		outline-color: rgba(102, 153, 255, .75)
	}

	/* 横向虚线 */
	.uni-cropper-dashed-h {
		position: absolute;
		top: 33.33333333%;
		left: 0;
		width: 100%;
		height: 33.33333333%;
		border-top: 1rpx dashed rgba(255, 255, 255, 0.5);
		border-bottom: 1rpx dashed rgba(255, 255, 255, 0.5);
	}

	/* 纵向虚线 */
	.uni-cropper-dashed-v {
		position: absolute;
		left: 33.33333333%;
		top: 0;
		width: 33.33333333%;
		height: 100%;
		border-left: 1rpx dashed rgba(255, 255, 255, 0.5);
		border-right: 1rpx dashed rgba(255, 255, 255, 0.5);
	}

	/* 四个方向的线  为了之后的拖动事件*/
	.uni-cropper-line-t {
		position: absolute;
		display: block;
		width: 100%;
		background-color: #69f;
		top: 0;
		left: 0;
		height: 1rpx;
		opacity: 0.1;
		cursor: n-resize;
	}

	.uni-cropper-line-t::before {
		content: '';
		position: absolute;
		top: 50%;
		right: 0rpx;
		width: 100%;
		-webkit-transform: translate3d(0, -50%, 0);
		transform: translate3d(0, -50%, 0);
		bottom: 0;
		height: 41rpx;
		background: transparent;
		z-index: 11;
	}

	.uni-cropper-line-r {
		position: absolute;
		display: block;
		background-color: #69f;
		top: 0;
		right: 0rpx;
		width: 1rpx;
		opacity: 0.1;
		height: 100%;
		cursor: e-resize;
	}

	.uni-cropper-line-r::before {
		content: '';
		position: absolute;
		top: 0;
		left: 50%;
		width: 41rpx;
		-webkit-transform: translate3d(-50%, 0, 0);
		transform: translate3d(-50%, 0, 0);
		bottom: 0;
		height: 100%;
		background: transparent;
		z-index: 11;
	}

	.uni-cropper-line-b {
		position: absolute;
		display: block;
		width: 100%;
		background-color: #69f;
		bottom: 0;
		left: 0;
		height: 1rpx;
		opacity: 0.1;
		cursor: s-resize;
	}

	.uni-cropper-line-b::before {
		content: '';
		position: absolute;
		top: 50%;
		right: 0rpx;
		width: 100%;
		-webkit-transform: translate3d(0, -50%, 0);
		transform: translate3d(0, -50%, 0);
		bottom: 0;
		height: 41rpx;
		background: transparent;
		z-index: 11;
	}

	.uni-cropper-line-l {
		position: absolute;
		display: block;
		background-color: #69f;
		top: 0;
		left: 0;
		width: 1rpx;
		opacity: 0.1;
		height: 100%;
		cursor: w-resize;
	}

	.uni-cropper-line-l::before {
		content: '';
		position: absolute;
		top: 0;
		left: 50%;
		width: 41rpx;
		-webkit-transform: translate3d(-50%, 0, 0);
		transform: translate3d(-50%, 0, 0);
		bottom: 0;
		height: 100%;
		background: transparent;
		z-index: 11;
	}

	.uni-cropper-point {
		width: 5rpx;
		height: 5rpx;
		background-color: #69f;
		opacity: .75;
		position: absolute;
		z-index: 3;
	}

	.point-t {
		top: -3rpx;
		left: 50%;
		margin-left: -3rpx;
		cursor: n-resize;
	}

	.point-tr {
		top: -3rpx;
		left: 100%;
		margin-left: -3rpx;
		cursor: n-resize;
	}

	.point-r {
		top: 50%;
		left: 100%;
		margin-left: -3rpx;
		margin-top: -3rpx;
		cursor: n-resize;
	}

	.point-rb {
		left: 100%;
		top: 100%;
		-webkit-transform: translate3d(-50%, -50%, 0);
		transform: translate3d(-50%, -50%, 0);
		cursor: n-resize;
		width: 36rpx;
		height: 36rpx;
		background-color: #69f;
		position: absolute;
		z-index: 1112;
		opacity: 1;
	}

	.point-b {
		left: 50%;
		top: 100%;
		margin-left: -3rpx;
		margin-top: -3rpx;
		cursor: n-resize;
	}

	.point-bl {
		left: 0%;
		top: 100%;
		margin-left: -3rpx;
		margin-top: -3rpx;
		cursor: n-resize;
	}

	.point-l {
		left: 0%;
		top: 50%;
		margin-left: -3rpx;
		margin-top: -3rpx;
		cursor: n-resize;
	}

	.point-lt {
		left: 0%;
		top: 0%;
		margin-left: -3rpx;
		margin-top: -3rpx;
		cursor: n-resize;
	}

	/* 裁剪框预览内容 */
	.uni-cropper-viewer {
		position: relative;
		width: 100%;
		height: 100%;
		overflow: hidden;
	}

	.uni-cropper-viewer image {
		position: absolute;
		z-index: 2;
	}
</style>

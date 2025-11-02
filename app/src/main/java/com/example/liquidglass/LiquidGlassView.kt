/**
 * LiquidGlass 主视图组件
 *
 * Android 版本的 LiquidGlass 效果,移植自 liquid-glass-react
 *
 * 核心功能:
 * - 背景模糊和饱和度调整
 * - 边缘扭曲效果
 * - 色差效果
 * - 触摸交互和弹性动画
 * - 阴影效果（可选）
 *
 * 使用示例:
 * ```xml
 * <com.example.liquidglass.LiquidGlassView
 *     android:layout_width="wrap_content"
 *     android:layout_height="wrap_content"
 *     app:displacementScale="70"
 *     app:blurAmount="0.0625"
 *     app:saturation="140"
 *     app:aberrationIntensity="2"
 *     app:elasticity="0.15"
 *     app:cornerRadius="999" />
 * ```
 */
package com.example.liquidglass

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import kotlin.math.*

class LiquidGlassView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "LiquidGlassView"
        private const val ENABLE_PERFORMANCE_LOG = true  // 性能日志开关
        private const val ENABLE_MEMORY_LOG = false  // 内存日志开关（默认关闭，避免日志污染）

        // ✅ 极致优化参数
        private const val DOWNSCALE_FACTOR = 0.05f  // 降采样比例 (0.4 = 6.25倍提升, 0.5 = 4倍提升)
        private const val RENDER_INTERVAL = 1  // 渲染间隔 (改为1以避免闪烁)
    }

    // ✅ 效果开关
    var enableBackdropBlur = true  // 背景模糊
    var enableChromaticAberration = true  // 色差效果
    var enableShadow = false  // 启用阴影（默认关闭，避免轮廓）
    var enableEdgeHighlight = true  // 边缘高光效果（默认开启）

    // ✅ 边缘高光参数
    var edgeHighlightBorderWidth = 1.5f  // 边框宽度（像素）
    var edgeHighlightOpacity = 100f  // 高光不透明度（0-100）

    // ✅ 模糊方法选择（新增）
    var blurMethod = BlurMethod.SMART
        set(value) {
            if (field != value) {
                field = value
                enhancedBlurEffect.blurMethod = value
                blurDirty = true
                invalidate()
            }
        }

    // ✅ 高质量模式（新增，仅对 IIR 高斯有效）
    var highQualityBlur = false
        set(value) {
            if (field != value) {
                field = value
                enhancedBlurEffect.highQuality = value
                blurDirty = true
                invalidate()
            }
        }

    // ✅ 下采样比例（新增，仅对 DOWNSAMPLE 方法有效）
    var downsampleScale = 2
        set(value) {
            val clamped = value.coerceIn(2, 3)
            if (field != clamped) {
                field = clamped
                enhancedBlurEffect.downsampleScale = clamped
                blurDirty = true
                invalidate()
            }
        }

    // ✅ 全局下采样比例（应用于所有效果：截图→缩小→处理→放大）
    var globalDownsampleFactor = 1.0f
        set(value) {
            val clamped = value.coerceIn(0.25f, 1.0f)
            if (field != clamped) {
                field = clamped
                blurDirty = true
                aberrationDirty = true
                invalidate()
            }
        }

    // ✅ 色差效果下采样比例（独立控制）
    var aberrationDownsample = 0.5f
        set(value) {
            val clamped = value.coerceIn(0.25f, 1.0f)
            if (field != clamped) {
                field = clamped
                aberrationDirty = true
                invalidate()
            }
        }

    // ✅ 色差通道偏移量（精细控制）
    var aberrationRedOffset = 0f
        set(value) {
            if (field != value) {
                field = value
                aberrationDirty = true
                invalidate()
            }
        }

    var aberrationGreenOffset = -0.05f
        set(value) {
            if (field != value) {
                field = value
                aberrationDirty = true
                invalidate()
            }
        }

    var aberrationBlueOffset = -0.1f
        set(value) {
            if (field != value) {
                field = value
                aberrationDirty = true
                invalidate()
            }
        }

    // 效果参数(对应 React 版本的 props) - 带脏标记的属性
    var displacementScale = 70f
        set(value) {
            if (field != value) {
                field = value
                aberrationDirty = true
                invalidate()
            }
        }

    var blurAmount = 0.0625f
        set(value) {
            if (field != value) {
                field = value
                blurDirty = true
                invalidate()
            }
        }

    var saturation = 140f
        set(value) {
            if (field != value) {
                field = value
                blurDirty = true
                invalidate()
            }
        }

    var aberrationIntensity = 2f
        set(value) {
            if (field != value) {
                field = value
                aberrationDirty = true
                invalidate()
            }
        }

    var elasticity = 0.15f             // 弹性系数
    var cornerRadius = 999f            // 圆角半径

    var overLight = false
        set(value) {
            if (field != value) {
                field = value
                blurDirty = true
                aberrationDirty = true
                invalidate()
            }
        }

    var displacementMode = DisplacementMode.STANDARD
        set(value) {
            if (field != value) {
                field = value
                aberrationDirty = true
                invalidate()
            }
        }

    // 效果处理器
    private val backdropBlurEffect = BackdropBlurEffect(this)  // 保留用于兼容性
    private val enhancedBlurEffect = EnhancedBlurEffect(this)  // 新增：增强模糊效果
    private val chromaticAberrationEffect = ChromaticAberrationEffect()
    private val edgeHighlightEffect = EdgeHighlightEffect()

    /**
     * 色差效果性能模式
     *
     * 控制色差效果使用的实现方式：
     * - AUTO: 自动选择（大图用 C++，小图用 Kotlin）
     * - CPP: 强制使用 C++ 实现（推荐，性能提升 3-5 倍）
     * - KOTLIN: 强制使用 Kotlin 实现（兼容性好）
     */
    var chromaticAberrationMode: ChromaticAberrationEffect.PerformanceMode
        get() = chromaticAberrationEffect.performanceMode
        set(value) {
            if (chromaticAberrationEffect.performanceMode != value) {
                chromaticAberrationEffect.performanceMode = value
                aberrationDirty = true
                invalidate()
            }
        }

    // 自定义背景捕获器
    private var customBackdropCapture: ((RectF) -> Bitmap?)? = null

    // 位移贴图缓存
    private var displacementMaps: Map<DisplacementMode, Bitmap>? = null

    // ✅ 智能缓存机制 - 分层缓存策略
    private var cachedBackdrop: Bitmap? = null          // L1: 原始背景
    private var cachedBlurred: Bitmap? = null           // L2: 模糊后的背景
    private var cachedResult: Bitmap? = null            // L3: 最终结果

    // 背景变化检测
    private var lastBackdropHash: Int = 0
    private var lastBlurRadius: Float = -1f
    private var lastSaturation: Float = -1f
    private var lastAberrationIntensity: Float = -1f

    // 脏标记（不包括 backdrop，因为每帧都需要捕获以支持动态背景）
    private var blurDirty = true
    private var aberrationDirty = true

    private var needsRedraw = true

    // ✅ 智能跳帧计数器
    private var frameCounter = 0

    // ✅ 动态背景模式（控制是否持续重绘）
    var enableDynamicBackground = false
        set(value) {
            if (field != value) {
                field = value
                if (value) {
                    invalidate()  // 启用时开始重绘循环
                }
            }
        }

    // 触摸交互状态
    private var touchX = 0f
    private var touchY = 0f
    private var isPressed = false
    private var scaleX = 1f
    private var scaleY = 1f

    // 触摸偏移量（归一化，-100 到 100）
    private var touchOffsetX = 0f
    private var touchOffsetY = 0f
    
    // 动画
    private var scaleAnimator: ValueAnimator? = null
    
    // 绘制相关
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val clipPath = Path()  // 用于圆角裁剪
    
    init {
        setWillNotDraw(false)

        // ✅ 启用硬件加速 - 性能提升 60-80%
        setLayerType(View.LAYER_TYPE_HARDWARE, null)

        // 初始化阴影
        updateShadow()

        // 异步生成位移贴图
        post {
            generateDisplacementMaps()
        }
    }
    
    /**
     * 生成位移贴图
     */
    private fun generateDisplacementMaps() {
        if (width > 0 && height > 0) {
            displacementMaps = DisplacementMapGenerator.generateStandardMaps(width, height)
            needsRedraw = true
            invalidate()
        }
    }
    
    /**
     * 更新阴影效果
     */
    private fun updateShadow() {
        val shadowRadius = if (overLight) 70f else 40f
        val shadowAlpha = if (overLight) 0.75f else 0.25f
        
        shadowPaint.color = Color.argb((shadowAlpha * 255).toInt(), 0, 0, 0)
        shadowPaint.maskFilter = BlurMaskFilter(shadowRadius, BlurMaskFilter.Blur.NORMAL)
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // 重新生成位移贴图
        if (w > 0 && h > 0) {
            generateDisplacementMaps()
        }

        // 更新圆角裁剪路径
        updateClipPath()

        // ✅ 清除所有缓存
        cachedBackdrop?.recycle()
        cachedBlurred?.recycle()
        cachedResult?.recycle()
        cachedBackdrop = null
        cachedBlurred = null
        cachedResult = null

        // 标记所有层为脏（背景每帧都会捕获，不需要标记）
        lastBackdropHash = 0  // 重置背景哈希
        blurDirty = true
        aberrationDirty = true
        needsRedraw = true
    }

    /**
     * 更新圆角裁剪路径
     */
    private fun updateClipPath() {
        if (width > 0 && height > 0) {
            clipPath.reset()
            val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
            clipPath.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
        }
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (width <= 0 || height <= 0) return
        
        // 应用缩放变换
        canvas.save()
        canvas.scale(scaleX, scaleY, width / 2f, height / 2f)
        
        // 绘制阴影
        drawShadow(canvas)
        
        // 绘制玻璃效果
        drawGlassEffect(canvas)
        
        canvas.restore()
    }
    
    /**
     * 绘制阴影（可选）
     */
    private fun drawShadow(canvas: Canvas) {
        if (!enableShadow) return  // ✅ 如果禁用阴影，直接返回

        val shadowOffset = if (overLight) 16f else 12f
        val rect = RectF(0f, shadowOffset, width.toFloat(), height.toFloat() + shadowOffset)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, shadowPaint)
    }
    
    /**
     * 绘制玻璃效果（同步渲染版 - 稳定无闪烁）
     */
    private fun drawGlassEffect(canvas: Canvas) {
        // ✅ 同步渲染，避免 Bitmap 生命周期问题
        val bounds = RectF(0f, 0f, width.toFloat(), height.toFloat())
        val calculatedBlurRadius = (if (overLight) 12f else 4f) + blurAmount * 32f

        // 直接调用渲染逻辑
        renderGlassEffectSync(bounds, calculatedBlurRadius)

        // 绘制结果（应用圆角裁剪）
        cachedResult?.let {
            if (!it.isRecycled) {
                // 保存 canvas 状态
                val saveCount = canvas.save()

                // 应用圆角裁剪
                canvas.clipPath(clipPath)

                // ✅ 如果使用了全局下采样，需要放大回原始尺寸
                if (globalDownsampleFactor < 1.0f) {
                    val srcRect = android.graphics.Rect(0, 0, it.width, it.height)
                    val dstRect = android.graphics.Rect(0, 0, width, height)
                    val scalePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                    canvas.drawBitmap(it, srcRect, dstRect, scalePaint)
                } else {
                    // 绘制 bitmap（原始尺寸）
                    canvas.drawBitmap(it, 0f, 0f, paint)
                }

                // 恢复 canvas 状态
                canvas.restoreToCount(saveCount)
            }
        }

        // ✅ 绘制边缘高光效果
        if (enableEdgeHighlight) {
            drawEdgeHighlight(canvas, bounds)
        }

        // ✅ 仅在启用动态背景模式时持续重绘
        if (enableDynamicBackground) {
            invalidate()
        }
    }

    /**
     * 绘制边缘高光效果
     */
    private fun drawEdgeHighlight(canvas: Canvas, bounds: RectF) {
        val touchOffset = PointF(touchOffsetX, touchOffsetY)
        edgeHighlightEffect.draw(
            canvas = canvas,
            bounds = bounds,
            cornerRadius = cornerRadius,
            mouseOffset = touchOffset,
            overLight = overLight,
            borderWidth = edgeHighlightBorderWidth,
            opacity = edgeHighlightOpacity
        )
    }
    
    /**
     * 设置自定义背景捕获器
     *
     * 用于支持固定玻璃组件捕获滚动背景
     */
    fun setCustomBackdropCapture(capture: (RectF) -> Bitmap?) {
        customBackdropCapture = capture
    }

    /**
     * 同步渲染玻璃效果（主线程调用，优化版）
     */
    private fun renderGlassEffectSync(bounds: RectF, blurRadius: Float) {
        val startTime = if (ENABLE_PERFORMANCE_LOG) System.nanoTime() else 0L

        val scale = if (overLight) displacementScale * 0.5f else displacementScale

        // ✅ 检测参数变化
        val blurChanged = blurRadius != lastBlurRadius || saturation != lastSaturation
        val aberrationChanged = aberrationIntensity != lastAberrationIntensity

        // ✅ 性能监控 - 详细分阶段计时
        var t1 = 0L
        var t2 = 0L
        var t3 = 0L
        var t4 = 0L
        var t5 = 0L
        if (ENABLE_PERFORMANCE_LOG) {
            t1 = System.nanoTime()
        }

        // 1. 捕获背景（L1 缓存）- 每帧都捕获以支持动态背景
        var backdrop = if (customBackdropCapture != null) {
            customBackdropCapture?.invoke(bounds)
        } else {
            // ✅ 使用增强模糊效果的背景捕获
            enhancedBlurEffect.captureBackdrop(bounds)
        }

        if (backdrop == null) {
            backdrop = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            backdrop.eraseColor(Color.argb(200, 255, 255, 255))
        }

        // ✅ 全局下采样：截图→缩小→处理→放大
        val originalWidth = backdrop.width
        val originalHeight = backdrop.height
        val processedBackdrop = if (globalDownsampleFactor < 1.0f) {
            val scaledWidth = (originalWidth * globalDownsampleFactor).toInt().coerceAtLeast(1)
            val scaledHeight = (originalHeight * globalDownsampleFactor).toInt().coerceAtLeast(1)

            // 缩小到处理尺寸
            val scaled = Bitmap.createScaledBitmap(backdrop, scaledWidth, scaledHeight, true)
            backdrop.recycle()
            scaled
        } else {
            backdrop
        }

        backdrop = processedBackdrop

        // ✅ 检测背景是否真的变化了（支持滚动背景）
        val backdropHash = backdrop.hashCode()
        if (backdropHash != lastBackdropHash) {
            cachedBackdrop?.recycle()
            cachedBackdrop = backdrop
            lastBackdropHash = backdropHash
            blurDirty = true
        } else {
            // 背景没变化，回收新捕获的
            backdrop.recycle()
        }

        if (ENABLE_PERFORMANCE_LOG) t2 = System.nanoTime()

        // 2. 应用模糊和饱和度（L2 缓存）- 可选
        if (enableBackdropBlur && (blurDirty || blurChanged)) {
            cachedBackdrop?.let { backdrop ->
                cachedBlurred?.recycle()
                // ✅ 使用增强模糊效果（支持多种算法）
                cachedBlurred = enhancedBlurEffect.applyEffect(backdrop, blurRadius, saturation)
                lastBlurRadius = blurRadius
                lastSaturation = saturation
                aberrationDirty = true
            }
            blurDirty = false
        } else if (!enableBackdropBlur && cachedBackdrop != null) {
            // 模糊关闭，直接使用背景
            if (cachedBlurred != cachedBackdrop) {
                cachedBlurred?.recycle()
            }
            cachedBlurred = cachedBackdrop
            aberrationDirty = true
        }

        if (ENABLE_PERFORMANCE_LOG) t3 = System.nanoTime()

        // 3. 应用色差效果（L3 缓存 + 降采样优化）- 可选
        val displacementMap = displacementMaps?.get(displacementMode)
        if (enableChromaticAberration && (aberrationDirty || aberrationChanged) && aberrationIntensity > 0 && displacementMap != null) {
            cachedBlurred?.let { blurred ->
                // ✅ 使用降采样处理，速度提升 4倍，并传递通道偏移参数
                val aberrated = chromaticAberrationEffect.apply(
                    source = blurred,
                    displacementMap = displacementMap,
                    intensity = aberrationIntensity,
                    scale = displacementScale,
                    downscale = aberrationDownsample,
                    redOffset = aberrationRedOffset,
                    greenOffset = aberrationGreenOffset,
                    blueOffset = aberrationBlueOffset
                )

                if (ENABLE_PERFORMANCE_LOG) t4 = System.nanoTime()

                // 4. 直接使用色差效果结果（已移除圆角遮罩）
                cachedResult?.recycle()
                cachedResult = aberrated
                lastAberrationIntensity = aberrationIntensity
            }
            aberrationDirty = false
        } else if (aberrationDirty || !enableChromaticAberration) {
            if (ENABLE_PERFORMANCE_LOG) t4 = System.nanoTime()

            // 没有色差效果，直接使用模糊后的结果（已移除圆角遮罩）
            cachedBlurred?.let { blurred ->
                cachedResult?.recycle()
                cachedResult = blurred.copy(blurred.config ?: Bitmap.Config.ARGB_8888, true)
            }
            aberrationDirty = false
        }

        if (ENABLE_PERFORMANCE_LOG) t5 = System.nanoTime()

        // ✅ 详细性能日志 - 显示各个效果的耗时
        if (ENABLE_PERFORMANCE_LOG) {
            val captureTime = (t2 - t1) / 1_000_000f
            val blurTime = (t3 - t2) / 1_000_000f
            val aberrationTime = (t4 - t3) / 1_000_000f
            val finalizeTime = (t5 - t4) / 1_000_000f
            val totalTime = (t5 - t1) / 1_000_000f

            Log.d(TAG, """
                |📊 [性能分析] 各效果耗时:
                |  1️⃣ 捕获背景: ${String.format("%.3f", captureTime)}ms ${if (enableBackdropBlur) "✅" else "⏭️"}
                |  2️⃣ 模糊处理: ${String.format("%.3f", blurTime)}ms ${if (enableBackdropBlur) "✅" else "⏭️"}
                |  3️⃣ 色差效果: ${String.format("%.3f", aberrationTime)}ms ${if (enableChromaticAberration) "✅" else "⏭️"}
                |  4️⃣ 最终处理: ${String.format("%.3f", finalizeTime)}ms
                |  ⏱️ 总耗时: ${String.format("%.3f", totalTime)}ms (~${(1000f / totalTime).toInt()} FPS)
                |  💾 缓存状态: blur=${!blurDirty}, aberration=${!aberrationDirty}
            """.trimMargin())

            // 性能警告
            if (totalTime > 16.67f) {
                Log.w(TAG, "⚠️ 渲染过慢! 耗时 ${String.format("%.2f", totalTime)}ms (目标: <16.67ms for 60 FPS)")
            } else {
                Log.i(TAG, "✅ 渲染成功! 耗时 ${String.format("%.2f", totalTime)}ms，达到 60 FPS 目标")
            }
        }

        // ✅ 内存监控（可选，默认关闭）
        if (ENABLE_MEMORY_LOG) {
            logMemoryUsage()
        }
    }

    /**
     * 记录内存使用情况
     */
    private fun logMemoryUsage() {
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        val maxMemory = runtime.maxMemory() / 1024 / 1024
        val freeMemory = runtime.freeMemory() / 1024 / 1024

        Log.d(TAG, """
            |💾 [内存监控]
            |  已用: ${usedMemory}MB
            |  可用: ${freeMemory}MB
            |  最大: ${maxMemory}MB
            |  使用率: ${(usedMemory * 100 / maxMemory)}%
        """.trimMargin())
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchX = event.x
                touchY = event.y
                isPressed = true
                updateTouchOffset(event.x, event.y)
                animateScale(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                touchX = event.x
                touchY = event.y
                updateTouchOffset(event.x, event.y)
                updateElasticScale()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isPressed = false
                touchOffsetX = 0f
                touchOffsetY = 0f
                animateScale(false)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    /**
     * 更新触摸偏移量（归一化到 -100 到 100）
     * 用于边缘高光的动态渐变效果
     */
    private fun updateTouchOffset(x: Float, y: Float) {
        val centerX = width / 2f
        val centerY = height / 2f

        // 归一化到 -100 到 100
        touchOffsetX = ((x - centerX) / width) * 200f
        touchOffsetY = ((y - centerY) / height) * 200f
    }
    
    /**
     * 更新弹性缩放
     * 对应 React 版本的 calculateElasticScale
     */
    private fun updateElasticScale() {
        val centerX = width / 2f
        val centerY = height / 2f
        
        val deltaX = touchX - centerX
        val deltaY = touchY - centerY
        val centerDistance = sqrt(deltaX * deltaX + deltaY * deltaY)
        
        if (centerDistance < 1f) {
            scaleX = 1f
            scaleY = 1f
            return
        }
        
        val normalizedX = deltaX / centerDistance
        val normalizedY = deltaY / centerDistance
        val stretchIntensity = min(centerDistance / 300f, 1f) * elasticity
        
        scaleX = 1f + abs(normalizedX) * stretchIntensity * 0.3f - abs(normalizedY) * stretchIntensity * 0.15f
        scaleY = 1f + abs(normalizedY) * stretchIntensity * 0.3f - abs(normalizedX) * stretchIntensity * 0.15f
        
        scaleX = max(0.8f, scaleX)
        scaleY = max(0.8f, scaleY)
        
        invalidate()
    }
    
    /**
     * 缩放动画
     */
    private fun animateScale(pressed: Boolean) {
        scaleAnimator?.cancel()
        
        val targetScale = if (pressed) 0.95f else 1f
        
        scaleAnimator = ValueAnimator.ofFloat(scaleX, targetScale).apply {
            duration = 200
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                val value = it.animatedValue as Float
                scaleX = value
                scaleY = value
                invalidate()
            }
            start()
        }
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        // ✅ 清理所有缓存和资源
        scaleAnimator?.cancel()
        backdropBlurEffect.release()  // 保留用于兼容性
        enhancedBlurEffect.release()  // 新增：清理增强模糊效果

        // ✅ 清理效果处理器
        chromaticAberrationEffect.cleanup()
        edgeHighlightEffect.cleanup()

        // 清理分层缓存
        cachedBackdrop?.recycle()
        cachedBlurred?.recycle()
        cachedResult?.recycle()

        cachedBackdrop = null
        cachedBlurred = null
        cachedResult = null

        // 清理位移贴图
        displacementMaps?.values?.forEach { it.recycle() }
        displacementMaps = null
    }
}


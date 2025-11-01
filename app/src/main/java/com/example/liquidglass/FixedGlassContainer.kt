/**
 * 固定玻璃容器
 * 
 * 自定义 FrameLayout,用于支持固定的玻璃组件捕获滚动背景
 * 
 * 工作原理:
 * 1. 包含一个可滚动的背景层(ScrollView)
 * 2. 包含一个固定的玻璃组件层(LiquidGlassView)
 * 3. 监听背景滚动事件,强制玻璃组件重绘
 * 4. 玻璃组件捕获背景时,考虑滚动偏移量
 */
package com.example.liquidglass

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ScrollView

class FixedGlassContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    
    private var backgroundScroll: ScrollView? = null
    private var backgroundLayer: View? = null
    private var glassButton: LiquidGlassView? = null
    
    override fun onFinishInflate() {
        super.onFinishInflate()
        
        // 查找子视图
        backgroundScroll = findViewById(R.id.backgroundScroll)
        backgroundLayer = findViewById(R.id.backgroundLayer)
        glassButton = findViewById(R.id.glassButton)
        
        // 设置自定义背景捕获器
        glassButton?.let { glass ->
            glass.setCustomBackdropCapture { bounds ->
                captureScrollingBackground(bounds)
            }
        }
        
        // 监听滚动事件
        backgroundScroll?.viewTreeObserver?.addOnScrollChangedListener {
            // 滚动时强制玻璃按钮重绘
            glassButton?.invalidate()
        }
    }
    
    /**
     * 捕获滚动背景
     * 
     * 考虑 ScrollView 的滚动偏移量,捕获玻璃组件背后的实际内容
     */
    private fun captureScrollingBackground(bounds: RectF): Bitmap? {
        val scroll = backgroundScroll ?: return null
        val layer = backgroundLayer ?: return null
        val glass = glassButton ?: return null
        
        // 获取玻璃按钮在屏幕上的位置
        val glassLocation = IntArray(2)
        glass.getLocationOnScreen(glassLocation)
        val glassScreenX = glassLocation[0]
        val glassScreenY = glassLocation[1]
        
        // 获取背景层在屏幕上的位置
        val layerLocation = IntArray(2)
        layer.getLocationOnScreen(layerLocation)
        val layerScreenX = layerLocation[0]
        val layerScreenY = layerLocation[1]
        
        // 计算玻璃按钮相对于背景层的位置
        val relativeX = glassScreenX - layerScreenX
        val relativeY = glassScreenY - layerScreenY
        
        // 创建背景 Bitmap
        val width = bounds.width().toInt().coerceAtLeast(1)
        val height = bounds.height().toInt().coerceAtLeast(1)
        val backdrop = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(backdrop)
        
        // 平移画布到玻璃按钮相对于背景层的位置
        canvas.translate(-relativeX.toFloat(), -relativeY.toFloat())
        
        // 绘制背景层
        layer.draw(canvas)
        
        return backdrop
    }
}


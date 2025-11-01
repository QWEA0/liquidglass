/**
 * 背景模糊效果处理器（简化版）
 *
 * 实现类似 CSS backdrop-filter 的效果:
 * - 实时捕获视图背后的背景
 * - 应用盒式模糊（Box Blur）
 * - 调整饱和度
 *
 * 性能优化:
 * - 使用 AdvancedFastBlur 的缩小-模糊-放大策略
 * - 降采样处理减少计算量
 * - 无 RenderScript 依赖，代码简单
 *
 * 对应 React 版本的 backdropFilter 属性
 */
package com.example.liquidglass

import android.graphics.*
import android.view.View
import kotlin.math.roundToInt

/**
 * 背景模糊效果处理器
 */
class BackdropBlurEffect(
    private val view: View
) {

    // 使用优化的快速模糊工具
    private val fastBlur = AdvancedFastBlur()
    
    /**
     * 捕获视图背后的背景
     * 
     * @param bounds 视图边界
     * @return 背景 Bitmap
     */
    fun captureBackdrop(bounds: RectF): Bitmap? {
        val parent = view.parent as? View ?: return null

        // 创建背景 Bitmap
        val width = bounds.width().toInt().coerceAtLeast(1)
        val height = bounds.height().toInt().coerceAtLeast(1)
        val backdrop = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(backdrop)

        try {
            // ✅ 获取视图在父容器中的实际位置
            val location = IntArray(2)
            view.getLocationInWindow(location)
            val parentLocation = IntArray(2)
            parent.getLocationInWindow(parentLocation)

            // ✅ 计算视图相对于父容器的偏移
            val offsetX = (location[0] - parentLocation[0]).toFloat()
            val offsetY = (location[1] - parentLocation[1]).toFloat()

            // ✅ 平移画布到视图在父容器中的实际位置
            canvas.translate(-offsetX, -offsetY)

            // ✅ 临时隐藏当前视图，避免绘制自己
            val wasVisible = view.visibility
            view.visibility = android.view.View.INVISIBLE

            // 绘制父视图(不包括当前视图)
            parent.draw(canvas)

            // ✅ 恢复视图可见性
            view.visibility = wasVisible
        } catch (e: Exception) {
            // 如果捕获失败，返回半透明白色背景
            canvas.drawColor(android.graphics.Color.argb(200, 255, 255, 255))
        }

        return backdrop
    }
    
    /**
     * 应用模糊和饱和度效果
     * 
     * @param backdrop 原始背景
     * @param blurRadius 模糊半径 (0-25)
     * @param saturation 饱和度 (100 = 原始, 140 = 增强 40%)
     * @return 处理后的背景
     */
    fun applyEffect(
        backdrop: Bitmap,
        blurRadius: Float,
        saturation: Float
    ): Bitmap {
        var result = backdrop
        
        // 1. 应用模糊
        if (blurRadius > 0f) {
            result = applyBlur(result, blurRadius)
        }
        
        // 2. 应用饱和度
        if (saturation != 100f) {
            result = applySaturation(result, saturation / 100f)
        }
        
        return result
    }
    
    /**
     * 应用盒式模糊（使用 AdvancedFastBlur 优化）
     *
     * @param bitmap 原始图像
     * @param radius 模糊半径 (0-25)
     * @return 模糊后的图像
     */
    private fun applyBlur(bitmap: Bitmap, radius: Float): Bitmap {
        val clampedRadius = radius.coerceIn(0f, 25f)

        // ✅ 使用降采样策略以提升性能
        // downscale = 0.5 表示缩小到 50% 尺寸，速度提升 4 倍
        // 对于模糊效果，质量损失不明显
        return fastBlur.blur(
            bitmap = bitmap,
            radius = clampedRadius,
            downscale = 0.5f  // 保持 0.5，提升性能
        )
    }
    
    /**
     * 应用饱和度调整
     * 
     * @param bitmap 原始图像
     * @param saturation 饱和度系数 (1.0 = 原始, 1.4 = 增强 40%)
     * @return 调整后的图像
     */
    private fun applySaturation(bitmap: Bitmap, saturation: Float): Bitmap {
        val result = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val paint = Paint()
        
        // 使用 ColorMatrix 调整饱和度
        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(saturation)
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        return result
    }
    
    /**
     * 释放资源
     */
    fun release() {
        fastBlur.cleanup()
    }
}

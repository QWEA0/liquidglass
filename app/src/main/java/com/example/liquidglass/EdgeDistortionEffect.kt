/**
 * 边缘扭曲效果处理器（异步优化版）
 *
 * 基于位移贴图实现边缘像素扭曲,模拟玻璃折射效果
 * 对应 React 版本中的 feDisplacementMap SVG filter
 *
 * 实现原理:
 * 1. 读取位移贴图的 R/G 通道作为 X/Y 方向的位移量
 * 2. 使用降采样 + 最近邻采样优化
 * 3. 配合异步渲染实现流畅体验
 */
package com.example.liquidglass

import android.graphics.*
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * 边缘扭曲效果处理器
 */
class EdgeDistortionEffect {
    
    /**
     * 应用边缘扭曲效果（异步优化版）
     *
     * @param source 原始图像
     * @param displacementMap 位移贴图
     * @param scale 位移缩放系数 (对应 React 的 displacementScale)
     * @param downscale 降采样比例 (0.5 = 处理速度提升4倍, 0.25 = 提升16倍)
     * @return 扭曲后的图像
     */
    fun apply(
        source: Bitmap,
        displacementMap: Bitmap,
        scale: Float = 70f,
        downscale: Float = 0.5f  // ✅ 默认 0.5x 降采样
    ): Bitmap {
        val originalWidth = source.width
        val originalHeight = source.height

        // ✅ 降采样：在更小的分辨率上处理
        val processWidth = (originalWidth * downscale).toInt().coerceAtLeast(1)
        val processHeight = (originalHeight * downscale).toInt().coerceAtLeast(1)

        val smallSource = if (downscale < 1.0f) {
            Bitmap.createScaledBitmap(source, processWidth, processHeight, true)
        } else {
            source
        }

        val result = Bitmap.createBitmap(processWidth, processHeight, Bitmap.Config.ARGB_8888)

        // 缩放位移贴图到处理尺寸
        val scaledMap = if (displacementMap.width != processWidth || displacementMap.height != processHeight) {
            Bitmap.createScaledBitmap(displacementMap, processWidth, processHeight, true)
        } else {
            displacementMap
        }

        // ✅ 使用像素数组替代 getPixel/setPixel
        val sourcePixels = IntArray(processWidth * processHeight)
        val mapPixels = IntArray(processWidth * processHeight)
        val resultPixels = IntArray(processWidth * processHeight)

        smallSource.getPixels(sourcePixels, 0, processWidth, 0, 0, processWidth, processHeight)
        scaledMap.getPixels(mapPixels, 0, processWidth, 0, 0, processWidth, processHeight)

        // 位移缩放因子（需要根据降采样调整）
        val scaleFactor = (scale * downscale) / 255f

        // ✅ 使用最近邻采样替代双线性插值（性能提升 2-3倍）
        for (y in 0 until processHeight) {
            for (x in 0 until processWidth) {
                val index = y * processWidth + x
                val mapColor = mapPixels[index]

                // R 通道 = X 方向位移, G 通道 = Y 方向位移
                val dx = (Color.red(mapColor) - 128) * scaleFactor
                val dy = (Color.green(mapColor) - 128) * scaleFactor

                // 计算采样位置
                val srcX = x + dx
                val srcY = y + dy

                // ✅ 最近邻采样（比双线性插值快 2-3倍）
                val sampledColor = sampleNearestFromArray(sourcePixels, processWidth, processHeight, srcX, srcY)
                resultPixels[index] = sampledColor
            }
        }

        result.setPixels(resultPixels, 0, processWidth, 0, 0, processWidth, processHeight)

        // 清理临时 Bitmap
        if (scaledMap != displacementMap) {
            scaledMap.recycle()
        }
        if (smallSource != source) {
            smallSource.recycle()
        }

        // ✅ 放大回原始尺寸
        val finalResult = if (downscale < 1.0f) {
            val upscaled = Bitmap.createScaledBitmap(result, originalWidth, originalHeight, true)
            result.recycle()
            upscaled
        } else {
            result
        }

        return finalResult
    }
    
    /**
     * 从像素数组进行最近邻采样（极致优化版 - 比双线性插值快 2-3倍）
     *
     * @param pixels 像素数组
     * @param width 图像宽度
     * @param height 图像高度
     * @param x X 坐标(可以是小数)
     * @param y Y 坐标(可以是小数)
     * @return 采样后的颜色
     */
    private fun sampleNearestFromArray(pixels: IntArray, width: Int, height: Int, x: Float, y: Float): Int {
        val clampedX = x.roundToInt().coerceIn(0, width - 1)
        val clampedY = y.roundToInt().coerceIn(0, height - 1)
        return pixels[clampedY * width + clampedX]
    }

    /**
     * 从像素数组进行双线性插值采样（旧版 - 已弃用，使用最近邻采样更快）
     *
     * @param pixels 像素数组
     * @param width 图像宽度
     * @param height 图像高度
     * @param x X 坐标(可以是小数)
     * @param y Y 坐标(可以是小数)
     * @return 插值后的颜色
     */
    @Deprecated("Use sampleNearestFromArray for better performance")
    private fun sampleBilinearFromArray(pixels: IntArray, width: Int, height: Int, x: Float, y: Float): Int {
        // 边界检查
        if (x < 0 || x >= width - 1 || y < 0 || y >= height - 1) {
            val clampedX = x.roundToInt().coerceIn(0, width - 1)
            val clampedY = y.roundToInt().coerceIn(0, height - 1)
            return pixels[clampedY * width + clampedX]
        }

        // 获取四个相邻像素
        val x0 = floor(x).toInt()
        val y0 = floor(y).toInt()
        val x1 = x0 + 1
        val y1 = y0 + 1

        val c00 = pixels[y0 * width + x0]
        val c10 = pixels[y0 * width + x1]
        val c01 = pixels[y1 * width + x0]
        val c11 = pixels[y1 * width + x1]

        // 计算插值权重
        val fx = x - x0
        val fy = y - y0

        // 对每个通道进行双线性插值
        val a = lerpChannel(Color.alpha(c00), Color.alpha(c10), Color.alpha(c01), Color.alpha(c11), fx, fy)
        val r = lerpChannel(Color.red(c00), Color.red(c10), Color.red(c01), Color.red(c11), fx, fy)
        val g = lerpChannel(Color.green(c00), Color.green(c10), Color.green(c01), Color.green(c11), fx, fy)
        val b = lerpChannel(Color.blue(c00), Color.blue(c10), Color.blue(c01), Color.blue(c11), fx, fy)

        return Color.argb(a, r, g, b)
    }

    /**
     * 双线性插值采样（旧版 - 保留用于兼容）
     *
     * @param bitmap 源图像
     * @param x X 坐标(可以是小数)
     * @param y Y 坐标(可以是小数)
     * @return 插值后的颜色
     */
    @Deprecated("Use sampleBilinearFromArray for better performance")
    private fun sampleBilinear(bitmap: Bitmap, x: Float, y: Float): Int {
        val width = bitmap.width
        val height = bitmap.height

        // 边界检查
        if (x < 0 || x >= width - 1 || y < 0 || y >= height - 1) {
            val clampedX = x.roundToInt().coerceIn(0, width - 1)
            val clampedY = y.roundToInt().coerceIn(0, height - 1)
            return bitmap.getPixel(clampedX, clampedY)
        }

        // 获取四个相邻像素
        val x0 = floor(x).toInt()
        val y0 = floor(y).toInt()
        val x1 = x0 + 1
        val y1 = y0 + 1

        val c00 = bitmap.getPixel(x0, y0)
        val c10 = bitmap.getPixel(x1, y0)
        val c01 = bitmap.getPixel(x0, y1)
        val c11 = bitmap.getPixel(x1, y1)

        // 计算插值权重
        val fx = x - x0
        val fy = y - y0

        // 对每个通道进行双线性插值
        val a = lerpChannel(Color.alpha(c00), Color.alpha(c10), Color.alpha(c01), Color.alpha(c11), fx, fy)
        val r = lerpChannel(Color.red(c00), Color.red(c10), Color.red(c01), Color.red(c11), fx, fy)
        val g = lerpChannel(Color.green(c00), Color.green(c10), Color.green(c01), Color.green(c11), fx, fy)
        val b = lerpChannel(Color.blue(c00), Color.blue(c10), Color.blue(c01), Color.blue(c11), fx, fy)

        return Color.argb(a, r, g, b)
    }
    
    /**
     * 对单个颜色通道进行双线性插值
     * 
     * @param c00 左上角值
     * @param c10 右上角值
     * @param c01 左下角值
     * @param c11 右下角值
     * @param fx X 方向权重
     * @param fy Y 方向权重
     * @return 插值后的值
     */
    private fun lerpChannel(
        c00: Int, c10: Int, c01: Int, c11: Int,
        fx: Float, fy: Float
    ): Int {
        // 先在 X 方向插值
        val c0 = c00 * (1 - fx) + c10 * fx
        val c1 = c01 * (1 - fx) + c11 * fx
        
        // 再在 Y 方向插值
        val result = c0 * (1 - fy) + c1 * fy
        
        return result.roundToInt().coerceIn(0, 255)
    }
    
    /**
     * 应用反向位移(对应 React 版本的 scale * -1)
     * 
     * 某些模式下需要反向位移
     */
    fun applyInverted(
        source: Bitmap,
        displacementMap: Bitmap,
        scale: Float = 70f
    ): Bitmap {
        return apply(source, displacementMap, -scale)
    }
    
    /**
     * 应用带边缘淡化的扭曲效果
     * 
     * 在边缘区域逐渐减弱扭曲强度,避免边缘突变
     */
    fun applyWithEdgeFade(
        source: Bitmap,
        displacementMap: Bitmap,
        scale: Float = 70f,
        fadeWidth: Int = 20
    ): Bitmap {
        val width = source.width
        val height = source.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        // 缩放位移贴图
        val scaledMap = if (displacementMap.width != width || displacementMap.height != height) {
            Bitmap.createScaledBitmap(displacementMap, width, height, true)
        } else {
            displacementMap
        }
        
        val scaleFactor = scale / 255f
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                // 计算到边缘的距离
                val distToEdge = minOf(x, y, width - x - 1, height - y - 1)
                
                // 计算淡化系数
                val fadeFactor = if (distToEdge < fadeWidth) {
                    distToEdge.toFloat() / fadeWidth
                } else {
                    1f
                }
                
                // 读取位移贴图
                val mapColor = scaledMap.getPixel(x, y)
                val dx = (Color.red(mapColor) - 128) * scaleFactor * fadeFactor
                val dy = (Color.green(mapColor) - 128) * scaleFactor * fadeFactor
                
                // 采样
                val srcX = x + dx
                val srcY = y + dy
                val sampledColor = sampleBilinear(source, srcX, srcY)
                
                result.setPixel(x, y, sampledColor)
            }
        }
        
        if (scaledMap != displacementMap) {
            scaledMap.recycle()
        }

        return result
    }

    /**
     * 清理资源（占位方法，CPU 版本无需清理）
     */
    fun cleanup() {
        // CPU 版本无需清理特殊资源
    }
}


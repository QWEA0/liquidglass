/**
 * NativeGaussTest - 单元测试与视觉回归测试
 * 
 * 测试覆盖：
 * 1. 参数验证（格式、可编辑性）
 * 2. 边界条件（σ=0, radius=0）
 * 3. 能量守恒（模糊前后亮度不应系统性偏移）
 * 4. 视觉质量（直方图分布、PSNR）
 * 5. 错误处理（非法输入）
 */
package com.example.blur

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

@RunWith(AndroidJUnit4::class)
class NativeGaussTest {
    
    /**
     * 测试：基本功能
     */
    @Test
    fun testBasicBlur() {
        val bitmap = createSolidBitmap(128, 128, 0xFF808080.toInt())
        
        // 纯色图像模糊后应保持不变
        NativeGauss.gaussianIIRInplace(bitmap, 6f, false)
        
        val pixel = bitmap.getPixel(64, 64)
        assertEquals(0xFF808080.toInt(), pixel)
        
        bitmap.recycle()
    }
    
    /**
     * 测试：边界条件（σ = 0）
     */
    @Test
    fun testZeroSigma() {
        val bitmap = createTestPattern(64, 64)
        val original = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        
        // σ = 0 应该不做任何处理
        NativeGauss.gaussianIIRInplace(bitmap, 0f, false)
        
        assertTrue(bitmapsEqual(original, bitmap))
        
        bitmap.recycle()
        original.recycle()
    }
    
    /**
     * 测试：边界条件（radius = 0）
     */
    @Test
    fun testZeroRadius() {
        val bitmap = createTestPattern(64, 64)
        val original = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        
        // radius = 0 应该不做任何处理
        NativeGauss.box3Inplace(bitmap, 0)
        
        assertTrue(bitmapsEqual(original, bitmap))
        
        bitmap.recycle()
        original.recycle()
    }
    
    /**
     * 测试：能量守恒（亮度不应系统性偏移）
     */
    @Test
    fun testEnergyConservation() {
        val bitmap = createTestPattern(128, 128)
        
        val brightnessBefore = calculateAverageBrightness(bitmap)
        
        NativeGauss.gaussianIIRInplace(bitmap, 12f, false)
        
        val brightnessAfter = calculateAverageBrightness(bitmap)
        
        // 允许 ±2% 的误差（由于边界效应和量化误差）
        val diff = abs(brightnessAfter - brightnessBefore) / brightnessBefore
        assertTrue("Brightness shift: ${diff * 100}%", diff < 0.02)
        
        bitmap.recycle()
    }
    
    /**
     * 测试：线性模式 vs 快速模式
     */
    @Test
    fun testLinearVsFast() {
        val bitmap1 = createTestPattern(128, 128)
        val bitmap2 = bitmap1.copy(Bitmap.Config.ARGB_8888, true)
        
        NativeGauss.gaussianIIRInplace(bitmap1, 12f, false)  // Fast
        NativeGauss.gaussianIIRInplace(bitmap2, 12f, true)   // Linear
        
        // 两种模式结果应该不同（但都合理）
        assertFalse(bitmapsEqual(bitmap1, bitmap2))
        
        // 但亮度应该接近
        val brightness1 = calculateAverageBrightness(bitmap1)
        val brightness2 = calculateAverageBrightness(bitmap2)
        val diff = abs(brightness2 - brightness1) / brightness1
        assertTrue("Brightness diff: ${diff * 100}%", diff < 0.1)
        
        bitmap1.recycle()
        bitmap2.recycle()
    }
    
    /**
     * 测试：IIR vs Box3 近似质量
     */
    @Test
    fun testIIRvsBox3() {
        val bitmap1 = createTestPattern(128, 128)
        val bitmap2 = bitmap1.copy(Bitmap.Config.ARGB_8888, true)
        
        val sigma = 6f
        val radius = (sigma * 1.2f).toInt()
        
        NativeGauss.gaussianIIRInplace(bitmap1, sigma, false)
        NativeGauss.box3Inplace(bitmap2, radius)
        
        // 计算 PSNR（应该 > 30 dB）
        val psnr = calculatePSNR(bitmap1, bitmap2)
        assertTrue("PSNR: $psnr dB", psnr > 30.0)
        
        bitmap1.recycle()
        bitmap2.recycle()
    }
    
    /**
     * 测试：错误处理（不可编辑的 Bitmap）
     */
    @Test(expected = IllegalStateException::class)
    fun testImmutableBitmap() {
        val bitmap = createSolidBitmap(64, 64, 0xFF000000.toInt())
        val immutable = bitmap.copy(Bitmap.Config.ARGB_8888, false)  // 不可编辑
        bitmap.recycle()
        
        // 应该抛出异常
        NativeGauss.gaussianIIRInplace(immutable, 6f, false)
    }
    
    /**
     * 测试：错误处理（错误的格式）
     */
    @Test(expected = IllegalArgumentException::class)
    fun testWrongFormat() {
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.RGB_565)
        
        // 应该抛出异常
        NativeGauss.gaussianIIRInplace(bitmap, 6f, false)
    }
    
    /**
     * 测试：大 σ 值
     */
    @Test
    fun testLargeSigma() {
        val bitmap = createTestPattern(128, 128)
        
        // σ = 30 应该产生非常强的模糊
        NativeGauss.gaussianIIRInplace(bitmap, 30f, false)
        
        // 检查结果是否接近均匀（方差应该很小）
        val variance = calculateVariance(bitmap)
        assertTrue("Variance: $variance", variance < 500.0)
        
        bitmap.recycle()
    }
    
    /**
     * 测试：智能模糊
     */
    @Test
    fun testSmartBlur() {
        val smallBitmap = createTestPattern(48, 48)
        val largeBitmap = createTestPattern(256, 256)
        
        // 小图应该使用 Box3
        NativeGauss.smartBlur(smallBitmap, 6f, false)
        
        // 大图应该使用 IIR
        NativeGauss.smartBlur(largeBitmap, 6f, false)
        
        // 两者都应该成功（不抛异常）
        assertNotNull(smallBitmap)
        assertNotNull(largeBitmap)
        
        smallBitmap.recycle()
        largeBitmap.recycle()
    }
    
    // ========== 辅助函数 ==========
    
    private fun createSolidBitmap(w: Int, h: Int, color: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(color)
        return bitmap
    }
    
    private fun createTestPattern(w: Int, h: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        
        for (y in 0 until h) {
            for (x in 0 until w) {
                // 棋盘格 + 渐变
                val checker = if ((x / 8 + y / 8) % 2 == 0) 64 else 192
                val gradient = (x * 255 / w)
                val value = (checker + gradient) / 2
                pixels[y * w + x] = 0xFF000000.toInt() or (value shl 16) or (value shl 8) or value
            }
        }
        
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
        return bitmap
    }
    
    private fun bitmapsEqual(b1: Bitmap, b2: Bitmap): Boolean {
        if (b1.width != b2.width || b1.height != b2.height) return false
        
        for (y in 0 until b1.height) {
            for (x in 0 until b1.width) {
                if (b1.getPixel(x, y) != b2.getPixel(x, y)) return false
            }
        }
        
        return true
    }
    
    private fun calculateAverageBrightness(bitmap: Bitmap): Double {
        var sum = 0.0
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        
        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            sum += (r + g + b) / 3.0
        }
        
        return sum / pixels.size
    }
    
    private fun calculateVariance(bitmap: Bitmap): Double {
        val mean = calculateAverageBrightness(bitmap)
        var sumSq = 0.0
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        
        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            val brightness = (r + g + b) / 3.0
            sumSq += (brightness - mean) * (brightness - mean)
        }
        
        return sumSq / pixels.size
    }
    
    private fun calculatePSNR(b1: Bitmap, b2: Bitmap): Double {
        require(b1.width == b2.width && b1.height == b2.height)
        
        var mse = 0.0
        val pixels1 = IntArray(b1.width * b1.height)
        val pixels2 = IntArray(b2.width * b2.height)
        
        b1.getPixels(pixels1, 0, b1.width, 0, 0, b1.width, b1.height)
        b2.getPixels(pixels2, 0, b2.width, 0, 0, b2.width, b2.height)
        
        for (i in pixels1.indices) {
            val r1 = (pixels1[i] shr 16) and 0xFF
            val g1 = (pixels1[i] shr 8) and 0xFF
            val b1Val = pixels1[i] and 0xFF
            
            val r2 = (pixels2[i] shr 16) and 0xFF
            val g2 = (pixels2[i] shr 8) and 0xFF
            val b2Val = pixels2[i] and 0xFF
            
            mse += (r1 - r2) * (r1 - r2)
            mse += (g1 - g2) * (g1 - g2)
            mse += (b1Val - b2Val) * (b1Val - b2Val)
        }
        
        mse /= (pixels1.size * 3.0)
        
        if (mse < 1e-10) return 100.0  // 完全相同
        
        return 10 * log10(255.0 * 255.0 / mse)
    }
}


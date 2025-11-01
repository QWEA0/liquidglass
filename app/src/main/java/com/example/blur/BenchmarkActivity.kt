/**
 * BenchmarkActivity - 性能基准测试
 * 
 * 测试场景：
 * 1. 不同尺寸：64×64, 128×128, 192×192
 * 2. 不同强度：σ = 6, 12, 18
 * 3. 不同算法：IIR(Linear), IIR(Fast), Box3
 * 
 * 性能指标：
 * - 平均耗时（ms）
 * - 每像素耗时（ns/px）
 * - 吞吐量（Mpx/s）
 * 
 * 验收标准（arm64-v8a, Pixel 7 级别）：
 * - 128×128 @ σ=12, IIR(Fast): < 0.35 ms
 * - 128×128 @ σ=12, Box3: < 0.25 ms
 */
package com.example.blur

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import kotlin.random.Random

class BenchmarkActivity : AppCompatActivity() {
    
    private lateinit var resultText: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 简单布局
        resultText = TextView(this).apply {
            textSize = 12f
            setPadding(32, 32, 32, 32)
        }
        setContentView(resultText)
        
        // 延迟执行基准测试（避免启动时的性能抖动）
        resultText.postDelayed({
            runBenchmarks()
        }, 500)
    }
    
    private fun runBenchmarks() {
        val results = StringBuilder()
        results.append("=== IIR 高斯模糊性能基准 ===\n\n")

        // 检测 NEON 支持
        val hasNeon = try {
            NativeGauss.hasNeonSupport()
        } catch (e: Exception) {
            false
        }
        results.append("NEON 支持: ${if (hasNeon) "✓ 是" else "✗ 否"}\n\n")

        // 测试配置
        val sizes = listOf(64, 128, 192)
        val sigmas = listOf(6f, 12f, 18f)
        val warmupRuns = 3
        val benchmarkRuns = 10

        // 预热 JNI
        results.append("预热中...\n")
        val warmupBitmap = createTestBitmap(64)
        repeat(warmupRuns) {
            NativeGauss.gaussianIIRInplace(warmupBitmap, 6f, false)
            if (hasNeon) {
                NativeGauss.gaussianIIRNeonInplace(warmupBitmap, 6f, false)
            }
        }
        warmupBitmap.recycle()
        results.append("预热完成\n\n")
        
        // 遍历测试场景
        for (size in sizes) {
            results.append("--- 尺寸: ${size}×${size} (${size * size} px) ---\n")

            for (sigma in sigmas) {
                results.append("\nσ = $sigma:\n")

                // IIR (Fast)
                val iirFastTime = benchmarkIIR(size, sigma, false, benchmarkRuns)
                val iirFastNsPerPx = (iirFastTime * 1_000_000) / (size * size)
                results.append(String.format(Locale.US,
                    "  IIR(Fast):   %.3f ms  (%.1f ns/px, %.1f Mpx/s)\n",
                    iirFastTime, iirFastNsPerPx, 1000.0 / iirFastNsPerPx))

                // IIR (Linear)
                val iirLinearTime = benchmarkIIR(size, sigma, true, benchmarkRuns)
                val iirLinearNsPerPx = (iirLinearTime * 1_000_000) / (size * size)
                val overhead = ((iirLinearTime / iirFastTime - 1) * 100)
                results.append(String.format(Locale.US,
                    "  IIR(Linear): %.3f ms  (%.1f ns/px, %.1f Mpx/s, +%.1f%%)\n",
                    iirLinearTime, iirLinearNsPerPx, 1000.0 / iirLinearNsPerPx, overhead))

                // NEON (如果支持)
                if (hasNeon) {
                    val neonTime = benchmarkNeon(size, sigma, false, benchmarkRuns)
                    val neonNsPerPx = (neonTime * 1_000_000) / (size * size)
                    val neonSpeedup = iirFastTime / neonTime
                    results.append(String.format(Locale.US,
                        "  IIR(NEON):   %.3f ms  (%.1f ns/px, %.1f Mpx/s, %.2fx faster)\n",
                        neonTime, neonNsPerPx, 1000.0 / neonNsPerPx, neonSpeedup))
                }

                // Box3
                val radius = (sigma * 1.2f).toInt().coerceAtLeast(1)
                val box3Time = benchmarkBox3(size, radius, benchmarkRuns)
                val box3NsPerPx = (box3Time * 1_000_000) / (size * size)
                val speedup = iirFastTime / box3Time
                results.append(String.format(Locale.US,
                    "  Box3(r=%2d):  %.3f ms  (%.1f ns/px, %.1f Mpx/s, %.2fx faster)\n",
                    radius, box3Time, box3NsPerPx, 1000.0 / box3NsPerPx, speedup))
            }

            results.append("\n")
        }
        
        // 下采样管线测试
        results.append("\n=== 下采样管线性能 ===\n")
        results.append("(128×128 @ σ=12)\n\n")

        val baselineTime = benchmarkIIR(128, 12f, false, benchmarkRuns)
        results.append(String.format(Locale.US,
            "基准 (无下采样):  %.3f ms\n", baselineTime))

        val downsample2xTime = benchmarkDownsample(128, 12f, 2, benchmarkRuns)
        val speedup2x = baselineTime / downsample2xTime
        results.append(String.format(Locale.US,
            "下采样 2×:        %.3f ms  (%.2fx faster)\n",
            downsample2xTime, speedup2x))

        val downsample3xTime = benchmarkDownsample(128, 12f, 3, benchmarkRuns)
        val speedup3x = baselineTime / downsample3xTime
        results.append(String.format(Locale.US,
            "下采样 3×:        %.3f ms  (%.2fx faster)\n",
            downsample3xTime, speedup3x))

        // 验收检查
        results.append("\n=== 验收检查 ===\n")
        val checkTime = if (hasNeon) {
            benchmarkNeon(128, 12f, false, benchmarkRuns)
        } else {
            benchmarkIIR(128, 12f, false, benchmarkRuns)
        }
        val passed = checkTime < 0.35
        val method = if (hasNeon) "IIR(NEON)" else "IIR(Fast)"
        results.append(String.format(Locale.US,
            "128×128 @ σ=12, $method: %.3f ms %s\n",
            checkTime, if (passed) "✓ PASS" else "✗ FAIL (目标 < 0.35 ms)"))

        // 显示结果
        resultText.text = results.toString()
        Log.d("Benchmark", results.toString())
    }
    
    /**
     * 基准测试：IIR 高斯模糊
     */
    private fun benchmarkIIR(size: Int, sigma: Float, linear: Boolean, runs: Int): Double {
        val bitmap = createTestBitmap(size)
        val times = mutableListOf<Long>()
        
        repeat(runs) {
            // 重置位图（避免缓存影响）
            fillRandomPixels(bitmap)
            
            val start = System.nanoTime()
            NativeGauss.gaussianIIRInplace(bitmap, sigma, linear)
            val end = System.nanoTime()
            
            times.add(end - start)
        }
        
        bitmap.recycle()
        
        // 返回中位数（更稳定）
        times.sort()
        return times[runs / 2] / 1_000_000.0
    }
    
    /**
     * 基准测试：NEON 优化 IIR 模糊
     */
    private fun benchmarkNeon(size: Int, sigma: Float, linear: Boolean, runs: Int): Double {
        val bitmap = createTestBitmap(size)
        val times = mutableListOf<Long>()

        repeat(runs) {
            fillRandomPixels(bitmap)

            val start = System.nanoTime()
            NativeGauss.gaussianIIRNeonInplace(bitmap, sigma, linear)
            val end = System.nanoTime()

            times.add(end - start)
        }

        bitmap.recycle()

        times.sort()
        return times[runs / 2] / 1_000_000.0
    }

    /**
     * 基准测试：Box3 模糊
     */
    private fun benchmarkBox3(size: Int, radius: Int, runs: Int): Double {
        val bitmap = createTestBitmap(size)
        val times = mutableListOf<Long>()

        repeat(runs) {
            fillRandomPixels(bitmap)

            val start = System.nanoTime()
            NativeGauss.box3Inplace(bitmap, radius)
            val end = System.nanoTime()

            times.add(end - start)
        }

        bitmap.recycle()

        times.sort()
        return times[runs / 2] / 1_000_000.0
    }

    /**
     * 基准测试：下采样模糊管线
     */
    private fun benchmarkDownsample(size: Int, sigma: Float, scale: Int, runs: Int): Double {
        val bitmap = createTestBitmap(size)
        val times = mutableListOf<Long>()

        repeat(runs) {
            fillRandomPixels(bitmap)

            val start = System.nanoTime()
            val result = NativeGauss.downsampleBlur(bitmap, sigma, scale, false)
            val end = System.nanoTime()

            result.recycle()
            times.add(end - start)
        }

        bitmap.recycle()

        times.sort()
        return times[runs / 2] / 1_000_000.0
    }
    
    /**
     * 创建测试位图
     */
    private fun createTestBitmap(size: Int): Bitmap {
        return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    }
    
    /**
     * 填充随机像素（模拟真实图像）
     */
    private fun fillRandomPixels(bitmap: Bitmap) {
        val pixels = IntArray(bitmap.width * bitmap.height)
        for (i in pixels.indices) {
            // 生成随机 ARGB 颜色
            val a = 255
            val r = Random.nextInt(256)
            val g = Random.nextInt(256)
            val b = Random.nextInt(256)
            pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }
        bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    }
}


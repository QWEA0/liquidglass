/**
 * MainActivity - 演示 IIR 高斯模糊的使用
 * 
 * 功能：
 * - 加载示例图片
 * - 实时调整模糊强度（SeekBar）
 * - 切换质量模式（Switch）
 * - 显示性能指标
 */
package com.example.blur

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    
    private lateinit var imageView: ImageView
    private lateinit var sigmaSeekBar: SeekBar
    private lateinit var sigmaText: TextView
    private lateinit var qualitySwitch: Switch
    private lateinit var algorithmSpinner: Spinner
    private lateinit var performanceText: TextView
    
    private var originalBitmap: Bitmap? = null
    private var workingBitmap: Bitmap? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 创建简单的 UI
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        
        // 图像显示
        imageView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        layout.addView(imageView)
        
        // σ 控制
        layout.addView(TextView(this).apply {
            text = "模糊强度 (σ):"
            textSize = 16f
        })
        
        sigmaText = TextView(this).apply {
            text = "σ = 12.0"
            textSize = 14f
            setTextColor(Color.GRAY)
        }
        layout.addView(sigmaText)
        
        sigmaSeekBar = SeekBar(this).apply {
            max = 300  // 0.0 - 30.0
            progress = 120  // σ = 12.0
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val sigma = progress / 10.0f
                    sigmaText.text = String.format("σ = %.1f", sigma)
                    if (fromUser) {
                        applyBlur()
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        layout.addView(sigmaSeekBar)
        
        // 质量模式
        qualitySwitch = Switch(this).apply {
            text = "高质量模式（线性色彩空间）"
            setOnCheckedChangeListener { _, _ -> applyBlur() }
        }
        layout.addView(qualitySwitch)
        
        // 算法选择
        layout.addView(TextView(this).apply {
            text = "算法:"
            textSize = 16f
        })
        
        algorithmSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_item,
                listOf("IIR 高斯", "Box3 近似", "智能选择")
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                    applyBlur()
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
        layout.addView(algorithmSpinner)
        
        // 性能指标
        performanceText = TextView(this).apply {
            text = "性能: --"
            textSize = 14f
            setTextColor(Color.BLUE)
        }
        layout.addView(performanceText)
        
        // 重置按钮
        layout.addView(Button(this).apply {
            text = "重置图像"
            setOnClickListener {
                resetImage()
            }
        })
        
        // 基准测试按钮
        layout.addView(Button(this).apply {
            text = "运行基准测试"
            setOnClickListener {
                startActivity(android.content.Intent(this@MainActivity, BenchmarkActivity::class.java))
            }
        })
        
        setContentView(layout)
        
        // 创建示例图像
        createSampleImage()
    }
    
    /**
     * 创建示例图像（渐变 + 噪声）
     */
    private fun createSampleImage() {
        val size = 256
        originalBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        
        val canvas = Canvas(originalBitmap!!)
        val paint = Paint()
        
        // 绘制渐变背景
        for (y in 0 until size) {
            for (x in 0 until size) {
                val r = (x * 255 / size)
                val g = (y * 255 / size)
                val b = ((x + y) * 128 / size)
                paint.color = Color.rgb(r, g, b)
                canvas.drawPoint(x.toFloat(), y.toFloat(), paint)
            }
        }
        
        // 添加一些形状
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        canvas.drawCircle(size * 0.3f, size * 0.3f, size * 0.15f, paint)
        
        paint.color = Color.BLACK
        canvas.drawRect(
            size * 0.6f, size * 0.6f,
            size * 0.9f, size * 0.9f,
            paint
        )
        
        // 添加噪声
        val pixels = IntArray(size * size)
        originalBitmap!!.getPixels(pixels, 0, size, 0, 0, size, size)
        for (i in pixels.indices) {
            if (Random.nextFloat() < 0.1f) {
                val noise = Random.nextInt(50) - 25
                val r = ((pixels[i] shr 16) and 0xFF) + noise
                val g = ((pixels[i] shr 8) and 0xFF) + noise
                val b = (pixels[i] and 0xFF) + noise
                pixels[i] = Color.rgb(
                    r.coerceIn(0, 255),
                    g.coerceIn(0, 255),
                    b.coerceIn(0, 255)
                )
            }
        }
        originalBitmap!!.setPixels(pixels, 0, size, 0, 0, size, size)
        
        imageView.setImageBitmap(originalBitmap)
    }
    
    /**
     * 应用模糊
     */
    private fun applyBlur() {
        val original = originalBitmap ?: return
        
        // 复制原图
        workingBitmap?.recycle()
        workingBitmap = original.copy(Bitmap.Config.ARGB_8888, true)
        
        val sigma = sigmaSeekBar.progress / 10.0f
        val highQuality = qualitySwitch.isChecked
        val algorithm = algorithmSpinner.selectedItemPosition
        
        // 测量性能
        val startTime = System.nanoTime()
        
        when (algorithm) {
            0 -> {
                // IIR 高斯
                NativeGauss.gaussianIIRInplace(workingBitmap!!, sigma, highQuality)
            }
            1 -> {
                // Box3
                val radius = (sigma * 1.2f).toInt().coerceAtLeast(1)
                NativeGauss.box3Inplace(workingBitmap!!, radius)
            }
            2 -> {
                // 智能选择
                NativeGauss.smartBlur(workingBitmap!!, sigma, highQuality)
            }
        }
        
        val endTime = System.nanoTime()
        val elapsedMs = (endTime - startTime) / 1_000_000.0
        
        // 更新显示
        imageView.setImageBitmap(workingBitmap)
        
        val pixels = workingBitmap!!.width * workingBitmap!!.height
        val nsPerPx = (endTime - startTime).toDouble() / pixels
        performanceText.text = String.format(
            "性能: %.2f ms (%.1f ns/px, %.1f Mpx/s)",
            elapsedMs, nsPerPx, 1000.0 / nsPerPx
        )
    }
    
    /**
     * 重置图像
     */
    private fun resetImage() {
        imageView.setImageBitmap(originalBitmap)
        performanceText.text = "性能: --"
    }
    
    override fun onDestroy() {
        super.onDestroy()
        originalBitmap?.recycle()
        workingBitmap?.recycle()
    }
}


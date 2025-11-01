/**
 * 可滚动测试 Activity
 * 
 * 提供可滚动的背景来测试 LiquidGlass 效果
 * 包含效果开关，可以单独测试每个效果
 */
package com.example.liquidglass

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView

class ScrollableTestActivity : AppCompatActivity() {

    private lateinit var liquidGlassView: LiquidGlassView
    private lateinit var statusText: TextView
    private lateinit var performanceText: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 创建主布局
        val mainLayout = FrameLayout(this)
        
        // 创建可滚动的背景内容
        val scrollView = NestedScrollView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        
        // 背景内容 - 多彩的渐变块
        val scrollContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 0)
        }
        
        // 添加多个彩色块作为背景，带文本
        val colorData = listOf(
            Pair(0xFFFF6B6B.toInt(), "🌹 红色区域\nRed Zone"),
            Pair(0xFF4ECDC4.toInt(), "🌊 青色区域\nCyan Zone"),
            Pair(0xFF45B7D1.toInt(), "💙 蓝色区域\nBlue Zone"),
            Pair(0xFFFFA07A.toInt(), "🍊 橙色区域\nOrange Zone"),
            Pair(0xFF98D8C8.toInt(), "🌿 绿色区域\nGreen Zone"),
            Pair(0xFFF7DC6F.toInt(), "⭐ 黄色区域\nYellow Zone"),
            Pair(0xFFBB8FCE.toInt(), "💜 紫色区域\nPurple Zone"),
            Pair(0xFF85C1E2.toInt(), "☁️ 浅蓝区域\nLight Blue Zone")
        )

        colorData.forEach { (color, text) ->
            // 创建容器
            val container = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    400
                )
                setBackgroundColor(color)
            }

            // 添加文本
            val textView = TextView(this).apply {
                this.text = text
                textSize = 24f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setShadowLayer(4f, 2f, 2f, Color.BLACK)
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }

            container.addView(textView)
            scrollContent.addView(container)
        }
        
        scrollView.addView(scrollContent)
        mainLayout.addView(scrollView)
        
        // 创建控制面板（固定在底部）
        val controlPanel = createControlPanel()
        mainLayout.addView(controlPanel)
        
        // 创建 LiquidGlass 按钮（居中）
        liquidGlassView = LiquidGlassView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
            }
        }

        // ✅ 添加按钮内容 - 使用 TextView 替代 Button，避免长方形轮廓
        val buttonContent = TextView(this).apply {
            text = "✨ LiquidGlass\n效果演示"
            textSize = 20f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(100, 80, 100, 80)

            // ✅ 设置半透明背景，无边框
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(0x00FFFFFF.toInt()) // 完全透明，让 LiquidGlass 效果显示
                cornerRadius = 24f
            }

            // ✅ 添加文字阴影，增强可读性
            setShadowLayer(8f, 0f, 2f, Color.BLACK)

            // ✅ 设置点击效果
            isClickable = true
            isFocusable = true
        }
        liquidGlassView.addView(buttonContent)
        
        mainLayout.addView(liquidGlassView)
        
        setContentView(mainLayout)
    }
    
    private fun createControlPanel(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM
            }
            setBackgroundColor(0xEEFFFFFF.toInt())
            setPadding(24, 24, 24, 24)
            elevation = 8f
            
            // 标题
            val title = TextView(this@ScrollableTestActivity).apply {
                text = "效果控制面板"
                textSize = 18f
                setTextColor(Color.BLACK)
                setPadding(0, 0, 0, 16)
            }
            addView(title)
            
            // 状态文本
            statusText = TextView(this@ScrollableTestActivity).apply {
                text = "当前效果：全部开启"
                textSize = 14f
                setTextColor(0xFF666666.toInt())
                setPadding(0, 0, 0, 8)
            }
            addView(statusText)

            // ✅ 性能监控文本
            performanceText = TextView(this@ScrollableTestActivity).apply {
                text = "性能监控：等待数据..."
                textSize = 12f
                setTextColor(0xFF009688.toInt())
                setPadding(0, 0, 0, 16)
                typeface = android.graphics.Typeface.MONOSPACE
            }
            addView(performanceText)

            // ✅ 启动性能监控
            startPerformanceMonitoring()
            
            // 效果开关
            val blurCheckbox = CheckBox(this@ScrollableTestActivity).apply {
                text = "背景模糊"
                isChecked = true
                setOnCheckedChangeListener { _, isChecked ->
                    liquidGlassView.enableBackdropBlur = isChecked
                    updateStatus()
                }
            }
            addView(blurCheckbox)

            val aberrationCheckbox = CheckBox(this@ScrollableTestActivity).apply {
                text = "色差效果"
                isChecked = true
                setOnCheckedChangeListener { _, isChecked ->
                    liquidGlassView.enableChromaticAberration = isChecked
                    updateStatus()
                }
            }
            addView(aberrationCheckbox)
            
            // 快捷按钮
            val buttonLayout = LinearLayout(this@ScrollableTestActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 16, 0, 0)
            }
            
            val allOnButton = Button(this@ScrollableTestActivity).apply {
                text = "全部开启"
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
                setOnClickListener {
                    blurCheckbox.isChecked = true
                    aberrationCheckbox.isChecked = true
                }
            }
            buttonLayout.addView(allOnButton)

            val aberrationOnlyButton = Button(this@ScrollableTestActivity).apply {
                text = "仅色差"
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    marginStart = 8
                }
                setOnClickListener {
                    blurCheckbox.isChecked = false
                    aberrationCheckbox.isChecked = true
                }
            }
            buttonLayout.addView(aberrationOnlyButton)

            val allOffButton = Button(this@ScrollableTestActivity).apply {
                text = "全部关闭"
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    marginStart = 8
                }
                setOnClickListener {
                    blurCheckbox.isChecked = false
                    aberrationCheckbox.isChecked = false
                }
            }
            buttonLayout.addView(allOffButton)

            addView(buttonLayout)

            // 提示文本
            val hint = TextView(this@ScrollableTestActivity).apply {
                text = "💡 滚动背景查看效果变化"
                textSize = 12f
                setTextColor(0xFF999999.toInt())
                setPadding(0, 16, 0, 0)
            }
            addView(hint)
        }
    }

    private fun updateStatus() {
        val effects = mutableListOf<String>()
        if (liquidGlassView.enableBackdropBlur) effects.add("模糊")
        if (liquidGlassView.enableChromaticAberration) effects.add("色差")

        statusText.text = if (effects.isEmpty()) {
            "当前效果：无"
        } else {
            "当前效果：${effects.joinToString(" + ")}"
        }
    }

    /**
     * ✅ 启动性能监控
     */
    private fun startPerformanceMonitoring() {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val updateInterval = 500L // 每500ms更新一次

        val runnable = object : Runnable {
            override fun run() {
                // 从 logcat 读取最新的性能数据
                try {
                    val process = Runtime.getRuntime().exec("logcat -d -s LiquidGlassView:D -t 1")
                    val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
                    var line: String?
                    var captureTime = ""
                    var blurTime = ""
                    var effectTime = ""
                    var totalTime = ""

                    while (reader.readLine().also { line = it } != null) {
                        line?.let {
                            if (it.contains("捕获背景:")) {
                                captureTime = it.substringAfter("捕获背景: ").substringBefore("ms")
                            }
                            if (it.contains("模糊处理:")) {
                                blurTime = it.substringAfter("模糊处理: ").substringBefore("ms")
                            }
                            if (it.contains("效果处理:")) {
                                effectTime = it.substringAfter("效果处理: ").substringBefore("ms")
                            }
                            if (it.contains("总耗时:")) {
                                totalTime = it.substringAfter("总耗时: ").substringBefore("ms")
                            }
                        }
                    }

                    if (totalTime.isNotEmpty()) {
                        val fps = (1000.0 / totalTime.toDouble()).toInt()
                        performanceText.text = """
                            性能监控 (FPS: ~$fps)
                            捕获: ${captureTime}ms | 模糊: ${blurTime}ms
                            效果: ${effectTime}ms | 总计: ${totalTime}ms
                        """.trimIndent()
                    }
                } catch (e: Exception) {
                    // 忽略错误
                }

                handler.postDelayed(this, updateInterval)
            }
        }

        handler.post(runnable)
    }
}


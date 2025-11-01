/**
 * LiquidGlass 专业演示 Activity
 * 
 * 功能特性：
 * - 可滚动的彩色背景
 * - 悬浮按钮（FAB）
 * - 侧边栏调试面板（DrawerLayout）
 * - 实时性能监控显示
 */
package com.example.liquidglass

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ProfessionalDemoActivity : AppCompatActivity() {

    // 主要组件
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var glassView: LiquidGlassView
    private lateinit var fabSettings: FloatingActionButton
    
    // 性能监控
    private lateinit var tvPerformanceOverlay: TextView
    private val performanceHandler = Handler(Looper.getMainLooper())
    private var isMonitoring = true
    
    // 调试面板控件
    private lateinit var seekBlur: SeekBar
    private lateinit var seekSaturation: SeekBar
    private lateinit var seekAberration: SeekBar
    private lateinit var spinnerBlurMethod: Spinner
    private lateinit var spinnerAberrationMethod: Spinner
    private lateinit var switchHighQuality: Switch
    private lateinit var switchPerformanceOverlay: Switch
    private lateinit var switchEnableBlur: Switch
    private lateinit var switchEnableAberration: Switch
    private lateinit var switchEnableSaturation: Switch

    private lateinit var tvBlur: TextView
    private lateinit var tvSaturation: TextView
    private lateinit var tvAberration: TextView
    private lateinit var tvBlurMethod: TextView
    private lateinit var tvAberrationMethod: TextView
    private lateinit var tvDebugInfo: TextView
    private lateinit var tvImageSizes: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 创建主布局
        createMainLayout()
        
        // 初始化组件
        initViews()
        setupControls()
        startPerformanceMonitoring()
    }

    private fun createMainLayout() {
        // 创建 DrawerLayout
        drawerLayout = DrawerLayout(this).apply {
            layoutParams = DrawerLayout.LayoutParams(
                DrawerLayout.LayoutParams.MATCH_PARENT,
                DrawerLayout.LayoutParams.MATCH_PARENT
            )
        }
        
        // 创建主内容区域
        val mainContent = createMainContent()
        drawerLayout.addView(mainContent)
        
        // 创建侧边栏
        val drawer = createDrawer()
        drawerLayout.addView(drawer)
        
        setContentView(drawerLayout)
    }

    private fun createMainContent(): FrameLayout {
        val container = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        
        // 创建可滚动背景
        val scrollView = createScrollableBackground()
        container.addView(scrollView)
        
        // 创建 LiquidGlass 悬浮按钮
        glassView = LiquidGlassView(this).apply {
            id = View.generateViewId()
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
            }
            // ✅ 启用动态背景模式（因为有滚动背景）
            enableDynamicBackground = true
        }
        
        // 添加按钮内容
        val buttonContent = TextView(this).apply {
            text = "✨ LiquidGlass\n效果演示"
            textSize = 20f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(100, 80, 100, 80)
            setShadowLayer(8f, 0f, 2f, Color.BLACK)
        }
        glassView.addView(buttonContent)
        container.addView(glassView)
        
        // 创建性能监控悬浮窗
        tvPerformanceOverlay = TextView(this).apply {
            id = View.generateViewId()
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                setMargins(16, 16, 16, 16)
            }
            setBackgroundColor(0xCC000000.toInt())
            setTextColor(Color.GREEN)
            textSize = 10f
            typeface = android.graphics.Typeface.MONOSPACE
            setPadding(12, 8, 12, 8)
            text = "性能监控：等待数据..."
        }
        container.addView(tvPerformanceOverlay)
        
        // 创建 FAB
        fabSettings = FloatingActionButton(this).apply {
            id = View.generateViewId()
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.END
                setMargins(0, 0, 32, 32)
            }
            setImageResource(android.R.drawable.ic_menu_preferences)
            setOnClickListener {
                drawerLayout.openDrawer(GravityCompat.END)
            }
        }
        container.addView(fabSettings)
        
        return container
    }

    private fun createScrollableBackground(): ScrollView {
        val scrollView = ScrollView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        
        val scrollContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        
        // 添加多个彩色块作为背景
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
            val container = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    400
                )
                setBackgroundColor(color)
            }

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
        return scrollView
    }

    private fun createDrawer(): LinearLayout {
        return LinearLayout(this).apply {
            layoutParams = DrawerLayout.LayoutParams(
                (resources.displayMetrics.widthPixels * 0.85f).toInt(),
                DrawerLayout.LayoutParams.MATCH_PARENT
            ).apply {
                gravity = GravityCompat.END
            }
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFFFFFFF.toInt())
            setPadding(24, 48, 24, 24)
            
            // 标题
            addView(TextView(this@ProfessionalDemoActivity).apply {
                text = "⚙️ 调试面板"
                textSize = 24f
                setTextColor(Color.BLACK)
                setPadding(0, 0, 0, 24)
            })
            
            // 创建 ScrollView 包含所有控件
            val scrollView = ScrollView(this@ProfessionalDemoActivity).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
            }
            
            val controlsContainer = LinearLayout(this@ProfessionalDemoActivity).apply {
                orientation = LinearLayout.VERTICAL
            }
            
            // 添加所有控件到 controlsContainer
            addDrawerControls(controlsContainer)
            
            scrollView.addView(controlsContainer)
            addView(scrollView)
        }
    }

    private fun addDrawerControls(container: LinearLayout) {
        // 性能监控开关
        container.addView(createSectionTitle("性能监控"))
        
        switchPerformanceOverlay = Switch(this).apply {
            id = View.generateViewId()
            text = "显示性能悬浮窗"
            setTextColor(Color.BLACK)
            isChecked = true
            setOnCheckedChangeListener { _, isChecked ->
                tvPerformanceOverlay.visibility = if (isChecked) View.VISIBLE else View.GONE
                isMonitoring = isChecked
            }
        }
        container.addView(switchPerformanceOverlay)
        
        tvDebugInfo = TextView(this).apply {
            id = View.generateViewId()
            textSize = 11f
            setTextColor(Color.BLACK)
            typeface = android.graphics.Typeface.MONOSPACE
            setPadding(0, 8, 0, 16)
            text = "等待性能数据..."
        }
        container.addView(tvDebugInfo)
        
        // 模糊方法选择
        container.addView(createSectionTitle("模糊算法"))

        tvBlurMethod = TextView(this).apply {
            id = View.generateViewId()
            text = "当前：智能选择"
            textSize = 12f
            setTextColor(Color.BLACK)
            setPadding(0, 0, 0, 4)
        }
        container.addView(tvBlurMethod)

        spinnerBlurMethod = Spinner(this).apply {
            id = View.generateViewId()
        }
        container.addView(spinnerBlurMethod)

        // 算法说明
        val blurMethodDesc = TextView(this).apply {
            text = """
                • 智能选择: 自动选择最优算法
                • Box Blur: 传统盒式模糊
                • IIR 高斯: C++ 递归高斯模糊
                • IIR NEON: ARM SIMD 向量化
                • Box3: 3次盒式近似高斯
                • 下采样: 强模糊优化管线
            """.trimIndent()
            textSize = 10f
            setTextColor(0xFF666666.toInt())
            setPadding(0, 8, 0, 0)
        }
        container.addView(blurMethodDesc)

        container.addView(createDivider())

        // 色差算法选择
        container.addView(createSectionTitle("色差算法"))

        tvAberrationMethod = TextView(this).apply {
            id = View.generateViewId()
            text = "当前：自动选择"
            textSize = 12f
            setTextColor(Color.BLACK)
            setPadding(0, 0, 0, 4)
        }
        container.addView(tvAberrationMethod)

        spinnerAberrationMethod = Spinner(this).apply {
            id = View.generateViewId()
        }
        container.addView(spinnerAberrationMethod)

        // 算法说明
        val aberrationMethodDesc = TextView(this).apply {
            text = """
                • 自动选择: 根据图像大小智能选择
                • C++ 实现: 高性能原生实现 (3-5x 提升)
                • Kotlin 实现: 纯 Kotlin 实现 (兼容性好)

                推荐：
                - 大图/实时处理 → C++ 实现
                - 小图/静态图片 → Kotlin 实现
            """.trimIndent()
            textSize = 10f
            setTextColor(0xFF666666.toInt())
            setPadding(0, 8, 0, 0)
        }
        container.addView(aberrationMethodDesc)

        container.addView(createDivider())

        // 效果开关
        container.addView(createSectionTitle("效果开关"))

        switchEnableBlur = Switch(this).apply {
            id = View.generateViewId()
            text = "启用模糊效果"
            setTextColor(Color.BLACK)
            isChecked = true
            setOnCheckedChangeListener { _, isChecked ->
                glassView.enableBackdropBlur = isChecked
            }
        }
        container.addView(switchEnableBlur)

        switchEnableAberration = Switch(this).apply {
            id = View.generateViewId()
            text = "启用色差效果"
            setTextColor(Color.BLACK)
            isChecked = true
            setOnCheckedChangeListener { _, isChecked ->
                glassView.enableChromaticAberration = isChecked
            }
        }
        container.addView(switchEnableAberration)

        switchEnableSaturation = Switch(this).apply {
            id = View.generateViewId()
            text = "启用饱和度调节"
            setTextColor(Color.BLACK)
            isChecked = true
            setOnCheckedChangeListener { _, isChecked ->
                // 饱和度通过设置为 100 来禁用
                if (!isChecked) {
                    glassView.saturation = 100f
                    seekSaturation.isEnabled = false
                } else {
                    seekSaturation.isEnabled = true
                }
            }
        }
        container.addView(switchEnableSaturation)

        container.addView(createDivider())

        // 模糊参数
        container.addView(createSectionTitle("模糊参数"))

        tvBlur = TextView(this).apply { id = View.generateViewId() }
        container.addView(tvBlur)
        seekBlur = SeekBar(this).apply { id = View.generateViewId() }
        container.addView(seekBlur)

        container.addView(createDivider())

        // 饱和度
        container.addView(createSectionTitle("饱和度"))

        tvSaturation = TextView(this).apply { id = View.generateViewId() }
        container.addView(tvSaturation)
        seekSaturation = SeekBar(this).apply { id = View.generateViewId() }
        container.addView(seekSaturation)

        container.addView(createDivider())

        // 色差强度
        container.addView(createSectionTitle("色差强度"))

        tvAberration = TextView(this).apply { id = View.generateViewId() }
        container.addView(tvAberration)
        seekAberration = SeekBar(this).apply { id = View.generateViewId() }
        container.addView(seekAberration)

        container.addView(createDivider())

        // ✅ 全局下采样比例
        container.addView(createSectionTitle("全局下采样（所有效果）"))

        val tvGlobalDownsample = TextView(this).apply {
            id = View.generateViewId()
            setTextColor(Color.BLACK)
            text = "全局下采样: 1.00x (无下采样)"
        }
        container.addView(tvGlobalDownsample)

        val seekGlobalDownsample = SeekBar(this).apply {
            id = View.generateViewId()
            max = 100  // 0.25 - 1.0
            progress = 75  // 默认 1.0
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val factor = 0.25f + (progress / 100f) * 0.75f
                    glassView.globalDownsampleFactor = factor
                    tvGlobalDownsample.text = "全局下采样: ${String.format("%.2f", factor)}x"
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        container.addView(seekGlobalDownsample)

        container.addView(createDivider())

        // ✅ 色差下采样比例
        container.addView(createSectionTitle("色差效果下采样"))

        val tvAberrationDownsample = TextView(this).apply {
            id = View.generateViewId()
            setTextColor(Color.BLACK)
            text = "色差下采样: 0.50x"
        }
        container.addView(tvAberrationDownsample)

        val seekAberrationDownsample = SeekBar(this).apply {
            id = View.generateViewId()
            max = 100  // 0.25 - 1.0
            progress = 33  // 默认 0.5
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val factor = 0.25f + (progress / 100f) * 0.75f
                    glassView.aberrationDownsample = factor
                    tvAberrationDownsample.text = "色差下采样: ${String.format("%.2f", factor)}x"
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        container.addView(seekAberrationDownsample)

        container.addView(createDivider())

        // ✅ 色差通道偏移量
        container.addView(createSectionTitle("色差通道偏移"))

        // 红色通道偏移
        val tvRedOffset = TextView(this).apply {
            id = View.generateViewId()
            setTextColor(Color.BLACK)
            text = "红色偏移: 0.00"
        }
        container.addView(tvRedOffset)

        val seekRedOffset = SeekBar(this).apply {
            id = View.generateViewId()
            max = 200  // -0.2 到 0.2
            progress = 100  // 默认 0
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val offset = (progress - 100) / 500f  // -0.2 到 0.2
                    glassView.aberrationRedOffset = offset
                    tvRedOffset.text = "红色偏移: ${String.format("%.2f", offset)}"
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        container.addView(seekRedOffset)

        // 绿色通道偏移
        val tvGreenOffset = TextView(this).apply {
            id = View.generateViewId()
            setTextColor(Color.BLACK)
            text = "绿色偏移: -0.05"
        }
        container.addView(tvGreenOffset)

        val seekGreenOffset = SeekBar(this).apply {
            id = View.generateViewId()
            max = 200  // -0.2 到 0.2
            progress = 75  // 默认 -0.05
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val offset = (progress - 100) / 500f  // -0.2 到 0.2
                    glassView.aberrationGreenOffset = offset
                    tvGreenOffset.text = "绿色偏移: ${String.format("%.2f", offset)}"
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        container.addView(seekGreenOffset)

        // 蓝色通道偏移
        val tvBlueOffset = TextView(this).apply {
            id = View.generateViewId()
            setTextColor(Color.BLACK)
            text = "蓝色偏移: -0.10"
        }
        container.addView(tvBlueOffset)

        val seekBlueOffset = SeekBar(this).apply {
            id = View.generateViewId()
            max = 200  // -0.2 到 0.2
            progress = 50  // 默认 -0.1
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val offset = (progress - 100) / 500f  // -0.2 到 0.2
                    glassView.aberrationBlueOffset = offset
                    tvBlueOffset.text = "蓝色偏移: ${String.format("%.2f", offset)}"
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        container.addView(seekBlueOffset)

        container.addView(createDivider())

        // 高质量模式（移到效果参数下方）
        switchHighQuality = Switch(this).apply {
            id = View.generateViewId()
            text = "高质量模糊（线性色彩空间）"
            setTextColor(Color.BLACK)
            isChecked = false
            setOnCheckedChangeListener { _, isChecked ->
                glassView.highQualityBlur = isChecked
            }
        }
        container.addView(switchHighQuality)

        container.addView(createDivider())

        // 图片尺寸信息
        container.addView(createSectionTitle("图片尺寸信息"))

        tvImageSizes = TextView(this).apply {
            id = View.generateViewId()
            textSize = 11f
            setTextColor(Color.BLACK)
            typeface = android.graphics.Typeface.MONOSPACE
            setPadding(0, 8, 0, 16)
            text = "等待数据..."
        }
        container.addView(tvImageSizes)
    }

    private fun createSectionTitle(title: String): TextView {
        return TextView(this).apply {
            text = title
            textSize = 14f
            setTextColor(Color.BLACK)
            setPadding(0, 16, 0, 8)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
    }

    private fun createDivider(): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1
            ).apply {
                setMargins(0, 16, 0, 0)
            }
            setBackgroundColor(0xFFE0E0E0.toInt())
        }
    }

    private fun initViews() {
        // 初始化已在 createDrawer 中完成
    }

    private fun setupControls() {
        // 模糊方法选择
        val blurMethods = arrayOf(
            "智能选择 (推荐)",
            "传统 Box Blur",
            "IIR 高斯 (标量)",
            "IIR 高斯 (NEON)",
            "Box3 快速模糊",
            "下采样管线"
        )
        val blurAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, blurMethods)
        blurAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerBlurMethod.adapter = blurAdapter
        spinnerBlurMethod.setSelection(0)

        spinnerBlurMethod.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                glassView.blurMethod = when (position) {
                    0 -> BlurMethod.SMART
                    1 -> BlurMethod.BOX_BLUR
                    2 -> BlurMethod.IIR_GAUSSIAN
                    3 -> BlurMethod.IIR_GAUSSIAN_NEON
                    4 -> BlurMethod.BOX3
                    5 -> BlurMethod.DOWNSAMPLE
                    else -> BlurMethod.SMART
                }
                tvBlurMethod.text = "当前：${blurMethods[position]}"
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // 色差算法选择
        val aberrationMethods = arrayOf(
            "自动选择 (推荐)",
            "C++ 实现 (高性能)",
            "Kotlin 实现 (兼容)"
        )
        val aberrationAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, aberrationMethods)
        aberrationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerAberrationMethod.adapter = aberrationAdapter
        spinnerAberrationMethod.setSelection(0)

        spinnerAberrationMethod.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                glassView.chromaticAberrationMode = when (position) {
                    0 -> ChromaticAberrationEffect.PerformanceMode.AUTO
                    1 -> ChromaticAberrationEffect.PerformanceMode.CPP
                    2 -> ChromaticAberrationEffect.PerformanceMode.KOTLIN
                    else -> ChromaticAberrationEffect.PerformanceMode.AUTO
                }
                tvAberrationMethod.text = "当前：${aberrationMethods[position]}"
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // 模糊强度
        seekBlur.max = 100
        seekBlur.progress = (glassView.blurAmount * 1000).toInt()
        tvBlur.text = "模糊强度: ${String.format("%.3f", glassView.blurAmount)}"
        seekBlur.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress / 1000f
                glassView.blurAmount = value
                tvBlur.text = "模糊强度: ${String.format("%.3f", value)}"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 饱和度
        seekSaturation.max = 200
        seekSaturation.progress = glassView.saturation.toInt()
        tvSaturation.text = "饱和度: ${glassView.saturation}%"
        seekSaturation.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                glassView.saturation = progress.toFloat()
                tvSaturation.text = "饱和度: $progress%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 色差强度
        seekAberration.max = 100
        seekAberration.progress = (glassView.aberrationIntensity * 10).toInt()
        tvAberration.text = "色差强度: ${String.format("%.1f", glassView.aberrationIntensity)}"
        seekAberration.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress / 10f
                glassView.aberrationIntensity = value
                tvAberration.text = "色差强度: ${String.format("%.1f", value)}"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 高质量模式
        switchHighQuality.setOnCheckedChangeListener { _, isChecked ->
            glassView.highQualityBlur = isChecked
        }
    }

    private fun startPerformanceMonitoring() {
        val updateInterval = 500L
        
        val runnable = object : Runnable {
            override fun run() {
                if (isMonitoring) {
                    updatePerformanceDisplay()
                }
                performanceHandler.postDelayed(this, updateInterval)
            }
        }
        
        performanceHandler.post(runnable)
    }

    private fun updatePerformanceDisplay() {
        // 从 LiquidGlassView 的日志中提取性能数据（在后台线程执行）
        Thread {
            var process: Process? = null
            var reader: java.io.BufferedReader? = null
            try {
                process = Runtime.getRuntime().exec("logcat -d -s LiquidGlassView:D NativeGauss:D -t 20")
                reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
                var line: String?
                var captureTime = ""
                var blurTime = ""
                var aberrationTime = ""
                var totalTime = ""
                var capturedSize = ""
                var blurredSize = ""

                while (reader.readLine().also { line = it } != null) {
                    line?.let {
                        if (it.contains("捕获背景:")) {
                            captureTime = it.substringAfter("捕获背景: ").substringBefore("ms").trim()
                        }
                        if (it.contains("模糊处理:")) {
                            blurTime = it.substringAfter("模糊处理: ").substringBefore("ms").trim()
                        }
                        if (it.contains("色差效果:")) {
                            aberrationTime = it.substringAfter("色差效果: ").substringBefore("ms").trim()
                        }
                        if (it.contains("总耗时:")) {
                            totalTime = it.substringAfter("总耗时: ").substringBefore("ms").trim()
                        }
                        // 提取图片尺寸信息
                        if (it.contains("gaussianIIRNeonInplace:") || it.contains("box3Inplace:")) {
                            // 格式: "gaussianIIRNeonInplace: 550x304, sigma=1.98"
                            val sizeMatch = Regex("(\\d+)x(\\d+)").find(it)
                            if (sizeMatch != null) {
                                blurredSize = "${sizeMatch.groupValues[1]}×${sizeMatch.groupValues[2]}"
                            }
                        }
                    }
                }

                // 获取 LiquidGlassView 的尺寸作为捕获尺寸
                capturedSize = "${glassView.width}×${glassView.height}"

                if (totalTime.isNotEmpty()) {
                    val fps = (1000.0 / totalTime.toDouble()).toInt()

                    // 获取当前算法信息
                    val blurMethodName = when (glassView.blurMethod) {
                        BlurMethod.SMART -> "智能"
                        BlurMethod.BOX_BLUR -> "Box"
                        BlurMethod.IIR_GAUSSIAN -> "IIR"
                        BlurMethod.IIR_GAUSSIAN_NEON -> "NEON"
                        BlurMethod.BOX3 -> "Box3"
                        BlurMethod.DOWNSAMPLE -> "下采样"
                    }

                    val aberrationMethodName = when (glassView.chromaticAberrationMode) {
                        ChromaticAberrationEffect.PerformanceMode.AUTO -> "自动"
                        ChromaticAberrationEffect.PerformanceMode.CPP -> "C++"
                        ChromaticAberrationEffect.PerformanceMode.KOTLIN -> "KT"
                    }

                    val overlayText = """
                        FPS: ~$fps
                        捕获: ${captureTime}ms
                        模糊: ${blurTime}ms ($blurMethodName)
                        色差: ${aberrationTime}ms ($aberrationMethodName)
                        总计: ${totalTime}ms
                    """.trimIndent()

                    val debugText = """
                        性能详情：
                        - 捕获背景: ${captureTime}ms
                        - 模糊处理: ${blurTime}ms (算法: $blurMethodName)
                        - 色差效果: ${aberrationTime}ms (算法: $aberrationMethodName)
                        - 总耗时: ${totalTime}ms
                        - 帧率: ~${fps} FPS

                        算法说明：
                        - 模糊: $blurMethodName = ${getBlurMethodDescription()}
                        - 色差: $aberrationMethodName = ${getAberrationMethodDescription()}
                    """.trimIndent()

                    // 更新图片尺寸信息
                    val sizeText = if (blurredSize.isNotEmpty()) {
                        """
                            截取图片尺寸: $capturedSize
                            实际模糊尺寸: $blurredSize

                            说明：
                            - 截取尺寸 = LiquidGlass 按钮大小
                            - 模糊尺寸 = 实际处理的图片大小
                            - 如果两者不同，说明使用了下采样优化
                        """.trimIndent()
                    } else {
                        """
                            截取图片尺寸: $capturedSize
                            实际模糊尺寸: 等待数据...
                        """.trimIndent()
                    }

                    // 在主线程更新 UI
                    runOnUiThread {
                        tvPerformanceOverlay.text = overlayText
                        tvDebugInfo.text = debugText
                        tvImageSizes.text = sizeText
                    }
                }
            } catch (e: Exception) {
                Log.e("ProfessionalDemo", "Failed to read performance data", e)
            } finally {
                // 确保关闭资源
                try {
                    reader?.close()
                    process?.destroy()
                } catch (e: Exception) {
                    Log.e("ProfessionalDemo", "Failed to close resources", e)
                }
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        performanceHandler.removeCallbacksAndMessages(null)
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END)
        } else {
            super.onBackPressed()
        }
    }

    /**
     * 获取模糊算法描述
     */
    private fun getBlurMethodDescription(): String {
        return when (glassView.blurMethod) {
            BlurMethod.SMART -> "自动选择最优算法"
            BlurMethod.BOX_BLUR -> "传统盒式模糊"
            BlurMethod.IIR_GAUSSIAN -> "C++ 递归高斯(标量)"
            BlurMethod.IIR_GAUSSIAN_NEON -> "C++ 递归高斯(NEON)"
            BlurMethod.BOX3 -> "3次盒式近似高斯"
            BlurMethod.DOWNSAMPLE -> "下采样优化管线"
        }
    }

    /**
     * 获取色差算法描述
     */
    private fun getAberrationMethodDescription(): String {
        return when (glassView.chromaticAberrationMode) {
            ChromaticAberrationEffect.PerformanceMode.AUTO -> "根据图像大小自动选择"
            ChromaticAberrationEffect.PerformanceMode.CPP -> "C++ 原生实现 (3-5x 提升)"
            ChromaticAberrationEffect.PerformanceMode.KOTLIN -> "Kotlin 实现 (兼容性好)"
        }
    }
}


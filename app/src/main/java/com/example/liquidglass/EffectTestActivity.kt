/**
 * 效果测试 Activity
 * 
 * 提供开关来单独测试每个效果：
 * - 背景模糊
 * - 边缘扭曲
 * - 色差效果
 * - Window 背景模糊（API 31+）
 */
package com.example.liquidglass

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class EffectTestActivity : AppCompatActivity() {
    
    private lateinit var liquidGlassView: LiquidGlassView
    private lateinit var statusText: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 创建布局
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        
        // 标题
        val title = TextView(this).apply {
            text = "LiquidGlass 效果测试"
            textSize = 24f
            setPadding(0, 0, 0, 32)
        }
        rootLayout.addView(title)
        
        // LiquidGlass 视图
        liquidGlassView = LiquidGlassView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                400
            )
        }
        
        // 添加一些内容到 LiquidGlass
        val content = TextView(this).apply {
            text = "LiquidGlass 效果演示\n\n点击下方开关测试不同效果"
            textSize = 18f
            setPadding(48, 48, 48, 48)
            setTextColor(0xFF000000.toInt())
        }
        liquidGlassView.addView(content)
        rootLayout.addView(liquidGlassView)
        
        // 状态文本
        statusText = TextView(this).apply {
            text = "当前效果：全部开启"
            textSize = 16f
            setPadding(0, 32, 0, 16)
        }
        rootLayout.addView(statusText)
        
        // 效果开关
        val blurCheckbox = CheckBox(this).apply {
            text = "背景模糊 (Backdrop Blur)"
            isChecked = true
            setOnCheckedChangeListener { _, isChecked ->
                liquidGlassView.enableBackdropBlur = isChecked
                updateStatus()
            }
        }
        rootLayout.addView(blurCheckbox)
        
        val aberrationCheckbox = CheckBox(this).apply {
            text = "色差效果 (Chromatic Aberration)"
            isChecked = true
            setOnCheckedChangeListener { _, isChecked ->
                liquidGlassView.enableChromaticAberration = isChecked
                updateStatus()
            }
        }
        rootLayout.addView(aberrationCheckbox)

        // 快捷按钮
        val buttonLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 32, 0, 0)
        }

        val allOnButton = Button(this).apply {
            text = "全部开启"
            setOnClickListener {
                blurCheckbox.isChecked = true
                aberrationCheckbox.isChecked = true
            }
        }
        buttonLayout.addView(allOnButton)

        val allOffButton = Button(this).apply {
            text = "全部关闭"
            setOnClickListener {
                blurCheckbox.isChecked = false
                aberrationCheckbox.isChecked = false
            }
        }
        buttonLayout.addView(allOffButton)

        val aberrationOnlyButton = Button(this).apply {
            text = "仅色差"
            setOnClickListener {
                blurCheckbox.isChecked = false
                aberrationCheckbox.isChecked = true
            }
        }
        buttonLayout.addView(aberrationOnlyButton)
        
        rootLayout.addView(buttonLayout)
        
        // 说明文本
        val infoText = TextView(this).apply {
            text = """

                效果说明：
                • 背景模糊：模糊背景并调整饱和度
                  - 支持多种算法：IIR 高斯、NEON、Box3 等
                  - C++ 实现，性能优异

                • 边缘扭曲：使用位移贴图扭曲边缘
                  - 基于 SDF 的位移贴图生成

                • 色差效果：RGB 通道分离产生色差
                  - C++ 实现：高性能原生算法 (3-5x 提升)
                  - Kotlin 实现：纯 Kotlin 算法 (兼容性好)
                  - 自动选择：根据图像大小智能选择

                • Window 背景模糊：系统级 GPU 加速（仅 API 31+）

                提示：
                - 单独开启"边缘扭曲"可以看到纯扭曲效果
                - Window 背景模糊性能最高，但需要 Android 12+
                - 推荐使用 C++ 色差算法以获得最佳性能
            """.trimIndent()
            textSize = 12f
            setPadding(0, 16, 0, 0)
            setTextColor(0xFF666666.toInt())
        }
        rootLayout.addView(infoText)
        
        // 包装在 ScrollView 中
        val scrollView = ScrollView(this).apply {
            addView(rootLayout)
        }
        
        setContentView(scrollView)
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
}


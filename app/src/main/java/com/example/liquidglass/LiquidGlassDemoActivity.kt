/**
 * LiquidGlass 效果演示 Activity
 * 
 * 展示各种 LiquidGlass 效果组合和参数调整
 */
package com.example.liquidglass

import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

class LiquidGlassDemoActivity : AppCompatActivity() {

    private lateinit var glassView: LiquidGlassView

    // 参数控制
    private lateinit var seekBarDisplacement: SeekBar
    private lateinit var seekBarBlur: SeekBar
    private lateinit var seekBarSaturation: SeekBar
    private lateinit var seekBarAberration: SeekBar
    private lateinit var seekBarElasticity: SeekBar
    private lateinit var seekBarCornerRadius: SeekBar

    // 参数显示
    private lateinit var tvDisplacement: TextView
    private lateinit var tvBlur: TextView
    private lateinit var tvSaturation: TextView
    private lateinit var tvAberration: TextView
    private lateinit var tvElasticity: TextView
    private lateinit var tvCornerRadius: TextView

    // ✅ 新增：模糊方法选择
    private lateinit var spinnerBlurMethod: Spinner
    private lateinit var tvBlurMethod: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_liquid_glass_demo)
        
        initViews()
        setupControls()
    }
    
    private fun initViews() {
        glassView = findViewById(R.id.liquidGlassView)

        // SeekBars
        seekBarDisplacement = findViewById(R.id.seekBarDisplacement)
        seekBarBlur = findViewById(R.id.seekBarBlur)
        seekBarSaturation = findViewById(R.id.seekBarSaturation)
        seekBarAberration = findViewById(R.id.seekBarAberration)
        seekBarElasticity = findViewById(R.id.seekBarElasticity)
        seekBarCornerRadius = findViewById(R.id.seekBarCornerRadius)

        // TextViews
        tvDisplacement = findViewById(R.id.tvDisplacement)
        tvBlur = findViewById(R.id.tvBlur)
        tvSaturation = findViewById(R.id.tvSaturation)
        tvAberration = findViewById(R.id.tvAberration)
        tvElasticity = findViewById(R.id.tvElasticity)
        tvCornerRadius = findViewById(R.id.tvCornerRadius)

        // Switches
        val switchEdgeHighlight = findViewById<SwitchCompat>(R.id.switchEdgeHighlight)
        switchEdgeHighlight.setOnCheckedChangeListener { _, isChecked ->
            glassView.enableEdgeHighlight = isChecked
        }

        val switchHighQuality = findViewById<SwitchCompat>(R.id.switchHighQuality)
        switchHighQuality.setOnCheckedChangeListener { _, isChecked ->
            glassView.highQualityBlur = isChecked
        }

        // ✅ 新增：模糊方法选择
        spinnerBlurMethod = findViewById(R.id.spinnerBlurMethod)
        tvBlurMethod = findViewById(R.id.tvBlurMethod)

        // 设置模糊方法选项
        val blurMethods = arrayOf(
            "智能选择 (推荐)",
            "传统 Box Blur",
            "IIR 高斯 (标量)",
            "IIR 高斯 (NEON)",
            "Box3 快速模糊",
            "下采样管线"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, blurMethods)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerBlurMethod.adapter = adapter
        spinnerBlurMethod.setSelection(0)  // 默认智能选择

        spinnerBlurMethod.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                glassView.blurMethod = when (position) {
                    0 -> BlurMethod.SMART
                    1 -> BlurMethod.BOX_BLUR
                    2 -> BlurMethod.IIR_GAUSSIAN
                    3 -> BlurMethod.IIR_GAUSSIAN_NEON
                    4 -> BlurMethod.BOX3
                    5 -> BlurMethod.DOWNSAMPLE
                    else -> BlurMethod.SMART
                }
                tvBlurMethod.text = "模糊方法: ${blurMethods[position]}"
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    private fun setupControls() {
        // Displacement Scale (0-200)
        seekBarDisplacement.max = 200
        seekBarDisplacement.progress = 70
        seekBarDisplacement.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                glassView.displacementScale = progress.toFloat()
                tvDisplacement.text = "Displacement: $progress"
                glassView.invalidate()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Blur Amount (0-100, 映射到 0-1)
        seekBarBlur.max = 100
        seekBarBlur.progress = 6  // 0.0625 * 100
        seekBarBlur.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                glassView.blurAmount = progress / 100f
                tvBlur.text = "Blur: ${String.format("%.2f", progress / 100f)}"
                glassView.invalidate()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Saturation (100-200)
        seekBarSaturation.max = 100
        seekBarSaturation.progress = 40  // 140 - 100
        seekBarSaturation.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                glassView.saturation = (100 + progress).toFloat()
                tvSaturation.text = "Saturation: ${100 + progress}%"
                glassView.invalidate()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Aberration Intensity (0-10)
        seekBarAberration.max = 100
        seekBarAberration.progress = 20  // 2.0 * 10
        seekBarAberration.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                glassView.aberrationIntensity = progress / 10f
                tvAberration.text = "Aberration: ${String.format("%.1f", progress / 10f)}"
                glassView.invalidate()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Elasticity (0-1)
        seekBarElasticity.max = 100
        seekBarElasticity.progress = 15  // 0.15 * 100
        seekBarElasticity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                glassView.elasticity = progress / 100f
                tvElasticity.text = "Elasticity: ${String.format("%.2f", progress / 100f)}"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Corner Radius (0-200)
        seekBarCornerRadius.max = 200
        seekBarCornerRadius.progress = 100  // 默认 100
        seekBarCornerRadius.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                glassView.cornerRadius = progress.toFloat()
                tvCornerRadius.text = "Corner Radius: $progress"
                glassView.invalidate()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // 初始化显示
        tvDisplacement.text = "Displacement: 70"
        tvBlur.text = "Blur: 0.06"
        tvSaturation.text = "Saturation: 140%"
        tvAberration.text = "Aberration: 2.0"
        tvElasticity.text = "Elasticity: 0.15"
        tvCornerRadius.text = "Corner Radius: 100"
        tvBlurMethod.text = "模糊方法: 智能选择 (推荐)"
    }
}


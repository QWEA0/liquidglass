/**
 * LiquidGlass 增强演示 Activity
 * 
 * 展示多种 UI 组件和可滚动的动态背景效果
 */
package com.example.liquidglass

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class EnhancedDemoActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enhanced_demo)
        
        setupClickListeners()
    }
    
    private fun setupClickListeners() {
        // 按钮点击事件
        findViewById<LiquidGlassView>(R.id.glassButton1)?.setOnClickListener {
            showToast("Button 1 clicked!")
        }
        
        findViewById<LiquidGlassView>(R.id.glassButton2)?.setOnClickListener {
            showToast("Button 2 clicked!")
        }
        
        findViewById<LiquidGlassView>(R.id.glassButton3)?.setOnClickListener {
            showToast("Button 3 clicked!")
        }
        
        // 卡片点击事件
        findViewById<LiquidGlassView>(R.id.glassCard1)?.setOnClickListener {
            showToast("Card 1 clicked!")
        }
        
        findViewById<LiquidGlassView>(R.id.glassCard2)?.setOnClickListener {
            showToast("Card 2 clicked!")
        }
        
        findViewById<LiquidGlassView>(R.id.glassCard3)?.setOnClickListener {
            showToast("Card 3 clicked!")
        }
        
        // 导航栏点击事件
        findViewById<LiquidGlassView>(R.id.glassNavBar)?.setOnClickListener {
            showToast("Navigation bar clicked!")
        }
        
        // 浮动按钮点击事件
        findViewById<LiquidGlassView>(R.id.glassFab)?.setOnClickListener {
            showToast("Floating button clicked!")
        }
    }
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}


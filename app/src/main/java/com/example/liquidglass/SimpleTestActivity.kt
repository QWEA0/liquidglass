/**
 * 简化测试 Activity
 *
 * 只包含一个玻璃按钮,用于测试滚动时的动态背景模糊效果
 */
package com.example.liquidglass

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SimpleTestActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_test)

        // 设置按钮点击事件
        findViewById<LiquidGlassView>(R.id.glassButton)?.setOnClickListener {
            Toast.makeText(this, "Glass Button Clicked!", Toast.LENGTH_SHORT).show()
        }
    }
}


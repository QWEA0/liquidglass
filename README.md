# LiquidGlass Android

<div align="center">

**A stunning glassmorphism UI component library for Android**

**令人惊艳的 Android 液态玻璃 UI 组件库**

<img src="assets\2983473432.jpg" alt="LiquidGlass Demo Screenshot" width="460">

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/platform-Android-green.svg)](https://developer.android.com)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)](https://android-arsenal.com/api?level=24)

[English](#english) | [中文](#chinese)

</div>

---

<a name="english"></a>

## 🌊 LiquidGlass Android

A high-performance glassmorphism UI component library for Android, featuring real-time backdrop blur, chromatic aberration, and liquid-like interactive effects.

### ✨ Features

- **🎨 Real-time Backdrop Blur** - Dynamic background blur with adjustable radius and saturation
- **🌈 Chromatic Aberration** - RGB channel separation effect for a premium glass look
- **💧 Liquid Distortion** - Edge distortion effects that respond to touch interactions
- **✨ Edge Highlights** - Dynamic light reflections based on touch position
- **⚡ High Performance** - Optimized with native C++ (NEON SIMD) and smart caching
- **🎯 Easy Integration** - Simple XML attributes and Kotlin API
- **🔧 Highly Customizable** - Fine-tune every aspect of the glass effect

### 📱 Demo

<div align="center">

#### 🎬 Video Demo
<img src="assets\mmexport1762090985763.gif" alt="LiquidGlass Demo Screenshot" width="460">



</div>

The demo app showcases:
- Scrollable colorful background with infinite tiling support
- Floating glass button with real-time effects
- Debug panel with live parameter adjustment
- Performance monitoring overlay
- Custom background image selection from gallery
- Bilingual UI (English/Chinese) with language switching
- Edge highlight controls (border width, opacity, over light mode)

### 🚀 Quick Start

#### 1. Add to Your Layout

```xml
<com.example.liquidglass.LiquidGlassView
    android:layout_width="200dp"
    android:layout_height="80dp"
    app:displacementScale="70"
    app:blurAmount="0.0625"
    app:saturation="140"
    app:aberrationIntensity="2"
    app:elasticity="0.15"
    app:cornerRadius="999">

    <!-- Your content here -->
    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Glass Button"
        android:textColor="#FFFFFF"
        android:layout_gravity="center" />

</com.example.liquidglass.LiquidGlassView>
```

#### 2. Customize in Code

```kotlin
val glassView = findViewById<LiquidGlassView>(R.id.glassView)

// Enable/disable effects
glassView.enableBackdropBlur = true
glassView.enableChromaticAberration = true
glassView.enableEdgeHighlight = true

// Adjust parameters
glassView.blurAmount = 0.0625f
glassView.saturation = 140f
glassView.aberrationIntensity = 2f
glassView.displacementScale = 70f

// Choose blur method
glassView.blurMethod = BlurMethod.SMART // AUTO, BOX, IIR_GAUSS, DOWNSAMPLE
```

### 🎛️ Customization Options

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `displacementScale` | Float | 70 | Edge distortion intensity |
| `blurAmount` | Float | 0.0625 | Blur radius (0-1) |
| `saturation` | Float | 140 | Color saturation percentage |
| `aberrationIntensity` | Float | 2 | Chromatic aberration strength |
| `elasticity` | Float | 0.15 | Touch interaction spring effect |
| `cornerRadius` | Float | 999 | Corner radius (999 = pill shape) |
| `enableBackdropBlur` | Boolean | true | Enable background blur |
| `enableChromaticAberration` | Boolean | true | Enable RGB separation |
| `enableEdgeHighlight` | Boolean | true | Enable edge lighting |
| `blurMethod` | Enum | SMART | Blur algorithm selection |

### 🏗️ Architecture

**Core Components:**
- `LiquidGlassView` - Main view component with touch interaction
- `EnhancedBlurEffect` - Multi-algorithm blur engine (Box, IIR Gaussian, Downsample)
- `ChromaticAberrationEffect` - RGB channel separation processor
- `EdgeDistortionEffect` - Liquid-like edge deformation
- `EdgeHighlightEffect` - Dynamic light reflection renderer
- `AsyncRenderer` - Background thread rendering for smooth performance

**Native Optimization:**
- `gauss_iir_neon.cpp` - ARM NEON SIMD accelerated IIR Gaussian blur
- `chromatic_aberration.cpp` - Hardware-accelerated RGB separation
- `boxblur.cpp` - Fast box blur implementation

### 📊 Performance

- **Optimized Rendering**: Smart caching with 3-layer strategy (backdrop → blur → final)
- **Native Acceleration**: ARM NEON SIMD for 4-8x performance boost
- **Adaptive Quality**: Automatic downsampling for smooth 60fps on mid-range devices
- **Memory Efficient**: Bitmap pooling and automatic resource recycling

### 🔧 Requirements

- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 35 (Android 15)
- **Language**: Kotlin
- **NDK**: Required for native blur acceleration

### 📦 Dependencies

```gradle
dependencies {
    implementation "androidx.core:core-ktx:1.12.0"
    implementation "androidx.appcompat:appcompat:1.6.1"
    implementation "com.google.android.material:material:1.11.0"
}
```

### 🛠️ Build

```bash
# Clone the repository
git clone https://github.com/yourusername/liquidglass-android.git

# Open in Android Studio
# Build and run the demo app
./gradlew assembleDebug
```

### 📄 License

This project is open source. Feel free to use it in your projects.

### 🙏 Credits

Inspired by the glassmorphism design trend and liquid-glass-react library.

---

<a name="chinese"></a>

## 🌊 LiquidGlass Android

一个高性能的 Android 液态玻璃 UI 组件库，具有实时背景模糊、色差效果和液态交互特性。

### ✨ 特性

- **🎨 实时背景模糊** - 动态背景模糊，可调节模糊半径和饱和度
- **🌈 色差效果** - RGB 通道分离效果，呈现高级玻璃质感
- **💧 液态扭曲** - 响应触摸交互的边缘扭曲效果
- **✨ 边缘高光** - 基于触摸位置的动态光线反射
- **⚡ 高性能** - 使用原生 C++ (NEON SIMD) 和智能缓存优化
- **🎯 易于集成** - 简单的 XML 属性和 Kotlin API
- **🔧 高度可定制** - 精细调节玻璃效果的每个方面

### 📱 演示

<div align="center">

#### 🎬 视频演示

https://github.com/QWEA0/liquidglass/assets/2106699696.mp4

#### 📸 截图

<img src="2983473432.jpg" alt="LiquidGlass 演示截图" width="300">

</div>

演示应用展示：
- 支持无限拼接的可滚动彩色背景
- 带实时效果的悬浮玻璃按钮
- 实时参数调节的调试面板
- 性能监控覆盖层
- 从相册选择自定义背景图片
- 双语界面（中文/英文）支持语言切换
- 边缘高光控制（边框宽度、不透明度、亮背景模式）

### 🚀 快速开始

#### 1. 添加到布局文件

```xml
<com.example.liquidglass.LiquidGlassView
    android:layout_width="200dp"
    android:layout_height="80dp"
    app:displacementScale="70"
    app:blurAmount="0.0625"
    app:saturation="140"
    app:aberrationIntensity="2"
    app:elasticity="0.15"
    app:cornerRadius="999">

    <!-- 在这里放置你的内容 -->
    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="玻璃按钮"
        android:textColor="#FFFFFF"
        android:layout_gravity="center" />

</com.example.liquidglass.LiquidGlassView>
```

#### 2. 代码中自定义

```kotlin
val glassView = findViewById<LiquidGlassView>(R.id.glassView)

// 启用/禁用效果
glassView.enableBackdropBlur = true
glassView.enableChromaticAberration = true
glassView.enableEdgeHighlight = true

// 调整参数
glassView.blurAmount = 0.0625f
glassView.saturation = 140f
glassView.aberrationIntensity = 2f
glassView.displacementScale = 70f

// 选择模糊方法
glassView.blurMethod = BlurMethod.SMART // AUTO, BOX, IIR_GAUSS, DOWNSAMPLE
```

### 🎛️ 自定义选项

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `displacementScale` | Float | 70 | 边缘扭曲强度 |
| `blurAmount` | Float | 0.0625 | 模糊半径 (0-1) |
| `saturation` | Float | 140 | 颜色饱和度百分比 |
| `aberrationIntensity` | Float | 2 | 色差强度 |
| `elasticity` | Float | 0.15 | 触摸交互弹性效果 |
| `cornerRadius` | Float | 999 | 圆角半径 (999 = 胶囊形状) |
| `enableBackdropBlur` | Boolean | true | 启用背景模糊 |
| `enableChromaticAberration` | Boolean | true | 启用 RGB 分离 |
| `enableEdgeHighlight` | Boolean | true | 启用边缘光效 |
| `blurMethod` | Enum | SMART | 模糊算法选择 |

### 🏗️ 架构

**核心组件：**
- `LiquidGlassView` - 主视图组件，支持触摸交互
- `EnhancedBlurEffect` - 多算法模糊引擎（Box、IIR 高斯、降采样）
- `ChromaticAberrationEffect` - RGB 通道分离处理器
- `EdgeDistortionEffect` - 液态边缘变形效果
- `EdgeHighlightEffect` - 动态光线反射渲染器
- `AsyncRenderer` - 后台线程渲染，保证流畅性能

**原生优化：**
- `gauss_iir_neon.cpp` - ARM NEON SIMD 加速的 IIR 高斯模糊
- `chromatic_aberration.cpp` - 硬件加速的 RGB 分离
- `boxblur.cpp` - 快速盒式模糊实现

### 📊 性能

- **优化渲染**：智能三层缓存策略（背景 → 模糊 → 最终结果）
- **原生加速**：ARM NEON SIMD 提供 4-8 倍性能提升
- **自适应质量**：自动降采样，在中端设备上保持流畅 60fps
- **内存高效**：位图池和自动资源回收

### 🔧 要求

- **最低 SDK**: 24 (Android 7.0)
- **目标 SDK**: 35 (Android 15)
- **语言**: Kotlin
- **NDK**: 需要用于原生模糊加速

### 📦 依赖

```gradle
dependencies {
    implementation "androidx.core:core-ktx:1.12.0"
    implementation "androidx.appcompat:appcompat:1.6.1"
    implementation "com.google.android.material:material:1.11.0"
}
```

### 🛠️ 构建

```bash
# 克隆仓库
git clone https://github.com/yourusername/liquidglass-android.git

# 在 Android Studio 中打开
# 构建并运行演示应用
./gradlew assembleDebug
```

### 📄 许可证

本项目为开源项目，欢迎在你的项目中使用。

### 🙏 致谢

灵感来源于玻璃态设计趋势和 liquid-glass-react 库。

---

<div align="center">

**Made with ❤️ for Android developers**

**为 Android 开发者用心打造**

</div>

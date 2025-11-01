/**
 * chromatic_aberration.cpp - 色差效果算法实现
 * 
 * 核心算法：
 * 1. 对每个像素，从位移贴图读取位移量（dx, dy）
 * 2. 计算三个颜色通道的采样位置（每个通道有不同的偏移）
 * 3. 使用双线性插值从源图像采样
 * 4. 合并三个通道生成最终像素
 * 
 * 双线性插值原理：
 * - 找到采样点周围的 4 个像素
 * - 根据采样点的小数部分计算权重
 * - 对 4 个像素进行加权平均
 * - 消除最近邻采样的马赛克效果
 * 
 * 性能优化：
 * - 内联函数减少函数调用开销
 * - 缓存友好的内存访问模式
 * - 避免重复计算
 * - 使用快速浮点运算
 * 
 * 时间复杂度：O(W×H)
 * 空间复杂度：O(1)（原位处理）
 */

#include "chromatic_aberration.h"
#include <algorithm>
#include <cmath>
#include <android/log.h>

#define LOG_TAG "ChromaticAberration"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

/**
 * 双线性插值采样单个颜色通道
 * 
 * @param pixels 像素数据
 * @param width 图像宽度
 * @param height 图像高度
 * @param stride 行跨度（字节数）
 * @param x 采样 X 坐标（可以是小数）
 * @param y 采样 Y 坐标（可以是小数）
 * @param channelOffset 通道偏移（0=B, 1=G, 2=R, 3=A）
 * @return 插值后的通道值（0-255）
 */
static inline uint8_t sample_bilinear_channel(
    const uint8_t* pixels,
    int width,
    int height,
    int stride,
    float x,
    float y,
    int channelOffset
) {
    // 边界检查
    if (x < 0.0f || x >= width - 1.0f || y < 0.0f || y >= height - 1.0f) {
        int clampedX = std::max(0, std::min(width - 1, static_cast<int>(x + 0.5f)));
        int clampedY = std::max(0, std::min(height - 1, static_cast<int>(y + 0.5f)));
        return pixels[clampedY * stride + clampedX * 4 + channelOffset];
    }
    
    // 获取整数部分和小数部分
    int x0 = static_cast<int>(x);
    int y0 = static_cast<int>(y);
    int x1 = std::min(x0 + 1, width - 1);
    int y1 = std::min(y0 + 1, height - 1);
    
    float fx = x - x0;  // X 方向的小数部分（权重）
    float fy = y - y0;  // Y 方向的小数部分（权重）
    
    // 获取 4 个角的像素值
    const uint8_t* row0 = pixels + y0 * stride;
    const uint8_t* row1 = pixels + y1 * stride;
    
    float c00 = row0[x0 * 4 + channelOffset];  // 左上
    float c10 = row0[x1 * 4 + channelOffset];  // 右上
    float c01 = row1[x0 * 4 + channelOffset];  // 左下
    float c11 = row1[x1 * 4 + channelOffset];  // 右下
    
    // 双线性插值
    // 先在 X 方向插值
    float c0 = c00 * (1.0f - fx) + c10 * fx;  // 上边插值
    float c1 = c01 * (1.0f - fx) + c11 * fx;  // 下边插值
    
    // 再在 Y 方向插值
    float result = c0 * (1.0f - fy) + c1 * fy;
    
    // 钳位到 [0, 255]
    return static_cast<uint8_t>(std::max(0.0f, std::min(255.0f, result + 0.5f)));
}

/**
 * 双线性插值采样完整像素（ARGB）
 * 
 * @param pixels 像素数据
 * @param width 图像宽度
 * @param height 图像高度
 * @param stride 行跨度（字节数）
 * @param x 采样 X 坐标
 * @param y 采样 Y 坐标
 * @param outB 输出蓝色通道
 * @param outG 输出绿色通道
 * @param outR 输出红色通道
 * @param outA 输出 Alpha 通道
 */
static inline void sample_bilinear_pixel(
    const uint8_t* pixels,
    int width,
    int height,
    int stride,
    float x,
    float y,
    uint8_t& outB,
    uint8_t& outG,
    uint8_t& outR,
    uint8_t& outA
) {
    outB = sample_bilinear_channel(pixels, width, height, stride, x, y, 0);
    outG = sample_bilinear_channel(pixels, width, height, stride, x, y, 1);
    outR = sample_bilinear_channel(pixels, width, height, stride, x, y, 2);
    outA = sample_bilinear_channel(pixels, width, height, stride, x, y, 3);
}

// 主处理函数
void chromatic_aberration_rgba8888(
    const uint8_t* source,
    const uint8_t* displacement,
    uint8_t* result,
    int width,
    int height,
    int sourceStride,
    int displacementStride,
    int resultStride,
    float intensity,
    float scale,
    float redOffset,
    float greenOffset,
    float blueOffset
) {
    // 参数校验
    if (!source || !displacement || !result) {
        LOGE("Invalid parameters: null pointer");
        return;
    }

    if (width <= 0 || height <= 0) {
        LOGE("Invalid dimensions: %dx%d", width, height);
        return;
    }

    if (sourceStride < width * 4 || displacementStride < width * 4 || resultStride < width * 4) {
        LOGE("Invalid stride: source=%d, displacement=%d, result=%d, min=%d",
             sourceStride, displacementStride, resultStride, width * 4);
        return;
    }

    // 位移缩放因子（与 Kotlin 实现一致）
    const float scaleFactor = scale / 255.0f;

    // ✅ 修复：Kotlin 层已经将 offset * intensity * downscale，所以这里直接使用传入的值
    // 不需要再乘以 intensity
    const float actualRedOffset = redOffset;
    const float actualGreenOffset = greenOffset;
    const float actualBlueOffset = blueOffset;

    LOGD("Processing %dx%d, intensity=%.2f, scale=%.2f, offsets=(%.3f, %.3f, %.3f)",
         width, height, intensity, scale, actualRedOffset, actualGreenOffset, actualBlueOffset);

    // 处理每个像素
    for (int y = 0; y < height; ++y) {
        const uint8_t* displacementRow = displacement + y * displacementStride;
        uint8_t* resultRow = result + y * resultStride;

        for (int x = 0; x < width; ++x) {
            // ✅ 修复：位移贴图实际上是 RGBA 格式，不是 BGRA！
            const uint8_t* mapPixel = displacementRow + x * 4;
            uint8_t mapR = mapPixel[0];  // R 通道 = X 方向位移
            uint8_t mapG = mapPixel[1];  // G 通道 = Y 方向位移
            uint8_t mapB = mapPixel[2];  // B 通道
            uint8_t mapA = mapPixel[3];  // A 通道

            // 计算基础位移（128 为中心点，表示无位移）
            float baseDx = (static_cast<float>(mapR) - 128.0f) * scaleFactor;
            float baseDy = (static_cast<float>(mapG) - 128.0f) * scaleFactor;

            // 调试：打印中心像素的信息和位移贴图值
            if (x == width / 2 && y == height / 2) {
                LOGD("C++ center pixel: BGRA=(%d,%d,%d,%d), baseDx=%.3f, baseDy=%.3f, offsets=(%.3f, %.3f, %.3f)",
                     mapB, mapG, mapR, mapA, baseDx, baseDy, actualRedOffset, actualGreenOffset, actualBlueOffset);
            }

            // 打印几个边缘像素的位移贴图值
            if ((x == 10 && y == 10) || (x == width - 10 && y == 10) ||
                (x == 10 && y == height - 10) || (x == width - 10 && y == height - 10)) {
                LOGD("C++ edge pixel (%d,%d): BGRA=(%d,%d,%d,%d), baseDx=%.3f, baseDy=%.3f",
                     x, y, mapB, mapG, mapR, mapA, baseDx, baseDy);
            }

            // 计算三个通道的采样位置（每个通道有不同的位移）
            // 注意：与 Kotlin 实现完全一致
            float rSrcX = x + baseDx + actualRedOffset;
            float rSrcY = y + baseDy + actualRedOffset;
            float gSrcX = x + baseDx + actualGreenOffset;
            float gSrcY = y + baseDy + actualGreenOffset;
            float bSrcX = x + baseDx + actualBlueOffset;
            float bSrcY = y + baseDy + actualBlueOffset;

            // ✅ 修复：使用双线性插值采样各个通道（与 Kotlin 实现一致）
            // 每个通道从对应的采样位置提取对应的颜色通道
            uint8_t r = sample_bilinear_channel(source, width, height, sourceStride, rSrcX, rSrcY, 2);  // R 通道
            uint8_t g = sample_bilinear_channel(source, width, height, sourceStride, gSrcX, gSrcY, 1);  // G 通道
            uint8_t b = sample_bilinear_channel(source, width, height, sourceStride, bSrcX, bSrcY, 0);  // B 通道

            // Alpha 通道取自原始像素
            const uint8_t* sourcePixel = source + y * sourceStride + x * 4;
            uint8_t alpha = sourcePixel[3];

            // 写入结果（BGRA 格式）
            uint8_t* outPixel = resultRow + x * 4;
            outPixel[0] = b;      // B 通道（从蓝色采样位置的 B 通道）
            outPixel[1] = g;      // G 通道（从绿色采样位置的 G 通道）
            outPixel[2] = r;      // R 通道（从红色采样位置的 R 通道）
            outPixel[3] = alpha;  // A 通道（保持原始 Alpha）
        }
    }
}

// 原位处理版本（简化参数）
void chromatic_aberration_rgba8888_inplace(
    const uint8_t* source,
    const uint8_t* displacement,
    uint8_t* result,
    int width,
    int height,
    int stride,
    float intensity,
    float scale,
    float redOffset,
    float greenOffset,
    float blueOffset
) {
    chromatic_aberration_rgba8888(
        source, displacement, result,
        width, height,
        stride, stride, stride,
        intensity, scale,
        redOffset, greenOffset, blueOffset
    );
}


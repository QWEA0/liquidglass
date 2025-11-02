/**
 * native-lib.cpp - JNI 绑定层
 *
 * 职责：
 * - 处理 Java Bitmap 对象的锁定/解锁
 * - 验证 Bitmap 格式（必须为 ARGB_8888）
 * - 验证 Bitmap 可编辑性（必须为 mutable）
 * - 调用底层 C++ 算法（模糊、色差效果等）
 * - 错误处理与日志记录
 *
 * 线程安全：
 * - JNI 函数本身是线程安全的
 * - 但不要对同一个 Bitmap 对象并发调用
 * - 底层算法不使用全局状态，不同 Bitmap 可并发处理
 *
 * 性能注意事项：
 * - 使用 AndroidBitmap_lockPixels 直接访问像素内存，避免复制
 * - 使用 stride 正确处理行对齐，避免内存越界
 * - 异常情况下确保 unlockPixels，防止内存泄漏
 */

#include <jni.h>
#include <android/bitmap.h>
#include <android/log.h>
#include <cstring>
#include "gauss_iir.h"
#include "gauss_iir_neon.h"
#include "boxblur.h"
#include "chromatic_aberration.h"

#define LOG_TAG "NativeGauss"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

/**
 * 辅助函数：验证并锁定 Bitmap
 * 
 * @param env JNI 环境
 * @param bitmap Bitmap 对象
 * @param info 输出：Bitmap 信息
 * @param pixels 输出：像素数据指针
 * @return true 成功，false 失败
 */
static bool lock_bitmap(
    JNIEnv* env,
    jobject bitmap,
    AndroidBitmapInfo* info,
    void** pixels
) {
    // 获取 Bitmap 信息
    int ret = AndroidBitmap_getInfo(env, bitmap, info);
    if (ret != ANDROID_BITMAP_RESULT_SUCCESS) {
        LOGE("AndroidBitmap_getInfo failed: %d", ret);
        return false;
    }
    
    // 验证格式
    if (info->format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGE("Bitmap format must be ARGB_8888, got: %d", info->format);
        jclass exClass = env->FindClass("java/lang/IllegalArgumentException");
        env->ThrowNew(exClass, "Bitmap must be ARGB_8888 format");
        return false;
    }
    
    // 验证尺寸
    if (info->width <= 0 || info->height <= 0) {
        LOGE("Invalid bitmap size: %dx%d", info->width, info->height);
        jclass exClass = env->FindClass("java/lang/IllegalArgumentException");
        env->ThrowNew(exClass, "Bitmap size must be positive");
        return false;
    }
    
    // 锁定像素
    ret = AndroidBitmap_lockPixels(env, bitmap, pixels);
    if (ret != ANDROID_BITMAP_RESULT_SUCCESS) {
        LOGE("AndroidBitmap_lockPixels failed: %d", ret);
        jclass exClass = env->FindClass("java/lang/IllegalStateException");
        env->ThrowNew(exClass, "Failed to lock bitmap pixels (bitmap may not be mutable)");
        return false;
    }
    
    if (*pixels == nullptr) {
        LOGE("Locked pixels is null");
        AndroidBitmap_unlockPixels(env, bitmap);
        jclass exClass = env->FindClass("java/lang/IllegalStateException");
        env->ThrowNew(exClass, "Bitmap pixels is null");
        return false;
    }
    
    return true;
}

/**
 * JNI: gaussianIIRInplace
 */
extern "C" JNIEXPORT void JNICALL
Java_com_example_blur_NativeGauss_gaussianIIRInplace(
    JNIEnv* env,
    jobject /* this */,
    jobject bitmap,
    jfloat sigma,
    jboolean linear
) {
    AndroidBitmapInfo info;
    void* pixels = nullptr;
    
    // 锁定 Bitmap
    if (!lock_bitmap(env, bitmap, &info, &pixels)) {
        return; // 异常已在 lock_bitmap 中抛出
    }
    
    // 调用底层算法
    LOGD("gaussianIIRInplace: %dx%d, sigma=%.2f, linear=%d, stride=%d",
         info.width, info.height, sigma, linear, info.stride);
    
    gaussian_iir_rgba8888_inplace(
        static_cast<uint8_t*>(pixels),
        static_cast<int>(info.width),
        static_cast<int>(info.height),
        static_cast<int>(info.stride),
        sigma,
        linear
    );
    
    // 解锁 Bitmap
    AndroidBitmap_unlockPixels(env, bitmap);
}

/**
 * JNI: gaussianIIRNeonInplace
 */
extern "C" JNIEXPORT void JNICALL
Java_com_example_blur_NativeGauss_gaussianIIRNeonInplace(
    JNIEnv* env,
    jobject /* this */,
    jobject bitmap,
    jfloat sigma,
    jboolean linear
) {
    AndroidBitmapInfo info;
    void* pixels = nullptr;

    // 锁定 Bitmap
    if (!lock_bitmap(env, bitmap, &info, &pixels)) {
        return;
    }

    // 调用 NEON 优化版本
    LOGD("gaussianIIRNeonInplace: %dx%d, sigma=%.2f, linear=%d, stride=%d",
         info.width, info.height, sigma, linear, info.stride);

    gaussian_iir_rgba8888_neon(
        static_cast<uint8_t*>(pixels),
        static_cast<int>(info.width),
        static_cast<int>(info.height),
        static_cast<int>(info.stride),
        sigma,
        linear
    );

    // 解锁 Bitmap
    AndroidBitmap_unlockPixels(env, bitmap);
}

/**
 * JNI: hasNeonSupport
 */
extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_blur_NativeGauss_hasNeonSupport(
    JNIEnv* env,
    jobject /* this */
) {
    return has_neon_support();
}

/**
 * JNI: box3Inplace
 */
extern "C" JNIEXPORT void JNICALL
Java_com_example_blur_NativeGauss_box3Inplace(
    JNIEnv* env,
    jobject /* this */,
    jobject bitmap,
    jint radius
) {
    AndroidBitmapInfo info;
    void* pixels = nullptr;

    // 锁定 Bitmap
    if (!lock_bitmap(env, bitmap, &info, &pixels)) {
        return; // 异常已在 lock_bitmap 中抛出
    }

    // 调用底层算法
    LOGD("box3Inplace: %dx%d, radius=%d, stride=%d",
         info.width, info.height, radius, info.stride);

    box3_rgba8888_inplace(
        static_cast<uint8_t*>(pixels),
        static_cast<int>(info.width),
        static_cast<int>(info.height),
        static_cast<int>(info.stride),
        radius
    );

    // 解锁 Bitmap
    AndroidBitmap_unlockPixels(env, bitmap);
}

/**
 * JNI: advancedBoxBlurInplace
 *
 * AdvancedFastBlur 风格的 Box Blur（降采样优化 - 快速版本）
 * 使用最近邻插值，速度快 5-10 倍
 *
 * @param bitmap 待处理位图（ARGB_8888，mutable）
 * @param radius 模糊半径（0-25）
 * @param downscale 降采样比例（0.01-1.0，推荐 0.5）
 */
extern "C" JNIEXPORT void JNICALL
Java_com_example_blur_NativeGauss_advancedBoxBlurInplace(
    JNIEnv* env,
    jobject /* this */,
    jobject bitmap,
    jfloat radius,
    jfloat downscale
) {
    AndroidBitmapInfo info;
    void* pixels = nullptr;

    // 锁定 Bitmap
    if (!lock_bitmap(env, bitmap, &info, &pixels)) {
        return; // 异常已在 lock_bitmap 中抛出
    }

    // 调用底层算法（原位处理，src 和 dst 相同）
    LOGD("advancedBoxBlurInplace: %dx%d, radius=%.2f, downscale=%.2f, stride=%d",
         info.width, info.height, radius, downscale, info.stride);

    advanced_box_blur_rgba8888(
        static_cast<uint8_t*>(pixels),  // src
        static_cast<uint8_t*>(pixels),  // dst (same as src)
        static_cast<int>(info.width),
        static_cast<int>(info.height),
        static_cast<int>(info.stride),
        radius,
        downscale
    );

    // 解锁 Bitmap
    AndroidBitmap_unlockPixels(env, bitmap);
}

/**
 * JNI: advancedBoxBlurInplaceHQ
 *
 * AdvancedFastBlur 风格的 Box Blur（降采样优化 - 高质量版本）
 * 使用双线性插值，质量好但速度较慢
 *
 * @param bitmap 待处理位图（ARGB_8888，mutable）
 * @param radius 模糊半径（0-25）
 * @param downscale 降采样比例（0.01-1.0，推荐 0.5）
 */
extern "C" JNIEXPORT void JNICALL
Java_com_example_blur_NativeGauss_advancedBoxBlurInplaceHQ(
    JNIEnv* env,
    jobject /* this */,
    jobject bitmap,
    jfloat radius,
    jfloat downscale
) {
    AndroidBitmapInfo info;
    void* pixels = nullptr;

    // 锁定 Bitmap
    if (!lock_bitmap(env, bitmap, &info, &pixels)) {
        return; // 异常已在 lock_bitmap 中抛出
    }

    // 调用底层算法（原位处理，src 和 dst 相同）
    LOGD("advancedBoxBlurInplaceHQ: %dx%d, radius=%.2f, downscale=%.2f, stride=%d",
         info.width, info.height, radius, downscale, info.stride);

    advanced_box_blur_rgba8888_hq(
        static_cast<uint8_t*>(pixels),  // src
        static_cast<uint8_t*>(pixels),  // dst (same as src)
        static_cast<int>(info.width),
        static_cast<int>(info.height),
        static_cast<int>(info.stride),
        radius,
        downscale
    );

    // 解锁 Bitmap
    AndroidBitmap_unlockPixels(env, bitmap);
}

/**
 * JNI: chromaticAberrationInplace
 *
 * 应用色差效果（Chromatic Aberration）
 *
 * @param source 源图像 Bitmap
 * @param displacement 位移贴图 Bitmap
 * @param result 结果图像 Bitmap（可以与 source 相同）
 * @param intensity 色差强度（0-10，推荐 2.0）
 * @param scale 位移缩放系数（推荐 70.0）
 * @param redOffset 红色通道偏移量（默认 0.0）
 * @param greenOffset 绿色通道偏移量（默认 -0.05）
 * @param blueOffset 蓝色通道偏移量（默认 -0.1）
 * @param useBilinear 是否使用双线性插值（默认 true）
 */
extern "C" JNIEXPORT void JNICALL
Java_com_example_liquidglass_NativeChromaticAberration_chromaticAberrationInplace(
    JNIEnv* env,
    jobject /* this */,
    jobject source,
    jobject displacement,
    jobject result,
    jfloat intensity,
    jfloat scale,
    jfloat redOffset,
    jfloat greenOffset,
    jfloat blueOffset,
    jboolean useBilinear
) {
    AndroidBitmapInfo sourceInfo, displacementInfo, resultInfo;
    void* sourcePixels = nullptr;
    void* displacementPixels = nullptr;
    void* resultPixels = nullptr;

    // 锁定源图像
    if (!lock_bitmap(env, source, &sourceInfo, &sourcePixels)) {
        return;
    }

    // 锁定位移贴图
    if (!lock_bitmap(env, displacement, &displacementInfo, &displacementPixels)) {
        AndroidBitmap_unlockPixels(env, source);
        return;
    }

    // 锁定结果图像
    if (!lock_bitmap(env, result, &resultInfo, &resultPixels)) {
        AndroidBitmap_unlockPixels(env, source);
        AndroidBitmap_unlockPixels(env, displacement);
        return;
    }

    // 验证尺寸一致性
    if (sourceInfo.width != displacementInfo.width ||
        sourceInfo.height != displacementInfo.height ||
        sourceInfo.width != resultInfo.width ||
        sourceInfo.height != resultInfo.height) {
        LOGE("Bitmap size mismatch: source=%dx%d, displacement=%dx%d, result=%dx%d",
             sourceInfo.width, sourceInfo.height,
             displacementInfo.width, displacementInfo.height,
             resultInfo.width, resultInfo.height);

        AndroidBitmap_unlockPixels(env, source);
        AndroidBitmap_unlockPixels(env, displacement);
        AndroidBitmap_unlockPixels(env, result);

        jclass exClass = env->FindClass("java/lang/IllegalArgumentException");
        env->ThrowNew(exClass, "Source, displacement, and result bitmaps must have the same dimensions");
        return;
    }

    // 调用底层算法
    LOGD("chromaticAberrationInplace: %dx%d, intensity=%.2f, scale=%.2f, offsets=(%.3f, %.3f, %.3f), useBilinear=%d",
         sourceInfo.width, sourceInfo.height, intensity, scale, redOffset, greenOffset, blueOffset, useBilinear);

    chromatic_aberration_rgba8888(
        static_cast<const uint8_t*>(sourcePixels),
        static_cast<const uint8_t*>(displacementPixels),
        static_cast<uint8_t*>(resultPixels),
        static_cast<int>(sourceInfo.width),
        static_cast<int>(sourceInfo.height),
        static_cast<int>(sourceInfo.stride),
        static_cast<int>(displacementInfo.stride),
        static_cast<int>(resultInfo.stride),
        intensity,
        scale,
        redOffset,
        greenOffset,
        blueOffset,
        useBilinear
    );

    // 解锁所有 Bitmap
    AndroidBitmap_unlockPixels(env, source);
    AndroidBitmap_unlockPixels(env, displacement);
    AndroidBitmap_unlockPixels(env, result);
}


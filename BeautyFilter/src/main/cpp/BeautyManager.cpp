#include <jni.h>
#include <android/log.h>
#include <cstdio>
#include <android/bitmap.h>
#include <cstring>
#include <unistd.h>
#include "bitmap/BitmapOperation.h"
#include "beauty/SimpleBeauty.h"

#define  LOG_TAG    "BeautyManager"
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL Java_com_frank_beautyfilter_BeautyManager_nativeInitBeauty(
	JNIEnv * env, jobject obj, jobject handle) {
	auto* jniBitmap = (JniBitmap*) env->GetDirectBufferAddress(handle);
	if (jniBitmap->_storedBitmapPixels == nullptr) {
		LOGE("no bitmap data was stored. returning null...");
		return;
	}
	SimpleBeauty::getInstance()->initMagicBeauty(jniBitmap);
}

JNIEXPORT void JNICALL Java_com_frank_beautyfilter_BeautyManager_nativeStartSkinWhite(
	JNIEnv * env, jobject obj, jfloat whiteLevel) {
	SimpleBeauty::getInstance()->startSkinWhite(whiteLevel);
}

JNIEXPORT void JNICALL Java_com_frank_beautyfilter_BeautyManager_nativeStartSkinSmooth(
	JNIEnv * env, jobject obj, jfloat DenoiseLevel) {
	float sigema = 10 + DenoiseLevel * DenoiseLevel * 5;
	SimpleBeauty::getInstance()->startSkinSmooth(sigema);
}

JNIEXPORT void JNICALL Java_com_frank_beautyfilter_BeautyManager_nativeUnInitBeauty(
	JNIEnv * env, jobject obj) {
	SimpleBeauty::getInstance()->unInitMagicBeauty();
}

JNIEXPORT jobject JNICALL Java_com_frank_beautyfilter_BeautyManager_nativeSetBitmap(
	JNIEnv * env, jobject obj, jobject bitmap) {
	return BitmapOperation::jniStoreBitmapData(env, obj, bitmap);
}

JNIEXPORT void JNICALL Java_com_frank_beautyfilter_BeautyManager_nativeFreeBitmap(
	JNIEnv * env, jobject obj, jobject handle) {
	BitmapOperation::jniFreeBitmapData(env, obj, handle);
}

JNIEXPORT jobject JNICALL Java_com_frank_beautyfilter_BeautyManager_nativeGetBitmap(
	JNIEnv * env, jobject obj, jobject handle) {
	return BitmapOperation::jniGetBitmapFromStoredBitmapData(env, obj, handle);
}
#ifdef __cplusplus
}
#endif

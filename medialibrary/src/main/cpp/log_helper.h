
#ifndef LOGHELPER_H
#define LOG_HELPER_H

#include "android/log.h"

#define LOGI(TAG, FORMAT, ...) __android_log_print(ANDROID_LOG_INFO, TAG, FORMAT, ##__VA_ARGS__)
#define LOGE(TAG, FORMAT, ...) __android_log_print(ANDROID_LOG_ERROR, TAG, FORMAT, ##__VA_ARGS__)

#define PLAYER_TAG "FFMediaPlayer"
#define ALOGD(FORMAT, ...) __android_log_print(ANDROID_LOG_DEBUG, PLAYER_TAG, FORMAT, ##__VA_ARGS__)
#define ALOGI(FORMAT, ...) __android_log_print(ANDROID_LOG_INFO, PLAYER_TAG, FORMAT, ##__VA_ARGS__)
#define ALOGE(FORMAT, ...) __android_log_print(ANDROID_LOG_ERROR, PLAYER_TAG, FORMAT, ##__VA_ARGS__)

#endif //LOGHELPER_H

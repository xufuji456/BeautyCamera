//
// Created by cain on 2018/12/30.
//

#include "GLESDevice.h"

GLESDevice::GLESDevice() {
    mWindow = nullptr;
}

GLESDevice::~GLESDevice() {

}

void GLESDevice::surfaceCreated(ANativeWindow *window) {
    if (mWindow != nullptr) {
        ANativeWindow_release(mWindow);
        mWindow = nullptr;
    }
    mWindow = window;
    av_log(nullptr, AV_LOG_ERROR, "surface created...");
}

void GLESDevice::onInit(int width, int height) {
    av_log(nullptr, AV_LOG_ERROR, "before width=%d,height=%d", width, height);
    if (mWindow != nullptr) {
        av_log(nullptr, AV_LOG_ERROR, "width=%d,height=%d", width, height);
        ANativeWindow_setBuffersGeometry(mWindow, width, height, WINDOW_FORMAT_RGBA_8888);
    }
}

int GLESDevice::onRender(uint8_t *data, int stride, int height) {
    // lock native window
    ANativeWindow_Buffer windowBuffer;
    int ret = ANativeWindow_lock(mWindow, &windowBuffer, nullptr);
    if (ret < 0) {
        return ret;
    }
    auto *dst = static_cast<uint8_t *>(windowBuffer.bits);
    int dstStride = windowBuffer.stride * 4;
    for (int h = 0; h < height; h++) {
        memcpy(dst + h * dstStride, data + h * stride, (size_t) stride);
    }
    ANativeWindow_unlockAndPost(mWindow);

    return 0;
}

void GLESDevice::onDestroy() {
    if (mWindow) {
        ANativeWindow_release(mWindow);
        mWindow = nullptr;
    }
}
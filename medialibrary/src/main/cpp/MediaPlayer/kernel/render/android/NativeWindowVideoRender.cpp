
#include "NativeWindowVideoRender.h"

NativeWindowVideoRender::NativeWindowVideoRender() {
    mWindow = nullptr;
}

NativeWindowVideoRender::~NativeWindowVideoRender() {

}

void NativeWindowVideoRender::setSurface(void *surface) {
    if (mWindow != nullptr) {
        ANativeWindow_release(mWindow);
        mWindow = nullptr;
    }
    mWindow = static_cast<ANativeWindow *>(surface);
}

void NativeWindowVideoRender::onInit(int width, int height) {
    if (mWindow == nullptr) {
        return;
    }
    ANativeWindow_setBuffersGeometry(mWindow, width, height, WINDOW_FORMAT_RGBA_8888);
}

int NativeWindowVideoRender::onRender(uint8_t *data, int stride, int height) {
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

void NativeWindowVideoRender::onDestroy() {
    if (mWindow) {
        ANativeWindow_release(mWindow);
        mWindow = nullptr;
    }
}
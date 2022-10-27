
#ifndef NATIVEWINDOW_VIDEORENDER_H
#define NATIVEWINDOW_VIDEORENDER_H

#include <render/VideoRender.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <string.h>
#include <Mutex.h>
#include <Condition.h>

class NativeWindowVideoRender : public VideoRender {
private:
    Mutex mMutex;
    Condition mCondition;

    ANativeWindow *mWindow;

public:
    NativeWindowVideoRender();

    virtual ~NativeWindowVideoRender();

    void setSurface(void *surface) override;

    void onInit(int width, int height) override;

    int onRender(uint8_t *data, int stride, int height) override;

    void onDestroy() override;
};

#endif //NATIVEWINDOW_VIDEORENDER_H

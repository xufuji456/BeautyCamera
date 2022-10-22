
#ifndef NATIVEWINDOW_VIDEORENDER_H
#define NATIVEWINDOW_VIDEORENDER_H

#include <render/VideoRender.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <string.h>
#include <Mutex.h>
#include <Condition.h>

class NativeWindowVideoRender : public VideoRender {
public:
    NativeWindowVideoRender();

    virtual ~NativeWindowVideoRender();

    void surfaceCreated(ANativeWindow *window);

    void onInit(int width, int height) override;

    int onRender(uint8_t *data, int stride, int height) override;

    void onDestroy() override;

private:
    Mutex mMutex;
    Condition mCondition;

    ANativeWindow *mWindow;
};

#endif //NATIVEWINDOW_VIDEORENDER_H

//
// Created by cain on 2018/12/30.
//

#ifndef GLESDEVICE_H
#define GLESDEVICE_H

#include <render/VideoRender.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>

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

#endif //GLESDEVICE_H

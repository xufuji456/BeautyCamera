//
// Created by cain on 2018/12/30.
//

#ifndef GLESDEVICE_H
#define GLESDEVICE_H

#include <device/VideoDevice.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>

class GLESDevice : public VideoDevice {
public:
    GLESDevice();

    virtual ~GLESDevice();

    void surfaceCreated(ANativeWindow *window);

    void onInit(int width, int height) override;

    int onRender(uint8_t *data, int stride, int height) override;

    void onDestroy() override;

private:
    Mutex mMutex;
    Condition mCondition;

    ANativeWindow *mWindow;
    int mSurfaceWidth;
    int mSurfaceHeight;

};

#endif //GLESDEVICE_H

//
// Created by cain on 2018/12/28.
//

#ifndef VIDEODEVICE_H
#define VIDEODEVICE_H

#include <player/PlayerState.h>

class VideoDevice {
public:
    VideoDevice();

    virtual ~VideoDevice();

    virtual void onInit(int width, int height);

    virtual int onRender(uint8_t *data, int stride, int height);

    virtual void onDestroy();

};

#endif //VIDEODEVICE_H

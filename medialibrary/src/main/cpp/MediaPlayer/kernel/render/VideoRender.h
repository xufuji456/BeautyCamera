
#ifndef VIDEORENDER_H
#define VIDEORENDER_H

#include <stdint.h>

class VideoRender {
public:

    virtual void onInit(int width, int height) = 0;

    virtual void setSurface(void *surface) = 0;

    virtual int onRender(uint8_t *data, int stride, int height) = 0;

    virtual void onDestroy() = 0;

};

#endif //VIDEORENDER_H

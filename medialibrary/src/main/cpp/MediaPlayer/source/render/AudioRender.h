
#ifndef AUDIORENDER_H
#define AUDIORENDER_H

#include <Mutex.h>
#include <Condition.h>
#include <Thread.h>
extern "C" {
#include <libavformat/avformat.h>
}

typedef void (*AudioPCMCallback) (void *opaque, uint8_t *stream, int len);

typedef struct AudioRenderSpec {
    int freq;
    uint32_t size;
    uint8_t channels;
    uint16_t samples;
    AVSampleFormat format;
    AudioPCMCallback callback;

    void *opaque;

} AudioRenderSpec;

class AudioRender : public Runnable {
public:

    virtual int open(const AudioRenderSpec *desired, AudioRenderSpec *obtained) = 0;

    virtual void start() = 0;

    virtual void stop() = 0;

    virtual void pause() = 0;

    virtual void resume() = 0;

    virtual void flush() = 0;

    virtual void setVolume(float volume) = 0;

    virtual void run() = 0;
};


#endif //AUDIORENDER_H

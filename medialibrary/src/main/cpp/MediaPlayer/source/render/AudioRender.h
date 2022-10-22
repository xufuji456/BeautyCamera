
#ifndef AUDIORENDER_H
#define AUDIORENDER_H

#include <Mutex.h>
#include <Condition.h>
#include <Thread.h>
extern "C" {
#include <libavformat/avformat.h>
}

typedef void (*AudioPCMCallback) (void *userdata, uint8_t *stream, int len);

typedef struct AudioDeviceSpec {
    int freq;                   // 采样率
    AVSampleFormat format;      // 音频采样格式
    uint8_t channels;           // 声道
    uint16_t samples;           // 采样大小
    uint32_t size;              // 缓冲区大小
    AudioPCMCallback callback;  // 音频回调
    void *userdata;             // 音频上下文
} AudioDeviceSpec;

class AudioRender : public Runnable {
public:

    virtual int open(const AudioDeviceSpec *desired, AudioDeviceSpec *obtained) = 0;

    virtual void start() = 0;

    virtual void stop() = 0;

    virtual void pause() = 0;

    virtual void resume() = 0;

    virtual void flush() = 0;

    virtual void setVolume(float volume) = 0;

    virtual void run() = 0;
};


#endif //AUDIORENDER_H

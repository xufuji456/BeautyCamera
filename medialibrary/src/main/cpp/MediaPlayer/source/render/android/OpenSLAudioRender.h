
#ifndef OpenSL_AUDIORENDER_H
#define OpenSL_AUDIORENDER_H

#include <render/AudioRender.h>

#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>
#include <pthread.h>

class OpenSLAudioRender : public AudioRender {
public:
    OpenSLAudioRender();

    virtual ~OpenSLAudioRender();

    int open(const AudioDeviceSpec *desired, AudioDeviceSpec *obtained) override;

    void start() override;

    void stop() override;

    void pause() override;

    void resume() override;

    void flush() override;

    void setVolume(float volume)override;

    virtual void run() override;

private:
    // 转换成SL采样率
    static SLuint32 getSLSampleRate(int sampleRate);

    // 获取SLES音量
    static SLmillibel getAmplificationLevel(float volumeLevel);

private:

    SLPlayItf slPlayItf;
    SLObjectItf slObject;
    SLEngineItf slEngine;
    SLVolumeItf slVolumeItf;
    SLObjectItf slPlayerObject;
    SLObjectItf slOutputMixObject;

    SLAndroidSimpleBufferQueueItf slBufferQueueItf;

    AudioDeviceSpec audioDeviceSpec{};
    int bytes_per_frame{};
    int milli_per_buffer{};
    int frames_per_buffer{};
    int bytes_per_buffer{};
    uint8_t *buffer{};
    size_t buffer_capacity{};

    Mutex mMutex;
    Condition mCondition;
    Thread *audioThread;
    int abortRequest;
    int pauseRequest;
    int flushRequest;

    bool updateVolume;
    float mVolume{};

};


#endif //OpenSL_AUDIORENDER_H


#ifndef OPENSL_AUDIORENDER_H
#define OPENSL_AUDIORENDER_H

#include <render/AudioRender.h>

#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>
#include <pthread.h>

class OpenSLAudioRender : public AudioRender {

private:

    SLPlayItf slPlayItf;
    SLObjectItf slObject;
    SLEngineItf slEngine;
    SLVolumeItf slVolumeItf;
    SLObjectItf slPlayerObject;
    SLObjectItf slOutputMixObject;

    SLAndroidSimpleBufferQueueItf slBufferQueueItf;

    uint8_t *buffer{};
    int bytes_per_frame{};
    int milli_per_buffer{};
    int frames_per_buffer{};
    int bytes_per_buffer{};
    size_t buffer_capacity{};

    Mutex mMutex;
    Condition mCondition;
    Thread *audioThread;

    float mVolume{};
    bool updateVolume;
    int abortRequest;
    int pauseRequest;
    int flushRequest;

    AudioRenderSpec audioRenderSpec{};

    static SLuint32 getSLSampleRate(int sampleRate);

    static SLmillibel getAmplificationLevel(float volumeLevel);

public:
    OpenSLAudioRender();

    virtual ~OpenSLAudioRender();

    int open(const AudioRenderSpec *desired, AudioRenderSpec *obtained) override;

    void start() override;

    void stop() override;

    void pause() override;

    void resume() override;

    void flush() override;

    void setVolume(float volume)override;

    virtual void run() override;

};

#endif //OPENSL_AUDIORENDER_H

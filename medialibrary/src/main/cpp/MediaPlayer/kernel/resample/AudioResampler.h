
#ifndef AUDIORESAMPLER_H
#define AUDIORESAMPLER_H

#include <player/PlayerParam.h>
#include <avsync/AVSync.h>
#include <soundtouch/SoundTouchHelper.h>
#include <render/AudioRender.h>

typedef struct AudioParams {
    int freq;
    int channels;
    int frame_size;
    int bytes_per_sec;
    int64_t channel_layout;
    enum AVSampleFormat fmt;
} AudioParams;

typedef struct AudioState {
    double m_audioClock;
    double audio_diff_cum;
    double audio_diff_avg_coef;
    double audio_diff_threshold;

    int bufferIndex;
    int m_audioBufSize;
    int m_writeBufSize;
    uint8_t *m_outputBuffer;
    int audio_diff_avg_count;
    SwrContext *m_swrContext;
    uint8_t *m_resampleBuffer;

    short *m_soundTouchBuffer;
    unsigned int m_bufferSize;
    unsigned int m_resampleSize;
    unsigned int m_soundTouchBufSize;

    AudioParams m_audioParamSrc;
    AudioParams m_audioParamDst;
    int64_t m_audioCallbackTime;

} AudioState;


class AudioResampler {

private:

    AVFrame *m_frame;
    AVSync *m_avSync;
    AudioState *m_audioState;
    PlayerParam *m_playerParam;
    AudioDecoder *m_audioDecoder;
    SoundTouchHelper *m_soundTouchHelper;

private:
    int audioFrameResample();

    int audioSynchronize(int nbSamples);

public:
    AudioResampler(PlayerParam *playerParam, AudioDecoder *audioDecoder, AVSync *avSync);

    virtual ~AudioResampler();

    int setResampleParams(AudioRenderSpec *spec, int64_t wanted_channel_layout);

    void pcmQueueCallback(uint8_t *stream, int len);

};


#endif //AUDIORESAMPLER_H

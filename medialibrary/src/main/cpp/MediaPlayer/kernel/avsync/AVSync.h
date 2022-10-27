
#ifndef AVSYNC_H
#define AVSYNC_H

#include <avsync/MediaClock.h>
#include <player/PlayerParam.h>
#include <decoder/VideoDecoder.h>
#include <decoder/AudioDecoder.h>
#include <render/VideoRender.h>

class AVSync : public Runnable {

private:

    bool   m_exit;
    bool   m_abortReq;
    double m_frameTimer;
    int    m_forceRefresh;
    int    m_timerRefresh;
    double m_maxFrameDuration;

    MediaClock *m_audioClock;
    MediaClock *m_videoClock;
    MediaClock *m_externalClock;

    VideoDecoder *m_videoDecoder;
    AudioDecoder *m_audioDecoder;

    Mutex m_syncMutex;
    Condition m_syncCond;
    Thread *m_syncThread;
    VideoRender *m_videoRender;
    PlayerParam *m_playerParam;

    uint8_t *m_buffer;
    AVFrame *m_frameRGBA;
    SwsContext *m_swsContext;

private:
    void refreshVideo(double *remaining_time);

    void checkExternalClockSpeed();

    double calculateDelay(double delay);

    double calculateDuration(Frame *vp, Frame *nextvp);

    void renderVideo();

public:
    AVSync(PlayerParam *playerParam);

    virtual ~AVSync();

    void reset();

    void start(VideoDecoder *videoDecoder, AudioDecoder *audioDecoder);

    void setVideoRender(VideoRender *videoRender);

    void setAudioDecoder(AudioDecoder *audioDecoder);

    void setMaxDuration(double maxDuration);

    void refreshVideoTimer();

    double getAudioDiffClock();

    void updateAudioClock(double pts, double time);

    void updateExternalClock(double pts);

    double getMasterClock();

    MediaClock *getAudioClock();

    MediaClock *getVideoClock();

    MediaClock *getExternalClock();

    void run() override;

    void stop();

};

#endif //AVSYNC_H

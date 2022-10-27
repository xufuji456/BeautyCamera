
#ifndef MEDIAPLAYER_H
#define MEDIAPLAYER_H

#include <avsync/MediaClock.h>
#include <player/PlayerParam.h>
#include <decoder/AudioDecoder.h>
#include <decoder/VideoDecoder.h>

#include <render/android/OpenSLAudioRender.h>
#include <render/android/NativeWindowVideoRender.h>

#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <avsync/AVSync.h>
#include <resample/AudioResampler.h>

class MediaPlayer : public Runnable {

private:
    Mutex m_playerMutex;
    Condition m_playerCond;
    Thread *m_readThread;

    PlayerParam *m_playerParam;

    AudioDecoder *m_audioDecoder;
    VideoDecoder *m_videoDecoder;

    int m_eof;
    bool m_exitPlay;
    int m_lastPause;
    int64_t m_duration;

    AVSync *m_avSync;
    AudioRender *m_audioRender;
    AudioResampler *m_audioResampler;

private:
    int readPackets();

    int openDecoder(int streamIndex);

    void closeDecoder(int streamIndex);

    int openAudioRender(int64_t wanted_channel_layout, int wanted_nb_channels,
                        int wanted_sample_rate);

    void startAudioRender(PlayerParam *playerParam);

    void run() override;

public:
    MediaPlayer();

    virtual ~MediaPlayer();

    int reset();

    void setDataSource(const char *url);

    void setVideoRender(VideoRender *render);

    int prepare();

    int prepareAsync();

    void start();

    void pause();

    void resume();

    void seekTo(long timeMs);

    int selectTrack(int trackId, bool selected);

    void setVolume(float volume);

    void setMute(int mute);

    void setRate(float rate);

    int getRotate();

    int getVideoWidth();

    int getVideoHeight();

    long getCurrentPosition();

    long getDuration();

    int isPlaying();

    FFMessageQueue *getMessageQueue();

    AVStream *getAVStream(int mediaType) const;

    AVFormatContext *getMetadata() const;

    void pcmQueueCallback(uint8_t *stream, int len);

    void stop();

};

#endif //MEDIAPLAYER_H

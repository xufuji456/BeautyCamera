
#ifndef MEDIAPLAYER_H
#define MEDIAPLAYER_H

#include <avsync/MediaClock.h>
#include <SoundTouchHelper.h>
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
    Mutex mMutex;
    Condition mCondition;
    Thread *readThread;

    PlayerParam *m_playerParam;

    AudioDecoder *audioDecoder;
    VideoDecoder *videoDecoder;
    bool mExit;

    int64_t mDuration;
    int lastPaused;
    int eof;
    int attachmentRequest;

    AudioRender *audioRender;
    AudioResampler *audioResampler;

    AVSync *mediaSync;

private:
    int readPackets();

    int openDecoder(int streamIndex);

    void closeDecoder(int streamIndex);

    int openAudioRender(int64_t wanted_channel_layout, int wanted_nb_channels,
                        int wanted_sample_rate);

    void run() override;

public:
    MediaPlayer();

    virtual ~MediaPlayer();

    status_t reset();

    void setDataSource(const char *url);

    void setVideoRender(VideoRender *render);

    status_t prepare();

    status_t prepareAsync();

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

    const char *getMediaFormat() const;

    AVFormatContext *getMetadata() const;

    void pcmQueueCallback(uint8_t *stream, int len);

    void stop();

};

#endif //MEDIAPLAYER_H

//
// Created by cain on 2018/12/26.
//

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
#include <convertor/AudioResampler.h>


class MediaPlayer : public Runnable {
public:
    MediaPlayer();

    virtual ~MediaPlayer();

    status_t reset();

    void setDataSource(const char *url, int64_t offset = 0);

    void setVideoRender(VideoRender *render);

    status_t prepare();

    status_t prepareAsync();

    void start();

    void pause();

    void resume();

    void stop();

    void seekTo(long timeMs);

    void setVolume(float volume);

    void setMute(int mute);

    void setRate(float rate);

    int getRotate();

    int getVideoWidth();

    int getVideoHeight();

    long getCurrentPosition();

    long getDuration();

    int isPlaying();

    AVMessageQueue *getMessageQueue();

    void pcmQueueCallback(uint8_t *stream, int len);

protected:
    void run() override;

private:
    int readPackets();

    // prepare decoder with stream_index
    int prepareDecoder(int streamIndex);

    // open an audio output device
    int openAudioDevice(int64_t wanted_channel_layout, int wanted_nb_channels,
                        int wanted_sample_rate);

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

};

#endif //MEDIAPLAYER_H

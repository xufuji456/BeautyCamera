
#ifndef FF_MEDIAPLAYER_H
#define FF_MEDIAPLAYER_H

#include <AndroidLog.h>
#include <Mutex.h>
#include <Condition.h>
#include <Thread.h>
#include <player/MediaPlayer.h>

enum media_event_type {
    MEDIA_NOP                = 0,
    MEDIA_PREPARED           = 1,
    MEDIA_RENDER_FIRST_FRAME = 2,
    MEDIA_STARTED            = 3,
    MEDIA_SEEK_COMPLETE      = 4,
    MEDIA_PLAYBACK_COMPLETE  = 5,
    MEDIA_TIMED_TEXT         = 6,
    MEDIA_ERROR              = 7,
    MEDIA_VIDEO_SIZE_CHANGED = 8,
    MEDIA_INFO               = 9
};

enum media_info_type {
    // 0xx
    MEDIA_INFO_UNKNOWN = 1,
    // The player was started because it was used as the next player for another
    // player, which just completed playback
    MEDIA_INFO_STARTED_AS_NEXT = 2,
    // The player just pushed the very first video frame for rendering
    MEDIA_INFO_RENDERING_START = 3,
    // 7xx
    // The video is too complex for the decoder: it can't decode frames m_decodeFastFlag
    // enough. Possibly only the audio plays fine at this stage.
    MEDIA_INFO_VIDEO_TRACK_LAGGING = 700,
    // MediaPlayer is temporarily pausing playback internally in order to
    // buffer more data.
    MEDIA_INFO_BUFFERING_START = 701,
    MEDIA_BUFFERING_UPDATE     = 702,
    MEDIA_INFO_BUFFERING_END   = 703,

    // The media is not seekable (e.g live stream).
    MEDIA_INFO_NOT_SEEKABLE = 801,
    // Audio can not be played.
    MEDIA_INFO_PLAY_AUDIO_ERROR = 804,
    // Video can not be played.
    MEDIA_INFO_PLAY_VIDEO_ERROR = 805,

    //9xx
    MEDIA_INFO_TIMED_TEXT_ERROR = 900,
};


class MediaPlayerListener {
public:
    virtual void notify(int msg, int ext1, int ext2, void *obj) {}
};

class FFMediaPlayer : public Runnable {
    
private:
    Mutex mMutex;
    Condition mCondition;
    Thread *msgThread;
    bool abortRequest;
    NativeWindowVideoRender *videoRender;
    MediaPlayer *mediaPlayer;
    MediaPlayerListener *mListener;

    bool mSeeking;
    long mSeekingPosition;
    bool mPrepareSync;
    status_t mPrepareStatus;

private:
    void postEvent(int what, int arg1, int arg2, void *obj = nullptr);

protected:
    void run() override;

public:
    FFMediaPlayer();

    virtual ~FFMediaPlayer();

    void init();

    void disconnect();

    status_t setDataSource(const char *url);

    status_t setVideoSurface(void* surface);

    status_t setListener(MediaPlayerListener *listener);

    status_t prepare();

    status_t prepareAsync();

    void start();

    void pause();

    void resume();

    bool isPlaying();

    int getRotate();

    int getVideoWidth();

    int getVideoHeight();

    status_t seekTo(long msec);

    long getCurrentPosition();

    long getDuration();

    int selectTrack(int trackId, bool selected);

    status_t setVolume(float volume);

    void setMute(bool mute);

    void setRate(float speed);

    void notify(int msg, int ext1, int ext2, void *obj = nullptr, int len = 0);

    AVStream *getAVStream(int mediaType) const;

    AVFormatContext *getMetadata() const;

    void stop();

    status_t reset();

};

#endif //FF_MEDIAPLAYER_H


#ifndef FFMPEG_PLAYER_H
#define FFMPEG_PLAYER_H

#include <../../log_helper.h>
#include <Thread.h>
#include <player/MediaPlayer.h>

enum media_event_type {
    MEDIA_PREPARED           = 0x01,
    MEDIA_RENDER_FIRST_FRAME = 0x02,
    MEDIA_STARTED            = 0x03,
    MEDIA_SEEK_COMPLETE      = 0x04,
    MEDIA_PLAYBACK_COMPLETE  = 0x05,
    MEDIA_TIMED_TEXT         = 0x06,
    MEDIA_ERROR              = 0x07,
    MEDIA_VIDEO_SIZE_CHANGED = 0x08,
    MEDIA_INFO               = 0x09,
    MEDIA_BUFFERING_START    = 0x0A,
    MEDIA_BUFFERING_UPDATE   = 0x0B,
    MEDIA_BUFFERING_END      = 0x0C
};


class MediaPlayerListener {
public:
    virtual void notify(int msg, int ext1, int ext2, void *obj) {}
};

class FFmpegPlayer : public Runnable {
    
private:
    bool m_exitReq;
    bool m_seeking;
    long m_seekingPos;

    Thread *m_msgThread;

    NativeWindowVideoRender *m_videoRender;
    MediaPlayer *m_mediaPlayer;
    MediaPlayerListener *m_playListener;

private:
    void postEvent(int what, int arg1, int arg2, void *obj = nullptr);

protected:
    void run() override;

public:
    FFmpegPlayer();

    void init();

    void disconnect();

    int setDataSource(const char *url);

    int setVideoSurface(void* surface);

    void setListener(MediaPlayerListener *listener);

    int prepare();

    int prepareAsync();

    void start();

    void pause();

    void resume();

    bool isPlaying();

    int getRotate();

    int getVideoWidth();

    int getVideoHeight();

    void seekTo(long msec);

    long getCurrentPosition();

    long getDuration();

    int selectTrack(int trackId, bool selected);

    void setVolume(float volume);

    void setMute(bool mute);

    void setRate(float speed);

    void notify(int msg, int ext1, int ext2, void *obj = nullptr, int len = 0);

    AVStream *getAVStream(int mediaType) const;

    AVFormatContext *getMetadata() const;

    void stop();

    void reset();

};

#endif //FFMPEG_PLAYER_H

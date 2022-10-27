
#include "FFmpegPlayer.h"
#include <message/FFMessageQueue.h>

FFmpegPlayer::FFmpegPlayer() {
    m_exitReq      = false;
    m_seeking      = false;
    m_seekingPos   = 0;
    m_msgThread    = nullptr;
    m_videoRender  = nullptr;
    m_mediaPlayer  = nullptr;
    m_playListener = nullptr;
}

void FFmpegPlayer::init() {
    if (m_videoRender == nullptr) {
        m_videoRender = new NativeWindowVideoRender();
    }
    if (m_msgThread == nullptr) {
        m_msgThread = new Thread(this);
        m_msgThread->start();
    }
}

void FFmpegPlayer::disconnect() {
    m_exitReq = true;
    reset();

    if (m_msgThread != nullptr) {
        m_msgThread->join();
        delete m_msgThread;
        m_msgThread = nullptr;
    }

    if (m_videoRender != nullptr) {
        delete m_videoRender;
        m_videoRender = nullptr;
    }
    if (m_playListener != nullptr) {
        delete m_playListener;
        m_playListener = nullptr;
    }
}

int FFmpegPlayer::setDataSource(const char *url) {
    if (url == nullptr) {
        return -1;
    }
    if (m_mediaPlayer == nullptr) {
        m_mediaPlayer = new MediaPlayer();
    }
    m_mediaPlayer->setDataSource(url);
    m_mediaPlayer->setVideoRender(m_videoRender);
    return 0;
}

int FFmpegPlayer::setVideoSurface(void *surface) {
    if (m_mediaPlayer == nullptr) {
        return -1;
    }
    if (surface != nullptr) {
        m_videoRender->setSurface(surface);
        return 0;
    }
    return -1;
}

void FFmpegPlayer::setListener(MediaPlayerListener *listener) {
    if (m_playListener != nullptr) {
        delete m_playListener;
        m_playListener = nullptr;
    }
    m_playListener = listener;
}

int FFmpegPlayer::prepare() {
    if (m_mediaPlayer == nullptr) {
        return -1;
    }
    int ret = m_mediaPlayer->prepare();
    return ret;
}

int FFmpegPlayer::prepareAsync() {
    if (m_mediaPlayer != nullptr) {
        return m_mediaPlayer->prepareAsync();
    }
    return -1;
}

void FFmpegPlayer::start() {
    if (m_mediaPlayer != nullptr) {
        m_mediaPlayer->start();
    }
}

void FFmpegPlayer::pause() {
    if (m_mediaPlayer) {
        m_mediaPlayer->pause();
    }
}

void FFmpegPlayer::resume() {
    if (m_mediaPlayer) {
        m_mediaPlayer->resume();
    }
}

bool FFmpegPlayer::isPlaying() {
    if (m_mediaPlayer) {
        return (m_mediaPlayer->isPlaying() != 0);
    }
    return false;
}

int FFmpegPlayer::getRotate() {
    if (m_mediaPlayer != nullptr) {
        return m_mediaPlayer->getRotate();
    }
    return 0;
}

int FFmpegPlayer::getVideoWidth() {
    if (m_mediaPlayer != nullptr) {
        return m_mediaPlayer->getVideoWidth();
    }
    return 0;
}

int FFmpegPlayer::getVideoHeight() {
    if (m_mediaPlayer != nullptr) {
        return m_mediaPlayer->getVideoHeight();
    }
    return 0;
}

void FFmpegPlayer::seekTo(long msec) {
    if (m_mediaPlayer != nullptr) {
        if (m_seeking) {
            m_mediaPlayer->getMessageQueue()->sendMessage(MSG_REQUEST_SEEK, msec);
        } else {
            m_mediaPlayer->seekTo(msec);
            m_seekingPos = (long) msec;
            m_seeking = true;
        }
    }
}

long FFmpegPlayer::getCurrentPosition() {
    if (m_mediaPlayer != nullptr) {
        if (m_seeking) {
            return m_seekingPos;
        }
        return m_mediaPlayer->getCurrentPosition();
    }
    return 0;
}

long FFmpegPlayer::getDuration() {
    if (m_mediaPlayer != nullptr) {
        return m_mediaPlayer->getDuration();
    }
    return -1;
}

int FFmpegPlayer::selectTrack(int trackId, bool selected) {
    if (m_mediaPlayer != nullptr) {
        return m_mediaPlayer->selectTrack(trackId, selected);
    }
    return -1;
}

void FFmpegPlayer::setVolume(float volume) {
    if (m_mediaPlayer != nullptr) {
        m_mediaPlayer->setVolume(volume);
    }
}

void FFmpegPlayer::setMute(bool mute) {
    if (m_mediaPlayer != nullptr) {
        m_mediaPlayer->setMute(mute);
    }
}

void FFmpegPlayer::setRate(float speed) {
    if (m_mediaPlayer != nullptr) {
        m_mediaPlayer->setRate(speed);
    }
}

AVStream *FFmpegPlayer::getAVStream(int mediaType) const {
    return m_mediaPlayer ? m_mediaPlayer->getAVStream(mediaType) : nullptr;
}

AVFormatContext *FFmpegPlayer::getMetadata() const {
    return m_mediaPlayer ? m_mediaPlayer->getMetadata() : nullptr;
}

void FFmpegPlayer::stop() {
    if (m_mediaPlayer) {
        m_mediaPlayer->stop();
    }
}

void FFmpegPlayer::reset() {
    if (m_mediaPlayer != nullptr) {
        m_mediaPlayer->reset();
        delete m_mediaPlayer;
        m_mediaPlayer = nullptr;
    }
}

void FFmpegPlayer::notify(int msg, int ext1, int ext2, void *obj, int len) {
    if (m_mediaPlayer != nullptr) {
        m_mediaPlayer->getMessageQueue()->sendMessage(msg, ext1, ext2, obj, len);
    }
}

void FFmpegPlayer::postEvent(int what, int arg1, int arg2, void *obj) {
    if (m_playListener != nullptr) {
        m_playListener->notify(what, arg1, arg2, obj);
    }
}

void FFmpegPlayer::run() {

    int ret;
    while (true) {

        if (m_exitReq) {
            break;
        }
        if (!m_mediaPlayer || !m_mediaPlayer->getMessageQueue()) {
            av_usleep(10 * 1000);
            continue;
        }

        FFMessage msg;
        ret = m_mediaPlayer->getMessageQueue()->getMessage(&msg);
        if (ret < 0) {
            ALOGE("getMessage error");
            break;
        }

        assert(ret > 0);

        switch (msg.what) {
            case MSG_FLUSH: {
                ALOGD("is flushing");
                break;
            }
            case MSG_ON_ERROR: {
                postEvent(MEDIA_ERROR, msg.arg1, 0);
                break;
            }
            case MSG_ON_PREPARED: {
                postEvent(MEDIA_PREPARED, 0, 0);
                break;
            }
            case MSG_ON_START: {
                postEvent(MEDIA_STARTED, 0, 0);
                break;
            }
            case MSG_ON_COMPLETE: {
                postEvent(MEDIA_PLAYBACK_COMPLETE, 0, 0);
                break;
            }
            case MSG_VIDEO_SIZE_CHANGED: {
                postEvent(MEDIA_VIDEO_SIZE_CHANGED, msg.arg1, msg.arg2);
                break;
            }
            case MSG_VIDEO_RENDER_START: {
                postEvent(MEDIA_RENDER_FIRST_FRAME, 1, 0);
                break;
            }
            case MSG_AUDIO_RENDER_START: {
                postEvent(MEDIA_RENDER_FIRST_FRAME, 0, 1);
                break;
            }
            case MSG_VIDEO_ROTATION_CHANGED: {
                ALOGD("video rotation is changing: %d", msg.arg1);
                break;
            }
            case MSG_AUDIO_DECODE_START: {
                ALOGD("start audio decoder");
                break;
            }
            case MSG_VIDEO_DECODE_START: {
                ALOGD("start video decoder");
                break;
            }
            case MSG_BUFFERING_START: {
                postEvent(MEDIA_INFO, MEDIA_BUFFERING_START, msg.arg1);
                break;
            }
            case MSG_BUFFERING_END: {
                postEvent(MEDIA_INFO, MEDIA_BUFFERING_END, msg.arg1);
                break;
            }
            case MSG_BUFFERING_UPDATE: {
                postEvent(MEDIA_BUFFERING_UPDATE, msg.arg1, msg.arg2);
                break;
            }
            case MSG_BUFFERING_TIME_UPDATE: {
                ALOGD("FFmpegPlayer buffering time update.");
                break;
            }
            case MSG_SEEK_COMPLETE: {
                m_seeking = false;
                postEvent(MEDIA_SEEK_COMPLETE, 0, 0);
                break;
            }
            case MSG_TIMED_TEXT: {
                postEvent(MEDIA_TIMED_TEXT, 0, 0, msg.obj);
                break;
            }
            case MSG_REQUEST_PREPARE: {
                int result = prepare();
                if (result < 0) {
                    ALOGE("prepare error:%d", result);
                }
                break;
            }
            case MSG_REQUEST_START: {
                ALOGD("request to start.");
                break;
            }
            case MSG_REQUEST_PAUSE: {
                pause();
                break;
            }
            case MSG_REQUEST_SEEK: {
                m_seeking = true;
                m_seekingPos = (long) msg.arg1;
                m_mediaPlayer->seekTo(m_seekingPos);
                break;
            }
            default: {
                ALOGE("FFmpegPlayer unknown msg:what=%d\n", msg.what);
                break;
            }
        }
        message_free_ptr(&msg);
    }
}

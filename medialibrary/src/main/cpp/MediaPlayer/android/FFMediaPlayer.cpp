
#include <message/FFMessageQueue.h>
#include "FFMediaPlayer.h"

FFMediaPlayer::FFMediaPlayer() {
    msgThread = nullptr;
    abortRequest = true;
    videoRender = nullptr;
    mediaPlayer = nullptr;
    mListener = nullptr;
    mPrepareSync = false;
    mPrepareStatus = NO_ERROR;
    mSeeking = false;
    mSeekingPosition = 0;
}

FFMediaPlayer::~FFMediaPlayer() {

}

void FFMediaPlayer::init() {

    mMutex.lock();
    abortRequest = false;
    mCondition.signal();
    mMutex.unlock();

    mMutex.lock();
    if (videoRender == nullptr) {
        videoRender = new NativeWindowVideoRender();
    }
    if (msgThread == nullptr) {
        msgThread = new Thread(this);
        msgThread->start();
    }
    mMutex.unlock();
}

void FFMediaPlayer::disconnect() {

    mMutex.lock();
    abortRequest = true;
    mCondition.signal();
    mMutex.unlock();

    reset();

    if (msgThread != nullptr) {
        msgThread->join();
        delete msgThread;
        msgThread = nullptr;
    }

    if (videoRender != nullptr) {
        delete videoRender;
        videoRender = nullptr;
    }
    if (mListener != nullptr) {
        delete mListener;
        mListener = nullptr;
    }

}

status_t FFMediaPlayer::setDataSource(const char *url) {
    if (url == nullptr) {
        return BAD_VALUE;
    }
    if (mediaPlayer == nullptr) {
        mediaPlayer = new MediaPlayer();
    }
    mediaPlayer->setDataSource(url);
    mediaPlayer->setVideoRender(videoRender);
    return NO_ERROR;
}

status_t FFMediaPlayer::setVideoSurface(void *surface) {
    if (mediaPlayer == nullptr) {
        return NO_INIT;
    }
    if (surface != nullptr) {
        videoRender->setSurface(surface);
        return NO_ERROR;
    }
    return BAD_VALUE;
}

status_t FFMediaPlayer::setListener(MediaPlayerListener *listener) {
    if (mListener != nullptr) {
        delete mListener;
        mListener = nullptr;
    }
    mListener = listener;
    return NO_ERROR;
}

status_t FFMediaPlayer::prepare() {
    if (mediaPlayer == nullptr) {
        return NO_INIT;
    }
    if (mPrepareSync) {
        return -EALREADY;
    }
    mPrepareSync = true;
    status_t ret = mediaPlayer->prepare();
    if (ret != NO_ERROR) {
        return ret;
    }
    mPrepareSync = false;
    return mPrepareStatus;
}

status_t FFMediaPlayer::prepareAsync() {
    if (mediaPlayer != nullptr) {
        return mediaPlayer->prepareAsync();
    }
    return INVALID_OPERATION;
}

void FFMediaPlayer::start() {
    if (mediaPlayer != nullptr) {
        mediaPlayer->start();
    }
}

void FFMediaPlayer::pause() {
    if (mediaPlayer) {
        mediaPlayer->pause();
    }
}

void FFMediaPlayer::resume() {
    if (mediaPlayer) {
        mediaPlayer->resume();
    }
}

bool FFMediaPlayer::isPlaying() {
    if (mediaPlayer) {
        return (mediaPlayer->isPlaying() != 0);
    }
    return false;
}

int FFMediaPlayer::getRotate() {
    if (mediaPlayer != nullptr) {
        return mediaPlayer->getRotate();
    }
    return 0;
}

int FFMediaPlayer::getVideoWidth() {
    if (mediaPlayer != nullptr) {
        return mediaPlayer->getVideoWidth();
    }
    return 0;
}

int FFMediaPlayer::getVideoHeight() {
    if (mediaPlayer != nullptr) {
        return mediaPlayer->getVideoHeight();
    }
    return 0;
}

status_t FFMediaPlayer::seekTo(long msec) {
    if (mediaPlayer != nullptr) {
        if (mSeeking) {
            mediaPlayer->getMessageQueue()->sendMessage(MSG_REQUEST_SEEK, msec);
        } else {
            mediaPlayer->seekTo(msec);
            mSeekingPosition = (long) msec;
            mSeeking = true;
        }
    }
    return NO_ERROR;
}

long FFMediaPlayer::getCurrentPosition() {
    if (mediaPlayer != nullptr) {
        if (mSeeking) {
            return mSeekingPosition;
        }
        return mediaPlayer->getCurrentPosition();
    }
    return 0;
}

long FFMediaPlayer::getDuration() {
    if (mediaPlayer != nullptr) {
        return mediaPlayer->getDuration();
    }
    return -1;
}

int FFMediaPlayer::selectTrack(int trackId, bool selected) {
    if (mediaPlayer != nullptr) {
        return mediaPlayer->selectTrack(trackId, selected);
    }
    return -1;
}

status_t FFMediaPlayer::setVolume(float volume) {
    if (mediaPlayer != nullptr) {
        mediaPlayer->setVolume(volume);
    }
    return NO_ERROR;
}

void FFMediaPlayer::setMute(bool mute) {
    if (mediaPlayer != nullptr) {
        mediaPlayer->setMute(mute);
    }
}

void FFMediaPlayer::setRate(float speed) {
    if (mediaPlayer != nullptr) {
        mediaPlayer->setRate(speed);
    }
}

AVStream *FFMediaPlayer::getAVStream(int mediaType) const {
    return mediaPlayer ? mediaPlayer->getAVStream(mediaType) : nullptr;
}

AVFormatContext *FFMediaPlayer::getMetadata() const {
    return mediaPlayer ? mediaPlayer->getMetadata() : nullptr;
}

void FFMediaPlayer::stop() {
    if (mediaPlayer) {
        mediaPlayer->stop();
    }
}

status_t FFMediaPlayer::reset() {
    mPrepareSync = false;
    if (mediaPlayer != nullptr) {
        mediaPlayer->reset();
        delete mediaPlayer;
        mediaPlayer = nullptr;
    }
    return NO_ERROR;
}

void FFMediaPlayer::notify(int msg, int ext1, int ext2, void *obj, int len) {
    if (mediaPlayer != nullptr) {
        mediaPlayer->getMessageQueue()->sendMessage(msg, ext1, ext2, obj, len);
    }
}

void FFMediaPlayer::postEvent(int what, int arg1, int arg2, void *obj) {
    if (mListener != nullptr) {
        mListener->notify(what, arg1, arg2, obj);
    }
}

void FFMediaPlayer::run() {

    int ret;
    while (true) {

        if (abortRequest) {
            break;
        }
        if (!mediaPlayer || !mediaPlayer->getMessageQueue()) {
            av_usleep(10 * 1000);
            continue;
        }

        FFMessage msg;
        ret = mediaPlayer->getMessageQueue()->getMessage(&msg);
        if (ret < 0) {
            ALOGE("getMessage error");
            break;
        }

        assert(ret > 0);

        switch (msg.what) {
            case MSG_FLUSH: {
                ALOGD("FFMediaPlayer is flushing.\n");
                postEvent(MEDIA_NOP, 0, 0);
                break;
            }
            case MSG_ON_ERROR: {
                ALOGD("FFMediaPlayer occurs error: %d\n", msg.arg1);
                if (mPrepareSync) {
                    mPrepareSync = false;
                    mPrepareStatus = msg.arg1;
                }
                postEvent(MEDIA_ERROR, msg.arg1, 0);
                break;
            }
            case MSG_ON_PREPARED: {
                ALOGD("FFMediaPlayer is prepared.\n");
                if (mPrepareSync) {
                    mPrepareSync = false;
                    mPrepareStatus = NO_ERROR;
                }
                postEvent(MEDIA_PREPARED, 0, 0);
                break;
            }
            case MSG_ON_START: {
                ALOGD("FFMediaPlayer is started!");
                postEvent(MEDIA_STARTED, 0, 0);
                break;
            }
            case MSG_ON_COMPLETE: {
                ALOGD("FFMediaPlayer is playback completed.\n");
                postEvent(MEDIA_PLAYBACK_COMPLETE, 0, 0);
                break;
            }
            case MSG_VIDEO_SIZE_CHANGED: {
                ALOGD("FFMediaPlayer is video size changing: %d, %d\n", msg.arg1, msg.arg2);
                postEvent(MEDIA_VIDEO_SIZE_CHANGED, msg.arg1, msg.arg2);
                break;
            }
            case MSG_VIDEO_RENDER_START: {
                ALOGD("FFMediaPlayer is video playing.\n");
                postEvent(MEDIA_RENDER_FIRST_FRAME, 1, 0);
                break;
            }
            case MSG_AUDIO_RENDER_START: {
                ALOGD("FFMediaPlayer is audio playing.\n");
                postEvent(MEDIA_RENDER_FIRST_FRAME, 0, 1);
                break;
            }
            case MSG_VIDEO_ROTATION_CHANGED: {
                ALOGD("FFMediaPlayer's video rotation is changing: %d\n", msg.arg1);
                break;
            }
            case MSG_AUDIO_DECODE_START: {
                ALOGD("FFMediaPlayer starts audio decoder.\n");
                break;
            }
            case MSG_VIDEO_DECODE_START: {
                ALOGD("FFMediaPlayer starts video decoder.\n");
                break;
            }
            case MSG_BUFFERING_START: {
                ALOGD("CanMediaPlayer is buffering start.\n");
                postEvent(MEDIA_INFO, MEDIA_BUFFERING_START, msg.arg1);
                break;
            }
            case MSG_BUFFERING_END: {
                ALOGD("FFMediaPlayer is buffering finish.\n");
                postEvent(MEDIA_INFO, MEDIA_BUFFERING_END, msg.arg1);
                break;
            }
            case MSG_BUFFERING_UPDATE: {
                ALOGD("FFMediaPlayer is buffering: %d, %d", msg.arg1, msg.arg2);
                postEvent(MEDIA_BUFFERING_UPDATE, msg.arg1, msg.arg2);
                break;
            }
            case MSG_BUFFERING_TIME_UPDATE: {
                ALOGD("FFMediaPlayer buffering time update.");
                break;
            }
            case MSG_SEEK_COMPLETE: {
                ALOGD("FFMediaPlayer seeks completed!\n");
                mSeeking = false;
                postEvent(MEDIA_SEEK_COMPLETE, 0, 0);
                break;
            }
            case MSG_TIMED_TEXT: {
                ALOGD("FFMediaPlayer is updating time text.");
                postEvent(MEDIA_TIMED_TEXT, 0, 0, msg.obj);
                break;
            }
            case MSG_REQUEST_PREPARE: {
                ALOGD("FFMediaPlayer is preparing...");
                status_t result = prepare();
                if (result != NO_ERROR) {
                    ALOGE("FFMediaPlayer prepare error:%d", result);
                }
                break;
            }
            case MSG_REQUEST_START: {
                ALOGD("FFMediaPlayer is waiting to start.");
                break;
            }
            case MSG_REQUEST_PAUSE: {
                ALOGD("FFMediaPlayer is pausing...");
                pause();
                break;
            }
            case MSG_REQUEST_SEEK: {
                ALOGD("FFMediaPlayer is seeking...");
                mSeeking = true;
                mSeekingPosition = (long) msg.arg1;
                mediaPlayer->seekTo(mSeekingPosition);
                break;
            }
            default: {
                ALOGE("FFMediaPlayer unknown msg:what=%d\n", msg.what);
                break;
            }
        }
        message_free_ptr(&msg);
    }
}

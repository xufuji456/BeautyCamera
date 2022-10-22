//
// Created by cain on 2019/2/1.
//

#include <player/AVMessageQueue.h>
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

status_t FFMediaPlayer::setDataSource(const char *url, int64_t offset) {
    if (url == nullptr) {
        return BAD_VALUE;
    }
    if (mediaPlayer == nullptr) {
        mediaPlayer = new MediaPlayer();
    }
    mediaPlayer->setDataSource(url, offset);
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

void FFMediaPlayer::stop() {
    if (mediaPlayer) {
        mediaPlayer->stop();
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

status_t FFMediaPlayer::seekTo(float msec) {
    if (mediaPlayer != nullptr) {
        // if in seeking state, put seek message in queue, to process after preview seeking.
        if (mSeeking) {
            mediaPlayer->getMessageQueue()->postMessage(MSG_REQUEST_SEEK, msec);
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

status_t FFMediaPlayer::reset() {
    mPrepareSync = false;
    if (mediaPlayer != nullptr) {
        mediaPlayer->reset();
        delete mediaPlayer;
        mediaPlayer = nullptr;
    }
    return NO_ERROR;
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

void FFMediaPlayer::notify(int msg, int ext1, int ext2, void *obj, int len) {
    if (mediaPlayer != nullptr) {
        mediaPlayer->getMessageQueue()->postMessage(msg, ext1, ext2, obj, len);
    }
}

void FFMediaPlayer::postEvent(int what, int arg1, int arg2, void *obj) {
    if (mListener != nullptr) {
        mListener->notify(what, arg1, arg2, obj);
    }
}

void FFMediaPlayer::run() {

    int retval;
    while (true) {

        if (abortRequest) {
            break;
        }

        // 如果此时播放器还没准备好，则睡眠10毫秒，等待播放器初始化
        if (!mediaPlayer || !mediaPlayer->getMessageQueue()) {
            av_usleep(10 * 1000);
            continue;
        }

        AVMessage msg;
        retval = mediaPlayer->getMessageQueue()->getMessage(&msg);
        if (retval < 0) {
            ALOGE("getMessage error");
            break;
        }

        assert(retval > 0);

        switch (msg.what) {
            case MSG_FLUSH: {
                ALOGD("FFMediaPlayer is flushing.\n");
                postEvent(MEDIA_NOP, 0, 0);
                break;
            }

            case MSG_ERROR: {
                ALOGD("FFMediaPlayer occurs error: %d\n", msg.arg1);
                if (mPrepareSync) {
                    mPrepareSync = false;
                    mPrepareStatus = msg.arg1;
                }
                postEvent(MEDIA_ERROR, msg.arg1, 0);
                break;
            }

            case MSG_PREPARED: {
                ALOGD("FFMediaPlayer is prepared.\n");
                if (mPrepareSync) {
                    mPrepareSync = false;
                    mPrepareStatus = NO_ERROR;
                }
                postEvent(MEDIA_PREPARED, 0, 0);
                break;
            }

            case MSG_STARTED: {
                ALOGD("FFMediaPlayer is started!");
                postEvent(MEDIA_STARTED, 0, 0);
                break;
            }

            case MSG_COMPLETED: {
                ALOGD("FFMediaPlayer is playback completed.\n");
                postEvent(MEDIA_PLAYBACK_COMPLETE, 0, 0);
                break;
            }

            case MSG_VIDEO_SIZE_CHANGED: {
                ALOGD("FFMediaPlayer is video size changing: %d, %d\n", msg.arg1, msg.arg2);
                postEvent(MEDIA_SET_VIDEO_SIZE, msg.arg1, msg.arg2);
                break;
            }

            case MSG_SAR_CHANGED: {
                ALOGD("FFMediaPlayer is sar changing: %d, %d\n", msg.arg1, msg.arg2);
                postEvent(MEDIA_SET_VIDEO_SAR, msg.arg1, msg.arg2);
                break;
            }

            case MSG_VIDEO_RENDERING_START: {
                ALOGD("FFMediaPlayer is video playing.\n");
                break;
            }

            case MSG_AUDIO_RENDERING_START: {
                ALOGD("FFMediaPlayer is audio playing.\n");
                break;
            }

            case MSG_VIDEO_ROTATION_CHANGED: {
                ALOGD("FFMediaPlayer's video rotation is changing: %d\n", msg.arg1);
                break;
            }

            case MSG_AUDIO_START: {
                ALOGD("FFMediaPlayer starts audio decoder.\n");
                break;
            }

            case MSG_VIDEO_START: {
                ALOGD("FFMediaPlayer starts video decoder.\n");
                break;
            }

            case MSG_OPEN_INPUT: {
                ALOGD("FFMediaPlayer is opening input file.\n");
                break;
            }

            case MSG_FIND_STREAM_INFO: {
                ALOGD("CanMediaPlayer is finding media stream info.\n");
                break;
            }

            case MSG_BUFFERING_START: {
                ALOGD("CanMediaPlayer is buffering start.\n");
                postEvent(MEDIA_INFO, MEDIA_INFO_BUFFERING_START, msg.arg1);
                break;
            }

            case MSG_BUFFERING_END: {
                ALOGD("FFMediaPlayer is buffering finish.\n");
                postEvent(MEDIA_INFO, MEDIA_INFO_BUFFERING_END, msg.arg1);
                break;
            }

            case MSG_BUFFERING_UPDATE: {
                ALOGD("FFMediaPlayer is buffering: %d, %d", msg.arg1, msg.arg2);
                postEvent(MEDIA_BUFFERING_UPDATE, msg.arg1, msg.arg2);
                break;
            }

            case MSG_BUFFERING_TIME_UPDATE: {
                ALOGD("FFMediaPlayer time text update");
                break;
            }

            case MSG_SEEK_COMPLETE: {
                ALOGD("FFMediaPlayer seeks completed!\n");
                mSeeking = false;
                postEvent(MEDIA_SEEK_COMPLETE, 0, 0);
                break;
            }

            case MSG_PLAYBACK_STATE_CHANGED: {
                ALOGD("FFMediaPlayer's playback state is changed.");
                break;
            }

            case MSG_TIMED_TEXT: {
                ALOGD("FFMediaPlayer is updating time text");
                postEvent(MEDIA_TIMED_TEXT, 0, 0, msg.obj);
                break;
            }

            case MSG_REQUEST_PREPARE: {
                ALOGD("FFMediaPlayer is preparing...");
                status_t ret = prepare();
                if (ret != NO_ERROR) {
                    ALOGE("FFMediaPlayer prepare error - '%d'", ret);
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

            case MSG_CURRENT_POSITON: {
                postEvent(MEDIA_CURRENT, msg.arg1, msg.arg2);
                break;
            }

            default: {
                ALOGE("FFMediaPlayer unknown MSG_xxx(%d)\n", msg.what);
                break;
            }
        }
        message_free_resouce(&msg);
    }
}

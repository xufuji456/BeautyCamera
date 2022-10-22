//
// Created by cain on 2018/12/30.
//

#include "MediaSync.h"

MediaSync::MediaSync(PlayerState *playerState) {
    this->playerState = playerState;
    audioDecoder = nullptr;
    videoDecoder = nullptr;
    audioClock = new MediaClock();
    videoClock = new MediaClock();
    extClock = new MediaClock();

    mExit = true;
    abortRequest = true;
    syncThread = nullptr;

    forceRefresh = 0;
    maxFrameDuration = 10.0;
    frameTimerRefresh = 1;
    frameTimer = 0;


    videoRender = nullptr;
    swsContext = nullptr;
    mBuffer = nullptr;
    pFrameARGB = nullptr;
}

MediaSync::~MediaSync() {

}

void MediaSync::reset() {
    stop();
    playerState = nullptr;
    videoDecoder = nullptr;
    audioDecoder = nullptr;
    videoRender = nullptr;

    if (pFrameARGB) {
        av_frame_free(&pFrameARGB);
        av_free(pFrameARGB);
        pFrameARGB = nullptr;
    }
    if (mBuffer) {
        av_freep(&mBuffer);
        mBuffer = nullptr;
    }
    if (swsContext) {
        sws_freeContext(swsContext);
        swsContext = nullptr;
    }
}

void MediaSync::start(VideoDecoder *videoDecoder, AudioDecoder *audioDecoder) {
    mMutex.lock();
    this->videoDecoder = videoDecoder;
    this->audioDecoder = audioDecoder;
    abortRequest = false;
    mExit = false;
    mCondition.signal();
    mMutex.unlock();
    if (videoDecoder && !syncThread) {
        syncThread = new Thread(this);
        syncThread->start();
    }
}

void MediaSync::stop() {
    mMutex.lock();
    abortRequest = true;
    mCondition.signal();
    mMutex.unlock();

    mMutex.lock();
    while (!mExit) {
        mCondition.wait(mMutex);
    }
    mMutex.unlock();
    if (syncThread) {
        syncThread->join();
        delete syncThread;
        syncThread = nullptr;
    }
}

void MediaSync::setVideoRender(VideoRender *render) {
    Mutex::Autolock lock(mMutex);
    this->videoRender = render;
}

void MediaSync::setMaxDuration(double maxDuration) {
    this->maxFrameDuration = maxDuration;
}

void MediaSync::refreshVideoTimer() {
    mMutex.lock();
    this->frameTimerRefresh = 1;
    mCondition.signal();
    mMutex.unlock();
}

void MediaSync::updateAudioClock(double pts, double time) {
    audioClock->setClock(pts, time);
    extClock->syncToSlave(audioClock);
}

double MediaSync::getAudioDiffClock() {
    return audioClock->getClock() - getMasterClock();
}

void MediaSync::updateExternalClock(double pts) {
    extClock->setClock(pts);
}

double MediaSync::getMasterClock() {
    double val = 0;
    switch (playerState->syncType) {
        case AV_SYNC_VIDEO: {
            val = videoClock->getClock();
            break;
        }
        case AV_SYNC_AUDIO: {
            val = audioClock->getClock();
            break;
        }
        case AV_SYNC_EXTERNAL: {
            val = extClock->getClock();
            break;
        }
    }
    return val;
}

MediaClock* MediaSync::getAudioClock() {
    return audioClock;
}

MediaClock *MediaSync::getVideoClock() {
    return videoClock;
}

MediaClock *MediaSync::getExternalClock() {
    return extClock;
}

void MediaSync::run() {
    double remaining_time = 0.0;
    while (true) {

        if (abortRequest || playerState->abortRequest) {
            if (videoRender != nullptr) {
                videoRender->onDestroy();
            }
            break;
        }

        // 处于暂停状态，则睡眠一定的时间
        if (playerState->pauseRequest) {
            av_usleep((int64_t) (REFRESH_RATE * 1000000.0));
        }

        // 是否立马刷新
        if (!playerState->pauseRequest || forceRefresh) {
            refreshVideo(&remaining_time);
        }
        if (remaining_time <= 0) {
            remaining_time = REFRESH_RATE;
        }
    }

    mExit = true;
    mCondition.signal();
}

void MediaSync::refreshVideo(double *remaining_time) {
    double time;

    // 检查外部时钟
    if (!playerState->pauseRequest &&
        playerState->syncType == AV_SYNC_EXTERNAL) {
        checkExternalClockSpeed();
    }

    for (;;) {

        if (playerState->abortRequest || !videoDecoder) {
            break;
        }

        // 判断是否存在帧队列是否存在数据
        if (videoDecoder->getFrameSize() > 0) {
            double lastDuration, duration, delay;
            Frame *currentFrame, *lastFrame;
            // 上一帧
            lastFrame = videoDecoder->getFrameQueue()->lastFrame();
            // 当前帧
            currentFrame = videoDecoder->getFrameQueue()->currentFrame();
            // 判断是否需要强制更新帧的时间
            if (frameTimerRefresh) {
                frameTimer = av_gettime_relative() / 1000000.0;
                frameTimerRefresh = 0;
            }

            // 如果处于暂停状态，则直接显示
            if (playerState->abortRequest || playerState->pauseRequest) {
                break;
            }

            // 计算上一次显示时长
            lastDuration = calculateDuration(lastFrame, currentFrame);
            // 根据上一次显示的时长，计算延时
            delay = calculateDelay(lastDuration);
            // 处理超过延时阈值的情况
            if (fabs(delay) > AV_SYNC_THRESHOLD_MAX) {
                if (delay > 0) {
                    delay = AV_SYNC_THRESHOLD_MAX;
                } else {
                    delay = 0;
                }
            }
            // 获取当前时间
            time = av_gettime_relative() / 1000000.0;
            if (isnan(frameTimer) || time < frameTimer) {
                frameTimer = time;
            }
            // 如果当前时间小于帧计时器的时间 + 延时时间，则表示还没到当前帧
            if (time < frameTimer + delay) {
                *remaining_time = FFMIN(frameTimer + delay - time, *remaining_time);
                break;
            }

            // 更新帧计时器
            frameTimer += delay;
            // 帧计时器落后当前时间超过了阈值，则用当前的时间作为帧计时器时间
            if (delay > 0 && time - frameTimer > AV_SYNC_THRESHOLD_MAX) {
                frameTimer = time;
            }

            // 更新视频时钟的pts
            mMutex.lock();
            if (!isnan(currentFrame->pts)) {
                videoClock->setClock(currentFrame->pts);
                extClock->syncToSlave(videoClock);
            }
            mMutex.unlock();

            // 如果队列中还剩余超过一帧的数据时，需要拿到下一帧，然后计算间隔，并判断是否需要进行舍帧操作
            if (videoDecoder->getFrameSize() > 1) {
                Frame *nextFrame = videoDecoder->getFrameQueue()->nextFrame();
                duration = calculateDuration(currentFrame, nextFrame);
                // 如果不处于同步到视频状态，并且处于跳帧状态，则跳过当前帧
                if ((time > frameTimer + duration)
                    && (playerState->frameDrop > 0
                        || (playerState->frameDrop && playerState->syncType != AV_SYNC_VIDEO))) {
                    videoDecoder->getFrameQueue()->popFrame();
                    continue;
                }
            }

            // 下一帧
            videoDecoder->getFrameQueue()->popFrame();
            forceRefresh = 1;
        }

        break;
    }

    // 回调当前时长
    if (playerState->messageQueue && playerState->syncType == AV_SYNC_VIDEO) {
        // 起始延时
        int64_t start_time = videoDecoder->getFormatContext()->start_time;
        int64_t start_diff = 0;
        if (start_time > 0 && start_time != AV_NOPTS_VALUE) {
            start_diff = av_rescale(start_time, 1000, AV_TIME_BASE);
        }
        // 计算主时钟的时间
        int64_t pos = 0;
        double clock = getMasterClock();
        if (isnan(clock)) {
            pos = playerState->seekPos;
        } else {
            pos = (int64_t)(clock * 1000);
        }
        if (pos < 0 || pos < start_diff) {
            pos = 0;
        }
        pos = (long) (pos - start_diff);
        if (playerState->videoDuration < 0) {
            pos = 0;
        }
        playerState->messageQueue->postMessage(MSG_CURRENT_POSITON, pos, playerState->videoDuration);
    }

    // 显示画面
    if (!playerState->displayDisable && forceRefresh && videoDecoder
        && videoDecoder->getFrameQueue()->getShowIndex()) {
        renderVideo();
    }
    forceRefresh = 0;
}

void MediaSync::checkExternalClockSpeed() {
    if (videoDecoder && videoDecoder->getPacketSize() <= EXTERNAL_CLOCK_MIN_FRAMES
        || audioDecoder && audioDecoder->getPacketSize() <= EXTERNAL_CLOCK_MIN_FRAMES) {
        extClock->setSpeed(FFMAX(EXTERNAL_CLOCK_SPEED_MIN,
                                 extClock->getSpeed() - EXTERNAL_CLOCK_SPEED_STEP));
    } else if ((!videoDecoder || videoDecoder->getPacketSize() > EXTERNAL_CLOCK_MAX_FRAMES)
               && (!audioDecoder || audioDecoder->getPacketSize() > EXTERNAL_CLOCK_MAX_FRAMES)) {
        extClock->setSpeed(FFMIN(EXTERNAL_CLOCK_SPEED_MAX,
                                 extClock->getSpeed() + EXTERNAL_CLOCK_SPEED_STEP));
    } else {
        double speed = extClock->getSpeed();
        if (speed != 1.0) {
            extClock->setSpeed(speed + EXTERNAL_CLOCK_SPEED_STEP * (1.0 - speed) / fabs(1.0 - speed));
        }
    }
}

double MediaSync::calculateDelay(double delay)  {
    double sync_threshold, diff = 0;
    // 如果不是同步到视频流，则需要计算延时时间
    if (playerState->syncType != AV_SYNC_VIDEO) {
        // 计算差值
        diff = videoClock->getClock() - getMasterClock();
        // 用差值与同步阈值计算延时
        sync_threshold = FFMAX(AV_SYNC_THRESHOLD_MIN, FFMIN(AV_SYNC_THRESHOLD_MAX, delay));
        if (!isnan(diff) && fabs(diff) < maxFrameDuration) {
            if (diff <= -sync_threshold) {
                delay = FFMAX(0, delay + diff);
            } else if (diff >= sync_threshold && delay > AV_SYNC_FRAMEDUP_THRESHOLD) {
                delay = delay + diff;
            } else if (diff >= sync_threshold) {
                delay = 2 * delay;
            }
        }
    }

    av_log(nullptr, AV_LOG_TRACE, "video: delay=%0.3f A-V=%f\n", delay, -diff);

    return delay;
}

double MediaSync::calculateDuration(Frame *vp, Frame *nextvp) {
    double duration = nextvp->pts - vp->pts;
    if (isnan(duration) || duration <= 0 || duration > maxFrameDuration) {
        return vp->duration;
    } else {
        return duration;
    }
}

void MediaSync::renderVideo() {
    mMutex.lock();
    if (!videoDecoder || !videoRender) {
        mMutex.unlock();
        return;
    }
    Frame *vp = videoDecoder->getFrameQueue()->lastFrame();
    int ret = 0;
    if (!vp->uploaded) {

        swsContext = sws_getCachedContext(swsContext,
                                          vp->frame->width, vp->frame->height,
                                          (AVPixelFormat) vp->frame->format,
                                          vp->frame->width, vp->frame->height,
                                          AV_PIX_FMT_RGBA, SWS_BILINEAR, nullptr, nullptr, nullptr);
        if (!mBuffer) {
            videoRender->onInit(vp->width, vp->height);
            int numBytes = av_image_get_buffer_size(AV_PIX_FMT_RGBA, vp->frame->width, vp->frame->height, 1);
            mBuffer = (uint8_t *)av_malloc(numBytes * sizeof(uint8_t));
            pFrameARGB = av_frame_alloc();
            av_image_fill_arrays(pFrameARGB->data, pFrameARGB->linesize, mBuffer, AV_PIX_FMT_RGBA,
                                 vp->frame->width, vp->frame->height, 1);
        }
        if (swsContext != nullptr) {
            sws_scale(swsContext, (uint8_t const *const *) vp->frame->data,
                      vp->frame->linesize, 0, vp->frame->height,
                      pFrameARGB->data, pFrameARGB->linesize);
        }
        ret = videoRender->onRender(pFrameARGB->data[0], pFrameARGB->linesize[0], vp->frame->height);
        if (ret < 0) {
            return;
        }

        vp->uploaded = 1;
    }

    mMutex.unlock();
}

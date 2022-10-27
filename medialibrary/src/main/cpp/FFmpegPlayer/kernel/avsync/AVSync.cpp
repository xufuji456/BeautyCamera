
#include "AVSync.h"

AVSync::AVSync(PlayerParam *playerParam) {
    this->m_playerParam = playerParam;

    m_audioClock    = new MediaClock();
    m_videoClock    = new MediaClock();
    m_externalClock = new MediaClock();

    m_exit     = true;
    m_abortReq = true;

    m_frameTimer       = 0;
    m_forceRefresh     = 0;
    m_timerRefresh     = 1;
    m_maxFrameDuration = 10.0;

    m_buffer       = nullptr;
    m_frameRGBA    = nullptr;
    m_swsContext   = nullptr;
    m_syncThread   = nullptr;
    m_videoRender  = nullptr;
    m_audioDecoder = nullptr;
    m_videoDecoder = nullptr;

}

AVSync::~AVSync() {

}

void AVSync::reset() {
    stop();

    if (m_buffer) {
        av_freep(&m_buffer);
        m_buffer = nullptr;
    }
    if (m_frameRGBA) {
        av_frame_free(&m_frameRGBA);
        m_frameRGBA = nullptr;
    }
    if (m_swsContext) {
        sws_freeContext(m_swsContext);
        m_swsContext = nullptr;
    }
}

void AVSync::start(VideoDecoder *videoDecoder, AudioDecoder *audioDecoder) {
    m_syncMutex.lock();
    this->m_videoDecoder = videoDecoder;
    this->m_audioDecoder = audioDecoder;
    m_exit     = false;
    m_abortReq = false;
    m_syncCond.signal();
    m_syncMutex.unlock();
    if (videoDecoder && !m_syncThread) {
        m_syncThread = new Thread(this);
        m_syncThread->start();
    }
}

void AVSync::setVideoRender(VideoRender *render) {
    Mutex::AutoLock lock(m_syncMutex);
    this->m_videoRender = render;
}

void AVSync::setAudioDecoder(AudioDecoder *audioDecoder) {
    this->m_audioDecoder = audioDecoder;
}

void AVSync::setMaxDuration(double maxDuration) {
    this->m_maxFrameDuration = maxDuration;
}

void AVSync::refreshVideoTimer() {
    m_syncMutex.lock();
    this->m_timerRefresh = 1;
    m_syncCond.signal();
    m_syncMutex.unlock();
}

void AVSync::updateAudioClock(double pts, double time) {
    m_audioClock->setClock(pts, time);
    m_externalClock->syncToSlave(m_audioClock);
}

double AVSync::getAudioDiffClock() {
    return m_audioClock->getClock() - getMasterClock();
}

void AVSync::updateExternalClock(double pts) {
    m_externalClock->setClock(pts);
}

double AVSync::getMasterClock() {
    double val = 0;
    switch (m_playerParam->m_syncType) {
        case AV_SYNC_VIDEO: {
            val = m_videoClock->getClock();
            break;
        }
        case AV_SYNC_AUDIO: {
            val = m_audioClock->getClock();
            break;
        }
        case AV_SYNC_EXTERNAL: {
            val = m_externalClock->getClock();
            break;
        }
    }
    return val;
}

MediaClock* AVSync::getAudioClock() {
    return m_audioClock;
}

MediaClock *AVSync::getVideoClock() {
    return m_videoClock;
}

MediaClock *AVSync::getExternalClock() {
    return m_externalClock;
}

void AVSync::refreshVideo(double *remaining_time) {
    double time;

    if (!m_playerParam->m_pauseReq &&
        m_playerParam->m_syncType == AV_SYNC_EXTERNAL) {
        checkExternalClockSpeed();
    }

    for (;;) {

        if (m_playerParam->m_abortReq || !m_videoDecoder) {
            break;
        }

        if (m_videoDecoder->getFrameSize() > 0) {
            double lastDuration, duration, delay;
            Frame *currentFrame, *lastFrame;
            lastFrame = m_videoDecoder->getFrameQueue()->lastFrame();
            currentFrame = m_videoDecoder->getFrameQueue()->currentFrame();

            if (m_timerRefresh) {
                m_frameTimer = av_gettime_relative() / 1000000.0;
                m_timerRefresh = 0;
            }

            if (m_playerParam->m_abortReq || m_playerParam->m_pauseReq) {
                break;
            }
            // use last m_frame and current m_frame to calculate m_duration
            lastDuration = calculateDuration(lastFrame, currentFrame);

            delay = calculateDelay(lastDuration);

            if (fabs(delay) > AV_SYNC_THRESHOLD_MAX) {
                if (delay > 0) {
                    delay = AV_SYNC_THRESHOLD_MAX;
                } else {
                    delay = 0;
                }
            }

            time = av_gettime_relative() / 1000000.0;
            if (isnan(m_frameTimer) || time < m_frameTimer) {
                m_frameTimer = time;
            }
            // video m_decodeFastFlag, just wait for rendering
            if (time < m_frameTimer + delay) {
                *remaining_time = FFMIN(m_frameTimer + delay - time, *remaining_time);
                break;
            }

            // update m_frame timer
            m_frameTimer += delay;
            if (delay > 0 && time - m_frameTimer > AV_SYNC_THRESHOLD_MAX) {
                m_frameTimer = time;
            }

            // update pts of video clock
            m_syncMutex.lock();
            if (!isnan(currentFrame->pts)) {
                m_videoClock->setClock(currentFrame->pts);
                m_externalClock->syncToSlave(m_videoClock);
            }
            m_syncMutex.unlock();

            if (m_videoDecoder->getFrameSize() > 1) {
                Frame *nextFrame = m_videoDecoder->getFrameQueue()->nextFrame();
                duration = calculateDuration(currentFrame, nextFrame);

                if ((time > m_frameTimer + duration)
                    && (m_playerParam->m_frameDrop > 0
                        || (m_playerParam->m_frameDrop && m_playerParam->m_syncType != AV_SYNC_VIDEO))) {
                    m_videoDecoder->getFrameQueue()->popFrame();
                    continue;
                }
            }

            m_videoDecoder->getFrameQueue()->popFrame();
            m_forceRefresh = 1;
        }
        break;
    }

    // refresh display
    if (!m_playerParam->m_displayDisable && m_forceRefresh && m_videoDecoder
        && m_videoDecoder->getFrameQueue()->getShowIndex()) {
        renderVideo();
    }
    m_forceRefresh = 0;
}

void AVSync::checkExternalClockSpeed() {
    if (m_videoDecoder && m_videoDecoder->getPacketSize() <= EXTERNAL_CLOCK_MIN_FRAMES
        || m_audioDecoder && m_audioDecoder->getPacketSize() <= EXTERNAL_CLOCK_MIN_FRAMES) {
        m_externalClock->setSpeed(FFMAX(EXTERNAL_CLOCK_SPEED_MIN,
                                        m_externalClock->getSpeed() - EXTERNAL_CLOCK_SPEED_STEP));
    } else if ((!m_videoDecoder || m_videoDecoder->getPacketSize() > EXTERNAL_CLOCK_MAX_FRAMES)
               && (!m_audioDecoder || m_audioDecoder->getPacketSize() > EXTERNAL_CLOCK_MAX_FRAMES)) {
        m_externalClock->setSpeed(FFMIN(EXTERNAL_CLOCK_SPEED_MAX,
                                        m_externalClock->getSpeed() + EXTERNAL_CLOCK_SPEED_STEP));
    } else {
        double speed = m_externalClock->getSpeed();
        if (speed != 1.0) {
            m_externalClock->setSpeed(speed + EXTERNAL_CLOCK_SPEED_STEP * (1.0 - speed) / fabs(1.0 - speed));
        }
    }
}

double AVSync::calculateDelay(double delay)  {
    double diff = 0;
    double sync_threshold = 0;

    if (m_playerParam->m_syncType != AV_SYNC_VIDEO) {
        // diff between video and master clock
        diff = m_videoClock->getClock() - getMasterClock();
        sync_threshold = FFMAX(AV_SYNC_THRESHOLD_MIN, FFMIN(AV_SYNC_THRESHOLD_MAX, delay));
        if (!isnan(diff) && fabs(diff) < m_maxFrameDuration) {
            if (diff <= -sync_threshold) {
                delay = FFMAX(0, delay + diff);
            } else if (diff >= sync_threshold && delay > AV_SYNC_FRAMEDUP_THRESHOLD) {
                delay = delay + diff;
            } else if (diff >= sync_threshold) {
                delay = 2 * delay;
            }
        }
    }

    return delay;
}

double AVSync::calculateDuration(Frame *vp, Frame *nextvp) {
    double duration = nextvp->pts - vp->pts;
    if (isnan(duration) || duration <= 0 || duration > m_maxFrameDuration) {
        return vp->duration;
    } else {
        return duration;
    }
}

void AVSync::renderVideo() {
    m_syncMutex.lock();
    if (!m_videoDecoder || !m_videoRender) {
        m_syncMutex.unlock();
        return;
    }
    Frame *vp = m_videoDecoder->getFrameQueue()->lastFrame();
    int ret = 0;
    if (!vp->uploaded) {

        m_swsContext = sws_getCachedContext(m_swsContext,
                                            vp->frame->width, vp->frame->height,
                                            (AVPixelFormat) vp->frame->format,
                                            vp->frame->width, vp->frame->height,
                                            AV_PIX_FMT_RGBA, SWS_BILINEAR, nullptr, nullptr, nullptr);
        if (!m_buffer) {
            m_videoRender->onInit(vp->width, vp->height);
            int numBytes = av_image_get_buffer_size(AV_PIX_FMT_RGBA, vp->frame->width, vp->frame->height, 1);
            m_buffer = (uint8_t *)av_malloc(numBytes * sizeof(uint8_t));
            m_frameRGBA = av_frame_alloc();
            av_image_fill_arrays(m_frameRGBA->data, m_frameRGBA->linesize, m_buffer, AV_PIX_FMT_RGBA,
                                 vp->frame->width, vp->frame->height, 1);
        }
        if (m_swsContext != nullptr) {
            sws_scale(m_swsContext, (uint8_t const *const *) vp->frame->data,
                      vp->frame->linesize, 0, vp->frame->height,
                      m_frameRGBA->data, m_frameRGBA->linesize);
        }
        ret = m_videoRender->onRender(m_frameRGBA->data[0], m_frameRGBA->linesize[0], vp->frame->height);
        if (ret < 0) {
            return;
        }
        if (!m_playerParam->m_firstVideoFrame && m_playerParam->m_messageQueue) {
            m_playerParam->m_firstVideoFrame = true;
            m_playerParam->m_messageQueue->sendMessage(MSG_VIDEO_RENDER_START);
        }

        vp->uploaded = 1;
    }

    m_syncMutex.unlock();
}

void AVSync::run() {
    double remaining_time = 0.0;
    while (true) {

        if (m_abortReq || m_playerParam->m_abortReq) {
            if (m_videoRender != nullptr) {
                m_videoRender->onDestroy();
            }
            break;
        }

        if (m_playerParam->m_pauseReq) {
            av_usleep((int64_t) (REFRESH_RATE * 1000000.0));
        }

        if (!m_playerParam->m_pauseReq || m_forceRefresh) {
            refreshVideo(&remaining_time);
        }
        if (remaining_time <= 0) {
            remaining_time = REFRESH_RATE;
        }
    }

    m_exit = true;
    m_syncCond.signal();
}

void AVSync::stop() {
    m_syncMutex.lock();
    m_abortReq = true;
    m_syncCond.signal();
    m_syncMutex.unlock();

    m_syncMutex.lock();
    while (!m_exit) {
        m_syncCond.wait(m_syncMutex);
    }
    m_syncMutex.unlock();
    if (m_syncThread) {
        m_syncThread->join();
        delete m_syncThread;
        m_syncThread = nullptr;
    }
}

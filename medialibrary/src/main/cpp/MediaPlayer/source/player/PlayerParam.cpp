//
// Created by cain on 2019/1/26.
//

#include <AndroidLog.h>
#include "PlayerParam.h"

PlayerParam::PlayerParam() {
    init();
    reset();
}

PlayerParam::~PlayerParam() {
    reset();
    if (messageQueue) {
        messageQueue->release();
        delete messageQueue;
        messageQueue = nullptr;
    }
}

void PlayerParam::init() {
    url                = nullptr;
    iformat            = nullptr;
    audioCodecName     = nullptr;
    videoCodecName     = nullptr;
    m_videoStream      = nullptr;
    m_audioStream      = nullptr;
    m_subtitleStream   = nullptr;
    m_audioCodecCtx    = nullptr;
    m_videoCodecCtx    = nullptr;
    m_subtitleCodecCtx = nullptr;
    messageQueue       = new FFMessageQueue();
}

void PlayerParam::reset() {
    if (url) {
        av_freep(&url);
        url = nullptr;
    }
    loop = 1;
    mute = 0;
    fast = 0;
    offset = 0;
    seekPos = 0;
    seekFlags = 0;
    frameDrop = 1;
    seekRequest = 0;
    abortRequest = 1;
    pauseRequest = 1;
    audioDisable = 0;
    videoDisable = 0;
    videoDuration = 0;
    m_videoIndex = -1;
    m_audioIndex = -1;
    displayDisable = 0;
    playbackRate = 1.0;
    m_subtitleIndex = -1;
    reorderVideoPts = -1;
    syncType = AV_SYNC_AUDIO;
    duration = AV_NOPTS_VALUE;
    startTime = AV_NOPTS_VALUE;
}

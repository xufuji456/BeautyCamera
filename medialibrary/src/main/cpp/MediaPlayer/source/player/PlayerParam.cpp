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
    url            = nullptr;
    iformat        = nullptr;
    audioCodecName = nullptr;
    videoCodecName = nullptr;
    messageQueue   = new AVMessageQueue();
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
    syncType = AV_SYNC_AUDIO;
    startTime = AV_NOPTS_VALUE;
    duration = AV_NOPTS_VALUE;
    audioDisable = 0;
    videoDisable = 0;
    displayDisable = 0;
    playbackRate = 1.0;
    reorderVideoPts = -1;
    videoDuration = 0;
    m_videoIndex = -1;
    m_audioIndex = -1;
    m_subtitleIndex = -1;
}

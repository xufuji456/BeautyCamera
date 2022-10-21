//
// Created by cain on 2019/1/26.
//

#include <AndroidLog.h>
#include "PlayerState.h"

PlayerState::PlayerState() {
    init();
    reset();
}

PlayerState::~PlayerState() {
    reset();
    if (messageQueue) {
        messageQueue->release();
        delete messageQueue;
        messageQueue = nullptr;
    }
}

void PlayerState::init() {
    url            = nullptr;
    iformat        = nullptr;
    audioCodecName = nullptr;
    videoCodecName = nullptr;
    messageQueue   = new AVMessageQueue();
}

void PlayerState::reset() {
    if (url) {
        av_freep(&url);
        url = nullptr;
    }
    offset = 0;
    abortRequest = 1;
    pauseRequest = 1;
    syncType = AV_SYNC_AUDIO;
    startTime = AV_NOPTS_VALUE;
    duration = AV_NOPTS_VALUE;
    realTime = 0;
    audioDisable = 0;
    videoDisable = 0;
    displayDisable = 0;
    fast = 0;
    playbackRate = 1.0;
    seekRequest = 0;
    seekFlags = 0;
    seekPos = 0;
    loop = 1;
    mute = 0;
    frameDrop = 1;
    reorderVideoPts = -1;
    videoDuration = 0;
}

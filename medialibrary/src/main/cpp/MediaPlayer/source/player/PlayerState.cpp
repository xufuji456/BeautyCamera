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
    sws_dict = (AVDictionary *) malloc(sizeof(AVDictionary));
    memset(sws_dict, 0, sizeof(AVDictionary));
    swr_opts = (AVDictionary *) malloc(sizeof(AVDictionary));
    memset(swr_opts, 0, sizeof(AVDictionary));
    format_opts = (AVDictionary *) malloc(sizeof(AVDictionary));
    memset(format_opts, 0, sizeof(AVDictionary));
    codec_opts = (AVDictionary *) malloc(sizeof(AVDictionary));
    memset(codec_opts, 0, sizeof(AVDictionary));

    iformat = NULL;
    url = NULL;
    headers = NULL;

    audioCodecName = NULL;
    videoCodecName = NULL;
    messageQueue = new AVMessageQueue();
}

void PlayerState::reset() {

    if (sws_dict) {
        av_dict_free(&sws_dict);
        av_dict_set(&sws_dict, "flags", "bicubic", 0);
    }
    if (swr_opts) {
        av_dict_free(&swr_opts);
    }
    if (format_opts) {
        av_dict_free(&format_opts);
    }
    if (codec_opts) {
        av_dict_free(&codec_opts);
    }
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
    infiniteBuffer = -1;
    audioDisable = 0;
    videoDisable = 0;
    displayDisable = 0;
    fast = 0;
    genpts = 0;
    lowres = 0;
    playbackRate = 1.0;
    seekRequest = 0;
    seekFlags = 0;
    seekPos = 0;
    seekRel = 0;
    seekRel = 0;
    autoExit = 0;
    loop = 1;
    mute = 0;
    frameDrop = 1;
    reorderVideoPts = -1;
    videoDuration = 0;
}

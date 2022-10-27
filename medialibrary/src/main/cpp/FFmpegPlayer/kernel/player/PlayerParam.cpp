
#include "PlayerParam.h"

PlayerParam::PlayerParam() {
    init();
}

PlayerParam::~PlayerParam() {
    if (url) {
        av_freep(&url);
        url = nullptr;
    }
    if (m_messageQueue) {
        m_messageQueue->release();
        delete m_messageQueue;
        m_messageQueue = nullptr;
    }
}

void PlayerParam::init() {
    url                = nullptr;
    m_videoStream      = nullptr;
    m_audioStream      = nullptr;
    m_subtitleStream   = nullptr;
    m_audioCodecCtx    = nullptr;
    m_videoCodecCtx    = nullptr;
    m_subtitleCodecCtx = nullptr;
    m_messageQueue     = new FFMessageQueue();

    m_loop = 1;
    m_mute = 0;
    m_decodeFastFlag = 0;
    m_seekPos = 0;
    m_seekFlag = 0;
    m_frameDrop = 1;
    m_seekRequest = 0;
    m_abortReq = 1;
    m_pauseReq = 1;
    m_audioDisable = 0;
    m_videoDisable = 0;
    m_videoIndex = -1;
    m_audioIndex = -1;
    m_displayDisable = 0;
    m_playbackRate = 1.0;
    m_subtitleIndex = -1;
    m_reorderVideoPts = -1;
    m_syncType = AV_SYNC_AUDIO;
    m_duration = AV_NOPTS_VALUE;
    m_startTime = AV_NOPTS_VALUE;
    m_firstAudioFrame = false;
    m_firstVideoFrame = false;
}

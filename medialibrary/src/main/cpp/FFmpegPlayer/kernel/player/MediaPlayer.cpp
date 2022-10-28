
#include "MediaPlayer.h"

MediaPlayer::MediaPlayer() {
    av_register_all();
    avformat_network_init();
    m_playerParam  = new PlayerParam();
    m_duration     = -1;
    m_lastPause    = -1;
    m_audioDecoder = nullptr;
    m_videoDecoder = nullptr;

    m_eof            = 0;
    m_exitPlay       = true;
    m_readThread     = nullptr;
    m_audioRender    = nullptr;
    m_audioResampler = nullptr;
    m_avSync = new AVSync(m_playerParam);
}

MediaPlayer::~MediaPlayer() {
    avformat_network_deinit();
}

int MediaPlayer::reset() {
    stop();
    if (m_avSync) {
        m_avSync->reset();
        delete m_avSync;
        m_avSync = nullptr;
    }
    if (m_audioDecoder != nullptr) {
        m_audioDecoder->stop();
        delete m_audioDecoder;
        m_audioDecoder = nullptr;
    }
    if (m_videoDecoder != nullptr) {
        m_videoDecoder->stop();
        delete m_videoDecoder;
        m_videoDecoder = nullptr;
    }
    if (m_audioRender != nullptr) {
        m_audioRender->stop();
        delete m_audioRender;
        m_audioRender = nullptr;
    }
    if (m_audioResampler) {
        delete m_audioResampler;
        m_audioResampler = nullptr;
    }
    if (m_playerParam) {
        if (m_playerParam->m_formatCtx != nullptr) {
            avformat_close_input(&m_playerParam->m_formatCtx);
            m_playerParam->m_formatCtx = nullptr;
        }
        delete m_playerParam;
        m_playerParam = nullptr;
    }
    return 0;
}

void MediaPlayer::setDataSource(const char *url) {
    Mutex::AutoLock lock(m_playerMutex);
    m_playerParam->url = av_strdup(url);
}

void MediaPlayer::setVideoRender(VideoRender *render) {
    Mutex::AutoLock lock(m_playerMutex);
    m_avSync->setVideoRender(render);
}

int MediaPlayer::prepare() {
    Mutex::AutoLock lock(m_playerMutex);
    if (!m_playerParam->url) {
        return -1;
    }
    m_playerParam->m_abortReq = 0;
    if (!m_readThread) {
        m_readThread = new Thread(this);
        m_readThread->start();
    }
    return 0;
}

int MediaPlayer::prepareAsync() {
    Mutex::AutoLock lock(m_playerMutex);
    if (!m_playerParam->url) {
        return -1;
    }
    if (m_playerParam->m_messageQueue) {
        m_playerParam->m_messageQueue->sendMessage(MSG_REQUEST_PREPARE);
    }
    return 0;
}

void MediaPlayer::start() {
    Mutex::AutoLock lock(m_playerMutex);
    m_playerParam->m_abortReq = 0;
    m_playerParam->m_pauseReq = 0;
    m_exitPlay = false;
    m_playerCond.signal();
}

void MediaPlayer::pause() {
    Mutex::AutoLock lock(m_playerMutex);
    m_playerParam->m_pauseReq = 1;
    m_playerCond.signal();
}

void MediaPlayer::resume() {
    Mutex::AutoLock lock(m_playerMutex);
    m_playerParam->m_pauseReq = 0;
    m_playerCond.signal();
}

void MediaPlayer::seekTo(long timeMs) {
    if (m_duration <= 0) {
        return;
    }

    m_playerMutex.lock();
    while (m_playerParam->m_seekRequest) {
        m_playerCond.wait(m_playerMutex);
    }
    m_playerMutex.unlock();

    if (!m_playerParam->m_seekRequest) {
        int64_t seek_pos = av_rescale(timeMs, AV_TIME_BASE, 1000);
        int64_t start_time = m_playerParam->m_formatCtx ? m_playerParam->m_formatCtx->start_time : 0;
        if (start_time > 0 && start_time != AV_NOPTS_VALUE) {
            seek_pos += start_time;
        }
        m_playerParam->m_seekPos = seek_pos;
        m_playerParam->m_seekFlag &= ~AVSEEK_FLAG_BYTE;
        m_playerParam->m_seekRequest = 1;
        m_playerCond.signal();
    }
}

int MediaPlayer::selectTrack(int streamId, bool selected) {
    PlayerParam *is = m_playerParam;
    AVFormatContext *ic = is->m_formatCtx;
    if (streamId < 0 || streamId >= ic->nb_streams) {
        av_log(nullptr, AV_LOG_ERROR, "invalid stream %d >= (%d)\n", streamId, ic->nb_streams);
        return -1;
    }

    AVCodecParameters *codecpar = ic->streams[streamId]->codecpar;
    long current_pos = getCurrentPosition();

    if (selected) {
        if (streamId == is->m_videoIndex || streamId == is->m_audioIndex || streamId == is->m_subtitleIndex) {
            av_log(nullptr, AV_LOG_ERROR, "stream has been selected\n");
            return 0;
        }
        switch (codecpar->codec_type) {
            case AVMEDIA_TYPE_VIDEO:
                if (streamId != is->m_videoIndex && is->m_videoIndex >= 0)
                    closeDecoder(is->m_videoIndex);
                break;
            case AVMEDIA_TYPE_AUDIO:
                if (streamId != is->m_audioIndex && is->m_audioIndex >= 0)
                    closeDecoder(is->m_audioIndex);
                break;
            case AVMEDIA_TYPE_SUBTITLE:
                if (streamId != is->m_subtitleIndex && is->m_subtitleIndex >= 0)
                    closeDecoder(is->m_subtitleIndex);
                break;
            default:
                av_log(nullptr, AV_LOG_ERROR, "select invalid stream: %d, type: %d\n", streamId, codecpar->codec_type);
                return -1;
        }
        int ret = openDecoder(streamId);
        seekTo(current_pos);
        return ret;
    } else {
        switch (codecpar->codec_type) {
            case AVMEDIA_TYPE_VIDEO:
                if (streamId == is->m_videoIndex)
                    closeDecoder(is->m_videoIndex);
                break;
            case AVMEDIA_TYPE_AUDIO:
                if (streamId == is->m_audioIndex)
                    closeDecoder(is->m_audioIndex);
                break;
            case AVMEDIA_TYPE_SUBTITLE:
                if (streamId == is->m_subtitleIndex)
                    closeDecoder(is->m_subtitleIndex);
                break;
            default:
                av_log(nullptr, AV_LOG_ERROR, "unselect invalid stream: %d, type: %d\n", streamId, codecpar->codec_type);
                return -1;
        }
        return 0;
    }
}

void MediaPlayer::setVolume(float volume) {
    if (m_audioRender) {
        m_audioRender->setVolume(volume);
    }
}

void MediaPlayer::setMute(int mute) {
    m_playerMutex.lock();
    m_playerParam->m_mute = mute;
    m_playerCond.signal();
    m_playerMutex.unlock();
}

void MediaPlayer::setRate(float rate) {
    m_playerMutex.lock();
    m_playerParam->m_playbackRate = rate;
    m_playerCond.signal();
    m_playerMutex.unlock();
}

int MediaPlayer::getRotate() {
    Mutex::AutoLock lock(m_playerMutex);
    if (m_videoDecoder) {
        return m_videoDecoder->getRotate();
    }
    return 0;
}

int MediaPlayer::getVideoWidth() {
    Mutex::AutoLock lock(m_playerMutex);
    if (m_videoDecoder) {
        return m_videoDecoder->getCodecContext()->width;
    }
    return 0;
}

int MediaPlayer::getVideoHeight() {
    Mutex::AutoLock lock(m_playerMutex);
    if (m_videoDecoder) {
        return m_videoDecoder->getCodecContext()->height;
    }
    return 0;
}

long MediaPlayer::getCurrentPosition() {
    Mutex::AutoLock lock(m_playerMutex);
    // 处于定位
    if (m_playerParam->m_seekRequest) {
        return m_playerParam->m_seekPos;
    } else {
        // 起始延时
        int64_t start_time = m_playerParam->m_formatCtx->start_time;
        int64_t start_diff = 0;
        if (start_time > 0 && start_time != AV_NOPTS_VALUE) {
            start_diff = av_rescale(start_time, 1000, AV_TIME_BASE);
        }

        // 计算主时钟的时间
        int64_t pos;
        double clock = m_avSync->getMasterClock();
        if (isnan(clock)) {
            pos = m_playerParam->m_seekPos;
        } else {
            pos = (int64_t)(clock * 1000);
        }
        if (pos < 0 || pos < start_diff) {
            return 0;
        }
        return (long) (pos - start_diff);
    }
}

long MediaPlayer::getDuration() {
    Mutex::AutoLock lock(m_playerMutex);
    return (long)m_duration;
}

int MediaPlayer::isPlaying() {
    Mutex::AutoLock lock(m_playerMutex);
    return !m_playerParam->m_abortReq && !m_playerParam->m_pauseReq;
}

static int avformat_interrupt_cb(void *ctx) {
    auto *playerState = (PlayerParam *) ctx;
    if (playerState->m_abortReq) {
        return AVERROR_EOF;
    }
    return 0;
}

FFMessageQueue *MediaPlayer::getMessageQueue() {
    Mutex::AutoLock lock(m_playerMutex);
    return m_playerParam->m_messageQueue;
}

AVStream *MediaPlayer::getAVStream(int mediaType) const {
    if (mediaType == AVMEDIA_TYPE_AUDIO) {
        return m_playerParam->m_audioStream;
    } else if (mediaType == AVMEDIA_TYPE_VIDEO) {
        return m_playerParam->m_videoStream;
    } else if (mediaType == AVMEDIA_TYPE_SUBTITLE) {
        return m_playerParam->m_subtitleStream;
    } else {
        return nullptr;
    }
}

AVFormatContext *MediaPlayer::getMetadata() const {
    return m_playerParam->m_formatCtx;
}

void MediaPlayer::stop() {
    m_playerMutex.lock();
    m_playerParam->m_abortReq = 1;
    m_playerCond.signal();
    m_playerMutex.unlock();
    m_playerMutex.lock();
    while (!m_exitPlay) {
        m_playerCond.wait(m_playerMutex);
    }
    m_playerMutex.unlock();
    if (m_readThread != nullptr) {
        m_readThread->join();
        delete m_readThread;
        m_readThread = nullptr;
    }
}

void MediaPlayer::run() {
    readPackets();
}

void startAudioDecoder(PlayerParam *playerParam, AudioDecoder *audioDecoder) {
    if (audioDecoder != nullptr) {
        audioDecoder->start();
    } else {
        if (playerParam->m_syncType == AV_SYNC_AUDIO) {
            playerParam->m_syncType = AV_SYNC_EXTERNAL;
        }
    }
}

void MediaPlayer::startAudioRender(PlayerParam *playerParam) {
    if (playerParam->m_audioCodecCtx != nullptr) {
        AVCodecContext *avctx = playerParam->m_audioCodecCtx;
        int ret = openAudioRender((int64_t)avctx->channel_layout, avctx->channels,
                              avctx->sample_rate);
        if (ret < 0) {
            av_log(nullptr, AV_LOG_ERROR, "couldn't open audio render\n");
            // audio render fail, use external as master clock
            if (playerParam->m_syncType == AV_SYNC_AUDIO) {
                playerParam->m_syncType = AV_SYNC_EXTERNAL;
            }
        } else {
            m_audioRender->start();
        }
    }
}

int MediaPlayer::readPackets() {
    int ret = 0;
    AVFormatContext *ic;

    m_playerMutex.lock();
    do {
        ic = avformat_alloc_context();
        if (!ic) {
            av_log(nullptr, AV_LOG_ERROR, "Could not allocate context.\n");
            ret = AVERROR(ENOMEM);
            break;
        }
        m_playerParam->m_formatCtx = ic;

        ic->interrupt_callback.callback = avformat_interrupt_cb;
        ic->interrupt_callback.opaque = m_playerParam;

        ret = avformat_open_input(&ic, m_playerParam->url, nullptr, nullptr);
        if (ret < 0) {
            av_log(nullptr, AV_LOG_ERROR, "open input err:url=%s, msg=%s", m_playerParam->url, strerror(ret));
            ret = -1;
            break;
        }

        av_format_inject_global_side_data(ic);
        avformat_find_stream_info(ic, nullptr);

        if (ic->duration != AV_NOPTS_VALUE) {
            m_duration = av_rescale(ic->duration, 1000, AV_TIME_BASE);
        }
        if (ic->pb) {
            ic->pb->eof_reached = 0;
        }

        // seek
        if (m_playerParam->m_startTime != AV_NOPTS_VALUE) {
            int64_t timestamp;

            timestamp = m_playerParam->m_startTime;
            if (ic->start_time != AV_NOPTS_VALUE) {
                timestamp += ic->start_time;
            }
            m_playerParam->m_playMutex.lock();
            ret = avformat_seek_file(ic, -1, INT64_MIN, timestamp, INT64_MAX, 0);
            m_playerParam->m_playMutex.unlock();
            if (ret < 0) {
                av_log(nullptr, AV_LOG_ERROR, "%s: could not seek to position %0.3f\n",
                       m_playerParam->url, (double)timestamp / AV_TIME_BASE);
            }
        }

        int audioIndex = -1;
        int videoIndex = -1;
        for (int i = 0; i < ic->nb_streams; ++i) {
            if (ic->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_AUDIO) {
                if (audioIndex == -1) {
                    audioIndex = i;
                }
            } else if (ic->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
                if (videoIndex == -1) {
                    videoIndex = i;
                }
            }
        }

        if (!m_playerParam->m_videoDisable) {
            videoIndex = av_find_best_stream(ic, AVMEDIA_TYPE_VIDEO,
                                             videoIndex, -1, nullptr, 0);
        }
        if (!m_playerParam->m_audioDisable) {
            audioIndex = av_find_best_stream(ic, AVMEDIA_TYPE_AUDIO,
                                             audioIndex, videoIndex, nullptr, 0);
        }
        if (audioIndex == -1 && videoIndex == -1) {
            av_log(nullptr, AV_LOG_ERROR,"%s: no audio and video stream\n", m_playerParam->url);
            ret = -1;
            break;
        }

        if (audioIndex >= 0) {
            openDecoder(audioIndex);
            if (m_playerParam->m_messageQueue) {
                m_playerParam->m_messageQueue->sendMessage(MSG_AUDIO_DECODER_OPEN);
            }
        }
        if (videoIndex >= 0) {
            openDecoder(videoIndex);
            if (m_playerParam->m_messageQueue) {
                m_playerParam->m_messageQueue->sendMessage(MSG_VIDEO_DECODER_OPEN);
            }
        }

        if (!m_audioDecoder && !m_videoDecoder) {
            ret = -1;
            break;
        }
        ret = 0;

    } while (false);
    m_playerMutex.unlock();

    if (ret < 0) {
        m_exitPlay = true;
        m_playerCond.signal();
        if (m_playerParam->m_messageQueue) {
            if (m_playerParam->m_messageQueue) {
                const char *msg = "open decoder error";
                m_playerParam->m_messageQueue->sendMessage(MSG_ON_ERROR, 0, 0,
                                                           (void *) msg, (int)strlen(msg));
            }
        }
        return -1;
    }

    if (m_videoDecoder) {
        AVCodecParameters *codecpar = m_playerParam->m_videoStream->codecpar;
        if (m_playerParam->m_messageQueue) {
            m_playerParam->m_messageQueue->sendMessage(MSG_VIDEO_SIZE_CHANGED,
                                                       codecpar->width, codecpar->height);
        }
    }

    if (m_playerParam->m_messageQueue) {
        m_playerParam->m_messageQueue->sendMessage(MSG_ON_PREPARED);
    }

    if (m_videoDecoder != nullptr) {
        m_videoDecoder->start();
        if (m_playerParam->m_messageQueue) {
            m_playerParam->m_messageQueue->sendMessage(MSG_VIDEO_DECODE_START);
        }
    } else {
        if (m_playerParam->m_syncType == AV_SYNC_VIDEO) {
            m_playerParam->m_syncType = AV_SYNC_AUDIO;
        }
    }

    if (m_videoDecoder) {
        if (m_playerParam->m_syncType == AV_SYNC_AUDIO) {
            m_videoDecoder->setMasterClock(m_avSync->getAudioClock());
        } else if (m_playerParam->m_syncType == AV_SYNC_VIDEO) {
            m_videoDecoder->setMasterClock(m_avSync->getVideoClock());
        } else {
            m_videoDecoder->setMasterClock(m_avSync->getExternalClock());
        }
    }

    // 开始同步
    m_avSync->start(m_videoDecoder, m_audioDecoder);

    if (m_playerParam->m_messageQueue) {
        m_playerParam->m_messageQueue->sendMessage(MSG_ON_START);
    }

    ret = 0;
    m_eof = 0;
    int64_t pkt_ts;
    int waitToSeek = 0;
    int playInRange;
    int64_t stream_start_time;
    AVPacket pkt1, *pkt = &pkt1;

    for (;;) {

        if (m_playerParam->m_abortReq) {
            break;
        }

        if (m_playerParam->m_pauseReq != m_lastPause) {
            m_lastPause = m_playerParam->m_pauseReq;
            if (m_playerParam->m_pauseReq) {
                av_read_pause(ic);
            } else {
                av_read_play(ic);
            }
        }

        if (m_playerParam->m_seekRequest) {
            int64_t seek_target = m_playerParam->m_seekPos;
            int64_t seek_min = INT64_MIN;
            int64_t seek_max = INT64_MAX;
            m_playerParam->m_playMutex.lock();
            ret = avformat_seek_file(ic, -1, seek_min, seek_target, seek_max, m_playerParam->m_seekFlag);
            m_playerParam->m_playMutex.unlock();
            if (ret < 0) {
                av_log(nullptr, AV_LOG_ERROR, "%s: error while seeking\n", m_playerParam->url);
            } else {
                if (m_audioDecoder) {
                    m_audioDecoder->flush();
                }
                if (m_videoDecoder) {
                    m_videoDecoder->flush();
                }

                if (m_playerParam->m_seekFlag & AVSEEK_FLAG_BYTE) {
                    m_avSync->updateExternalClock(NAN);
                } else {
                    m_avSync->updateExternalClock((double)seek_target / AV_TIME_BASE);
                }
                m_avSync->refreshVideoTimer();
            }
            m_playerParam->m_seekRequest = 0;
            m_playerCond.signal();
            m_eof = 0;
            // 定位完成回调通知
            if (m_playerParam->m_messageQueue) {
                int seekTime = (int) av_rescale(seek_target, 1000, AV_TIME_BASE);
                m_playerParam->m_messageQueue->sendMessage(MSG_SEEK_COMPLETE, seekTime, ret);
            }
        }

        // waiting
        if (((m_audioDecoder ? m_audioDecoder->getMemorySize() : 0) + (m_videoDecoder ? m_videoDecoder->getMemorySize() : 0) > MAX_QUEUE_SIZE
             || (!m_audioDecoder || m_audioDecoder->hasEnoughPackets(m_playerParam->m_audioStream))
             && (!m_videoDecoder || m_videoDecoder->hasEnoughPackets(m_playerParam->m_videoStream)))) {
            continue;
        }

        if (!waitToSeek) {
            ret = av_read_frame(ic, pkt);
        } else {
            ret = -1;
        }
        if (ret < 0) {
            if ((ret == AVERROR_EOF || avio_feof(ic->pb)) && !m_eof) {
                m_eof = 1;
            }
            if (ic->pb && ic->pb->error) {
                ret = -1;
                break;
            }

            if (!m_playerParam->m_pauseReq && (!m_audioDecoder || m_audioDecoder->getPacketSize() == 0)
                && (!m_videoDecoder || (m_videoDecoder->getPacketSize() == 0
                                        && m_videoDecoder->getFrameSize() == 0))) {
                if (m_playerParam->m_loop) {
                    seekTo(m_playerParam->m_startTime != AV_NOPTS_VALUE ? m_playerParam->m_startTime : 0);
                } else {
                    ret = AVERROR_EOF;
                    break;
                }
            }

            av_usleep(10 * 1000);
            continue;
        } else {
            m_eof = 0;
        }

        // 计算pkt的pts是否处于播放范围内
        stream_start_time = ic->streams[pkt->stream_index]->start_time;
        int64_t start_time = stream_start_time != AV_NOPTS_VALUE ? stream_start_time : 0;
        pkt_ts = pkt->pts == AV_NOPTS_VALUE ? pkt->dts : pkt->pts;
        // 播放范围
        playInRange = m_playerParam->m_duration == AV_NOPTS_VALUE
                      || (double)(pkt_ts - start_time) *
                         av_q2d(ic->streams[pkt->stream_index]->time_base)
                         - (double)(m_playerParam->m_startTime != AV_NOPTS_VALUE ? m_playerParam->m_startTime : 0) / 1000000
                         <= ((double)m_playerParam->m_duration / 1000000);
        if (playInRange && m_audioDecoder && pkt->stream_index == m_playerParam->m_audioIndex) {
            m_audioDecoder->pushPacket(pkt);
        } else if (playInRange && m_videoDecoder && pkt->stream_index == m_playerParam->m_videoIndex) {
            m_videoDecoder->pushPacket(pkt);
        } else {
            av_packet_unref(pkt);
        }

        if (!playInRange) {
            waitToSeek = 1;
        }
    }

    if (m_playerParam->m_audioIndex >= 0) {
        closeDecoder(m_playerParam->m_audioIndex);
    }
    if (m_playerParam->m_videoIndex >= 0) {
        closeDecoder(m_playerParam->m_videoIndex);
    }
    m_exitPlay = true;
    m_playerCond.signal();

    if (ret < 0) {
        if (m_playerParam->m_messageQueue) {
            const char *msg = "read frame error";
            m_playerParam->m_messageQueue->sendMessage(MSG_ON_ERROR, 0, 0,
                                                       (void *) msg, (int)strlen(msg));
        }
    } else {
        if (m_playerParam->m_messageQueue) {
            m_playerParam->m_messageQueue->sendMessage(MSG_ON_COMPLETE);
        }
    }

    if (m_playerParam->m_messageQueue) {
        m_playerParam->m_messageQueue->stop();
    }

    return ret;
}

int MediaPlayer::openDecoder(int streamIndex) {
    int ret;
    AVCodecContext *avctx;

    if (streamIndex < 0 || streamIndex >= m_playerParam->m_formatCtx->nb_streams) {
        return -1;
    }

    avctx = avcodec_alloc_context3(nullptr);
    if (!avctx) {
        return AVERROR(ENOMEM);
    }

    do {
        ret = avcodec_parameters_to_context(avctx, m_playerParam->m_formatCtx->streams[streamIndex]->codecpar);
        if (ret < 0) {
            break;
        }

        avctx->pkt_timebase = m_playerParam->m_formatCtx->streams[streamIndex]->time_base;
        const AVCodec *codec = avcodec_find_decoder(avctx->codec_id);

        if (!codec) {
            av_log(nullptr, AV_LOG_ERROR,
                   "No codec could be found with id %d\n", avctx->codec_id);
            ret = AVERROR(EINVAL);
            break;
        }
        avctx->codec_id = codec->id;

        if (m_playerParam->m_decodeFastFlag) {
            avctx->flags2 |= AV_CODEC_FLAG2_FAST;
        }
        if ((ret = avcodec_open2(avctx, codec, nullptr)) < 0) {
            break;
        }

        m_playerParam->m_formatCtx->streams[streamIndex]->discard = AVDISCARD_DEFAULT;
        switch (avctx->codec_type) {
            case AVMEDIA_TYPE_AUDIO: {
                m_playerParam->m_audioIndex    = streamIndex;
                m_playerParam->m_audioStream   = m_playerParam->m_formatCtx->streams[streamIndex];
                m_playerParam->m_audioCodecCtx = avctx;
                m_audioDecoder = new AudioDecoder(m_playerParam);
                startAudioDecoder(m_playerParam, m_audioDecoder);
                startAudioRender(m_playerParam);
                m_avSync->setAudioDecoder(m_audioDecoder);
                break;
            }
            case AVMEDIA_TYPE_VIDEO: {
                m_playerParam->m_videoIndex    = streamIndex;
                m_playerParam->m_videoStream   = m_playerParam->m_formatCtx->streams[streamIndex];
                m_playerParam->m_videoCodecCtx = avctx;
                m_videoDecoder = new VideoDecoder(m_playerParam);
                break;
            }
            default:{
                break;
            }
        }
    } while (false);

    if (ret < 0) {
        avcodec_free_context(&avctx);
    }

    return ret;
}

void MediaPlayer::closeDecoder(int streamIndex) {
    if (streamIndex < 0 || streamIndex >= m_playerParam->m_formatCtx->nb_streams)
        return;
    AVStream *stream = m_playerParam->m_formatCtx->streams[streamIndex];
    switch (stream->codecpar->codec_type) {
        case AVMEDIA_TYPE_AUDIO:
            if (m_audioDecoder) {
                m_audioDecoder->flush();
            }
            if (m_audioRender) {
                m_audioRender->stop();
                delete m_audioRender;
                m_audioRender = nullptr;
            }
            if (m_audioResampler) {
                delete m_audioResampler;
                m_audioResampler = nullptr;
            }
            if (m_audioDecoder) {
                m_audioDecoder->stop();
                delete m_audioDecoder;
                m_audioDecoder = nullptr;
            }
            m_playerParam->m_audioIndex  = -1;
            m_playerParam->m_audioStream = nullptr;
            break;
        case AVMEDIA_TYPE_VIDEO:
            if (m_videoDecoder) {
                m_videoDecoder->stop();
                delete m_videoDecoder;
                m_videoDecoder = nullptr;
            }
            m_playerParam->m_videoIndex  = -1;
            m_playerParam->m_videoStream = nullptr;
            break;
        case AVMEDIA_TYPE_SUBTITLE:
            av_log(nullptr, AV_LOG_INFO, "not implementation...");
            break;
        default:
            break;
    }
}

void audioPCMCallback(void *opaque, uint8_t *stream, int len) {
    auto *mediaPlayer = (MediaPlayer *) opaque;
    mediaPlayer->pcmCallback(stream, len);
}

int MediaPlayer::openAudioRender(int64_t wanted_channel_layout, int wanted_nb_channels,
                                 int wanted_sample_rate) {
    AudioRenderSpec wanted_spec, spec;
    const int next_nb_channels[] = {0, 0, 1, 6, 2, 6, 4, 6};
    const int next_sample_rates[] = {44100, 48000};
    int next_sample_rate_idx = FF_ARRAY_ELEMS(next_sample_rates) - 1;
    if (wanted_nb_channels != av_get_channel_layout_nb_channels(wanted_channel_layout)
        || !wanted_channel_layout) {
        wanted_channel_layout = av_get_default_channel_layout(wanted_nb_channels);
        wanted_channel_layout &= ~AV_CH_LAYOUT_STEREO_DOWNMIX;
    }
    wanted_nb_channels = av_get_channel_layout_nb_channels(wanted_channel_layout);
    wanted_spec.channels = wanted_nb_channels;
    wanted_spec.freq = wanted_sample_rate;
    if (wanted_spec.freq <= 0 || wanted_spec.channels <= 0) {
        av_log(nullptr, AV_LOG_ERROR, "Invalid sample rate or channel count!\n");
        return -1;
    }
    while (next_sample_rate_idx && next_sample_rates[next_sample_rate_idx] >= wanted_spec.freq) {
        next_sample_rate_idx--;
    }

    wanted_spec.format = AV_SAMPLE_FMT_S16;
    wanted_spec.samples = FFMAX(AUDIO_MIN_BUFFER_SIZE,
                                2 << av_log2(wanted_spec.freq / AUDIO_MAX_CALLBACKS_PER_SEC));
    wanted_spec.callback = audioPCMCallback;
    wanted_spec.opaque = this;

    // Audio Render
    m_audioRender = new OpenSLAudioRender();
    while (m_audioRender->open(&wanted_spec, &spec) < 0) {
        av_log(nullptr, AV_LOG_ERROR, "open audio render error: channel=%d, sampleRate=%d\n",
               wanted_spec.channels, wanted_spec.freq);
        wanted_spec.channels = next_nb_channels[FFMIN(7, wanted_spec.channels)];
        if (!wanted_spec.channels) {
            wanted_spec.freq = next_sample_rates[next_sample_rate_idx--];
            wanted_spec.channels = wanted_nb_channels;
            if (!wanted_spec.freq) {
                av_log(nullptr, AV_LOG_ERROR, "audio open failed\n");
                return -1;
            }
        }
        wanted_channel_layout = av_get_default_channel_layout(wanted_spec.channels);
    }

    if (spec.format != AV_SAMPLE_FMT_S16) {
        av_log(nullptr, AV_LOG_ERROR, "audio format %d is not supported!\n", spec.format);
        return -1;
    }

    if (spec.channels != wanted_spec.channels) {
        wanted_channel_layout = av_get_default_channel_layout(spec.channels);
        if (!wanted_channel_layout) {
            av_log(nullptr, AV_LOG_ERROR, "don't support channel:%d\n", spec.channels);
            return -1;
        }
    }

    m_audioResampler = new AudioResampler(m_playerParam, m_audioDecoder, m_avSync);
    m_audioResampler->setResampleParams(&spec, wanted_channel_layout);

    return 0;
}

void MediaPlayer::pcmCallback(uint8_t *stream, int len) {
    if (!m_audioResampler) {
        memset(stream, 0, sizeof(len));
        return;
    }
    m_audioResampler->pcmQueueCallback(stream, len);
    if (!m_playerParam->m_firstAudioFrame && m_playerParam->m_messageQueue) {
        m_playerParam->m_firstAudioFrame = true;
        m_playerParam->m_messageQueue->sendMessage(MSG_AUDIO_RENDER_START);
    }
}

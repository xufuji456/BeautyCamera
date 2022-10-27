
#include "VideoDecoder.h"

VideoDecoder::VideoDecoder(PlayerParam *playerParam)
        : MediaDecoder(playerParam) {
    m_decodeThread = nullptr;
    m_masterClock  = nullptr;
    m_frameQueue   = new FrameQueue(VIDEO_QUEUE_SIZE, 1);

    AVDictionaryEntry *entry = av_dict_get(playerParam->m_videoStream->metadata,
                                           "rotate", nullptr, AV_DICT_MATCH_CASE);
    if (entry && entry->value) {
        m_rotate = atoi(entry->value);
    } else {
        m_rotate = 0;
    }
}

VideoDecoder::~VideoDecoder() {
    m_decodeMutex.lock();
    if (m_frameQueue) {
        m_frameQueue->flush();
        delete m_frameQueue;
        m_frameQueue = nullptr;
    }
    m_masterClock = nullptr;
    m_decodeMutex.unlock();
}

void VideoDecoder::setMasterClock(MediaClock *clock) {
    Mutex::AutoLock lock(m_decodeMutex);
    this->m_masterClock = clock;
}

void VideoDecoder::start() {
    MediaDecoder::start();
    if (m_frameQueue) {
        m_frameQueue->start();
    }
    if (!m_decodeThread) {
        m_decodeThread = new Thread(this);
        m_decodeThread->start();
        m_exit = false;
    }
}

void VideoDecoder::stop() {
    MediaDecoder::stop();
    if (m_frameQueue) {
        m_frameQueue->abort();
    }
    m_decodeMutex.lock();
    while (!m_exit) {
        m_decodeCond.wait(m_decodeMutex);
    }
    m_decodeMutex.unlock();
    if (m_decodeThread) {
        m_decodeThread->join();
        delete m_decodeThread;
        m_decodeThread = nullptr;
    }
}

void VideoDecoder::flush() {
    m_decodeMutex.lock();
    MediaDecoder::flush();
    if (m_frameQueue) {
        m_frameQueue->flush();
    }
    m_decodeCond.signal();
    m_decodeMutex.unlock();
}

int VideoDecoder::getFrameSize() {
    Mutex::AutoLock lock(m_decodeMutex);
    return m_frameQueue ? m_frameQueue->getFrameSize() : 0;
}

int VideoDecoder::getRotate() {
    Mutex::AutoLock lock(m_decodeMutex);
    return m_rotate;
}

FrameQueue *VideoDecoder::getFrameQueue() {
    Mutex::AutoLock lock(m_decodeMutex);
    return m_frameQueue;
}

AVCodecContext *VideoDecoder::getCodecContext() {
    return m_playerParam->m_videoCodecCtx;
}

void VideoDecoder::run() {
    decodeVideo();
}

int VideoDecoder::decodeVideo() {
    int ret;
    Frame *vp;
    int got_picture;
    AVFrame *frame = av_frame_alloc();

    AVRational timebase = m_playerParam->m_videoStream->time_base;
    AVRational frame_rate = av_guess_frame_rate(
            m_playerParam->m_formatCtx, m_playerParam->m_videoStream, nullptr);
    if (!frame) {
        m_exit = true;
        m_decodeCond.signal();
        return AVERROR(ENOMEM);
    }

    AVPacket *packet = av_packet_alloc();
    if (!packet) {
        m_exit = true;
        m_decodeCond.signal();
        return AVERROR(ENOMEM);
    }

    for (;;) {

        if (m_abortReq || m_playerParam->m_abortReq) {
            ret = -1;
            break;
        }
        if (m_playerParam->m_seekRequest) {
            continue;
        }
        if (m_packetQueue->getPacket(packet) < 0) {
            ret = -1;
            break;
        }

        m_playerParam->m_playMutex.lock();
        ret = avcodec_send_packet(getCodecContext(), packet);
        if (ret < 0 && ret != AVERROR(EAGAIN) && ret != AVERROR_EOF) {
            av_packet_unref(packet);
            m_playerParam->m_playMutex.unlock();
            continue;
        }

        ret = avcodec_receive_frame(getCodecContext(), frame);
        m_playerParam->m_playMutex.unlock();
        if (ret < 0 && ret != AVERROR_EOF) {
            av_frame_unref(frame);
            av_packet_unref(packet);
            continue;
        } else {
            got_picture = 1;

            if (m_playerParam->m_reorderVideoPts == -1) {
                frame->pts = frame->best_effort_timestamp;
            } else if (!m_playerParam->m_reorderVideoPts) {
                frame->pts = frame->pkt_dts;
            }

            if (m_masterClock != nullptr) {
                double pts = NAN;
                if (frame->pts != AV_NOPTS_VALUE) {
                    pts = av_q2d(m_playerParam->m_videoStream->time_base) * (double)frame->pts;
                }
                frame->sample_aspect_ratio = av_guess_sample_aspect_ratio(
                        m_playerParam->m_formatCtx, m_playerParam->m_videoStream, frame);
                // drop m_frame
                if (m_playerParam->m_frameDrop > 0 ||
                    (m_playerParam->m_frameDrop > 0 && m_playerParam->m_syncType != AV_SYNC_VIDEO)) {
                    if (frame->pts != AV_NOPTS_VALUE) {
                        double diff = pts - m_masterClock->getClock();
                        if (!isnan(diff) && fabs(diff) < AV_NOSYNC_THRESHOLD &&
                            diff < 0 && m_packetQueue->getPacketSize() > 0) {
                            av_frame_unref(frame);
                            got_picture = 0;
                        }
                    }
                }
            }
        }

        if (got_picture) {
            if (!(vp = m_frameQueue->peekWritable())) {
                ret = -1;
                break;
            }

            vp->uploaded = 0;
            vp->width    = frame->width;
            vp->height   = frame->height;
            vp->format   = frame->format;
            vp->pts      = (frame->pts == AV_NOPTS_VALUE) ? NAN : (double)frame->pts * av_q2d(timebase);
            vp->duration = frame_rate.num && frame_rate.den
                           ? av_q2d((AVRational){frame_rate.den, frame_rate.num}) : 0;
            av_frame_move_ref(vp->frame, frame);

            m_frameQueue->pushFrame();
        }

        av_frame_unref(frame);
        av_packet_unref(packet);
    }

    av_frame_free(&frame);
    av_free(frame);
    frame = nullptr;

    av_packet_free(&packet);
    av_free(packet);
    packet = nullptr;

    m_exit = true;
    m_decodeCond.signal();

    return ret;
}

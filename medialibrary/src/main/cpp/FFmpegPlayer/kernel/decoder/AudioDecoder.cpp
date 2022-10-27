
#include "AudioDecoder.h"

AudioDecoder::AudioDecoder(PlayerParam *playerParam)
        : MediaDecoder(playerParam) {
    m_packet = av_packet_alloc();
    m_pktPending = false;
}

AudioDecoder::~AudioDecoder() {
    m_decodeMutex.lock();
    m_pktPending = false;
    if (m_packet) {
        av_packet_free(&m_packet);
        av_freep(&m_packet);
        m_packet = nullptr;
    }
    m_decodeMutex.unlock();
}

AVCodecContext *AudioDecoder::getCodecContext() {
    return m_playerParam->m_audioCodecCtx;
}

int AudioDecoder::getAudioFrame(AVFrame *frame) {
    int ret = 0;
    int got_frame = 0;

    if (!frame) {
        return AVERROR(ENOMEM);
    }
    av_frame_unref(frame);

    do {
        if (m_abortReq) {
            ret = -1;
            break;
        }
        if (m_playerParam->m_seekRequest) {
            continue;
        }

        AVPacket pkt;
        if (m_pktPending) {
            av_packet_move_ref(&pkt, m_packet);
            m_pktPending = false;
        } else {
            if (m_packetQueue->getPacket(&pkt) < 0) {
                ret = -1;
                break;
            }
        }

        m_playerParam->m_playMutex.lock();
        ret = avcodec_send_packet(getCodecContext(), &pkt);
        if (ret < 0) {
            if (ret == AVERROR(EAGAIN)) {
                av_packet_move_ref(m_packet, &pkt);
                m_pktPending = true;
            } else {
                av_packet_unref(&pkt);
                m_pktPending = false;
            }
            m_playerParam->m_playMutex.unlock();
            continue;
        }

        ret = avcodec_receive_frame(getCodecContext(), frame);
        m_playerParam->m_playMutex.unlock();
        av_packet_unref(m_packet);
        if (ret < 0) {
            av_frame_unref(frame);
            got_frame = 0;
            continue;
        } else {
            got_frame = 1;
            // rescale m_pts using timebase
            AVRational tb = (AVRational){1, frame->sample_rate};
            if (frame->pts != AV_NOPTS_VALUE) {
                frame->pts = av_rescale_q(frame->pts, getCodecContext()->pkt_timebase, tb);
            } else if (m_next_pts != AV_NOPTS_VALUE) {
                frame->pts = av_rescale_q(m_next_pts, m_next_pts_tb, tb);
            }
            if (frame->pts != AV_NOPTS_VALUE) {
                m_next_pts = frame->pts + frame->nb_samples;
                m_next_pts_tb = tb;
            }
        }
    } while (!got_frame);

    if (ret < 0) {
        return -1;
    }

    return got_frame;
}









#include "AudioDecoder.h"

AudioDecoder::AudioDecoder(AVCodecContext *codecCtx, AVStream *stream, PlayerState *playerState)
        : MediaDecoder(codecCtx, stream, playerState) {
    packet = av_packet_alloc();
    packetPending = false;
}

AudioDecoder::~AudioDecoder() {
    mMutex.lock();
    packetPending = false;
    if (packet) {
        av_packet_free(&packet);
        av_freep(&packet);
        packet = nullptr;
    }
    mMutex.unlock();
}

int AudioDecoder::getAudioFrame(AVFrame *frame) {
    int ret = 0;
    int got_frame = 0;

    if (!frame) {
        return AVERROR(ENOMEM);
    }
    av_frame_unref(frame);

    do {
        if (abortRequest) {
            ret = -1;
            break;
        }
        if (playerState->seekRequest) {
            continue;
        }

        AVPacket pkt;
        if (packetPending) {
            av_packet_move_ref(&pkt, packet);
            packetPending = false;
        } else {
            if (packetQueue->getPacket(&pkt) < 0) {
                ret = -1;
                break;
            }
        }

        playerState->mMutex.lock();
        ret = avcodec_send_packet(codecContext, &pkt);
        if (ret < 0) {
            if (ret == AVERROR(EAGAIN)) {
                av_packet_move_ref(packet, &pkt);
                packetPending = true;
            } else {
                av_packet_unref(&pkt);
                packetPending = false;
            }
            playerState->mMutex.unlock();
            continue;
        }

        ret = avcodec_receive_frame(codecContext, frame);
        playerState->mMutex.unlock();
        av_packet_unref(packet);
        if (ret < 0) {
            av_frame_unref(frame);
            got_frame = 0;
            continue;
        } else {
            got_frame = 1;
            // rescale pts using timebase
            AVRational tb = (AVRational){1, frame->sample_rate};
            if (frame->pts != AV_NOPTS_VALUE) {
                frame->pts = av_rescale_q(frame->pts, av_codec_get_pkt_timebase(codecContext), tb);
            } else if (next_pts != AV_NOPTS_VALUE) {
                frame->pts = av_rescale_q(next_pts, next_pts_tb, tb);
            }
            if (frame->pts != AV_NOPTS_VALUE) {
                next_pts = frame->pts + frame->nb_samples;
                next_pts_tb = tb;
            }
        }
    } while (!got_frame);

    if (ret < 0) {
        return -1;
    }

    return got_frame;
}








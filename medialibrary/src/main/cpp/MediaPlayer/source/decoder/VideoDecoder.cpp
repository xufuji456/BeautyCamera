
#include "VideoDecoder.h"

VideoDecoder::VideoDecoder(AVFormatContext *formatCtx, AVCodecContext *codecCtx, PlayerParam *playerState)
        : MediaDecoder(codecCtx, playerState) {
    this->pFormatCtx = formatCtx;
    decodeThread = nullptr;
    masterClock  = nullptr;
    frameQueue   = new FrameQueue(VIDEO_QUEUE_SIZE, 1);

    AVDictionaryEntry *entry = av_dict_get(playerState->m_videoStream->metadata,
                                           "rotate", nullptr, AV_DICT_MATCH_CASE);
    if (entry && entry->value) {
        mRotate = atoi(entry->value);
    } else {
        mRotate = 0;
    }
}

VideoDecoder::~VideoDecoder() {
    mMutex.lock();
    pFormatCtx = nullptr;
    if (frameQueue) {
        frameQueue->flush();
        delete frameQueue;
        frameQueue = nullptr;
    }
    masterClock = nullptr;
    mMutex.unlock();
}

void VideoDecoder::setMasterClock(MediaClock *clock) {
    Mutex::Autolock lock(mMutex);
    this->masterClock = clock;
}

void VideoDecoder::start() {
    MediaDecoder::start();
    if (frameQueue) {
        frameQueue->start();
    }
    if (!decodeThread) {
        decodeThread = new Thread(this);
        decodeThread->start();
        mExit = false;
    }
}

void VideoDecoder::stop() {
    MediaDecoder::stop();
    if (frameQueue) {
        frameQueue->abort();
    }
    mMutex.lock();
    while (!mExit) {
        mCondition.wait(mMutex);
    }
    mMutex.unlock();
    if (decodeThread) {
        decodeThread->join();
        delete decodeThread;
        decodeThread = nullptr;
    }
}

void VideoDecoder::flush() {
    mMutex.lock();
    MediaDecoder::flush();
    if (frameQueue) {
        frameQueue->flush();
    }
    mCondition.signal();
    mMutex.unlock();
}

int VideoDecoder::getFrameSize() {
    Mutex::Autolock lock(mMutex);
    return frameQueue ? frameQueue->getFrameSize() : 0;
}

int VideoDecoder::getRotate() {
    Mutex::Autolock lock(mMutex);
    return mRotate;
}

FrameQueue *VideoDecoder::getFrameQueue() {
    Mutex::Autolock lock(mMutex);
    return frameQueue;
}

AVFormatContext *VideoDecoder::getFormatContext() {
    Mutex::Autolock lock(mMutex);
    return pFormatCtx;
}

void VideoDecoder::run() {
    decodeVideo();
}

int VideoDecoder::decodeVideo() {
    int ret;
    Frame *vp;
    int got_picture;
    AVFrame *frame = av_frame_alloc();

    AVRational timebase = playerState->m_videoStream->time_base;
    AVRational frame_rate = av_guess_frame_rate(pFormatCtx, playerState->m_videoStream, nullptr);

    if (!frame) {
        mExit = true;
        mCondition.signal();
        return AVERROR(ENOMEM);
    }

    AVPacket *packet = av_packet_alloc();
    if (!packet) {
        mExit = true;
        mCondition.signal();
        return AVERROR(ENOMEM);
    }

    for (;;) {

        if (abortRequest || playerState->abortRequest) {
            ret = -1;
            break;
        }
        if (playerState->seekRequest) {
            continue;
        }
        if (packetQueue->getPacket(packet) < 0) {
            ret = -1;
            break;
        }

        playerState->mMutex.lock();
        ret = avcodec_send_packet(codecContext, packet);
        if (ret < 0 && ret != AVERROR(EAGAIN) && ret != AVERROR_EOF) {
            av_packet_unref(packet);
            playerState->mMutex.unlock();
            continue;
        }

        ret = avcodec_receive_frame(codecContext, frame);
        playerState->mMutex.unlock();
        if (ret < 0 && ret != AVERROR_EOF) {
            av_frame_unref(frame);
            av_packet_unref(packet);
            continue;
        } else {
            got_picture = 1;

            if (playerState->reorderVideoPts == -1) {
                frame->pts = av_frame_get_best_effort_timestamp(frame);
            } else if (!playerState->reorderVideoPts) {
                frame->pts = frame->pkt_dts;
            }

            if (masterClock != nullptr) {
                double pts = NAN;
                if (frame->pts != AV_NOPTS_VALUE) {
                    pts = av_q2d(playerState->m_videoStream->time_base) * (double)frame->pts;
                }
                frame->sample_aspect_ratio = av_guess_sample_aspect_ratio(pFormatCtx, playerState->m_videoStream, frame);
                // drop frame
                if (playerState->frameDrop > 0 ||
                    (playerState->frameDrop > 0 && playerState->syncType != AV_SYNC_VIDEO)) {
                    if (frame->pts != AV_NOPTS_VALUE) {
                        double diff = pts - masterClock->getClock();
                        if (!isnan(diff) && fabs(diff) < AV_NOSYNC_THRESHOLD &&
                            diff < 0 && packetQueue->getPacketSize() > 0) {
                            av_frame_unref(frame);
                            got_picture = 0;
                        }
                    }
                }
            }
        }

        if (got_picture) {
            if (!(vp = frameQueue->peekWritable())) {
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

            frameQueue->pushFrame();
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

    mExit = true;
    mCondition.signal();

    return ret;
}

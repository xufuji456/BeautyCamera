
#include "MediaDecoder.h"

MediaDecoder::MediaDecoder(AVCodecContext *codecCtx, PlayerParam *playerState) {
    packetQueue = new PacketQueue();
    this->codecContext = codecCtx;
    this->playerState  = playerState;
}

MediaDecoder::~MediaDecoder() {
    mMutex.lock();
    if (packetQueue) {
        packetQueue->flush();
        delete packetQueue;
        packetQueue = nullptr;
    }
    if (codecContext) {
        avcodec_close(codecContext);
        avcodec_free_context(&codecContext);
        codecContext = nullptr;
    }
    playerState = nullptr;
    mMutex.unlock();
}

void MediaDecoder::start() {
    if (packetQueue) {
        packetQueue->start();
    }
    mMutex.lock();
    abortRequest = false;
    mCondition.signal();
    mMutex.unlock();
}

void MediaDecoder::stop() {
    mMutex.lock();
    abortRequest = true;
    mCondition.signal();
    mMutex.unlock();
    if (packetQueue) {
        packetQueue->abort();
    }
}

void MediaDecoder::flush() {
    if (packetQueue) {
        packetQueue->flush();
    }
    playerState->mMutex.lock();
    avcodec_flush_buffers(getCodecContext());
    playerState->mMutex.unlock();
}

int MediaDecoder::pushPacket(AVPacket *pkt) {
    if (packetQueue) {
        return packetQueue->pushPacket(pkt);
    }
    return 0;
}

int MediaDecoder::getPacketSize() {
    return packetQueue ? packetQueue->getPacketSize() : 0;
}

AVCodecContext *MediaDecoder::getCodecContext() {
    return codecContext;
}

int MediaDecoder::getMemorySize() {
    return packetQueue ? packetQueue->getSize() : 0;
}

int MediaDecoder::hasEnoughPackets(AVStream *stream) {
    Mutex::Autolock lock(mMutex);
    return (packetQueue == nullptr) || (packetQueue->isAbort())
           || (stream->disposition & AV_DISPOSITION_ATTACHED_PIC)
           || (packetQueue->getPacketSize() > MIN_FRAMES)
              && (!packetQueue->getDuration()
                  || av_q2d(stream->time_base) * (double)packetQueue->getDuration() > 1.0);
}

void MediaDecoder::run() {

}


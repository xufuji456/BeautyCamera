
#include "MediaDecoder.h"

MediaDecoder::MediaDecoder(AVCodecContext *codecCtx, AVStream *stream, PlayerParam *playerState) {
    packetQueue = new PacketQueue();
    this->codecContext = codecCtx;
    this->avStream     = stream;
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

AVStream *MediaDecoder::getStream() {
    return avStream;
}

AVCodecContext *MediaDecoder::getCodecContext() {
    return codecContext;
}

int MediaDecoder::getMemorySize() {
    return packetQueue ? packetQueue->getSize() : 0;
}

int MediaDecoder::hasEnoughPackets() {
    Mutex::Autolock lock(mMutex);
    return (packetQueue == nullptr) || (packetQueue->isAbort())
           || (avStream->disposition & AV_DISPOSITION_ATTACHED_PIC)
           || (packetQueue->getPacketSize() > MIN_FRAMES)
              && (!packetQueue->getDuration()
                  || av_q2d(avStream->time_base) * (double)packetQueue->getDuration() > 1.0);
}

void MediaDecoder::run() {

}


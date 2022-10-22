
#include "MediaDecoder.h"

MediaDecoder::MediaDecoder(AVCodecContext *avctx, AVStream *stream, int streamIndex, PlayerState *playerState) {
    packetQueue = new PacketQueue();
    this->pCodecCtx   = avctx;
    this->avStream    = stream;
    this->streamIndex = streamIndex;
    this->playerState = playerState;
}

MediaDecoder::~MediaDecoder() {
    mMutex.lock();
    if (packetQueue) {
        packetQueue->flush();
        delete packetQueue;
        packetQueue = nullptr;
    }
    if (pCodecCtx) {
        avcodec_close(pCodecCtx);
        avcodec_free_context(&pCodecCtx);
        pCodecCtx = nullptr;
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

int MediaDecoder::getStreamIndex() const {
    return streamIndex;
}

AVStream *MediaDecoder::getStream() {
    return avStream;
}

AVCodecContext *MediaDecoder::getCodecContext() {
    return pCodecCtx;
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
                  || av_q2d(avStream->time_base) * packetQueue->getDuration() > 1.0);
}

void MediaDecoder::run() {

}


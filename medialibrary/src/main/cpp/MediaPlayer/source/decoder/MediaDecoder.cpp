
#include "MediaDecoder.h"

MediaDecoder::MediaDecoder(PlayerParam *playerParam) {
    m_packetQueue = new PacketQueue();
    this->m_playerParam  = playerParam;
}

MediaDecoder::~MediaDecoder() {
    m_decodeMutex.lock();
    if (m_packetQueue) {
        m_packetQueue->flush();
        delete m_packetQueue;
        m_packetQueue = nullptr;
    }
    m_decodeMutex.unlock();
}

void MediaDecoder::start() {
    if (m_packetQueue) {
        m_packetQueue->start();
    }
    m_decodeMutex.lock();
    m_abortReq = false;
    m_decodeCond.signal();
    m_decodeMutex.unlock();
}

void MediaDecoder::stop() {
    m_decodeMutex.lock();
    m_abortReq = true;
    AVCodecContext *codecContext = getCodecContext();
    if (codecContext) {
        avcodec_free_context(&codecContext);
        codecContext = nullptr;
    }
    m_decodeCond.signal();
    m_decodeMutex.unlock();
    if (m_packetQueue) {
        m_packetQueue->abort();
    }
}

void MediaDecoder::flush() {
    if (m_packetQueue) {
        m_packetQueue->flush();
    }
    m_playerParam->m_playMutex.lock();
    avcodec_flush_buffers(getCodecContext());
    m_playerParam->m_playMutex.unlock();
}

int MediaDecoder::pushPacket(AVPacket *pkt) {
    if (m_packetQueue) {
        return m_packetQueue->pushPacket(pkt);
    }
    return 0;
}

int MediaDecoder::getPacketSize() {
    return m_packetQueue ? m_packetQueue->getPacketSize() : 0;
}

AVCodecContext *MediaDecoder::getCodecContext() {
    return nullptr;
}

int MediaDecoder::getMemorySize() {
    return m_packetQueue ? m_packetQueue->getSize() : 0;
}

int MediaDecoder::hasEnoughPackets(AVStream *stream) {
    Mutex::AutoLock lock(m_decodeMutex);
    return (m_packetQueue == nullptr) || (m_packetQueue->isAbort())
           || (stream->disposition & AV_DISPOSITION_ATTACHED_PIC)
           || (m_packetQueue->getPacketSize() > MIN_FRAMES)
              && (!m_packetQueue->getDuration()
                  || av_q2d(stream->time_base) * (double)m_packetQueue->getDuration() > 1.0);
}

void MediaDecoder::run() {

}


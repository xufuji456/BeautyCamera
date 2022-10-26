
#include "PacketQueue.h"

PacketQueue::PacketQueue() {
    size = 0;
    duration = 0;
    nb_packets = 0;
    abort_request = 0;
    last_pkt = nullptr;
    first_pkt = nullptr;
}

PacketQueue::~PacketQueue() {
    abort();
    flush();
}

int PacketQueue::put(AVPacket *pkt) {
    PacketList *pkt1;

    if (abort_request) {
        return -1;
    }

    pkt1 = (PacketList *) av_malloc(sizeof(PacketList));
    if (!pkt1) {
        return -1;
    }
    pkt1->pkt  = *pkt;
    pkt1->next = nullptr;

    if (!last_pkt) {
        first_pkt = pkt1;
    } else {
        last_pkt->next = pkt1;
    }
    last_pkt = pkt1;
    nb_packets++;
    size += pkt1->pkt.size + sizeof(*pkt1);
    duration += pkt1->pkt.duration;
    return 0;
}

int PacketQueue::pushPacket(AVPacket *pkt) {
    int ret;
    mMutex.lock();
    ret = put(pkt);
    mCondition.signal();
    mMutex.unlock();

    if (ret < 0) {
        av_packet_unref(pkt);
    }

    return ret;
}

void PacketQueue::flush() {
    PacketList *pkt, *pkt1;

    mMutex.lock();
    for (pkt = first_pkt; pkt; pkt = pkt1) {
        pkt1 = pkt->next;
        av_packet_unref(&pkt->pkt);
        av_freep(&pkt);
    }
    size = 0;
    duration = 0;
    nb_packets = 0;
    last_pkt = nullptr;
    first_pkt = nullptr;
    mCondition.signal();
    mMutex.unlock();
}

void PacketQueue::start() {
    mMutex.lock();
    abort_request = 0;
    mCondition.signal();
    mMutex.unlock();
}

int PacketQueue::getPacket(AVPacket *pkt) {
    return getPacket(pkt, 1);
}

int PacketQueue::getPacket(AVPacket *pkt, int block) {
    PacketList *pkt1;
    int ret;

    mMutex.lock();
    for (;;) {
        if (abort_request) {
            ret = -1;
            break;
        }

        pkt1 = first_pkt;
        if (pkt1) {
            first_pkt = pkt1->next;
            if (!first_pkt) {
                last_pkt = nullptr;
            }
            nb_packets--;
            size -= pkt1->pkt.size + sizeof(*pkt1);
            duration -= pkt1->pkt.duration;
            *pkt = pkt1->pkt;
            av_free(pkt1);
            ret = 1;
            break;
        } else if (!block) {
            ret = 0;
            break;
        } else {
            mCondition.wait(mMutex);
        }
    }
    mMutex.unlock();
    return ret;
}

int PacketQueue::getPacketSize() {
    Mutex::AutoLock lock(mMutex);
    return nb_packets;
}

int PacketQueue::getSize() const {
    return size;
}

int64_t PacketQueue::getDuration() const {
    return duration;
}

int PacketQueue::isAbort() const {
    return abort_request;
}

void PacketQueue::abort() {
    mMutex.lock();
    abort_request = 1;
    mCondition.signal();
    mMutex.unlock();
}


#ifndef FFPLAYER_PACKETQUEUE_H
#define FFPLAYER_PACKETQUEUE_H

#include <queue>
#include <Mutex.h>
#include <Condition.h>

extern "C" {
#include <libavcodec/avcodec.h>
}

typedef struct PacketList {
    AVPacket pkt;
    struct PacketList *next;
} PacketList;

class PacketQueue {
private:
    Mutex mMutex;
    Condition mCondition;

    int size;
    int nb_packets;
    int64_t duration;
    int abort_request;
    PacketList *first_pkt, *last_pkt;

    int put(AVPacket *pkt);

public:
    PacketQueue();

    virtual ~PacketQueue();

    int pushPacket(AVPacket *pkt);

    void flush();

    void start();

    int getPacket(AVPacket *pkt);

    int getPacket(AVPacket *pkt, int block);

    int getPacketSize();

    int getSize() const;

    int64_t getDuration() const;

    int isAbort() const;

    void abort();
};


#endif //PACKETQUEUE_H

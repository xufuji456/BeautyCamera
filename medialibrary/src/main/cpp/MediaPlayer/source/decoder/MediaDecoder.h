
#ifndef MEDIADECODER_H
#define MEDIADECODER_H

#include <AndroidLog.h>
#include <player/PlayerParam.h>
#include <queue/PacketQueue.h>
#include <queue/FrameQueue.h>

class MediaDecoder : public Runnable {

protected:
    Mutex mMutex;
    Condition mCondition;

    bool abortRequest{};
    PlayerParam *playerState;
    PacketQueue *packetQueue;

public:
    MediaDecoder(PlayerParam *playerState);

    virtual ~MediaDecoder();

    virtual void start();

    virtual void stop();

    virtual void flush();

    virtual void run();

    virtual AVCodecContext *getCodecContext();

    int pushPacket(AVPacket *pkt);

    int getPacketSize();

    int getMemorySize();

    int hasEnoughPackets(AVStream *stream);

};

#endif //MEDIADECODER_H

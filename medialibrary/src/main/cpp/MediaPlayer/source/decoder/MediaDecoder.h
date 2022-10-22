
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
    AVCodecContext *codecContext;

public:
    MediaDecoder(AVCodecContext *codecCtx, PlayerParam *playerState);

    virtual ~MediaDecoder();

    virtual void start();

    virtual void stop();

    virtual void flush();

    int pushPacket(AVPacket *pkt);

    int getPacketSize();

    AVCodecContext *getCodecContext();

    int getMemorySize();

    int hasEnoughPackets(AVStream *stream);

    virtual void run();

};

#endif //MEDIADECODER_H

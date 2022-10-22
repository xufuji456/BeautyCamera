
#ifndef MEDIADECODER_H
#define MEDIADECODER_H

#include <AndroidLog.h>
#include <player/PlayerState.h>
#include <queue/PacketQueue.h>
#include <queue/FrameQueue.h>

class MediaDecoder : public Runnable {

protected:
    Mutex mMutex;
    Condition mCondition;

    bool abortRequest{};
    AVStream *avStream;
    PlayerState *playerState;
    PacketQueue *packetQueue;
    AVCodecContext *codecContext;

public:
    MediaDecoder(AVCodecContext *codecCtx, AVStream *stream, PlayerState *playerState);

    virtual ~MediaDecoder();

    virtual void start();

    virtual void stop();

    virtual void flush();

    int pushPacket(AVPacket *pkt);

    int getPacketSize();

    AVStream *getStream();

    AVCodecContext *getCodecContext();

    int getMemorySize();

    int hasEnoughPackets();

    virtual void run();

};


#endif //MEDIADECODER_H

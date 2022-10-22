
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

    int streamIndex;
    bool abortRequest{};
    AVStream *avStream;
    PlayerState *playerState;
    PacketQueue *packetQueue;
    AVCodecContext *pCodecCtx;

public:
    MediaDecoder(AVCodecContext *avctx, AVStream *stream, int streamIndex, PlayerState *playerState);

    virtual ~MediaDecoder();

    virtual void start();

    virtual void stop();

    virtual void flush();

    int pushPacket(AVPacket *pkt);

    int getPacketSize();

    int getStreamIndex() const;

    AVStream *getStream();

    AVCodecContext *getCodecContext();

    int getMemorySize();

    int hasEnoughPackets();

    virtual void run();

};


#endif //MEDIADECODER_H

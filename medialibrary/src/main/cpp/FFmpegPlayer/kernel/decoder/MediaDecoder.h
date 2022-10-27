
#ifndef MEDIADECODER_H
#define MEDIADECODER_H

#include <player/PlayerParam.h>
#include <queue/PacketQueue.h>
#include <queue/FrameQueue.h>

class MediaDecoder : public Runnable {

protected:
    Mutex m_decodeMutex;
    Condition m_decodeCond;

    bool m_abortReq{};
    PlayerParam *m_playerParam;
    PacketQueue *m_packetQueue;

public:
    MediaDecoder(PlayerParam *playerParam);

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

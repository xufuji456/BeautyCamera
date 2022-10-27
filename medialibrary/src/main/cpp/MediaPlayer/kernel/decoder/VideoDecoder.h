
#ifndef VIDEODECODER_H
#define VIDEODECODER_H

#include <decoder/MediaDecoder.h>
#include <player/PlayerParam.h>
#include <avsync/MediaClock.h>

class VideoDecoder : public MediaDecoder {

private:

    bool m_exit{};
    int m_rotate;
    Thread *m_decodeThread;
    FrameQueue *m_frameQueue;
    MediaClock *m_masterClock;

    int decodeVideo();

public:
    VideoDecoder(PlayerParam *playerParam);

    virtual ~VideoDecoder();

    void setMasterClock(MediaClock *masterClock);

    void start() override;

    void stop() override;

    void flush() override;

    void run() override;

    AVCodecContext *getCodecContext() override;

    int getFrameSize();

    int getRotate();

    FrameQueue *getFrameQueue();

};


#endif //VIDEODECODER_H


#ifndef VIDEODECODER_H
#define VIDEODECODER_H

#include <decoder/MediaDecoder.h>
#include <player/PlayerState.h>
#include <sync/MediaClock.h>

class VideoDecoder : public MediaDecoder {

private:
    AVFormatContext *pFormatCtx;
    FrameQueue *frameQueue;

    bool mExit{};
    int mRotate;
    Thread *decodeThread;
    MediaClock *masterClock;

    int decodeVideo();

public:
    VideoDecoder(AVFormatContext *pFormatCtx, AVCodecContext *avctx,
                 AVStream *stream, PlayerState *playerState);

    virtual ~VideoDecoder();

    void setMasterClock(MediaClock *masterClock);

    void start() override;

    void stop() override;

    void flush() override;

    int getFrameSize();

    int getRotate();

    FrameQueue *getFrameQueue();

    AVFormatContext *getFormatContext();

    void run() override;

};


#endif //VIDEODECODER_H

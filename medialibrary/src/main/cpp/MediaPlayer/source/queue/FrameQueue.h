
#ifndef FFPLAYER_FRAMEQUEUE_H
#define FFPLAYER_FRAMEQUEUE_H

#include <Mutex.h>
#include <Condition.h>

extern "C" {
#include <libavcodec/avcodec.h>
}

#define FRAME_QUEUE_SIZE 16

typedef struct Frame {
    AVFrame *frame;
    AVSubtitle sub;
    double pts;
    double duration;
    int width;
    int height;
    int format;
    int uploaded;
} Frame;

class FrameQueue {

private:
    Mutex mMutex;
    Condition mCondition;
    int abort_request;
    Frame queue[FRAME_QUEUE_SIZE]{};
    int rindex;
    int windex;
    int size;
    int max_size;
    int keep_last;
    int show_index;

    static void unrefFrame(Frame *vp);

public:
    FrameQueue(int max_size, int keep_last);

    virtual ~FrameQueue();

    void start();

    void abort();

    Frame *currentFrame();

    Frame *nextFrame();

    Frame *lastFrame();

    Frame *peekWritable();

    void pushFrame();

    void popFrame();

    void flush();

    int getFrameSize() const;

    int getShowIndex() const;

};


#endif //FFPLAYER_FRAMEQUEUE_H

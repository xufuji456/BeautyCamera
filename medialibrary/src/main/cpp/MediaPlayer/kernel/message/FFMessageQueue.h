
#ifndef FF_MESSAGEQUEUE_H
#define FF_MESSAGEQUEUE_H

#include <Mutex.h>
#include <Condition.h>
#include <cstring>
#include <assert.h>

extern "C" {
#include <libavutil/mem.h>
}

#include "message/MessageDefine.h"

typedef struct FFMessage {
    int what;
    int arg1;
    int arg2;
    void *obj;
    void (*free)(void *obj);
    struct FFMessage *next;
} FFMessage;

inline static void message_init(FFMessage *msg) {
    memset(msg, 0, sizeof(FFMessage));
}

inline static void message_free(void *obj) {
    av_free(obj);
}

inline static void message_free_ptr(FFMessage *msg) {
    if (!msg || !msg->obj) {
        return;
    }
    assert(msg->free);
    msg->free(msg->obj);
    msg->obj = NULL;
}

class FFMessageQueue {

private:
    Mutex m_msgMutex;
    Condition m_msgCond;

    FFMessage *m_firstMsg;
    FFMessage *m_lastMsg;

    bool m_abortReq;
    int m_msgQueueSize;

    int putMessage(FFMessage *msg);

public:
    FFMessageQueue();

    virtual ~FFMessageQueue();

    void start();

    void flush();

    void sendMessage(int what);

    void sendMessage(int what, int arg1);

    void sendMessage(int what, int arg1, int arg2);

    void sendMessage(int what, int arg1, int arg2, void *obj, int len);

    int getMessage(FFMessage *msg);

    int getMessage(FFMessage *msg, int block);

    void stop();

    void release();

};

#endif //FF_MESSAGEQUEUE_H

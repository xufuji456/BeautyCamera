
#include "FFMessageQueue.h"

FFMessageQueue::FFMessageQueue() {
    m_msgQueueSize = 0;
    m_firstMsg = nullptr;
    m_lastMsg  = nullptr;
    m_abortReq = false;
}

FFMessageQueue::~FFMessageQueue() = default;

void FFMessageQueue::start() {
    Mutex::AutoLock lock(m_msgMutex);
    m_abortReq = false;
    FFMessage msg;
    message_init(&msg);
    msg.what = MSG_FLUSH;
}

void FFMessageQueue::flush() {
    FFMessage *msg, *msg1;
    Mutex::AutoLock lock(m_msgMutex);
    for (msg = m_firstMsg; msg != nullptr; msg = msg1) {
        msg1 = msg->next;
        av_freep(&msg);
    }
    m_msgQueueSize = 0;
    m_firstMsg = nullptr;
    m_lastMsg  = nullptr;
    m_msgCond.signal();
}

void FFMessageQueue::sendMessage(int what) {
    FFMessage msg;
    message_init(&msg);
    msg.what = what;
    putMessage(&msg);
}

void FFMessageQueue::sendMessage(int what, int arg1) {
    FFMessage msg;
    message_init(&msg);
    msg.what = what;
    msg.arg1 = arg1;
    putMessage(&msg);
}

void FFMessageQueue::sendMessage(int what, int arg1, int arg2) {
    FFMessage msg;
    message_init(&msg);
    msg.what = what;
    msg.arg1 = arg1;
    msg.arg2 = arg2;
    putMessage(&msg);
}

void FFMessageQueue::sendMessage(int what, int arg1, int arg2, void *obj, int len) {
    FFMessage msg;
    message_init(&msg);
    msg.what = what;
    msg.arg1 = arg1;
    msg.arg2 = arg2;
    msg.obj = av_malloc(sizeof(len));
    memcpy(msg.obj, obj, len);
    msg.free = message_free;
    putMessage(&msg);
}

int FFMessageQueue::getMessage(FFMessage *msg) {
    return getMessage(msg, 1);
}

int FFMessageQueue::getMessage(FFMessage *msg, int block) {
    FFMessage *msg1;
    int ret;
    m_msgMutex.lock();
    for (;;) {
        if (m_abortReq) {
            ret = -1;
            break;
        }
        msg1 = m_firstMsg;
        if (msg1) {
            m_firstMsg = msg1->next;
            if (!m_firstMsg) {
                m_lastMsg = nullptr;
            }
            m_msgQueueSize--;
            *msg = *msg1;
            msg1->obj = nullptr;
            av_free(msg1);
            ret = 1;
            break;
        } else if (!block) {
            ret = 0;
            break;
        } else {
            m_msgCond.wait(m_msgMutex);
        }
    }
    m_msgMutex.unlock();

    return ret;
}

int FFMessageQueue::putMessage(FFMessage *msg) {
    Mutex::AutoLock lock(m_msgMutex);
    FFMessage *message;
    if (m_abortReq) {
        return -1;
    }
    message = (FFMessage *) av_malloc(sizeof(FFMessage));
    if (!message) {
        return -1;
    }
    *message = *msg;
    message->next = nullptr;

    if (!m_lastMsg) {
        m_firstMsg = message;
    } else {
        m_lastMsg->next = message;
    }
    m_lastMsg = message;
    m_msgQueueSize++;
    m_msgCond.signal();
    return 0;
}

void FFMessageQueue::stop() {
    Mutex::AutoLock lock(m_msgMutex);
    m_abortReq = true;
    m_msgCond.signal();
}

void FFMessageQueue::release() {
    flush();
}

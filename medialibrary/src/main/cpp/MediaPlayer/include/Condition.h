
#ifndef CONDITION_H
#define CONDITION_H

#include <stdint.h>
#include <sys/types.h>
#include <time.h>
#include <pthread.h>

#include <Mutex.h>

typedef int64_t nsecs_t;

class Condition {
public:
    enum {
        PRIVATE = 0,
        SHARED = 1
    };

    enum WakeUpType {
        WAKE_UP_ONE = 0,
        WAKE_UP_ALL = 1
    };

    Condition();
    Condition(int type);
    ~Condition();

    int wait(Mutex& mutex);
    int waitRelative(Mutex& mutex, nsecs_t timeNs);
    int waitRelativeMs(Mutex& mutex, nsecs_t times);
    void signal();
    void signal(WakeUpType type) {
        if (type == WAKE_UP_ONE) {
            signal();
        } else {
            broadcast();
        }
    }
    void broadcast();
private:
    pthread_cond_t mCond;
};

inline Condition::Condition() {
    pthread_cond_init(&mCond, NULL);
}

inline Condition::Condition(int type) {
    if (type == SHARED) {
        pthread_condattr_t attr;
        pthread_condattr_init(&attr);
        pthread_condattr_setpshared(&attr, PTHREAD_PROCESS_SHARED);
        pthread_cond_init(&mCond, &attr);
        pthread_condattr_destroy(&attr);
    } else {
        pthread_cond_init(&mCond, NULL);
    }
}

inline Condition::~Condition() {
    pthread_cond_destroy(&mCond);
}

inline int Condition::wait(Mutex &mutex) {
    return pthread_cond_wait(&mCond, &mutex.mMutex);
}

inline int Condition::waitRelative(Mutex &mutex, nsecs_t time) {
    struct timeval t;
    struct timespec ts;
    gettimeofday(&t, NULL);
    ts.tv_sec  = t.tv_sec;
    ts.tv_nsec = t.tv_usec*1000;

    ts.tv_sec  += time / 1000000000;
    ts.tv_nsec += time % 1000000000;
    if (ts.tv_nsec >= 1000000000) {
        ts.tv_nsec -= 1000000000;
        ts.tv_sec  += 1;
    }
    return pthread_cond_timedwait(&mCond, &mutex.mMutex, &ts);
}

inline int Condition::waitRelativeMs(Mutex &mutex, nsecs_t times) {
    return waitRelative(mutex, times * 1000000);
}

inline void Condition::signal() {
    pthread_cond_signal(&mCond);
}

inline void Condition::broadcast() {
    pthread_cond_broadcast(&mCond);
}

#endif //CONDITION_H

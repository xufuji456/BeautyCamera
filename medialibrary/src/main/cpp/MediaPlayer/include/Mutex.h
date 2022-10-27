
#ifndef MUTEX_H
#define MUTEX_H

#include <stdint.h>
#include <sys/types.h>
#include <time.h>
#include <pthread.h>

class Condition;

class Mutex {
private:
    friend class Condition;

    Mutex(const Mutex&);
    Mutex& operator = (const Mutex&);

    pthread_mutex_t mMutex;

public:
    enum {
        PRIVATE = 0,
        SHARED  = 1
    };

    Mutex();
    Mutex(const char* name);
    Mutex(int type, const char* name = NULL);
    ~Mutex();

    int lock();
    int unlock();
    int tryLock();

    class AutoLock {
    public:
        inline AutoLock(Mutex& mutex) : mLock(mutex)  { mLock.lock(); }
        inline AutoLock(Mutex* mutex) : mLock(*mutex) { mLock.lock(); }
        inline ~AutoLock() { mLock.unlock(); }
    private:
        Mutex& mLock;
    };
};

inline Mutex::Mutex() {
    pthread_mutex_init(&mMutex, NULL);
}

inline Mutex::Mutex(const char* name) {
    pthread_mutex_init(&mMutex, NULL);
}


inline Mutex::Mutex(int type, const char* name) {
    if (type == SHARED) {
        pthread_mutexattr_t attr;
        pthread_mutexattr_init(&attr);
        pthread_mutexattr_setpshared(&attr, PTHREAD_PROCESS_SHARED);
        pthread_mutex_init(&mMutex, &attr);
        pthread_mutexattr_destroy(&attr);
    } else {
        pthread_mutex_init(&mMutex, NULL);
    }
}

inline Mutex::~Mutex() {
    pthread_mutex_destroy(&mMutex);
}

inline int Mutex::lock() {
    return pthread_mutex_lock(&mMutex);
}

inline int Mutex::unlock() {
    return pthread_mutex_unlock(&mMutex);
}

inline int Mutex::tryLock() {
    return pthread_mutex_trylock(&mMutex);
}

typedef Mutex::AutoLock AutoMutex;

#endif //MUTEX_H

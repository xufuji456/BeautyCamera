
#include <jni.h>
#include <Mutex.h>
#include <Condition.h>
#include <Errors.h>
#include "../../nativehelp/JNIHelp.h"
#include <FFMediaPlayer.h>

extern "C" {
#include <libavcodec/jni.h>
}

const char *CLASS_NAME = "com/frank/media/FFMediaPlayer";

#include <android/log.h>
#define PLAYER_TAG "FFmpegPlayer"
#define VLOGI(FORMAT, ...) __android_log_vprint(ANDROID_LOG_INFO, PLAYER_TAG, FORMAT, ##__VA_ARGS__)
#define VLOGE(FORMAT, ...) __android_log_vprint(ANDROID_LOG_ERROR, PLAYER_TAG, FORMAT, ##__VA_ARGS__)

struct fields_t {
    jfieldID    context;
    jmethodID   post_event;
};
static fields_t fields;

static JavaVM *javaVM = nullptr;

static JNIEnv *getJNIEnv() {
    JNIEnv *env;
    assert(javaVM != nullptr);
    if (javaVM->GetEnv((void **) &env, JNI_VERSION_1_4) != JNI_OK) {
        return nullptr;
    }
    return env;
}

// -------------------------------------------------------------------------------------------------
class JNIMediaPlayerListener : public MediaPlayerListener {
private:
    jclass mClass;
    jobject mObject;

public:
    JNIMediaPlayerListener(JNIEnv* env, jobject thiz, jobject weak_thiz);
    ~JNIMediaPlayerListener();
    void notify(int msg, int ext1, int ext2, void *obj) override;
};

JNIMediaPlayerListener::JNIMediaPlayerListener(JNIEnv *env, jobject thiz, jobject weak_thiz) {

    jclass clazz = env->GetObjectClass(thiz);
    if (clazz == nullptr) {
        ALOGE("Can't find FFMediaPlayer");
        jniThrowException(env, "java/lang/Exception");
        return;
    }
    mClass   = (jclass)env->NewGlobalRef(clazz);
    mObject  = env->NewGlobalRef(weak_thiz);
}

JNIMediaPlayerListener::~JNIMediaPlayerListener() {
    JNIEnv *env = getJNIEnv();
    env->DeleteGlobalRef(mObject);
    env->DeleteGlobalRef(mClass);
}

void JNIMediaPlayerListener::notify(int msg, int ext1, int ext2, void *obj) {
    JNIEnv *env = getJNIEnv();

    bool status = (javaVM->AttachCurrentThread(&env, nullptr) >= 0);

    env->CallStaticVoidMethod(mClass, fields.post_event, mObject,
                              msg, ext1, ext2, obj);

    if (env->ExceptionCheck()) {
        ALOGW("An exception occurred while notifying an event.");
        env->ExceptionClear();
    }

    if (status) {
        javaVM->DetachCurrentThread();
    }
}

static FFMediaPlayer *getMediaPlayer(JNIEnv *env, jobject thiz) {
    return (FFMediaPlayer *) env->GetLongField(thiz, fields.context);
}

static FFMediaPlayer *setMediaPlayer(JNIEnv *env, jobject thiz, long mediaPlayer) {
    auto *old = (FFMediaPlayer *) env->GetLongField(thiz, fields.context);
    env->SetLongField(thiz, fields.context, mediaPlayer);
    return old;
}

// If exception is NULL and opStatus is not OK, this method sends an error
// event to the client application; otherwise, if exception is not NULL and
// opStatus is not OK, this method throws the given exception to the client
// application.
static void process_media_player_call(JNIEnv *env, jobject thiz, int opStatus,
        const char* exception, const char *message) {
    if (exception == nullptr) {  // Don't throw exception. Instead, send an event.
        if (opStatus != (int) OK) {
            FFMediaPlayer* mp = getMediaPlayer(env, thiz);
            if (mp != nullptr) mp->notify(MEDIA_ERROR, opStatus, 0);
        }
    } else {
        if ( opStatus == (int) INVALID_OPERATION ) {
            jniThrowException(env, "java/lang/IllegalStateException");
        } else if ( opStatus == (int) PERMISSION_DENIED ) {
            jniThrowException(env, "java/lang/SecurityException");
        } else if ( opStatus != (int) OK ) {
            if (strlen(message) > 200) {
                jniThrowException( env, exception, message);
            } else {
                char msg[256];
                sprintf(msg, "%s: status=0x%X", message, opStatus);
                jniThrowException( env, exception, msg);
            }
        }
    }
}

void FFMediaPlayer_setDataSource(JNIEnv *env, jobject thiz, jstring path_) {

    FFMediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == nullptr) {
        jniThrowException(env, "java/lang/IllegalStateException");
        return;
    }

    if (path_ == nullptr) {
        jniThrowException(env, "java/lang/IllegalArgumentException");
        return;
    }

    const char *path = env->GetStringUTFChars(path_, JNI_FALSE);
    if (path == nullptr) {
        return;
    }

    status_t opStatus = mp->setDataSource(path);
    process_media_player_call(env, thiz, opStatus, "java/io/IOException",
            "setDataSource failed." );

    env->ReleaseStringUTFChars(path_, path);
}

void FFMediaPlayer_setDataSourceFD(JNIEnv *env, jobject thiz, jobject fileDescriptor, jlong length) {
    FFMediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == nullptr) {
        jniThrowException(env, "java/lang/IllegalStateException");
        return;
    }

    if (fileDescriptor == nullptr) {
        jniThrowException(env, "java/lang/IllegalArgumentException");
        return;
    }

    int fd = jniGetFDFromFileDescriptor(env, fileDescriptor);
    char path[256] = "";
    int myfd = dup(fd);
    char str[20];
    sprintf(str, "pipe:%d", myfd);
    strcat(path, str);

    status_t opStatus = mp->setDataSource(path);
    process_media_player_call( env, thiz, opStatus, "java/io/IOException",
            "setDataSourceFD failed.");
}

void log_callback(void *ptr, int level, const char *format, va_list args) {
    switch (level) {
        case AV_LOG_INFO:
            VLOGI(format, args);
            break;
        case AV_LOG_ERROR:
            VLOGE(format, args);
            break;
        default:
            break;
    }
}

void FFMediaPlayer_init(JNIEnv *env) {
    av_log_set_level(AV_LOG_INFO);
    av_log_set_callback(log_callback);

    jclass clazz = env->FindClass(CLASS_NAME);
    if (clazz == nullptr) {
        return;
    }
    fields.context = env->GetFieldID(clazz, "mNativeContext", "J");
    if (fields.context == nullptr) {
        return;
    }

    fields.post_event = env->GetStaticMethodID(clazz, "postEventFromNative",
                                               "(Ljava/lang/Object;IIILjava/lang/Object;)V");
    if (fields.post_event == nullptr) {
        return;
    }

    env->DeleteLocalRef(clazz);
}

void FFMediaPlayer_setup(JNIEnv *env, jobject thiz, jobject mediaplayer_this) {
    auto *mp = new FFMediaPlayer();
    mp->init();
    // create new listener and give it to MediaPlayer
    auto *listener = new JNIMediaPlayerListener(env, thiz, mediaplayer_this);
    mp->setListener(listener);
    // Stow our new C++ MediaPlayer in an opaque field in the Java object.
    setMediaPlayer(env, thiz, (long)mp);
}

void FFMediaPlayer_release(JNIEnv *env, jobject thiz) {
    FFMediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp != nullptr) {
        mp->disconnect();
        delete mp;
        setMediaPlayer(env, thiz, 0);
    }
}

void FFMediaPlayer_reset(JNIEnv *env, jobject thiz) {
    FFMediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == nullptr) {
        jniThrowException(env, "java/lang/IllegalStateException");
        return;
    }
    mp->reset();
}

void FFMediaPlayer_finalize(JNIEnv *env, jobject thiz) {
    FFMediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp != nullptr) {
        ALOGW("MediaPlayer finalized without being released");
    }
    FFMediaPlayer_release(env, thiz);
}

void FFMediaPlayer_setVideoSurface(JNIEnv *env, jobject thiz, jobject surface) {
    FFMediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == nullptr) {
        jniThrowException(env, "java/lang/IllegalStateException");
        return;
    }
    ANativeWindow *window = nullptr;
    if (surface != nullptr) {
        window = ANativeWindow_fromSurface(env, surface);
    }
    mp->setVideoSurface(window);
}

void FFMediaPlayer_prepare(JNIEnv *env, jobject thiz) {
    FFMediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == nullptr) {
        jniThrowException(env, "java/lang/IllegalStateException");
        return;
    }

    mp->prepare();
}

void FFMediaPlayer_prepareAsync(JNIEnv *env, jobject thiz) {
    FFMediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == nullptr) {
        jniThrowException(env, "java/lang/IllegalStateException");
        return;
    }
    mp->prepareAsync();
}

void FFMediaPlayer_start(JNIEnv *env, jobject thiz) {
    FFMediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == nullptr) {
        jniThrowException(env, "java/lang/IllegalStateException");
        return;
    }
    mp->start();
}

void FFMediaPlayer_pause(JNIEnv *env, jobject thiz) {
    FFMediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == nullptr) {
        jniThrowException(env, "java/lang/IllegalStateException");
        return;
    }
    mp->pause();
}

void FFMediaPlayer_resume(JNIEnv *env, jobject thiz) {
    FFMediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == nullptr) {
        jniThrowException(env, "java/lang/IllegalStateException");
        return;
    }
    mp->resume();

}

void FFMediaPlayer_stop(JNIEnv *env, jobject thiz) {
    FFMediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == nullptr) {
        jniThrowException(env, "java/lang/IllegalStateException");
        return;
    }
    mp->stop();
}

void FFMediaPlayer_seekTo(JNIEnv *env, jobject thiz, jlong timeMs) {
    FFMediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == nullptr) {
        jniThrowException(env, "java/lang/IllegalStateException");
        return;
    }
    mp->seekTo(timeMs);
}

void FFMediaPlayer_setMute(JNIEnv *env, jobject thiz, jboolean mute) {
    FFMediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == nullptr) {
        jniThrowException(env, "java/lang/IllegalStateException");
        return;
    }
    mp->setMute(mute);
}

void FFMediaPlayer_setVolume(JNIEnv *env, jobject thiz, jfloat volume) {
    FFMediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == nullptr) {
        jniThrowException(env, "java/lang/IllegalStateException");
        return;
    }
    mp->setVolume(volume);
}

void FFMediaPlayer_setRate(JNIEnv *env, jobject thiz, jfloat speed) {
    FFMediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == nullptr) {
        jniThrowException(env, "java/lang/IllegalStateException");
        return;
    }
    mp->setRate(speed);
}

jlong FFMediaPlayer_getCurrentPosition(JNIEnv *env, jobject thiz) {

    FFMediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == nullptr) {
        jniThrowException(env, "java/lang/IllegalStateException");
        return 0L;
    }
    return mp->getCurrentPosition();
}

jlong FFMediaPlayer_getDuration(JNIEnv *env, jobject thiz) {
    FFMediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == nullptr) {
        jniThrowException(env, "java/lang/IllegalStateException");
        return 0L;
    }
    return mp->getDuration();
}

jboolean FFMediaPlayer_isPlaying(JNIEnv *env, jobject thiz) {
    FFMediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == nullptr) {
        jniThrowException(env, "java/lang/IllegalStateException");
        return JNI_FALSE;
    }
    return (jboolean)(mp->isPlaying() ? JNI_TRUE : JNI_FALSE);
}

jint FFMediaPlayer_getRotate(JNIEnv *env, jobject thiz) {
    FFMediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == nullptr) {
        jniThrowException(env, "java/lang/IllegalStateException");
        return 0;
    }
    return mp->getRotate();
}

jint FFMediaPlayer_getVideoWidth(JNIEnv *env, jobject thiz) {
    FFMediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == nullptr) {
        jniThrowException(env, "java/lang/IllegalStateException");
        return 0;
    }
    return mp->getVideoWidth();
}

jint FFMediaPlayer_getVideoHeight(JNIEnv *env, jobject thiz) {
    FFMediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == nullptr) {
        jniThrowException(env, "java/lang/IllegalStateException");
        return 0;
    }
    return mp->getVideoHeight();
}

jstring FFMediaPlayer_getMediaFormat(JNIEnv *env, jobject thiz) {
    FFMediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == nullptr) {
        return nullptr;
    }
    return env->NewStringUTF(mp->getMediaFormat());
}

static const JNINativeMethod gMethods[] = {
        {"_setDataSource", "(Ljava/lang/String;)V", (void *)FFMediaPlayer_setDataSource},
        {"_setDataSource", "(Ljava/io/FileDescriptor;J)V", (void *)FFMediaPlayer_setDataSourceFD},
        {"_setVideoSurface", "(Landroid/view/Surface;)V", (void *) FFMediaPlayer_setVideoSurface},
        {"_prepare", "()V", (void *) FFMediaPlayer_prepare},
        {"_prepareAsync", "()V", (void *) FFMediaPlayer_prepareAsync},
        {"_start", "()V", (void *) FFMediaPlayer_start},
        {"_stop", "()V", (void *) FFMediaPlayer_stop},
        {"_resume", "()V", (void *) FFMediaPlayer_resume},
        {"_getRotate", "()I", (void *) FFMediaPlayer_getRotate},
        {"_getVideoWidth", "()I", (void *) FFMediaPlayer_getVideoWidth},
        {"_getVideoHeight", "()I", (void *) FFMediaPlayer_getVideoHeight},
        {"_seekTo", "(J)V", (void *) FFMediaPlayer_seekTo},
        {"_pause", "()V", (void *) FFMediaPlayer_pause},
        {"_isPlaying", "()Z", (void *) FFMediaPlayer_isPlaying},
        {"_getCurrentPosition", "()J", (void *) FFMediaPlayer_getCurrentPosition},
        {"_getDuration", "()J", (void *) FFMediaPlayer_getDuration},
        {"_release", "()V", (void *) FFMediaPlayer_release},
        {"_reset", "()V", (void *) FFMediaPlayer_reset},
        {"_setVolume", "(F)V", (void *) FFMediaPlayer_setVolume},
        {"_setMute", "(Z)V", (void *) FFMediaPlayer_setMute},
        {"_setRate", "(F)V", (void *) FFMediaPlayer_setRate},
        {"native_getMediaFormat", "()Ljava/lang/String;", (void *) FFMediaPlayer_getMediaFormat},
        {"native_init", "()V", (void *)FFMediaPlayer_init},
        {"native_setup", "(Ljava/lang/Object;)V", (void *) FFMediaPlayer_setup},
        {"native_finalize", "()V", (void *) FFMediaPlayer_finalize},
};

// 注册CainMediaPlayer的Native方法
static int register_com_cgfay_media_CainMediaPlayer(JNIEnv *env) {
    int numMethods = (sizeof(gMethods) / sizeof( (gMethods)[0]));
    jclass clazz = env->FindClass(CLASS_NAME);
    if (clazz == nullptr) {
        ALOGE("Native registration unable to find class '%s'", CLASS_NAME);
        return JNI_ERR;
    }
    if (env->RegisterNatives(clazz, gMethods, numMethods) < 0) {
        ALOGE("Native registration unable to find class '%s'", CLASS_NAME);
        return JNI_ERR;
    }
    env->DeleteLocalRef(clazz);

    return JNI_OK;
}

extern "C"
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    av_jni_set_java_vm(vm, nullptr);
    javaVM = vm;
    JNIEnv *env;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_4) != JNI_OK) {
        return -1;
    }
    if (register_com_cgfay_media_CainMediaPlayer(env) != JNI_OK) {
        return -1;
    }
    return JNI_VERSION_1_4;
}

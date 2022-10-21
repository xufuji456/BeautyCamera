//
// Created by cain on 2018/11/29.
//

#if defined(__ANDROID__)

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

// -------------------------------------------------------------------------------------------------
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
public:
    JNIMediaPlayerListener(JNIEnv* env, jobject thiz, jobject weak_thiz);
    ~JNIMediaPlayerListener();
    void notify(int msg, int ext1, int ext2, void *obj) override;

private:
    JNIMediaPlayerListener();
    jclass mClass;
    jobject mObject;
};

JNIMediaPlayerListener::JNIMediaPlayerListener(JNIEnv *env, jobject thiz, jobject weak_thiz) {
    // Hold onto the MediaPlayer class for use in calling the static method
    // that posts events to the application thread.
    jclass clazz = env->GetObjectClass(thiz);
    if (clazz == nullptr) {
        ALOGE("Can't find com/cgfay/media/FFMediaPlayer");
        jniThrowException(env, "java/lang/Exception");
        return;
    }
    mClass = (jclass)env->NewGlobalRef(clazz);

    // We use a weak reference so the MediaPlayer object can be garbage collected.
    // The reference is only used as a proxy for callbacks.
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

// -------------------------------------------------------------------------------------------------

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
    } else {  // Throw exception!
        if ( opStatus == (int) INVALID_OPERATION ) {
            jniThrowException(env, "java/lang/IllegalStateException");
        } else if ( opStatus == (int) PERMISSION_DENIED ) {
            jniThrowException(env, "java/lang/SecurityException");
        } else if ( opStatus != (int) OK ) {
            if (strlen(message) > 230) {
                // if the message is too long, don't bother displaying the status code
                jniThrowException( env, exception, message);
            } else {
                char msg[256];
                // append the status code to the message
                sprintf(msg, "%s: status=0x%X", message, opStatus);
                jniThrowException( env, exception, msg);
            }
        }
    }
}

void CainMediaPlayer_setDataSourceAndHeaders(JNIEnv *env, jobject thiz, jstring path_,
        jobjectArray keys, jobjectArray values) {

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

    const char *restrict = strstr(path, "mms://");
    char *restrict_to = restrict ? strdup(restrict) : nullptr;
    if (restrict_to != nullptr) {
        strncpy(restrict_to, "mmsh://", 6);
        puts(path);
    }

    char *headers = nullptr;
    if (keys && values != nullptr) {
        int keysCount = env->GetArrayLength(keys);
        int valuesCount = env->GetArrayLength(values);

        if (keysCount != valuesCount) {
            ALOGE("keys and values arrays have different length");
            jniThrowException(env, "java/lang/IllegalArgumentException");
            return;
        }

        int i = 0;
        const char *rawString = nullptr;
        char hdrs[2048];

        for (i = 0; i < keysCount; i++) {
            jstring key = (jstring) env->GetObjectArrayElement(keys, i);
            rawString = env->GetStringUTFChars(key, JNI_FALSE);
            strcat(hdrs, rawString);
            strcat(hdrs, ": ");
            env->ReleaseStringUTFChars(key, rawString);

            jstring value = (jstring) env->GetObjectArrayElement(values, i);
            rawString = env->GetStringUTFChars(value, JNI_FALSE);
            strcat(hdrs, rawString);
            strcat(hdrs, "\r\n");
            env->ReleaseStringUTFChars(value, rawString);
        }

        headers = &hdrs[0];
    }

    status_t opStatus = mp->setDataSource(path, 0, headers);
    process_media_player_call(env, thiz, opStatus, "java/io/IOException",
            "setDataSource failed." );

    env->ReleaseStringUTFChars(path_, path);
}

void CainMediaPlayer_setDataSource(JNIEnv *env, jobject thiz, jstring path_) {
    CainMediaPlayer_setDataSourceAndHeaders(env, thiz, path_, nullptr, nullptr);
}

void CainMediaPlayer_setDataSourceFD(JNIEnv *env, jobject thiz, jobject fileDescriptor,
                                     jlong offset, jlong length) {
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
    if (offset < 0 || length < 0 || fd < 0) {
        if (offset < 0) {
            ALOGE("negative offset (%ld)", offset);
        }
        if (length < 0) {
            ALOGE("negative length (%ld)", length);
        }
        if (fd < 0) {
            ALOGE("invalid file descriptor");
        }
        jniThrowException(env, "java/lang/IllegalArgumentException");
        return;
    }

    char path[256] = "";
    int myfd = dup(fd);
    char str[20];
    sprintf(str, "pipe:%d", myfd);
    strcat(path, str);

    status_t opStatus = mp->setDataSource(path, offset, nullptr);
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

void CainMediaPlayer_init(JNIEnv *env) {
    // TODO: ffmpeg log callback
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

void CainMediaPlayer_setup(JNIEnv *env, jobject thiz, jobject mediaplayer_this) {

    FFMediaPlayer *mp = new FFMediaPlayer();

    mp->init();

    // create new listener and give it to MediaPlayer
    JNIMediaPlayerListener *listener = new JNIMediaPlayerListener(env, thiz, mediaplayer_this);
    mp->setListener(listener);

    // Stow our new C++ MediaPlayer in an opaque field in the Java object.
    setMediaPlayer(env, thiz, (long)mp);
}

void CainMediaPlayer_release(JNIEnv *env, jobject thiz) {
    FFMediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp != nullptr) {
        mp->disconnect();
        delete mp;
        setMediaPlayer(env, thiz, 0);
    }
}

void CainMediaPlayer_reset(JNIEnv *env, jobject thiz) {
    FFMediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == nullptr) {
        jniThrowException(env, "java/lang/IllegalStateException");
        return;
    }
    mp->reset();
}

void CainMediaPlayer_finalize(JNIEnv *env, jobject thiz) {
    FFMediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp != nullptr) {
        ALOGW("MediaPlayer finalized without being released");
    }
    CainMediaPlayer_release(env, thiz);
}

void CainMediaPlayer_setVideoSurface(JNIEnv *env, jobject thiz, jobject surface) {
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

void CainMediaPlayer_setLooping(JNIEnv *env, jobject thiz, jboolean looping) {
    FFMediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == nullptr) {
        jniThrowException(env, "java/lang/IllegalStateException");
        return;
    }
    mp->setLooping(looping);
}

jboolean CainMediaPlayer_isLooping(JNIEnv *env, jobject thiz) {
    FFMediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == nullptr) {
        jniThrowException(env, "java/lang/IllegalStateException");
        return JNI_FALSE;
    }
    return (jboolean)(mp->isLooping() ? JNI_TRUE : JNI_FALSE);
}

void CainMediaPlayer_prepare(JNIEnv *env, jobject thiz) {
    FFMediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == nullptr) {
        jniThrowException(env, "java/lang/IllegalStateException");
        return;
    }

    mp->prepare();
}

void CainMediaPlayer_prepareAsync(JNIEnv *env, jobject thiz) {
    FFMediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == nullptr) {
        jniThrowException(env, "java/lang/IllegalStateException");
        return;
    }
    mp->prepareAsync();
}

void CainMediaPlayer_start(JNIEnv *env, jobject thiz) {
    FFMediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == nullptr) {
        jniThrowException(env, "java/lang/IllegalStateException");
        return;
    }
    mp->start();
}

void CainMediaPlayer_pause(JNIEnv *env, jobject thiz) {
    FFMediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == nullptr) {
        jniThrowException(env, "java/lang/IllegalStateException");
        return;
    }
    mp->pause();
}

void CainMediaPlayer_resume(JNIEnv *env, jobject thiz) {
    FFMediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == nullptr) {
        jniThrowException(env, "java/lang/IllegalStateException");
        return;
    }
    mp->resume();

}

void CainMediaPlayer_stop(JNIEnv *env, jobject thiz) {
    FFMediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == nullptr) {
        jniThrowException(env, "java/lang/IllegalStateException");
        return;
    }
    mp->stop();
}

void CainMediaPlayer_seekTo(JNIEnv *env, jobject thiz, jfloat timeMs) {
    FFMediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == nullptr) {
        jniThrowException(env, "java/lang/IllegalStateException");
        return;
    }
    mp->seekTo(timeMs);
}

void CainMediaPlayer_setMute(JNIEnv *env, jobject thiz, jboolean mute) {
    FFMediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == nullptr) {
        jniThrowException(env, "java/lang/IllegalStateException");
        return;
    }
    mp->setMute(mute);
}

void CainMediaPlayer_setVolume(JNIEnv *env, jobject thiz, jfloat volume) {
    FFMediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == nullptr) {
        jniThrowException(env, "java/lang/IllegalStateException");
        return;
    }
    mp->setVolume(volume);
}

void CainMediaPlayer_setRate(JNIEnv *env, jobject thiz, jfloat speed) {
    FFMediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == nullptr) {
        jniThrowException(env, "java/lang/IllegalStateException");
        return;
    }
    mp->setRate(speed);
}

jlong CainMediaPlayer_getCurrentPosition(JNIEnv *env, jobject thiz) {

    FFMediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == nullptr) {
        jniThrowException(env, "java/lang/IllegalStateException");
        return 0L;
    }
    return mp->getCurrentPosition();
}

jlong CainMediaPlayer_getDuration(JNIEnv *env, jobject thiz) {
    FFMediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == nullptr) {
        jniThrowException(env, "java/lang/IllegalStateException");
        return 0L;
    }
    return mp->getDuration();
}

jboolean CainMediaPlayer_isPlaying(JNIEnv *env, jobject thiz) {
    FFMediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == nullptr) {
        jniThrowException(env, "java/lang/IllegalStateException");
        return JNI_FALSE;
    }
    return (jboolean)(mp->isPlaying() ? JNI_TRUE : JNI_FALSE);
}

jint CainMediaPlayer_getRotate(JNIEnv *env, jobject thiz) {
    FFMediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == nullptr) {
        jniThrowException(env, "java/lang/IllegalStateException");
        return 0;
    }
    return mp->getRotate();
}

jint CainMediaPlayer_getVideoWidth(JNIEnv *env, jobject thiz) {
    FFMediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == nullptr) {
        jniThrowException(env, "java/lang/IllegalStateException");
        return 0;
    }
    return mp->getVideoWidth();
}

jint CainMediaPlayer_getVideoHeight(JNIEnv *env, jobject thiz) {
    FFMediaPlayer *mp = getMediaPlayer(env, thiz);
    if (mp == nullptr) {
        jniThrowException(env, "java/lang/IllegalStateException");
        return 0;
    }
    return mp->getVideoHeight();
}

static const JNINativeMethod gMethods[] = {
        {"_setDataSource", "(Ljava/lang/String;)V", (void *)CainMediaPlayer_setDataSource},
        {"_setDataSource", "(Ljava/io/FileDescriptor;JJ)V", (void *)CainMediaPlayer_setDataSourceFD},
        {"_setVideoSurface", "(Landroid/view/Surface;)V", (void *) CainMediaPlayer_setVideoSurface},
        {"_prepare", "()V", (void *) CainMediaPlayer_prepare},
        {"_prepareAsync", "()V", (void *) CainMediaPlayer_prepareAsync},
        {"_start", "()V", (void *) CainMediaPlayer_start},
        {"_stop", "()V", (void *) CainMediaPlayer_stop},
        {"_resume", "()V", (void *) CainMediaPlayer_resume},
        {"_getRotate", "()I", (void *) CainMediaPlayer_getRotate},
        {"_getVideoWidth", "()I", (void *) CainMediaPlayer_getVideoWidth},
        {"_getVideoHeight", "()I", (void *) CainMediaPlayer_getVideoHeight},
        {"_seekTo", "(F)V", (void *) CainMediaPlayer_seekTo},
        {"_pause", "()V", (void *) CainMediaPlayer_pause},
        {"_isPlaying", "()Z", (void *) CainMediaPlayer_isPlaying},
        {"_getCurrentPosition", "()J", (void *) CainMediaPlayer_getCurrentPosition},
        {"_getDuration", "()J", (void *) CainMediaPlayer_getDuration},
        {"_release", "()V", (void *) CainMediaPlayer_release},
        {"_reset", "()V", (void *) CainMediaPlayer_reset},
        {"_setVolume", "(F)V", (void *) CainMediaPlayer_setVolume},
        {"_setMute", "(Z)V", (void *) CainMediaPlayer_setMute},
        {"_setRate", "(F)V", (void *) CainMediaPlayer_setRate},
        {"native_init", "()V", (void *)CainMediaPlayer_init},
        {"native_setup", "(Ljava/lang/Object;)V", (void *) CainMediaPlayer_setup},
        {"native_finalize", "()V", (void *) CainMediaPlayer_finalize},
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

#endif  /* defined(__ANDROID__) */

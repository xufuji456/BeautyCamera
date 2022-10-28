
#include <jni.h>
#include <Mutex.h>
#include <unistd.h>
#include <Condition.h>
#include <FFmpegPlayer.h>

extern "C" {
#include <libavcodec/jni.h>
}

const char *CLASS_NAME = "com/frank/media/player/FFmpegPlayer";

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
        ALOGE("Can't find FFmpegPlayer");
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
        ALOGE("An exception occurred while notifying an event.");
        env->ExceptionClear();
    }

    if (status) {
        javaVM->DetachCurrentThread();
    }
}

static FFmpegPlayer *getFFmpegPlayer(JNIEnv *env, jobject thiz) {
    return (FFmpegPlayer *) env->GetLongField(thiz, fields.context);
}

static FFmpegPlayer *setFFmpegPlayer(JNIEnv *env, jobject thiz, long mediaPlayer) {
    auto *old = (FFmpegPlayer *) env->GetLongField(thiz, fields.context);
    env->SetLongField(thiz, fields.context, mediaPlayer);
    return old;
}

static void process_media_player_call(JNIEnv *env, jobject thiz, int opStatus) {
    if (opStatus < 0) {
        FFmpegPlayer* mp = getFFmpegPlayer(env, thiz);
        if (mp != nullptr) mp->notify(MEDIA_ERROR, opStatus, 0);
    }
}

void FFmpegPlayer_setDataSource(JNIEnv *env, jobject thiz, jstring jpath) {
    FFmpegPlayer *mp = getFFmpegPlayer(env, thiz);
    if (mp == nullptr) {
        return;
    }
    if (jpath == nullptr) {
        ALOGE("datasource is nullptr");
        return;
    }

    const char *path = env->GetStringUTFChars(jpath, JNI_FALSE);
    if (path == nullptr) {
        return;
    }
    int opStatus = mp->setDataSource(path);
    process_media_player_call(env, thiz, opStatus);
    env->ReleaseStringUTFChars(jpath, path);
}

int jniGetFDFromFileDescriptor(JNIEnv* env, jobject fileDescriptor) {
    jint fd = -1;
    jclass fdClass = env->FindClass("java/io/FileDescriptor");

    if (fdClass != nullptr) {
        jfieldID fdClassDescriptorFieldID = env->GetFieldID(fdClass, "descriptor", "I");
        if (fdClassDescriptorFieldID != nullptr && fileDescriptor != nullptr) {
            fd = env->GetIntField(fileDescriptor, fdClassDescriptorFieldID);
        }
    }

    return fd;
}

void FFmpegPlayer_setDataSourceFD(JNIEnv *env, jobject thiz, jobject fileDescriptor, jlong length) {
    FFmpegPlayer *mp = getFFmpegPlayer(env, thiz);
    if (mp == nullptr) {
        return;
    }
    if (fileDescriptor == nullptr) {
        ALOGE("file descriptor is nullptr");
        return;
    }

    int fd = jniGetFDFromFileDescriptor(env, fileDescriptor);
    char path[256] = "";
    int myfd = dup(fd);
    char str[20];
    sprintf(str, "pipe:%d", myfd);
    strcat(path, str);

    int opStatus = mp->setDataSource(path);
    process_media_player_call( env, thiz, opStatus);
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

void FFmpegPlayer_init(JNIEnv *env) {
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

void FFmpegPlayer_setup(JNIEnv *env, jobject thiz, jobject mediaplayer_this) {
    auto *mp = new FFmpegPlayer();
    mp->init();
    // create new listener and give it to MediaPlayer
    auto *listener = new JNIMediaPlayerListener(env, thiz, mediaplayer_this);
    mp->setListener(listener);
    // Stow our new C++ MediaPlayer in an opaque field in the Java object.
    setFFmpegPlayer(env, thiz, (long) mp);
}

void FFmpegPlayer_release(JNIEnv *env, jobject thiz) {
    FFmpegPlayer *mp = getFFmpegPlayer(env, thiz);
    if (mp != nullptr) {
        mp->disconnect();
        delete mp;
        setFFmpegPlayer(env, thiz, 0);
    }
}

void FFmpegPlayer_reset(JNIEnv *env, jobject thiz) {
    FFmpegPlayer *mp = getFFmpegPlayer(env, thiz);
    if (mp == nullptr) {
        return;
    }
    mp->reset();
}

void FFmpegPlayer_finalize(JNIEnv *env, jobject thiz) {
    FFmpegPlayer *mp = getFFmpegPlayer(env, thiz);
    if (mp != nullptr) {
        ALOGE("MediaPlayer finalized without being released");
    }
    FFmpegPlayer_release(env, thiz);
}

void FFmpegPlayer_setVideoSurface(JNIEnv *env, jobject thiz, jobject surface) {
    FFmpegPlayer *mp = getFFmpegPlayer(env, thiz);
    if (mp == nullptr) {
        return;
    }
    ANativeWindow *window = nullptr;
    if (surface != nullptr) {
        window = ANativeWindow_fromSurface(env, surface);
    }
    mp->setVideoSurface(window);
}

void FFmpegPlayer_prepare(JNIEnv *env, jobject thiz) {
    FFmpegPlayer *mp = getFFmpegPlayer(env, thiz);
    if (mp == nullptr) {
        return;
    }
    mp->prepare();
}

void FFmpegPlayer_prepareAsync(JNIEnv *env, jobject thiz) {
    FFmpegPlayer *mp = getFFmpegPlayer(env, thiz);
    if (mp == nullptr) {
        return;
    }
    mp->prepareAsync();
}

void FFmpegPlayer_start(JNIEnv *env, jobject thiz) {
    FFmpegPlayer *mp = getFFmpegPlayer(env, thiz);
    if (mp == nullptr) {
        return;
    }
    mp->start();
}

void FFmpegPlayer_pause(JNIEnv *env, jobject thiz) {
    FFmpegPlayer *mp = getFFmpegPlayer(env, thiz);
    if (mp == nullptr) {
        return;
    }
    mp->pause();
}

void FFmpegPlayer_resume(JNIEnv *env, jobject thiz) {
    FFmpegPlayer *mp = getFFmpegPlayer(env, thiz);
    if (mp == nullptr) {
        return;
    }
    mp->resume();

}

void FFmpegPlayer_stop(JNIEnv *env, jobject thiz) {
    FFmpegPlayer *mp = getFFmpegPlayer(env, thiz);
    if (mp == nullptr) {
        return;
    }
    mp->stop();
}

void FFmpegPlayer_seekTo(JNIEnv *env, jobject thiz, jlong timeMs) {
    FFmpegPlayer *mp = getFFmpegPlayer(env, thiz);
    if (mp == nullptr) {
        return;
    }
    mp->seekTo(timeMs);
}

void FFmpegPlayer_setMute(JNIEnv *env, jobject thiz, jboolean mute) {
    FFmpegPlayer *mp = getFFmpegPlayer(env, thiz);
    if (mp == nullptr) {
        return;
    }
    mp->setMute(mute);
}

void FFmpegPlayer_setVolume(JNIEnv *env, jobject thiz, jfloat volume) {
    FFmpegPlayer *mp = getFFmpegPlayer(env, thiz);
    if (mp == nullptr) {
        return;
    }
    mp->setVolume(volume);
}

void FFmpegPlayer_setRate(JNIEnv *env, jobject thiz, jfloat speed) {
    FFmpegPlayer *mp = getFFmpegPlayer(env, thiz);
    if (mp == nullptr) {
        return;
    }
    mp->setRate(speed);
}

int FFmpegPlayer_selectTrack(JNIEnv *env, jobject thiz, int trackId, jboolean selected) {
    FFmpegPlayer *mp = getFFmpegPlayer(env, thiz);
    if (mp == nullptr) {
        return -1;
    }
    return mp->selectTrack(trackId, selected);
}

jlong FFmpegPlayer_getCurrentPosition(JNIEnv *env, jobject thiz) {

    FFmpegPlayer *mp = getFFmpegPlayer(env, thiz);
    if (mp == nullptr) {
        return 0L;
    }
    return mp->getCurrentPosition();
}

jlong FFmpegPlayer_getDuration(JNIEnv *env, jobject thiz) {
    FFmpegPlayer *mp = getFFmpegPlayer(env, thiz);
    if (mp == nullptr) {
        return 0L;
    }
    return mp->getDuration();
}

jboolean FFmpegPlayer_isPlaying(JNIEnv *env, jobject thiz) {
    FFmpegPlayer *mp = getFFmpegPlayer(env, thiz);
    if (mp == nullptr) {
        return JNI_FALSE;
    }
    return (jboolean)(mp->isPlaying() ? JNI_TRUE : JNI_FALSE);
}

jint FFmpegPlayer_getRotate(JNIEnv *env, jobject thiz) {
    FFmpegPlayer *mp = getFFmpegPlayer(env, thiz);
    if (mp == nullptr) {
        return 0;
    }
    return mp->getRotate();
}

jint FFmpegPlayer_getVideoWidth(JNIEnv *env, jobject thiz) {
    FFmpegPlayer *mp = getFFmpegPlayer(env, thiz);
    if (mp == nullptr) {
        return 0;
    }
    return mp->getVideoWidth();
}

jint FFmpegPlayer_getVideoHeight(JNIEnv *env, jobject thiz) {
    FFmpegPlayer *mp = getFFmpegPlayer(env, thiz);
    if (mp == nullptr) {
        return 0;
    }
    return mp->getVideoHeight();
}

int FFmpegPlayer_getTrackCount(JNIEnv *env, jobject thiz, int mediaType) {
    FFmpegPlayer *mp = getFFmpegPlayer(env, thiz);
    if (mp == nullptr || mp->getMetadata() == nullptr) {
        return 0;
    }
    int count = 0;
    AVFormatContext *formatCtx = mp->getMetadata();
    for (int i = 0; i < formatCtx->nb_streams; i++) {
        if (mediaType == formatCtx->streams[i]->codecpar->codec_type) {
            count++;
        }
    }
    return count;
}

void FFmpegPlayer_getMediaTrack(JNIEnv *env, jobject thiz, int mediaType, int index, jobject mediaTrack) {
    FFmpegPlayer *mp = getFFmpegPlayer(env, thiz);
    if (mp == nullptr || mp->getMetadata() == nullptr) {
        return;
    }
    int count = 0;
    jclass trackClass = env->GetObjectClass(mediaTrack);
    AVFormatContext *formatCtx = mp->getMetadata();
    for (int i = 0; i < formatCtx->nb_streams; i++) {
        if (mediaType != formatCtx->streams[i]->codecpar->codec_type) {
            continue;
        }
        if (count == index) {
            AVStream *stream = formatCtx->streams[i];
            jfieldID trackId = env->GetFieldID(trackClass, "trackId", "I");
            jmethodID languageId = env->GetMethodID(trackClass, "setLanguage", "(Ljava/lang/String;)V");
            env->SetIntField(mediaTrack, trackId, stream->index);
            if (stream->metadata) {
                for (i = 0; i < stream->metadata->count; i++) {
                    AVDictionaryEntry *entry = stream->metadata->elements + i;
                    if (strcmp("language", entry->key) == 0) {
                        jstring value = env->NewStringUTF(entry->value);
                        env->CallVoidMethod(mediaTrack, languageId, value);
                    }
                }
            }
            break;
        } else {
            count++;
        }
    }
}

void FFmpegPlayer_getMediaInfo(JNIEnv *env, jobject thiz, int mediaType, jobject mediaInfo) {
    FFmpegPlayer *mp = getFFmpegPlayer(env, thiz);
    if (mp == nullptr || mp->getAVStream(mediaType) == nullptr) {
        return;
    }
    jfieldID codecId = nullptr;
    jclass infoClass = env->GetObjectClass(mediaInfo);
    AVStream *stream = mp->getAVStream(mediaType);
    if (mediaType == AVMEDIA_TYPE_VIDEO) {
        jfieldID widthId = env->GetFieldID(infoClass, "width", "I");
        jfieldID heightId = env->GetFieldID(infoClass, "height", "I");
        jfieldID frameRateId = env->GetFieldID(infoClass, "frameRate", "F");
        codecId = env->GetFieldID(infoClass, "videoCodec", "Ljava/lang/String;");
        env->SetIntField(mediaInfo, widthId, stream->codecpar->width);
        env->SetIntField(mediaInfo, heightId, stream->codecpar->height);
        env->SetFloatField(mediaInfo, frameRateId, (float)av_q2d(stream->avg_frame_rate));
    } else if (mediaType == AVMEDIA_TYPE_AUDIO) {
        jfieldID sampleRateId = env->GetFieldID(infoClass, "sampleRate", "I");
        jfieldID channelsId = env->GetFieldID(infoClass, "channels", "I");
        codecId = env->GetFieldID(infoClass, "audioCodec", "Ljava/lang/String;");
        env->SetIntField(mediaInfo, sampleRateId, stream->codecpar->sample_rate);
        env->SetIntField(mediaInfo, channelsId, stream->codecpar->channels);
    }
    if (codecId) {
        const char *name = avcodec_get_name(stream->codecpar->codec_id);
        env->SetObjectField(mediaInfo, codecId, env->NewStringUTF(name));
    }
    AVFormatContext *formatContext = mp->getMetadata();
    if (formatContext->iformat && formatContext->iformat->name) {
        jfieldID format = env->GetFieldID(infoClass, "format", "Ljava/lang/String;");
        jstring formatStr = env->NewStringUTF(formatContext->iformat->name);
        env->SetObjectField(mediaInfo, format, formatStr);
    }
}

static const JNINativeMethod gMethods[] = {
        {"native_setDataSource", "(Ljava/lang/String;)V", (void *) FFmpegPlayer_setDataSource},
        {"native_setDataSource", "(Ljava/io/FileDescriptor;J)V", (void *) FFmpegPlayer_setDataSourceFD},
        {"native_setVideoSurface", "(Landroid/view/Surface;)V", (void *) FFmpegPlayer_setVideoSurface},
        {"native_prepare", "()V", (void *) FFmpegPlayer_prepare},
        {"native_prepareAsync", "()V", (void *) FFmpegPlayer_prepareAsync},
        {"native_start", "()V", (void *) FFmpegPlayer_start},
        {"native_stop", "()V", (void *) FFmpegPlayer_stop},
        {"native_resume", "()V", (void *) FFmpegPlayer_resume},
        {"native_getRotate", "()I", (void *) FFmpegPlayer_getRotate},
        {"native_getVideoWidth", "()I", (void *) FFmpegPlayer_getVideoWidth},
        {"native_getVideoHeight", "()I", (void *) FFmpegPlayer_getVideoHeight},
        {"native_seekTo", "(J)V", (void *) FFmpegPlayer_seekTo},
        {"native_pause", "()V", (void *) FFmpegPlayer_pause},
        {"native_isPlaying", "()Z", (void *) FFmpegPlayer_isPlaying},
        {"native_getCurrentPosition", "()J", (void *) FFmpegPlayer_getCurrentPosition},
        {"native_getDuration", "()J", (void *) FFmpegPlayer_getDuration},
        {"native_selectTrack", "(IZ)I", (void *)  FFmpegPlayer_selectTrack},
        {"native_setVolume", "(F)V", (void *) FFmpegPlayer_setVolume},
        {"native_setMute", "(Z)V", (void *) FFmpegPlayer_setMute},
        {"native_setRate", "(F)V", (void *) FFmpegPlayer_setRate},
        {"native_getMediaInfo", "(ILcom/frank/media/mediainfo/MediaInfo;)V", (void *) FFmpegPlayer_getMediaInfo},
        {"native_getTrackCount", "(I)I", (void *) FFmpegPlayer_getTrackCount},
        {"native_getMediaTrack", "(IILcom/frank/media/mediainfo/MediaTrack;)V", (void *) FFmpegPlayer_getMediaTrack},
        {"native_init", "()V", (void *) FFmpegPlayer_init},
        {"native_setup", "(Ljava/lang/Object;)V", (void *) FFmpegPlayer_setup},
        {"native_reset", "()V", (void *) FFmpegPlayer_reset},
        {"native_release", "()V", (void *) FFmpegPlayer_release},
        {"native_finalize", "()V", (void *) FFmpegPlayer_finalize},
};

static int register_com_frank_media_FFmpegPlayer(JNIEnv *env) {
    int numMethods = (sizeof(gMethods) / sizeof( (gMethods)[0]));
    jclass clazz = env->FindClass(CLASS_NAME);
    if (clazz == nullptr) {
        ALOGE("can't find class '%s'", CLASS_NAME);
        return JNI_ERR;
    }
    if (env->RegisterNatives(clazz, gMethods, numMethods) < 0) {
        ALOGE("can't register class '%s'", CLASS_NAME);
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
    if (register_com_frank_media_FFmpegPlayer(env) != JNI_OK) {
        return -1;
    }
    return JNI_VERSION_1_4;
}

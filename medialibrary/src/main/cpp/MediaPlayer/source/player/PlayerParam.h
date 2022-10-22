//
// Created by cain on 2019/1/26.
//

#ifndef PLAYERPARAM_H
#define PLAYERPARAM_H

#include <Mutex.h>
#include <Condition.h>
#include <Thread.h>

extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavutil/mem.h>
#include <libavutil/rational.h>
#include <libavutil/time.h>
#include <libswresample/swresample.h>
#include <libswscale/swscale.h>
#include <libavutil/imgutils.h>
#include <libavutil/avstring.h>
}
#include <player/AVMessageQueue.h>

#define VIDEO_QUEUE_SIZE 3
#define SAMPLE_QUEUE_SIZE 9

#define MAX_QUEUE_SIZE (15 * 1024 * 1024)
#define MIN_FRAMES 25

#define AUDIO_MIN_BUFFER_SIZE 512

#define AUDIO_MAX_CALLBACKS_PER_SEC 30

#define REFRESH_RATE 0.01

#define AV_SYNC_THRESHOLD_MIN 0.04

#define AV_SYNC_THRESHOLD_MAX 0.1

#define AV_SYNC_FRAMEDUP_THRESHOLD 0.1

#define AV_NOSYNC_THRESHOLD 10.0


#define EXTERNAL_CLOCK_MIN_FRAMES 2

#define EXTERNAL_CLOCK_MAX_FRAMES 10

#define EXTERNAL_CLOCK_SPEED_MIN  0.900

#define EXTERNAL_CLOCK_SPEED_MAX  1.010

#define EXTERNAL_CLOCK_SPEED_STEP 0.001

#define AUDIO_DIFF_AVG_NB   20

#define SAMPLE_CORRECTION_PERCENT_MAX 10

typedef enum {
    AV_SYNC_AUDIO,      // 同步到音频时钟
    AV_SYNC_VIDEO,      // 同步到视频时钟
    AV_SYNC_EXTERNAL,   // 同步到外部时钟
} SyncType;

struct AVDictionary {
    int count;
    AVDictionaryEntry *elements;
};

class PlayerParam {

public:
    PlayerParam();

    virtual ~PlayerParam();

    void reset();

private:
    void init();

public:
    Mutex mMutex;                   // 操作互斥锁

    AVStream *m_audioStream;
    AVStream *m_videoStream;
    AVStream *m_subtitleStream;

    AVMessageQueue *messageQueue;   // 播放器消息队列
    int64_t videoDuration;          // 视频时长

    AVInputFormat *iformat;         // 指定文件封装格式，也就是解复用器
    const char *url;                // 文件路径
    int64_t offset;                 // 文件偏移量

    const char *audioCodecName;     // 指定音频解码器名称
    const char *videoCodecName;     // 指定视频解码器名称

    int abortRequest;               // 退出标志
    int pauseRequest;               // 暂停标志
    SyncType syncType;              // 同步类型
    int64_t startTime;              // 播放起始位置
    int64_t duration;               // 播放时长
    int audioDisable;               // 是否禁止音频流
    int videoDisable;               // 是否禁止视频流
    int displayDisable;             // 是否禁止显示

    int fast;                       // 解码上下文的AV_CODEC_FLAG2_FAST标志

    float playbackRate;             // 播放速度

    int seekRequest;                // 定位请求
    int seekFlags;                  // 定位标志
    int64_t seekPos;                // 定位位置

    int loop;
    int mute;
    int frameDrop;
    int m_videoIndex;
    int m_audioIndex;
    int m_subtitleIndex;
    int reorderVideoPts;
};

#endif //PLAYERPARAM_H

//
// Created by cain on 2019/1/26.
//

#ifndef PLAYERPARAM_H
#define PLAYERPARAM_H

#include <Mutex.h>
#include <Condition.h>
#include <Thread.h>
#include <message/FFMessageQueue.h>

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
    AV_SYNC_AUDIO,
    AV_SYNC_VIDEO,
    AV_SYNC_EXTERNAL
} AVSyncType;

struct AVDictionary {
    int count;
    AVDictionaryEntry *elements;
};

class PlayerParam {

private:
    void init();

public:
    PlayerParam();

    virtual ~PlayerParam();

public:
    Mutex mMutex;

    AVFormatContext *m_formatCtx;

    AVStream *m_audioStream;
    AVStream *m_videoStream;
    AVStream *m_subtitleStream;

    AVCodecContext *m_audioCodecCtx;
    AVCodecContext *m_videoCodecCtx;
    AVCodecContext *m_subtitleCodecCtx;

    FFMessageQueue *messageQueue;
    int64_t videoDuration;

    AVInputFormat *iformat;
    const char *url;
    int64_t offset;

    const char *audioCodecName;
    const char *videoCodecName;

    int abortRequest;
    int pauseRequest;
    AVSyncType syncType;
    int64_t startTime;
    int64_t duration;
    int audioDisable;
    int videoDisable;
    int displayDisable;

    int fast;
    int seekFlags;
    int seekRequest;
    int64_t seekPos;
    float playbackRate;

    int loop;
    int mute;
    int frameDrop;
    int m_videoIndex;
    int m_audioIndex;
    int m_subtitleIndex;
    int reorderVideoPts;
};

#endif //PLAYERPARAM_H


#ifndef MESSAGE_DEFINE_H
#define MESSAGE_DEFINE_H

#define MSG_FLUSH                       0x000
#define MSG_ON_PREPARED                 0x100
#define MSG_ON_START                    0x200
#define MSG_ON_COMPLETE                 0x300
#define MSG_ON_ERROR                    0x400

#define MSG_AUDIO_DECODER_OPEN          0x500
#define MSG_VIDEO_DECODER_OPEN          0x501
#define MSG_VIDEO_SIZE_CHANGED          0x502
#define MSG_AUDIO_DECODE_START          0x503
#define MSG_AUDIO_RENDER_START          0x504
#define MSG_VIDEO_DECODE_START          0x505
#define MSG_VIDEO_RENDER_START          0x506
#define MSG_VIDEO_ROTATION_CHANGED      0x507

#define MSG_BUFFERING_START             0x600
#define MSG_BUFFERING_UPDATE            0x601
#define MSG_BUFFERING_TIME_UPDATE       0x602
#define MSG_BUFFERING_END               0x603

#define MSG_SEEK_COMPLETE               0x700
#define MSG_TIMED_TEXT                  0x800

#define MSG_REQUEST_PREPARE             0x900
#define MSG_REQUEST_START               0x901
#define MSG_REQUEST_PAUSE               0x902
#define MSG_REQUEST_SEEK                0x903

#endif //MESSAGE_DEFINE_H

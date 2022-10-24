
#ifndef MESSAGE_DEFINE_H
#define MESSAGE_DEFINE_H

#define MSG_FLUSH                       0x00    // 默认
#define MSG_ERROR                       0x10    // 出错回调
#define MSG_PREPARED                    0x20    // 准备完成回调
#define MSG_STARTED                     0x30    // 已经开始
#define MSG_COMPLETED                   0x40    // 播放完成回调

#define MSG_AUDIO_DECODER_OPEN          0x51    // 打开音频解码器
#define MSG_VIDEO_DECODER_OPEN          0x52    // 打开视频解码器
#define MSG_VIDEO_SIZE_CHANGED          0x53    // 视频大小变化
#define MSG_AUDIO_DECODE_START          0x54    // 开始音频解码
#define MSG_AUDIO_RENDER_START          0x55    // 音频渲染开始
#define MSG_VIDEO_DECODE_START          0x56    // 开始视频解码
#define MSG_VIDEO_RENDER_START          0x57    // 视频渲染开始
#define MSG_VIDEO_ROTATION_CHANGED      0x58    // 旋转角度变化

#define MSG_BUFFERING_START             0x60    // 缓冲开始
#define MSG_BUFFERING_END               0x61    // 缓冲完成
#define MSG_BUFFERING_UPDATE            0x62    // 缓冲更新
#define MSG_BUFFERING_TIME_UPDATE       0x63    // 缓冲时间更新

#define MSG_SEEK_COMPLETE               0x70    // 定位完成
#define MSG_PLAYBACK_STATE_CHANGED      0x80    // 播放状态变更
#define MSG_TIMED_TEXT                  0x90    // 字幕

#define MSG_REQUEST_PREPARE             0x200   // 异步请求准备
#define MSG_REQUEST_START               0x201   // 异步请求开始
#define MSG_REQUEST_PAUSE               0x202   // 请求暂停
#define MSG_REQUEST_SEEK                0x203   // 请求定位

#define MSG_CURRENT_POSITION            0x300   // 当前时钟

#endif //MESSAGE_DEFINE_H

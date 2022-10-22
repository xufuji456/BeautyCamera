
set(soundtouch_dir ../include/soundtouch)

# 添加头文件路径
include_directories( source
                     android
                     ${soundtouch_dir})

# 添加源文件
add_library( media_player

             SHARED

             source/convertor/AudioResampler.cpp

             source/decoder/AudioDecoder.cpp
             source/decoder/MediaDecoder.cpp
             source/decoder/VideoDecoder.cpp

        source/render/android/NativeWindowVideoRender.cpp
        source/render/android/OpenSLAudioRender.cpp

             source/queue/FrameQueue.cpp
             source/queue/PacketQueue.cpp

             source/sync/MediaClock.cpp
             source/sync/MediaSync.cpp

             source/player/AVMessageQueue.cpp
             source/player/MediaPlayer.cpp
             source/player/PlayerState.cpp

             # controller
             android/FFMediaPlayer.cpp
             ../nativehelp/JNIHelp.cpp
             android/com_frank_media_FFMediaPlayer.cpp)

# 链接静态库
target_link_libraries( media_player

                       ffmpeg
                       soundtouch

                       -landroid
                       -llog
                       -lOpenSLES
                       -lEGL
                       ${GLES-lib} )


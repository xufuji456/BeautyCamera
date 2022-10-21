
# 根据API版本判断使用哪个版本的OpenGLES
if (${ANDROID_PLATFORM_LEVEL} LESS 12)
  message(FATAL_ERROR "OpenGL 2 is not supported before API level 11 (currently using ${ANDROID_PLATFORM_LEVEL}).")
  return()
elseif (${ANDROID_PLATFORM_LEVEL} LESS 18)
  add_definitions("-DDYNAMIC_ES3")
  set(GLES-lib GLESv2)
else ()
  set(GLES-lib GLESv3)
endif (${ANDROID_PLATFORM_LEVEL} LESS 11)

# 添加头文件路径
include_directories( source
                     android)

# 添加源文件
add_library( media_player

             SHARED

             source/convertor/AudioResampler.cpp

             source/decoder/AudioDecoder.cpp
             source/decoder/MediaDecoder.cpp
             source/decoder/VideoDecoder.cpp

             source/render/NativeWindowVideoRender.cpp
             source/render/OpenSLAudioRender.cpp

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

                       -landroid
                       -llog
                       -lOpenSLES
                       -lEGL
                       ${GLES-lib} )


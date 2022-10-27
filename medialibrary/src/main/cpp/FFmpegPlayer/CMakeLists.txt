
include_directories( kernel
                     android
                     include)

aux_source_directory(kernel/decoder SRC_DECODER)
aux_source_directory(kernel/resample SRC_RESAMPLE)
aux_source_directory(kernel/avsync SRC_AVSYNC)
aux_source_directory(kernel/render/android SRC_RENDER)
aux_source_directory(kernel/queue SRC_QUEUE)
aux_source_directory(kernel/message SRC_MESSAGE)
aux_source_directory(kernel/player SRC_PLAYER)
aux_source_directory(android SRC_ANDROID)

add_library(ffmpeg_player
            SHARED
            ${SRC_DECODER}
            ${SRC_RESAMPLE}
            ${SRC_AVSYNC}
            ${SRC_RENDER}
            ${SRC_QUEUE}
            ${SRC_MESSAGE}
            ${SRC_PLAYER}
            ${SRC_ANDROID})

add_library(soundtouch SHARED IMPORTED)
set_target_properties(soundtouch
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/../libs/${ANDROID_ABI}/libsoundtouch.so)

target_link_libraries(  ffmpeg_player
                        ffmpeg
                        soundtouch

                        -llog
                        -landroid
                        -lOpenSLES
                        -lEGL)


cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")

include_directories(/src/main/cpp)
include_directories(/src/main/cpp/bitmap)
include_directories(/src/main/cpp/beauty)

find_library(
        #Defines the name of the path variable that stores the location of the NDK library.
        android_log_lib

        # Specifies the name of the NDK library that CMake needs to locate.
        log
)

add_library(
        # Specifies the name of library.
        BeautyManager

        # Set the library as a shared library.
        SHARED

        # Provides a relative path to yours source file(s).
        BeautyManager.cpp
        beauty/SimpleBeauty.cpp
        bitmap/Conversion.cpp
        bitmap/BitmapOperation.cpp
)

# Links your native library against one or more other native libraries.
target_link_libraries(
        # Specifies the target library.
        BeautyManager

        jnigraphics

        # Links the log library to the target library.
        ${android_log_lib}
)
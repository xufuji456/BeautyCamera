LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := BeautyManager
LOCAL_SRC_FILES := BeautyManager.cpp \
				bitmap/BitmapOperation.cpp \
				bitmap/Conversion.cpp \
				beauty/SimpleBeauty.cpp
				
LOCAL_LDLIBS := -llog 

LOCAL_LDFLAGS += -ljnigraphics

APP_STL:= stlport_static

include $(BUILD_SHARED_LIBRARY)

#if you need to add more module, do the same as the one we started with (the one with the CLEAR_VARS)

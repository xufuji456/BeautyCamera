
#include "SoundTouchHelper.h"

SoundTouchHelper::SoundTouchHelper() {
    create();
}

SoundTouchHelper::~SoundTouchHelper() {
    destroy();
}

void SoundTouchHelper::create() {
    mSoundTouch = new SoundTouch();
}

void SoundTouchHelper::destroy() {
    if (mSoundTouch) {
        mSoundTouch->clear();
        delete mSoundTouch;
        mSoundTouch = nullptr;
    }
}

int SoundTouchHelper::translate(short *data, float speed, float pitch, int len,
                                int nb_sample, int channel, int sampleRate) {

    int put_n_sample = len / channel;
    int nb = 0;
    int pcm_data_size = 0;
    if (mSoundTouch == nullptr) {
        return 0;
    }
    // 设置音调
    mSoundTouch->setPitch(pitch);
    // 设置速度
    mSoundTouch->setRate(speed);
    // 设置采样率
    mSoundTouch->setSampleRate(sampleRate);
    // 设置声道数
    mSoundTouch->setChannels(channel);
    // 压入采样数据
    mSoundTouch->putSamples((SAMPLETYPE*)data, put_n_sample);

    do {
        // 获取转换后的数据
        nb = mSoundTouch->receiveSamples((SAMPLETYPE*)data, sampleRate / channel);
        // 计算转换后的数量大小
        pcm_data_size += nb * channel * nb_sample;
    } while (nb != 0);

    // 返回转换后的数量大小
    return pcm_data_size;
}

void SoundTouchHelper::flush() {
    if (mSoundTouch != nullptr) {
        mSoundTouch->flush();
    }
}
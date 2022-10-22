
#include <AndroidLog.h>
#include "OpenSLAudioRender.h"

#define OPENSLES_BUFFERS 4
#define OPENSLES_BUF_SIZE  10

OpenSLAudioRender::OpenSLAudioRender() {
    slObject = nullptr;
    slEngine = nullptr;
    slOutputMixObject = nullptr;
    slPlayerObject = nullptr;
    slPlayItf = nullptr;
    slVolumeItf = nullptr;
    slBufferQueueItf = nullptr;
    memset(&audioDeviceSpec, 0, sizeof(AudioDeviceSpec));
    abortRequest = 1;
    pauseRequest = 0;
    flushRequest = 0;
    audioThread = nullptr;
    updateVolume = false;
}

OpenSLAudioRender::~OpenSLAudioRender() {
    mMutex.lock();
    memset(&audioDeviceSpec, 0, sizeof(AudioDeviceSpec));
    if (slPlayerObject != nullptr) {
        (*slPlayerObject)->Destroy(slPlayerObject);
        slPlayerObject = nullptr;
        slPlayItf = nullptr;
        slVolumeItf = nullptr;
        slBufferQueueItf = nullptr;
    }

    if (slOutputMixObject != nullptr) {
        (*slOutputMixObject)->Destroy(slOutputMixObject);
        slOutputMixObject = nullptr;
    }

    if (slObject != nullptr) {
        (*slObject)->Destroy(slObject);
        slObject = nullptr;
        slEngine = nullptr;
    }
    mMutex.unlock();
}

void OpenSLAudioRender::start() {
    if (audioDeviceSpec.callback != nullptr) {
        abortRequest = 0;
        pauseRequest = 0;
        if (!audioThread) {
            audioThread = new Thread(this, Priority_High);
            audioThread->start();
        }
    }
}

void OpenSLAudioRender::pause() {
    mMutex.lock();
    pauseRequest = 1;
    mCondition.signal();
    mMutex.unlock();
}

void OpenSLAudioRender::resume() {
    mMutex.lock();
    pauseRequest = 0;
    mCondition.signal();
    mMutex.unlock();
}

void OpenSLAudioRender::flush() {
    mMutex.lock();
    flushRequest = 1;
    mCondition.signal();
    mMutex.unlock();
}

void OpenSLAudioRender::setVolume(float volume) {
    Mutex::Autolock lock(mMutex);
    if (!updateVolume) {
        mVolume = volume;
        updateVolume = true;
    }
    mCondition.signal();
}

void OpenSLAudioRender::stop() {
    mMutex.lock();
    abortRequest = 1;
    mCondition.signal();
    mMutex.unlock();

    if (audioThread) {
        audioThread->join();
        delete audioThread;
        audioThread = nullptr;
    }
}

void OpenSLAudioRender::run() {
    uint8_t *next_buffer = nullptr;
    int next_buffer_index = 0;

    if (!abortRequest && !pauseRequest) {
        (*slPlayItf)->SetPlayState(slPlayItf, SL_PLAYSTATE_PLAYING);
    }

    while (true) {

        if (abortRequest) {
            break;
        }

        if (pauseRequest) {
            continue;
        }

        SLAndroidSimpleBufferQueueState slState = {0};
        SLresult slRet = (*slBufferQueueItf)->GetState(slBufferQueueItf, &slState);
        if (slRet != SL_RESULT_SUCCESS) {
            ALOGE("%s: slBufferQueueItf->GetState() failed\n", __func__);
            mMutex.unlock();
        }
        // 判断暂停或者队列中缓冲区填满了
        mMutex.lock();
        if (!abortRequest && (pauseRequest || slState.count >= OPENSLES_BUFFERS)) {
            while (!abortRequest && (pauseRequest || slState.count >= OPENSLES_BUFFERS)) {

                if (!pauseRequest) {
                    (*slPlayItf)->SetPlayState(slPlayItf, SL_PLAYSTATE_PLAYING);
                }
                mCondition.waitRelative(mMutex, 10 * 1000000);
                slRet = (*slBufferQueueItf)->GetState(slBufferQueueItf, &slState);
                if (slRet != SL_RESULT_SUCCESS) {
                    ALOGE("%s: slBufferQueueItf->GetState() failed\n", __func__);
                    mMutex.unlock();
                }

                if (pauseRequest) {
                    (*slPlayItf)->SetPlayState(slPlayItf, SL_PLAYSTATE_PAUSED);
                }
            }

            if (!abortRequest && !pauseRequest) {
                (*slPlayItf)->SetPlayState(slPlayItf, SL_PLAYSTATE_PLAYING);
            }

        }
        if (flushRequest) {
            (*slBufferQueueItf)->Clear(slBufferQueueItf);
            flushRequest = 0;
        }
        mMutex.unlock();

        mMutex.lock();
        // 通过回调填充PCM数据
        if (audioDeviceSpec.callback != nullptr) {
            next_buffer = buffer + next_buffer_index * bytes_per_buffer;
            next_buffer_index = (next_buffer_index + 1) % OPENSLES_BUFFERS;
            audioDeviceSpec.callback(audioDeviceSpec.userdata, next_buffer, bytes_per_buffer);
        }
        mMutex.unlock();

        if (updateVolume) {
            if (slVolumeItf != nullptr) {
                SLmillibel level = getAmplificationLevel(mVolume);
                SLresult result = (*slVolumeItf)->SetVolumeLevel(slVolumeItf, level);
                if (result != SL_RESULT_SUCCESS) {
                    ALOGE("slVolumeItf->SetVolumeLevel failed %d\n", (int)result);
                }
            }
            updateVolume = false;
        }

        if (flushRequest) {
            (*slBufferQueueItf)->Clear(slBufferQueueItf);
            flushRequest = 0;
        } else {
            if (slPlayItf != nullptr) {
                (*slPlayItf)->SetPlayState(slPlayItf, SL_PLAYSTATE_PLAYING);
            }
            slRet = (*slBufferQueueItf)->Enqueue(slBufferQueueItf, next_buffer, bytes_per_buffer);
            if (slRet == SL_RESULT_SUCCESS) {

            } else if (slRet == SL_RESULT_BUFFER_INSUFFICIENT) {
                ALOGE("SL_RESULT_BUFFER_INSUFFICIENT\n");
            } else {
                ALOGE("slBufferQueueItf->Enqueue() = %d\n", (int)slRet);
                break;
            }
        }
    }
    if (slPlayItf) {
        (*slPlayItf)->SetPlayState(slPlayItf, SL_PLAYSTATE_STOPPED);
    }
}

void slBufferPCMCallBack(SLAndroidSimpleBufferQueueItf bf, void *context) {

}

int OpenSLAudioRender::open(const AudioDeviceSpec *desired, AudioDeviceSpec *obtained) {
    SLresult result;
    result = slCreateEngine(&slObject, 0, nullptr, 0, nullptr, nullptr);
    if ((result) != SL_RESULT_SUCCESS) {
        ALOGE("%s: slCreateEngine() failed", __func__);
        return -1;
    }
    result = (*slObject)->Realize(slObject, SL_BOOLEAN_FALSE);
    if (result != SL_RESULT_SUCCESS) {
        ALOGE("%s: slObject->Realize() failed", __func__);
        return -1;
    }
    result = (*slObject)->GetInterface(slObject, SL_IID_ENGINE, &slEngine);
    if (result != SL_RESULT_SUCCESS) {
        ALOGE("%s: slObject->GetInterface() failed", __func__);
        return -1;
    }

    const SLInterfaceID mids[1] = {SL_IID_ENVIRONMENTALREVERB};
    const SLboolean mreq[1] = {SL_BOOLEAN_FALSE};
    result = (*slEngine)->CreateOutputMix(slEngine, &slOutputMixObject, 1, mids, mreq);
    if (result != SL_RESULT_SUCCESS) {
        ALOGE("%s: slEngine->CreateOutputMix() failed", __func__);
        return -1;
    }
    result = (*slOutputMixObject)->Realize(slOutputMixObject, SL_BOOLEAN_FALSE);
    if (result != SL_RESULT_SUCCESS) {
        ALOGE("%s: slOutputMixObject->Realize() failed", __func__);
        return -1;
    }
    SLDataLocator_OutputMix outputMix = {SL_DATALOCATOR_OUTPUTMIX, slOutputMixObject};
    SLDataSink audioSink = {&outputMix, nullptr};

    SLDataLocator_AndroidSimpleBufferQueue android_queue = {
            SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE,
            OPENSLES_BUFFERS
    };

    SLuint32 channelMask;
    switch (desired->channels) {
        case 2: {
            channelMask = SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT;
            break;
        }
        case 1: {
            channelMask = SL_SPEAKER_FRONT_CENTER;
            break;
        }
        default: {
            ALOGE("%s, invalid channel %d", __func__, desired->channels);
            return -1;
        }
    }
    SLDataFormat_PCM format_pcm = {
            SL_DATAFORMAT_PCM,
            desired->channels,
            getSLSampleRate(desired->freq),
            SL_PCMSAMPLEFORMAT_FIXED_16,
            SL_PCMSAMPLEFORMAT_FIXED_16,
            channelMask,
            SL_BYTEORDER_LITTLEENDIAN
    };

    SLDataSource slDataSource = {&android_queue, &format_pcm};

    const SLInterfaceID ids[3] = {SL_IID_ANDROIDSIMPLEBUFFERQUEUE, SL_IID_VOLUME, SL_IID_PLAY};
    const SLboolean req[3] = {SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE};

    result = (*slEngine)->CreateAudioPlayer(slEngine, &slPlayerObject, &slDataSource,
                                            &audioSink, 3, ids, req);
    if (result != SL_RESULT_SUCCESS) {
        ALOGE("%s: slEngine->CreateAudioPlayer() failed", __func__);
        return -1;
    }

    result = (*slPlayerObject)->Realize(slPlayerObject, SL_BOOLEAN_FALSE);
    if (result != SL_RESULT_SUCCESS) {
        ALOGE("%s: slPlayerObject->Realize() failed", __func__);
        return -1;
    }

    result = (*slPlayerObject)->GetInterface(slPlayerObject, SL_IID_PLAY, &slPlayItf);
    if (result != SL_RESULT_SUCCESS) {
        ALOGE("%s: slPlayerObject->GetInterface(SL_IID_PLAY) failed", __func__);
        return -1;
    }

    result = (*slPlayerObject)->GetInterface(slPlayerObject, SL_IID_VOLUME, &slVolumeItf);
    if (result != SL_RESULT_SUCCESS) {
        ALOGE("%s: slPlayerObject->GetInterface(SL_IID_VOLUME) failed", __func__);
        return -1;
    }

    result = (*slPlayerObject)->GetInterface(slPlayerObject, SL_IID_ANDROIDSIMPLEBUFFERQUEUE,
                                             &slBufferQueueItf);
    if (result != SL_RESULT_SUCCESS) {
        ALOGE("%s: slPlayerObject->GetInterface(SL_IID_ANDROIDSIMPLEBUFFERQUEUE) failed", __func__);
        return -1;
    }

    result = (*slBufferQueueItf)->RegisterCallback(slBufferQueueItf, slBufferPCMCallBack, this);
    if (result != SL_RESULT_SUCCESS) {
        ALOGE("%s: slBufferQueueItf->RegisterCallback() failed", __func__);
        return -1;
    }

    bytes_per_frame   = format_pcm.numChannels * format_pcm.bitsPerSample / 8;
    milli_per_buffer  = OPENSLES_BUF_SIZE;
    frames_per_buffer = milli_per_buffer * format_pcm.samplesPerSec / 1000000;
    bytes_per_buffer  = bytes_per_frame * frames_per_buffer;
    buffer_capacity   = OPENSLES_BUFFERS * bytes_per_buffer;

    if (obtained != nullptr) {
        *obtained = *desired;
        obtained->size = (uint32_t)buffer_capacity;
        obtained->freq = format_pcm.samplesPerSec / 1000;
    }
    audioDeviceSpec = *desired;

    buffer = (uint8_t *)malloc(buffer_capacity);
    if (!buffer) {
        ALOGE("%s: failed to alloc buffer %d\n", __func__, (int)buffer_capacity);
        return -1;
    }

    memset(buffer, 0, buffer_capacity);
    for(int i = 0; i < OPENSLES_BUFFERS; ++i) {
        result = (*slBufferQueueItf)->Enqueue(slBufferQueueItf,
                                              buffer + i * bytes_per_buffer,
                                              bytes_per_buffer);
        if (result != SL_RESULT_SUCCESS)  {
            ALOGE("%s: slBufferQueueItf->Enqueue(000...) failed", __func__);
        }
    }

    return buffer_capacity;
}

SLuint32 OpenSLAudioRender::getSLSampleRate(int sampleRate) {
    switch (sampleRate) {
        case 8000: {
            return SL_SAMPLINGRATE_8;
        }
        case 11025: {
            return SL_SAMPLINGRATE_11_025;
        }
        case 12000: {
            return SL_SAMPLINGRATE_12;
        }
        case 16000: {
            return SL_SAMPLINGRATE_16;
        }
        case 22050: {
            return SL_SAMPLINGRATE_22_05;
        }
        case 24000: {
            return SL_SAMPLINGRATE_24;
        }
        case 32000: {
            return SL_SAMPLINGRATE_32;
        }
        case 44100: {
            return SL_SAMPLINGRATE_44_1;
        }
        case 48000: {
            return SL_SAMPLINGRATE_48;
        }
        case 64000: {
            return SL_SAMPLINGRATE_64;
        }
        case 88200: {
            return SL_SAMPLINGRATE_88_2;
        }
        case 96000: {
            return SL_SAMPLINGRATE_96;
        }
        case 192000: {
            return SL_SAMPLINGRATE_192;
        }
        default: {
            return SL_SAMPLINGRATE_44_1;
        }
    }
}

SLmillibel OpenSLAudioRender::getAmplificationLevel(float volumeLevel) {
    if (volumeLevel < 0.00000001) {
        return SL_MILLIBEL_MIN;
    }
    auto mb = (short)lroundf(2000.f * log10f(volumeLevel));
    if (mb < SL_MILLIBEL_MIN) {
        mb = SL_MILLIBEL_MIN;
    } else if (mb > 0) {
        mb = 0;
    }
    return mb;
}



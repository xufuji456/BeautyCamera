
#ifndef SOUNDTOUCHHELPER_H
#define SOUNDTOUCHHELPER_H

#include <stdint.h>
#include "SoundTouch.h"

using namespace soundtouch;

class SoundTouchHelper {

public:
    SoundTouchHelper();

    void create();

    int translate(short* data, float speed, float pitch, int len, int nb_sample, int channel, int sampleRate);

    void flush();

    void destroy();

    ~SoundTouchHelper();

private:
    SoundTouch *mSoundTouch{};
};


#endif //SOUNDTOUCHHELPER_H

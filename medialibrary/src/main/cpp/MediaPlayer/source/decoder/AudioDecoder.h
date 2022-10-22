
#ifndef AUDIODECODER_H
#define AUDIODECODER_H

#include <decoder/MediaDecoder.h>
#include <player/PlayerState.h>

class AudioDecoder : public MediaDecoder {

private:
    AVPacket *packet;
    int64_t next_pts{};
    bool packetPending;
    AVRational next_pts_tb{};

public:
    AudioDecoder(AVCodecContext *avctx, AVStream *stream, PlayerState *playerState);

    virtual ~AudioDecoder();

    int getAudioFrame(AVFrame *frame);

};


#endif //AUDIODECODER_H


#ifndef AUDIODECODER_H
#define AUDIODECODER_H

#include <decoder/MediaDecoder.h>
#include <player/PlayerParam.h>

class AudioDecoder : public MediaDecoder {

private:
    bool m_pktPending;
    AVPacket *m_packet;
    int64_t m_next_pts{};
    AVRational m_next_pts_tb{};

public:
    AudioDecoder(PlayerParam *playerParam);

    virtual ~AudioDecoder();

    AVCodecContext *getCodecContext() override;

    int getAudioFrame(AVFrame *frame);

};


#endif //AUDIODECODER_H

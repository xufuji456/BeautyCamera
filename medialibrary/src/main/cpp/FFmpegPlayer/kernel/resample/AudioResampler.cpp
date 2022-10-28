
#include "AudioResampler.h"

AudioResampler::AudioResampler(PlayerParam *playerParam, AudioDecoder *audioDecoder, AVSync *avSync) {
    this->m_avSync       = avSync;
    this->m_playerParam  = playerParam;
    this->m_audioDecoder = audioDecoder;
    m_audioState = (AudioState *) av_mallocz(sizeof(AudioState));
    memset(m_audioState, 0, sizeof(AudioState));
    m_soundTouchHelper = new SoundTouchHelper();
    m_frame = av_frame_alloc();
}

AudioResampler::~AudioResampler() {
    m_avSync       = nullptr;
    m_playerParam  = nullptr;
    m_audioDecoder = nullptr;
    if (m_soundTouchHelper) {
        delete m_soundTouchHelper;
        m_soundTouchHelper = nullptr;
    }
    if (m_audioState) {
        swr_free(&m_audioState->m_swrContext);
        av_freep(&m_audioState->m_resampleBuffer);
        memset(m_audioState, 0, sizeof(AudioState));
        av_free(m_audioState);
        m_audioState = nullptr;
    }
    if (m_frame) {
        av_frame_unref(m_frame);
        av_frame_free(&m_frame);
        m_frame = nullptr;
    }
}

int AudioResampler::setResampleParams(AudioRenderSpec *spec, int64_t wanted_channel_layout) {

    m_audioState->bufferIndex     = 0;
    m_audioState->m_bufferSize    = 0;
    m_audioState->m_audioBufSize  = spec->size;
    m_audioState->m_audioParamSrc = m_audioState->m_audioParamDst;
    m_audioState->audio_diff_avg_coef  = exp(log(0.01) / AUDIO_DIFF_AVG_NB);
    m_audioState->audio_diff_avg_count = 0;
    m_audioState->audio_diff_threshold = (double) (m_audioState->m_audioBufSize) / m_audioState->m_audioParamDst.bytes_per_sec;

    m_audioState->m_audioParamDst.fmt            = AV_SAMPLE_FMT_S16;
    m_audioState->m_audioParamDst.freq           = spec->freq;
    m_audioState->m_audioParamDst.channels       = spec->channels;
    m_audioState->m_audioParamDst.frame_size     = av_samples_get_buffer_size(nullptr, m_audioState->m_audioParamDst.channels, 1,
                                                                          m_audioState->m_audioParamDst.fmt, 1);
    m_audioState->m_audioParamDst.channel_layout = wanted_channel_layout;
    m_audioState->m_audioParamDst.bytes_per_sec  = av_samples_get_buffer_size(nullptr, m_audioState->m_audioParamDst.channels,
                                                                             m_audioState->m_audioParamDst.freq,
                                                                             m_audioState->m_audioParamDst.fmt, 1);

    if (m_audioState->m_audioParamDst.bytes_per_sec <= 0 || m_audioState->m_audioParamDst.frame_size <= 0) {
        av_log(nullptr, AV_LOG_ERROR, "av_samples_get_buffer_size failed\n");
        return -1;
    }
    return 0;
}

void AudioResampler::pcmQueueCallback(uint8_t *stream, int len) {
    int length;
    int bufferSize;

    if (!m_audioDecoder) {
        memset(stream, 0, len);
        return;
    }

    m_audioState->m_audioCallbackTime = av_gettime_relative();
    while (len > 0) {
        if (m_audioState->bufferIndex >= m_audioState->m_bufferSize) {
            bufferSize = audioFrameResample();
            if (bufferSize < 0) {
                m_audioState->m_outputBuffer = nullptr;
                m_audioState->m_bufferSize = (unsigned int) (AUDIO_MIN_BUFFER_SIZE / m_audioState->m_audioParamDst.frame_size
                                                             * m_audioState->m_audioParamDst.frame_size);
            } else {
                m_audioState->m_bufferSize = bufferSize;
            }
            m_audioState->bufferIndex = 0;
        }

        length = m_audioState->m_bufferSize - m_audioState->bufferIndex;
        if (length > len) {
            length = len;
        }
        // copy pcm data to buffer
        if (m_audioState->m_outputBuffer != nullptr && !m_playerParam->m_mute) {
            memcpy(stream, m_audioState->m_outputBuffer + m_audioState->bufferIndex, length);
        } else {
            memset(stream, 0, length);
        }
        len -= length;
        stream += length;
        m_audioState->bufferIndex += length;
    }
    m_audioState->m_writeBufSize = m_audioState->m_bufferSize - m_audioState->bufferIndex;

    if (!isnan(m_audioState->m_audioClock) && m_avSync) {
        m_avSync->updateAudioClock(m_audioState->m_audioClock -
                                   (double) (2 * m_audioState->m_audioBufSize + m_audioState->m_writeBufSize)
                                   / m_audioState->m_audioParamDst.bytes_per_sec,
                                   m_audioState->m_audioCallbackTime / 1000000.0);
    }
}

int AudioResampler::audioSynchronize(int nbSamples) {
    int wanted_nb_samples = nbSamples;

    // sync with audio
    if (m_playerParam->m_syncType != AV_SYNC_AUDIO) {
        double diff, avg_diff;
        int min_nb_samples, max_nb_samples;
        diff = m_avSync ? m_avSync->getAudioDiffClock() : 0;
        if (!isnan(diff) && fabs(diff) < AV_NOSYNC_THRESHOLD) {
            m_audioState->audio_diff_cum = diff + m_audioState->audio_diff_avg_coef * m_audioState->audio_diff_cum;
            if (m_audioState->audio_diff_avg_count < AUDIO_DIFF_AVG_NB) {
                m_audioState->audio_diff_avg_count++;
            } else {
                avg_diff = m_audioState->audio_diff_cum * (1.0 - m_audioState->audio_diff_avg_coef);

                if (fabs(avg_diff) >= m_audioState->audio_diff_threshold) {
                    wanted_nb_samples = nbSamples + (int)(diff * m_audioState->m_audioParamSrc.freq);
                    min_nb_samples = ((nbSamples * (100 - SAMPLE_CORRECTION_PERCENT_MAX) / 100));
                    max_nb_samples = ((nbSamples * (100 + SAMPLE_CORRECTION_PERCENT_MAX) / 100));
                    wanted_nb_samples = av_clip(wanted_nb_samples, min_nb_samples, max_nb_samples);
                }
            }
        } else {
            m_audioState->audio_diff_avg_count = 0;
            m_audioState->audio_diff_cum = 0;
        }
    }

    return wanted_nb_samples;
}

int AudioResampler::audioFrameResample() {
    int ret = -1;
    int data_size;
    int wanted_nb_samples;
    int translate_time = 1;
    int resampled_data_size;
    int64_t dec_channel_layout;

    if (!m_audioDecoder || m_playerParam->m_abortReq || m_playerParam->m_pauseReq) {
        return -1;
    }

    for (;;) {

        if ((ret = m_audioDecoder->getAudioFrame(m_frame)) < 0) {
            return -1;
        }
        if (ret == 0) {
            continue;
        }

        data_size = av_samples_get_buffer_size(nullptr, m_frame->channels,
                                               m_frame->nb_samples,
                                               (AVSampleFormat)m_frame->format, 1);

        dec_channel_layout =
                (m_frame->channel_layout && m_frame->channels == av_get_channel_layout_nb_channels(m_frame->channel_layout))
                ? m_frame->channel_layout : av_get_default_channel_layout(m_frame->channels);
        wanted_nb_samples = audioSynchronize(m_frame->nb_samples);

        // check params between frame and resampler
        if (m_frame->format != m_audioState->m_audioParamSrc.fmt
            || dec_channel_layout != m_audioState->m_audioParamSrc.channel_layout
            || m_frame->sample_rate != m_audioState->m_audioParamSrc.freq
            || (wanted_nb_samples != m_frame->nb_samples && !m_audioState->m_swrContext)) {

            swr_free(&m_audioState->m_swrContext);
            m_audioState->m_swrContext = swr_alloc_set_opts(nullptr, m_audioState->m_audioParamDst.channel_layout,
                                                            m_audioState->m_audioParamDst.fmt, m_audioState->m_audioParamDst.freq,
                                                            dec_channel_layout, (AVSampleFormat)m_frame->format,
                                                            m_frame->sample_rate, 0, nullptr);

            if (!m_audioState->m_swrContext || swr_init(m_audioState->m_swrContext) < 0) {
                av_log(nullptr, AV_LOG_ERROR, "Create resampler error: %d Hz %s %d channels to %d Hz %s %d channels!\n",
                       m_frame->sample_rate,
                       av_get_sample_fmt_name((AVSampleFormat)m_frame->format),
                       m_frame->channels,
                       m_audioState->m_audioParamDst.freq,
                       av_get_sample_fmt_name(m_audioState->m_audioParamDst.fmt),
                       m_audioState->m_audioParamDst.channels);
                swr_free(&m_audioState->m_swrContext);
                return -1;
            }
            m_audioState->m_audioParamSrc.channel_layout = dec_channel_layout;
            m_audioState->m_audioParamSrc.channels = m_frame->channels;
            m_audioState->m_audioParamSrc.freq = m_frame->sample_rate;
            m_audioState->m_audioParamSrc.fmt = (AVSampleFormat)m_frame->format;
        }

        // do resampler
        if (m_audioState->m_swrContext) {
            const uint8_t **in = (const uint8_t **)m_frame->extended_data;
            uint8_t **out = &m_audioState->m_resampleBuffer;
            int out_count = (int64_t)wanted_nb_samples * m_audioState->m_audioParamDst.freq / m_frame->sample_rate + 256;
            int out_size  = av_samples_get_buffer_size(nullptr, m_audioState->m_audioParamDst.channels, out_count, m_audioState->m_audioParamDst.fmt, 0);
            int len2;
            if (out_size < 0) {
                av_log(nullptr, AV_LOG_ERROR, "av_samples_get_buffer_size() failed\n");
                return -1;
            }
            if (wanted_nb_samples != m_frame->nb_samples) {
                if (swr_set_compensation(m_audioState->m_swrContext, (wanted_nb_samples - m_frame->nb_samples) * m_audioState->m_audioParamDst.freq / m_frame->sample_rate,
                                         wanted_nb_samples * m_audioState->m_audioParamDst.freq / m_frame->sample_rate) < 0) {
                    av_log(nullptr, AV_LOG_ERROR, "swr_set_compensation() failed\n");
                    return -1;
                }
            }
            av_fast_malloc(&m_audioState->m_resampleBuffer, &m_audioState->m_resampleSize, out_size);
            if (!m_audioState->m_resampleBuffer) {
                return AVERROR(ENOMEM);
            }
            len2 = swr_convert(m_audioState->m_swrContext, out, out_count, in, m_frame->nb_samples);
            if (len2 < 0) {
                av_log(nullptr, AV_LOG_ERROR, "swr_convert() failed\n");
                return -1;
            }
            if (len2 == out_count) {
                av_log(nullptr, AV_LOG_WARNING, "audio buffer is probably too small\n");
                if (swr_init(m_audioState->m_swrContext) < 0) {
                    swr_free(&m_audioState->m_swrContext);
                }
            }
            m_audioState->m_outputBuffer = m_audioState->m_resampleBuffer;
            resampled_data_size = len2 * m_audioState->m_audioParamDst.channels * av_get_bytes_per_sample(m_audioState->m_audioParamDst.fmt);

            // use soundtouch to handle speed and pitch
            if (m_playerParam->m_playbackRate != 1.0f && !m_playerParam->m_abortReq) {
                int bytes_per_sample = av_get_bytes_per_sample(m_audioState->m_audioParamDst.fmt);
                av_fast_malloc(&m_audioState->m_soundTouchBuffer, &m_audioState->m_soundTouchBufSize, out_size * translate_time);
                for (int i = 0; i < (resampled_data_size / 2); i++) {
                    m_audioState->m_soundTouchBuffer[i] = (m_audioState->m_resampleBuffer[i * 2] | (m_audioState->m_resampleBuffer[i * 2 + 1] << 8));
                }
                if (!m_soundTouchHelper) {
                    m_soundTouchHelper = new SoundTouchHelper();
                }
                int ret_len = m_soundTouchHelper->translate(m_audioState->m_soundTouchBuffer, (float)(m_playerParam->m_playbackRate),
                                                            (float)(1.0f / m_playerParam->m_playbackRate),
                                                           resampled_data_size / 2, bytes_per_sample,
                                                            m_audioState->m_audioParamDst.channels, m_frame->sample_rate);
                if (ret_len > 0) {
                    m_audioState->m_outputBuffer = (uint8_t*)m_audioState->m_soundTouchBuffer;
                    resampled_data_size = ret_len;
                } else {
                    translate_time++;
                    av_frame_unref(m_frame);
                    continue;
                }
            }
        } else {
            m_audioState->m_outputBuffer = m_frame->data[0];
            resampled_data_size = data_size;
        }

        break;
    }

    // update AudioClock with pts
    if (m_frame->pts != AV_NOPTS_VALUE) {
        m_audioState->m_audioClock = m_frame->pts * av_q2d((AVRational){1, m_frame->sample_rate})
                                     + (double) m_frame->nb_samples / m_frame->sample_rate;
    } else {
        m_audioState->m_audioClock = NAN;
    }

    av_frame_unref(m_frame);

    return resampled_data_size;
}

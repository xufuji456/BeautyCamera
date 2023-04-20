package com.frank.videoedit.transform;

import static com.google.android.exoplayer2.source.SampleStream.FLAG_REQUIRE_FORMAT;

import com.frank.videoedit.transform.listener.Codec;
import com.frank.videoedit.transform.listener.TransformListener;
import com.frank.videoedit.transform.util.MediaUtil;

import com.google.android.exoplayer2.FormatHolder;
import com.google.android.exoplayer2.decoder.DecoderInputBuffer;

/* package */ final class TransformerAudioRenderer extends TransformerBaseRenderer {

  private static final String TAG = "TAudioRenderer";

  private final Codec.EncoderFactory encoderFactory;
  private final Codec.DecoderFactory decoderFactory;
  private final DecoderInputBuffer decoderInputBuffer;

  public TransformerAudioRenderer(
      MuxerWrapper muxerWrapper,
      TransformerMediaClock mediaClock,
      TransformationRequest transformationRequest,
      Codec.EncoderFactory encoderFactory,
      Codec.DecoderFactory decoderFactory,
      TransformListener transformListener,
      FallbackListener fallbackListener) {
    super(
        MediaUtil.TRACK_TYPE_AUDIO,
        muxerWrapper,
        mediaClock,
        transformationRequest,
        transformListener,
        fallbackListener);
    this.encoderFactory = encoderFactory;
    this.decoderFactory = decoderFactory;
    decoderInputBuffer =
        new DecoderInputBuffer(DecoderInputBuffer.BUFFER_REPLACEMENT_MODE_DISABLED);
  }

  @Override
  public String getName() {
    return TAG;
  }

  private Format convertFormat(com.google.android.exoplayer2.Format format) {
    Format inputFormat = null;
    if (MediaUtil.isVideo(format.sampleMimeType)) {
      inputFormat = Format.createVideoSampleFormat(format.id, format.sampleMimeType, format.codecs,
              format.bitrate, format.maxInputSize, format.width, format.height, format.frameRate,
              format.initializationData, null);
    } else if (MediaUtil.isAudio(format.sampleMimeType)) {
      inputFormat = Format.createAudioSampleFormat(format.id, format.sampleMimeType, format.codecs,
              format.bitrate, format.maxInputSize, format.channelCount, format.sampleRate,
              format.initializationData, null);
    }
    return inputFormat;
  }

  @Override
  protected boolean ensureConfigured() throws TransformationException {
    if (samplePipeline != null) {
      return true;
    }
    FormatHolder formatHolder = getFormatHolder();
    int result = readSource(formatHolder, decoderInputBuffer, /* readFlags= */ FLAG_REQUIRE_FORMAT);
    if (result != MediaUtil.RESULT_FORMAT_READ) {
      return false;
    }

    Format inputFormat = convertFormat(formatHolder.format); // TODO
    samplePipeline =
        new AudioTranscodeSamplePipeline(
            inputFormat,
            streamOffsetUs,
            streamStartPositionUs,
            transformationRequest,
            decoderFactory,
            encoderFactory,
            muxerWrapper,
            fallbackListener);

    return true;
  }

}

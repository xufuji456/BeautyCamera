package com.frank.videoedit.transform;

import static com.google.android.exoplayer2.source.SampleStream.FLAG_REQUIRE_FORMAT;

import android.content.Context;

import com.frank.videoedit.listener.FrameProcessor;
import com.frank.videoedit.transform.listener.Codec;

import com.frank.videoedit.transform.util.MediaUtil;
import com.frank.videoedit.util.CommonUtil;
import com.frank.videoedit.effect.listener.GlEffect;

import com.google.android.exoplayer2.FormatHolder;
import com.google.android.exoplayer2.decoder.DecoderInputBuffer;
import com.google.android.exoplayer2.source.SampleStream.ReadDataResult;
import com.google.common.collect.ImmutableList;

/* package */ final class TransformerVideoRenderer extends TransformerBaseRenderer {

  private static final String TAG = "TVideoRenderer";

  private final Context context;
  private final boolean clippingStartsAtKeyFrame;
  private final ImmutableList<GlEffect> effects;
  private final FrameProcessor.Factory frameProcessorFactory;
  private final Codec.EncoderFactory encoderFactory;
  private final Codec.DecoderFactory decoderFactory;
  private final DecoderInputBuffer decoderInputBuffer;

  public TransformerVideoRenderer(
      Context context,
      MuxerWrapper muxerWrapper,
      TransformerMediaClock mediaClock,
      TransformationRequest transformationRequest,
      boolean clippingStartsAtKeyFrame,
      ImmutableList<GlEffect> effects,
      FrameProcessor.Factory frameProcessorFactory,
      Codec.EncoderFactory encoderFactory,
      Codec.DecoderFactory decoderFactory,
      Transformer.AsyncErrorListener asyncErrorListener,
      FallbackListener fallbackListener) {
    super(
        MediaUtil.TRACK_TYPE_VIDEO,
        muxerWrapper,
        mediaClock,
        transformationRequest,
        asyncErrorListener,
        fallbackListener);
    this.context = context;
    this.clippingStartsAtKeyFrame = clippingStartsAtKeyFrame;
    this.effects = effects;
    this.frameProcessorFactory = frameProcessorFactory;
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
    @ReadDataResult
    int result = readSource(formatHolder, decoderInputBuffer, /* readFlags= */ FLAG_REQUIRE_FORMAT);
    if (result != MediaUtil.RESULT_FORMAT_READ) {
      return false;
    }

    Format inputFormat = convertFormat(formatHolder.format); // TODO
    if (shouldTranscode(inputFormat)) {
      samplePipeline =
          new VideoTranscodeSamplePipeline(
              context,
              inputFormat,
              streamOffsetUs,
              streamStartPositionUs,
              transformationRequest,
              effects,
              frameProcessorFactory,
              decoderFactory,
              encoderFactory,
              muxerWrapper,
              fallbackListener,
              asyncErrorListener);
    } else {
      samplePipeline =
          new PassthroughSamplePipeline(
              inputFormat,
              streamOffsetUs,
              streamStartPositionUs,
              transformationRequest,
              muxerWrapper,
              fallbackListener);
    }
    return true;
  }

  private boolean shouldTranscode(Format inputFormat) {
    if ((streamStartPositionUs - streamOffsetUs) != 0 && !clippingStartsAtKeyFrame) {
      return true;
    }
    if (encoderFactory.videoNeedsEncoding()) {
      return true;
    }
    if (transformationRequest.enableRequestSdrToneMapping) {
      return true;
    }
    if (transformationRequest.forceInterpretHdrVideoAsSdr) {
      return true;
    }
    if (transformationRequest.videoMimeType != null
        && !transformationRequest.videoMimeType.equals(inputFormat.sampleMimeType)) {
      return true;
    }
    if (transformationRequest.videoMimeType == null
        && !muxerWrapper.supportsSampleMimeType(inputFormat.sampleMimeType)) {
      return true;
    }
    if (inputFormat.pixelWidthHeightRatio != 1f) {
      return true;
    }
    // The decoder rotates encoded frames for display by inputFormat.rotationDegrees.
    int decodedHeight =
        (inputFormat.rotationDegrees % 180 == 0) ? inputFormat.height : inputFormat.width;
    if (transformationRequest.outputHeight != CommonUtil.LENGTH_UNSET
        && transformationRequest.outputHeight != decodedHeight) {
      return true;
    }
    if (!effects.isEmpty()) {
      return true;
    }
    return false;
  }
}

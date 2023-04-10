package com.frank.videoedit.transform;

import androidx.annotation.Nullable;

import com.frank.videoedit.transform.listener.SamplePipeline;
import com.google.android.exoplayer2.BaseRenderer;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.RendererCapabilities;
import com.google.android.exoplayer2.decoder.DecoderInputBuffer;
import com.google.android.exoplayer2.source.SampleStream.ReadDataResult;
import com.google.android.exoplayer2.util.MediaClock;
import com.google.android.exoplayer2.util.MimeTypes;

/* package */ abstract class TransformerBaseRenderer extends BaseRenderer {

  protected final MuxerWrapper muxerWrapper;
  protected final TransformerMediaClock mediaClock;
  protected final TransformationRequest transformationRequest;
  protected final Transformer.AsyncErrorListener asyncErrorListener;
  protected final FallbackListener fallbackListener;

  private boolean isTransformationRunning;
  protected long streamOffsetUs;
  protected long streamStartPositionUs;
  protected SamplePipeline samplePipeline;

  public TransformerBaseRenderer(
      int trackType,
      MuxerWrapper muxerWrapper,
      TransformerMediaClock mediaClock,
      TransformationRequest transformationRequest,
      Transformer.AsyncErrorListener asyncErrorListener,
      FallbackListener fallbackListener) {
    super(trackType);
    this.muxerWrapper = muxerWrapper;
    this.mediaClock = mediaClock;
    this.transformationRequest = transformationRequest;
    this.asyncErrorListener = asyncErrorListener;
    this.fallbackListener = fallbackListener;
  }

  /**
   * Returns whether the renderer supports the track type of the given format.
   *
   * @param format The format.
   * @return The {@link Capabilities} for this format.
   */
  @Override
  public final @Capabilities int supportsFormat(Format format) {
    return RendererCapabilities.create(
        MimeTypes.getTrackType(format.sampleMimeType) == getTrackType()
            ? C.FORMAT_HANDLED
            : C.FORMAT_UNSUPPORTED_TYPE);
  }

  @Override
  public final MediaClock getMediaClock() {
    return mediaClock;
  }

  @Override
  public final boolean isReady() {
    return isSourceReady();
  }

  @Override
  public final boolean isEnded() {
    return samplePipeline != null && samplePipeline.isEnded();
  }

  @Override
  public final void render(long positionUs, long elapsedRealtimeUs) {
    try {
      if (!isTransformationRunning || isEnded() || !ensureConfigured()) {
        return;
      }

      while (samplePipeline.processData() || feedPipelineFromInput()) {}
    } catch (TransformationException e) {
      isTransformationRunning = false;
      asyncErrorListener.onTransformationException(e);
    }
  }

  @Override
  protected final void onStreamChanged(Format[] formats, long startPositionUs, long offsetUs) {
    this.streamOffsetUs = offsetUs;
    this.streamStartPositionUs = startPositionUs;
  }

  @Override
  protected final void onEnabled(boolean joining, boolean mayRenderStartOfStream) {
    muxerWrapper.registerTrack();
    fallbackListener.registerTrack();
    mediaClock.updateTimeForTrackType(getTrackType(), 0L);
  }

  @Override
  protected final void onStarted() {
    isTransformationRunning = true;
  }

  @Override
  protected final void onStopped() {
    isTransformationRunning = false;
  }

  @Override
  protected final void onReset() {
    if (samplePipeline != null) {
      samplePipeline.release();
    }
  }

  protected abstract boolean ensureConfigured() throws TransformationException;

  private boolean feedPipelineFromInput() throws TransformationException {
    @Nullable DecoderInputBuffer samplePipelineInputBuffer = samplePipeline.dequeueInputBuffer();
    if (samplePipelineInputBuffer == null) {
      return false;
    }

    @ReadDataResult
    int result = readSource(getFormatHolder(), samplePipelineInputBuffer, /* readFlags= */ 0);
    switch (result) {
      case C.RESULT_BUFFER_READ:
        samplePipelineInputBuffer.flip();
        if (samplePipelineInputBuffer.isEndOfStream()) {
          samplePipeline.queueInputBuffer();
          return false;
        }
        mediaClock.updateTimeForTrackType(getTrackType(), samplePipelineInputBuffer.timeUs);
        samplePipeline.queueInputBuffer();
        return true;
      case C.RESULT_FORMAT_READ:
        throw new IllegalStateException("Format changes are not supported.");
      case C.RESULT_NOTHING_READ:
      default:
        return false;
    }
  }
}

package com.frank.videoedit.transform;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.decoder.DecoderInputBuffer;

/** Pipeline that passes through the samples without any re-encoding or transformation. */
/* package */ final class PassthroughSamplePipeline extends BaseSamplePipeline {

  private final DecoderInputBuffer buffer;
  private final Format format;

  private boolean hasPendingBuffer;

  public PassthroughSamplePipeline(
      Format format,
      long streamOffsetUs,
      long streamStartPositionUs,
      TransformationRequest transformationRequest,
      MuxerWrapper muxerWrapper,
      FallbackListener fallbackListener) {
    super(
        format,
        streamOffsetUs,
        streamStartPositionUs,
        muxerWrapper);
    this.format = format;
    buffer = new DecoderInputBuffer(DecoderInputBuffer.BUFFER_REPLACEMENT_MODE_DIRECT);
    fallbackListener.onTransformationRequestFinalized(transformationRequest);
  }

  @Override
  public void release() {}

  @Override
  @Nullable
  protected DecoderInputBuffer dequeueInputBufferInternal() {
    return hasPendingBuffer ? null : buffer;
  }

  @Override
  protected void queueInputBufferInternal() {
    if (buffer.data != null && buffer.data.hasRemaining()) {
      hasPendingBuffer = true;
    }
  }

  @Override
  protected boolean processDataUpToMuxer() {
    return false;
  }

  @Override
  protected Format getMuxerInputFormat() {
    return format;
  }

  @Override
  @Nullable
  protected DecoderInputBuffer getMuxerInputBuffer() {
    return hasPendingBuffer ? buffer : null;
  }

  @Override
  protected void releaseMuxerInputBuffer() {
    buffer.clear();
    hasPendingBuffer = false;
  }

  @Override
  protected boolean isMuxerInputEnded() {
    return buffer.isEndOfStream();
  }
}

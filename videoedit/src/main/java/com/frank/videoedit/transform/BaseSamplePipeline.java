package com.frank.videoedit.transform;

import androidx.annotation.Nullable;

import com.frank.videoedit.transform.listener.Muxer;
import com.frank.videoedit.transform.listener.SamplePipeline;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.decoder.DecoderInputBuffer;
import com.google.android.exoplayer2.util.MimeTypes;


/* package */ abstract class BaseSamplePipeline implements SamplePipeline {

  private final long streamOffsetUs;
  private final long streamStartPositionUs;
  private final MuxerWrapper muxerWrapper;
  private final @C.TrackType int trackType;

  @Nullable private DecoderInputBuffer inputBuffer;
  private boolean muxerWrapperTrackAdded;
  private boolean isEnded;

  public BaseSamplePipeline(
      Format inputFormat,
      long streamOffsetUs,
      long streamStartPositionUs,
      MuxerWrapper muxerWrapper) {
    this.streamOffsetUs = streamOffsetUs;
    this.streamStartPositionUs = streamStartPositionUs;
    this.muxerWrapper = muxerWrapper;
    trackType = MimeTypes.getTrackType(inputFormat.sampleMimeType);
  }

  @Nullable
  @Override
  public DecoderInputBuffer dequeueInputBuffer() throws TransformationException {
    inputBuffer = dequeueInputBufferInternal();
    return inputBuffer;
  }

  @Override
  public void queueInputBuffer() throws TransformationException {
    queueInputBufferInternal();
  }

  @Override
  public boolean processData() throws TransformationException {
    return feedMuxer() || processDataUpToMuxer();
  }

  @Override
  public boolean isEnded() {
    return isEnded;
  }

  @Nullable
  protected abstract DecoderInputBuffer dequeueInputBufferInternal() throws TransformationException;

  protected abstract void queueInputBufferInternal() throws TransformationException;

  protected abstract boolean processDataUpToMuxer() throws TransformationException;

  @Nullable
  protected abstract Format getMuxerInputFormat() throws TransformationException;

  @Nullable
  protected abstract DecoderInputBuffer getMuxerInputBuffer() throws TransformationException;

  protected abstract void releaseMuxerInputBuffer() throws TransformationException;

  protected abstract boolean isMuxerInputEnded();

  /**
   * Attempts to pass encoded data to the muxer, and returns whether it may be possible to pass more
   * data immediately by calling this method again.
   */
  private boolean feedMuxer() throws TransformationException {
    if (!muxerWrapperTrackAdded) {
      @Nullable Format inputFormat = getMuxerInputFormat();
      if (inputFormat == null) {
        return false;
      }
      try {
        muxerWrapper.addTrackFormat(inputFormat);
      } catch (Muxer.MuxerException e) {
        throw TransformationException.createForMuxer(
            e, TransformationException.ERROR_CODE_MUXING_FAILED);
      }
      muxerWrapperTrackAdded = true;
    }

    if (isMuxerInputEnded()) {
      muxerWrapper.endTrack(trackType);
      isEnded = true;
      return false;
    }

    @Nullable DecoderInputBuffer muxerInputBuffer = getMuxerInputBuffer();
    if (muxerInputBuffer == null) {
      return false;
    }

    long samplePresentationTimeUs = muxerInputBuffer.timeUs - streamStartPositionUs;
    // TODO(b/204892224): Consider subtracting the first sample timestamp from the sample pipeline
    //  buffer from all samples so that they are guaranteed to start from zero in the output file.
    try {
      if (!muxerWrapper.writeSample(
          trackType,
          muxerInputBuffer.data,
          muxerInputBuffer.isKeyFrame(),
          samplePresentationTimeUs)) {
        return false;
      }
    } catch (Muxer.MuxerException e) {
      throw TransformationException.createForMuxer(
          e, TransformationException.ERROR_CODE_MUXING_FAILED);
    }

    releaseMuxerInputBuffer();
    return true;
  }
}

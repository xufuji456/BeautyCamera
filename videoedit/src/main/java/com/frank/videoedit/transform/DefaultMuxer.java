package com.frank.videoedit.transform;

import android.os.ParcelFileDescriptor;

import com.frank.videoedit.transform.listener.Muxer;
import com.frank.videoedit.transform.util.MediaUtil;

import com.google.common.collect.ImmutableList;

import java.nio.ByteBuffer;

public final class DefaultMuxer implements Muxer {

  public static final class Factory implements Muxer.Factory {

    /** The default value returned by {@link #getMaxDelayBetweenSamplesMs()}. */
    public static final long DEFAULT_MAX_DELAY_BETWEEN_SAMPLES_MS = 3000;

    private final Muxer.Factory muxerFactory;

    public Factory() {
      this.muxerFactory = new FrameworkMuxer.Factory(DEFAULT_MAX_DELAY_BETWEEN_SAMPLES_MS);
    }

    public Factory(long maxDelayBetweenSamplesMs) {
      this.muxerFactory = new FrameworkMuxer.Factory(maxDelayBetweenSamplesMs);
    }

    @Override
    public Muxer create(String path) throws MuxerException {
      return new DefaultMuxer(muxerFactory.create(path));
    }

    @Override
    public Muxer create(ParcelFileDescriptor parcelFileDescriptor) throws MuxerException {
      return new DefaultMuxer(muxerFactory.create(parcelFileDescriptor));
    }

    @Override
    public ImmutableList<String> getSupportedSampleMimeTypes(@MediaUtil.TrackType int trackType) {
      return muxerFactory.getSupportedSampleMimeTypes(trackType);
    }
  }

  private final Muxer muxer;

  private DefaultMuxer(Muxer muxer) {
    this.muxer = muxer;
  }

  @Override
  public int addTrack(Format format) throws MuxerException {
    return muxer.addTrack(format);
  }

  @Override
  public void writeSampleData(
      int trackIndex, ByteBuffer data, boolean isKeyFrame, long presentationTimeUs)
      throws MuxerException {
    muxer.writeSampleData(trackIndex, data, isKeyFrame, presentationTimeUs);
  }

  @Override
  public void release(boolean forCancellation) throws MuxerException {
    muxer.release(forCancellation);
  }

  @Override
  public long getMaxDelayBetweenSamplesMs() {
    return muxer.getMaxDelayBetweenSamplesMs();
  }
}

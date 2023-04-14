package com.frank.videoedit.transform;

import androidx.annotation.Nullable;

import com.frank.videoedit.util.CommonUtil;

public final class TransformationResult {

  public static final class Builder {
    private long durationMs;
    private long fileSizeBytes;
    private int averageAudioBitrate;
    private int averageVideoBitrate;
    private int videoFrameCount;

    public Builder() {
      durationMs          = CommonUtil.TIME_UNSET;
      fileSizeBytes       = CommonUtil.LENGTH_UNSET;
      averageAudioBitrate = CommonUtil.RATE_UNSET_INT;
      averageVideoBitrate = CommonUtil.RATE_UNSET_INT;
    }

    public Builder setDurationMs(long durationMs) {
      this.durationMs = durationMs;
      return this;
    }

    public Builder setFileSizeBytes(long fileSizeBytes) {
      this.fileSizeBytes = fileSizeBytes;
      return this;
    }

    public Builder setAverageAudioBitrate(int averageAudioBitrate) {
      this.averageAudioBitrate = averageAudioBitrate;
      return this;
    }

    public Builder setAverageVideoBitrate(int averageVideoBitrate) {
      this.averageVideoBitrate = averageVideoBitrate;
      return this;
    }

    public Builder setVideoFrameCount(int videoFrameCount) {
      this.videoFrameCount = videoFrameCount;
      return this;
    }

    public TransformationResult build() {
      return new TransformationResult(
          durationMs, fileSizeBytes, averageAudioBitrate, averageVideoBitrate, videoFrameCount);
    }
  }

  public final long durationMs;

  public final long fileSizeBytes;

  public final int averageAudioBitrate;

  public final int averageVideoBitrate;

  public final int videoFrameCount;

  private TransformationResult(
      long durationMs,
      long fileSizeBytes,
      int averageAudioBitrate,
      int averageVideoBitrate,
      int videoFrameCount) {
    this.durationMs = durationMs;
    this.fileSizeBytes = fileSizeBytes;
    this.averageAudioBitrate = averageAudioBitrate;
    this.averageVideoBitrate = averageVideoBitrate;
    this.videoFrameCount = videoFrameCount;
  }

  public Builder buildUpon() {
    return new Builder()
        .setDurationMs(durationMs)
        .setFileSizeBytes(fileSizeBytes)
        .setAverageAudioBitrate(averageAudioBitrate)
        .setAverageVideoBitrate(averageVideoBitrate)
        .setVideoFrameCount(videoFrameCount);
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TransformationResult)) {
      return false;
    }
    TransformationResult result = (TransformationResult) o;
    return durationMs == result.durationMs
        && fileSizeBytes == result.fileSizeBytes
        && averageAudioBitrate == result.averageAudioBitrate
        && averageVideoBitrate == result.averageVideoBitrate
        && videoFrameCount == result.videoFrameCount;
  }

  @Override
  public int hashCode() {
    int result = (int) durationMs;
    result = 31 * result + (int) fileSizeBytes;
    result = 31 * result + averageAudioBitrate;
    result = 31 * result + averageVideoBitrate;
    result = 31 * result + videoFrameCount;
    return result;
  }
}

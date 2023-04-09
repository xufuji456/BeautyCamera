package com.frank.videoedit.transform;

import static com.google.android.exoplayer2.util.Assertions.checkArgument;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;

/** Information about the result of a successful transformation. */
public final class TransformationResult {

  /** A builder for {@link TransformationResult} instances. */
  public static final class Builder {
    private long durationMs;
    private long fileSizeBytes;
    private int averageAudioBitrate;
    private int averageVideoBitrate;
    private int videoFrameCount;

    public Builder() {
      durationMs = C.TIME_UNSET;
      fileSizeBytes = C.LENGTH_UNSET;
      averageAudioBitrate = C.RATE_UNSET_INT;
      averageVideoBitrate = C.RATE_UNSET_INT;
    }

    public Builder setDurationMs(long durationMs) {
      checkArgument(durationMs > 0 || durationMs == C.TIME_UNSET);
      this.durationMs = durationMs;
      return this;
    }

    public Builder setFileSizeBytes(long fileSizeBytes) {
      checkArgument(fileSizeBytes > 0 || fileSizeBytes == C.LENGTH_UNSET);
      this.fileSizeBytes = fileSizeBytes;
      return this;
    }

    public Builder setAverageAudioBitrate(int averageAudioBitrate) {
      checkArgument(averageAudioBitrate > 0 || averageAudioBitrate == C.RATE_UNSET_INT);
      this.averageAudioBitrate = averageAudioBitrate;
      return this;
    }

    public Builder setAverageVideoBitrate(int averageVideoBitrate) {
      checkArgument(averageVideoBitrate > 0 || averageVideoBitrate == C.RATE_UNSET_INT);
      this.averageVideoBitrate = averageVideoBitrate;
      return this;
    }

    public Builder setVideoFrameCount(int videoFrameCount) {
      checkArgument(videoFrameCount >= 0);
      this.videoFrameCount = videoFrameCount;
      return this;
    }

    public TransformationResult build() {
      return new TransformationResult(
          durationMs, fileSizeBytes, averageAudioBitrate, averageVideoBitrate, videoFrameCount);
    }
  }

  /** The duration of the file in milliseconds, or {@link C#TIME_UNSET} if unset or unknown. */
  public final long durationMs;
  /** The size of the file in bytes, or {@link C#LENGTH_UNSET} if unset or unknown. */
  public final long fileSizeBytes;
  /**
   * The average bitrate of the audio track data, or {@link C#RATE_UNSET_INT} if unset or unknown.
   */
  public final int averageAudioBitrate;
  /**
   * The average bitrate of the video track data, or {@link C#RATE_UNSET_INT} if unset or unknown.
   */
  public final int averageVideoBitrate;
  /** The number of video frames. */
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

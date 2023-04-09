package com.frank.videoedit.transform;

import android.media.MediaCodecInfo;

/** Provides bitrates for encoders to use as a target. */
public interface EncoderBitrateProvider {

  /**
   * Returns a recommended bitrate that the encoder should target.
   *
   * @param encoderName The name of the encoder, see {@link MediaCodecInfo#getName()}.
   * @param width The output width of the video after encoding.
   * @param height The output height of the video after encoding.
   * @param frameRate The expected output frame rate of the video after encoding.
   * @return The bitrate the encoder should target.
   */
  int getBitrate(String encoderName, int width, int height, float frameRate);
}

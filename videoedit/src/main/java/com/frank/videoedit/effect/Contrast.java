
package com.frank.videoedit.effect;

import static com.google.android.exoplayer2.util.Assertions.checkArgument;

import android.content.Context;

import com.frank.videoedit.effect.listener.GlEffect;
import com.google.android.exoplayer2.util.FrameProcessingException;

/** A {@link GlEffect} to control the contrast of video frames. */
public class Contrast implements GlEffect {

  /** Adjusts the contrast of video frames in the interval [-1, 1]. */
  public final float contrast;

  /**
   * Creates a new instance for the given contrast value.
   *
   * <p>Contrast values range from -1 (all gray pixels) to 1 (maximum difference of colors). 0 means
   * to add no contrast and leaves the frames unchanged.
   */
  public Contrast(float contrast) {
    checkArgument(-1 <= contrast && contrast <= 1, "Contrast needs to be in the interval [-1, 1].");
    this.contrast = contrast;
  }

  @Override
  public SingleFrameGlTextureProcessor toGlTextureProcessor(Context context, boolean useHdr)
      throws FrameProcessingException {
    return new ContrastProcessor(context, this, useHdr);
  }
}

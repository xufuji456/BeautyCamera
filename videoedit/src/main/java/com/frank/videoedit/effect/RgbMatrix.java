package com.frank.videoedit.effect;

import android.content.Context;

import com.google.android.exoplayer2.util.FrameProcessingException;
import com.google.common.collect.ImmutableList;

/**
 * Specifies a 4x4 RGB color transformation matrix to apply to each frame in the fragment shader.
 */
public interface RgbMatrix extends GlEffect {

  /**
   * Returns the 4x4 RGB transformation {@linkplain android.opengl.Matrix matrix} to apply to the
   * color values of each pixel in the frame with the given timestamp.
   *
   * @param presentationTimeUs The timestamp of the frame to apply the matrix on.
   * @param useHdr If {@code true}, colors will be in linear RGB BT.2020. If {@code false}, colors
   *     will be in linear RGB BT.709. Must be consistent with {@code useHdr} in {@link
   *     #toGlTextureProcessor(Context, boolean)}.
   * @return The {@code RgbMatrix} to apply to the frame.
   */
  float[] getMatrix(long presentationTimeUs, boolean useHdr);

  @Override
  default SingleFrameGlTextureProcessor toGlTextureProcessor(Context context, boolean useHdr)
      throws FrameProcessingException {
    return MatrixTextureProcessor.create(
        context,
        /* matrixTransformations= */ ImmutableList.of(),
        /* rgbMatrices= */ ImmutableList.of(this),
        useHdr);
  }
}

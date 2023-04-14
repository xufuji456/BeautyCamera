package com.frank.videoedit.effect.listener;

import android.content.Context;
import android.opengl.Matrix;
import android.util.Pair;

import com.frank.videoedit.effect.MatrixTextureProcessor;
import com.frank.videoedit.effect.SingleFrameGlTextureProcessor;
import com.frank.videoedit.util.FrameProcessingException;

import java.util.List;

/**
 * Specifies a 4x4 transformation {@link Matrix} to apply in the vertex shader for each frame.
 *
 * <p>The matrix is applied to points given in normalized device coordinates (-1 to 1 on x, y, and z
 * axes). Transformed pixels that are moved outside of the normal device coordinate range are
 * clipped.
 *
 * <p>Output frame pixels outside of the transformed input frame will be black, with alpha = 0 if
 * applicable.
 */
public interface GlMatrixTransformation extends GlEffect {
  /**
   * Configures the input and output dimensions.
   *
   * <p>Must be called before {@link #getGlMatrixArray(long)}.
   *
   * @param inputWidth The input frame width, in pixels.
   * @param inputHeight The input frame height, in pixels.
   * @return The output frame width and height, in pixels.
   */
  default Pair<Integer, Integer> configure(int inputWidth, int inputHeight) {
    return Pair.create(inputWidth, inputHeight);
  }

  /**
   * Returns the 4x4 transformation {@link Matrix} to apply to the frame with the given timestamp.
   */
  float[] getGlMatrixArray(long presentationTimeUs);

  @Override
  default SingleFrameGlTextureProcessor toGlTextureProcessor(Context context, boolean useHdr)
      throws FrameProcessingException {
    return MatrixTextureProcessor.create(
        context,
        /* matrixTransformations= */ List.of(this),
        useHdr);
  }
}

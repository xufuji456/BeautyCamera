package com.frank.videoedit.effect.listener;

import android.graphics.Matrix;

import com.frank.videoedit.effect.util.MatrixUtil;

/**
 * Specifies a 3x3 transformation {@link Matrix} to apply in the vertex shader for each frame.
 *
 * <p>The matrix is applied to points given in normalized device coordinates (-1 to 1 on x and y
 * axes). Transformed pixels that are moved outside of the normal device coordinate range are
 * clipped.
 *
 * <p>Output frame pixels outside of the transformed input frame will be black, with alpha = 0 if
 * applicable.
 */
public interface MatrixTransformation extends GlMatrixTransformation {
  /**
   * Returns the 3x3 transformation {@link Matrix} to apply to the frame with the given timestamp.
   */
  Matrix getMatrix(long presentationTimeUs);

  @Override
  default float[] getGlMatrixArray(long presentationTimeUs) {
    return MatrixUtil.getGlMatrixArray(getMatrix(presentationTimeUs));
  }
}

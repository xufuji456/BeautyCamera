
package com.frank.videoedit;

import android.graphics.Matrix;

import com.frank.videoedit.effect.MatrixTransformation;
import com.google.android.exoplayer2.C;

/* package */ final class MatrixTransformationFactory {

  public static MatrixTransformation createZoomInTransition() {
    return MatrixTransformationFactory::calculateZoomInTransitionMatrix;
  }

  private static final float ZOOM_DURATION_SECONDS = 2f;

  private static Matrix calculateZoomInTransitionMatrix(long presentationTimeUs) {
    Matrix transformationMatrix = new Matrix();
    float scale = Math.min(1, presentationTimeUs / (C.MICROS_PER_SECOND * ZOOM_DURATION_SECONDS));
    transformationMatrix.postScale(/* sx= */ scale, /* sy= */ scale);
    return transformationMatrix;
  }

}

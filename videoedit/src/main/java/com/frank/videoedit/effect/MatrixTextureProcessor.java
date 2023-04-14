package com.frank.videoedit.effect;


import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Pair;

import com.frank.videoedit.effect.listener.ExternalTextureProcessor;
import com.frank.videoedit.effect.listener.GlMatrixTransformation;
import com.frank.videoedit.effect.util.MatrixUtil;
import com.frank.videoedit.effect.util.GlUtil;
import com.frank.videoedit.effect.util.GlProgram;
import com.frank.videoedit.entity.ColorInfo;
import com.frank.videoedit.util.FrameProcessingException;

import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("FunctionalInterfaceClash") // b/228192298
public final class MatrixTextureProcessor extends SingleFrameGlTextureProcessor
    implements ExternalTextureProcessor {

  private static final String VERTEX_SHADER_TRANSFORMATION_PATH =
          "shaders/vertex_transform_es2.glsl";
  private static final String VERTEX_SHADER_TRANSFORMATION_ES3_PATH =
          "shaders/vertex_transform_es3.glsl";
  private static final String FRAGMENT_SHADER_TRANSFORMATION_PATH =
          "shaders/fragment_transform_es2.glsl";
  private static final String FRAGMENT_SHADER_OETF_ES3_PATH =
          "shaders/fragment_oetf_es3.glsl";
  private static final String FRAGMENT_SHADER_TRANSFORMATION_SDR_OETF_ES2_PATH =
          "shaders/fragment_transform_sdr_oetf_es2.glsl";
  private static final String FRAGMENT_SHADER_TRANSFORMATION_EXTERNAL_YUV_ES3_PATH =
          "shaders/fragment_transform_external_yuv_es3.glsl";
  private static final String FRAGMENT_SHADER_TRANSFORMATION_SDR_EXTERNAL_PATH =
          "shaders/fragment_transform_sdr_external_es2.glsl";
  private static final ImmutableList<float[]> NDC_SQUARE =
      ImmutableList.of(
          new float[] {-1, -1, 0, 1},
          new float[] {-1, 1, 0, 1},
          new float[] {1, 1, 0, 1},
          new float[] {1, -1, 0, 1});

  // YUV to RGB color transform coefficients can be calculated from the BT.2020 specification, by
  // inverting the RGB to YUV equations, and scaling for limited range.
  // https://www.itu.int/dms_pubrec/itu-r/rec/bt/R-REC-BT.2020-2-201510-I!!PDF-E.pdf
  private static final float[] BT2020_FULL_RANGE_YUV_TO_RGB_COLOR_TRANSFORM_MATRIX = {
    1.0000f, 1.0000f, 1.0000f,
    0.0000f, -0.1646f, 1.8814f,
    1.4746f, -0.5714f, 0.0000f
  };
  private static final float[] BT2020_LIMITED_RANGE_YUV_TO_RGB_COLOR_TRANSFORM_MATRIX = {
    1.1689f, 1.1689f, 1.1689f,
    0.0000f, -0.1881f, 2.1502f,
    1.6853f, -0.6530f, 0.0000f,
  };

  private final ImmutableList<GlMatrixTransformation> matrixTransformations;
  private final boolean useHdr;
  private final float[][] transformationMatrixCache;
  private final float[] compositeTransformationMatrixArray;
  private final float[] compositeRgbMatrixArray;
  private final float[] tempResultMatrix;
  private ImmutableList<float[]> visiblePolygon;

  private final GlProgram glProgram;

  public static MatrixTextureProcessor create(
      Context context,
      List<GlMatrixTransformation> matrixTransformations,
      boolean useHdr)
      throws FrameProcessingException {
    GlProgram glProgram =
        createGlProgram(
            context, VERTEX_SHADER_TRANSFORMATION_PATH, FRAGMENT_SHADER_TRANSFORMATION_PATH);

    // No transfer functions needed, because input and output are both optical colors.
    // TODO(b/241902517): Add transfer functions since existing color filters may change the colors.
    return new MatrixTextureProcessor(
        glProgram,
        ImmutableList.copyOf(matrixTransformations),
        useHdr);
  }

  public static MatrixTextureProcessor createWithExternalSamplerApplyingEotf(
      Context context,
      List<GlMatrixTransformation> matrixTransformations,
      ColorInfo electricalColorInfo)
      throws FrameProcessingException {
    boolean useHdr = ColorInfo.isTransferHdr(electricalColorInfo);
    String vertexShaderFilePath =
        useHdr ? VERTEX_SHADER_TRANSFORMATION_ES3_PATH : VERTEX_SHADER_TRANSFORMATION_PATH;
    String fragmentShaderFilePath =
        useHdr
            ? FRAGMENT_SHADER_TRANSFORMATION_EXTERNAL_YUV_ES3_PATH
            : FRAGMENT_SHADER_TRANSFORMATION_SDR_EXTERNAL_PATH;
    GlProgram glProgram = createGlProgram(context, vertexShaderFilePath, fragmentShaderFilePath);

    if (useHdr) {
      // In HDR editing mode the decoder output is sampled in YUV.
      if (!GlUtil.isYuvTargetExtensionSupported()) {
        throw new FrameProcessingException(
            "The EXT_YUV_target extension is required for HDR editing input.");
      }
      glProgram.setFloatsUniform(
          "uYuvToRgbColorTransform",
          electricalColorInfo.colorRange == ColorInfo.COLOR_RANGE_FULL
              ? BT2020_FULL_RANGE_YUV_TO_RGB_COLOR_TRANSFORM_MATRIX
              : BT2020_LIMITED_RANGE_YUV_TO_RGB_COLOR_TRANSFORM_MATRIX);

      @ColorInfo.ColorTransfer int colorTransfer = electricalColorInfo.colorTransfer;
      if(colorTransfer != ColorInfo.COLOR_TRANSFER_HLG && colorTransfer != ColorInfo.COLOR_TRANSFER_ST2084) {
        throw new IllegalArgumentException("don't support colorTransfer:" + colorTransfer);
      }
      glProgram.setIntUniform("uEotfColorTransfer", colorTransfer);
    } else {
      glProgram.setIntUniform("uApplyOetf", 0);
    }

    return new MatrixTextureProcessor(
        glProgram,
        ImmutableList.copyOf(matrixTransformations),
        useHdr);
  }

  public static MatrixTextureProcessor createApplyingOetf(
      Context context,
      List<GlMatrixTransformation> matrixTransformations,
      ColorInfo electricalColorInfo)
      throws FrameProcessingException {
    boolean useHdr = ColorInfo.isTransferHdr(electricalColorInfo);
    String vertexShaderFilePath =
        useHdr ? VERTEX_SHADER_TRANSFORMATION_ES3_PATH : VERTEX_SHADER_TRANSFORMATION_PATH;
    String fragmentShaderFilePath =
        useHdr ? FRAGMENT_SHADER_OETF_ES3_PATH : FRAGMENT_SHADER_TRANSFORMATION_SDR_OETF_ES2_PATH;
    GlProgram glProgram = createGlProgram(context, vertexShaderFilePath, fragmentShaderFilePath);

    if (useHdr) {
      @ColorInfo.ColorTransfer int colorTransfer = electricalColorInfo.colorTransfer;
      if(colorTransfer != ColorInfo.COLOR_TRANSFER_HLG && colorTransfer != ColorInfo.COLOR_TRANSFER_ST2084) {
        throw new IllegalArgumentException("don't support colorTransfer:" + colorTransfer);
      }
      glProgram.setIntUniform("uOetfColorTransfer", colorTransfer);
    }

    return new MatrixTextureProcessor(
        glProgram,
        ImmutableList.copyOf(matrixTransformations),
        useHdr);
  }

  public static MatrixTextureProcessor createWithExternalSamplerApplyingEotfThenOetf(
      Context context,
      List<GlMatrixTransformation> matrixTransformations,
      ColorInfo electricalColorInfo)
      throws FrameProcessingException {
    boolean useHdr = ColorInfo.isTransferHdr(electricalColorInfo);
    String vertexShaderFilePath =
        useHdr ? VERTEX_SHADER_TRANSFORMATION_ES3_PATH : VERTEX_SHADER_TRANSFORMATION_PATH;
    String fragmentShaderFilePath =
        useHdr
            ? FRAGMENT_SHADER_TRANSFORMATION_EXTERNAL_YUV_ES3_PATH
            : FRAGMENT_SHADER_TRANSFORMATION_SDR_EXTERNAL_PATH;
    GlProgram glProgram = createGlProgram(context, vertexShaderFilePath, fragmentShaderFilePath);

    if (useHdr) {
      // In HDR editing mode the decoder output is sampled in YUV.
      if (!GlUtil.isYuvTargetExtensionSupported()) {
        throw new FrameProcessingException(
            "The EXT_YUV_target extension is required for HDR editing input.");
      }
      glProgram.setFloatsUniform(
          "uYuvToRgbColorTransform",
          electricalColorInfo.colorRange == ColorInfo.COLOR_RANGE_FULL
              ? BT2020_FULL_RANGE_YUV_TO_RGB_COLOR_TRANSFORM_MATRIX
              : BT2020_LIMITED_RANGE_YUV_TO_RGB_COLOR_TRANSFORM_MATRIX);

      // No transfer functions needed, because the EOTF and OETF cancel out.
      glProgram.setIntUniform("uEotfColorTransfer", -1);
    } else {
      glProgram.setIntUniform("uApplyOetf", 1);
    }

    return new MatrixTextureProcessor(
        glProgram,
        ImmutableList.copyOf(matrixTransformations),
        useHdr);
  }

  private MatrixTextureProcessor(
      GlProgram glProgram,
      ImmutableList<GlMatrixTransformation> matrixTransformations,
      boolean useHdr) {
    super(useHdr);
    this.glProgram = glProgram;
    this.matrixTransformations = matrixTransformations;
    this.useHdr = useHdr;

    transformationMatrixCache = new float[matrixTransformations.size()][16];
    compositeTransformationMatrixArray = GlUtil.create4x4IdentityMatrix();
    compositeRgbMatrixArray = GlUtil.create4x4IdentityMatrix();
    tempResultMatrix = new float[16];
    visiblePolygon = NDC_SQUARE;
  }

  private static GlProgram createGlProgram(
      Context context, String vertexShaderFilePath, String fragmentShaderFilePath)
      throws FrameProcessingException {

    GlProgram glProgram;
    try {
      glProgram = new GlProgram(context, vertexShaderFilePath, fragmentShaderFilePath);
    } catch (IOException | GlUtil.GlException e) {
      throw new FrameProcessingException(e);
    }

    float[] identityMatrix = GlUtil.create4x4IdentityMatrix();
    glProgram.setFloatsUniform("uTexTransformationMatrix", identityMatrix);
    return glProgram;
  }

  @Override
  public void setTextureTransformMatrix(float[] textureTransformMatrix) {
    glProgram.setFloatsUniform("uTexTransformationMatrix", textureTransformMatrix);
  }

  @Override
  public Pair<Integer, Integer> configure(int inputWidth, int inputHeight) {
    return MatrixUtil.configureAndGetOutputSize(inputWidth, inputHeight, matrixTransformations);
  }

  @Override
  public void drawFrame(int inputTexId, long presentationTimeUs) throws FrameProcessingException {
    updateCompositeTransformationMatrixAndVisiblePolygon(presentationTimeUs);
    if (visiblePolygon.size() < 3) {
      return; // Need at least three visible vertices for a triangle.
    }

    try {
      glProgram.use();
      glProgram.setSamplerTexIdUniform("uTexSampler", inputTexId, /* texUnitIndex= */ 0);
      glProgram.setFloatsUniform("uTransformationMatrix", compositeTransformationMatrixArray);
      glProgram.setFloatsUniform("uRgbMatrix", compositeRgbMatrixArray);
      glProgram.setBufferAttribute(
          "aFramePosition",
          GlUtil.createVertexBuffer(visiblePolygon),
          GlUtil.HOMOGENEOUS_COORDINATE_VECTOR_SIZE);
      glProgram.bindAttributesAndUniforms();
      GLES20.glDrawArrays(
          GLES20.GL_TRIANGLE_FAN, /* first= */ 0, /* count= */ visiblePolygon.size());
      GlUtil.checkGlError();
    } catch (GlUtil.GlException e) {
      throw new FrameProcessingException(e, presentationTimeUs);
    }
  }

  @Override
  public void release() throws FrameProcessingException {
    super.release();
    try {
      glProgram.delete();
    } catch (GlUtil.GlException e) {
      throw new FrameProcessingException(e);
    }
  }

  /**
   * Updates {@link #compositeTransformationMatrixArray} and {@link #visiblePolygon} based on the
   * given frame timestamp.
   */
  private void updateCompositeTransformationMatrixAndVisiblePolygon(long presentationTimeUs) {
    float[][] matricesAtPresentationTime = new float[matrixTransformations.size()][16];
    for (int i = 0; i < matrixTransformations.size(); i++) {
      matricesAtPresentationTime[i] =
          matrixTransformations.get(i).getGlMatrixArray(presentationTimeUs);
    }

    if (!updateMatrixCache(transformationMatrixCache, matricesAtPresentationTime)) {
      return;
    }

    // Compute the compositeTransformationMatrix and transform and clip the visiblePolygon for each
    // MatrixTransformation's matrix.
    GlUtil.setToIdentity(compositeTransformationMatrixArray);
    visiblePolygon = NDC_SQUARE;
    for (float[] transformationMatrix : transformationMatrixCache) {
      Matrix.multiplyMM(
          /* result= */ tempResultMatrix,
          /* resultOffset= */ 0,
          /* lhs= */ transformationMatrix,
          /* lhsOffset= */ 0,
          /* rhs= */ compositeTransformationMatrixArray,
          /* rhsOffset= */ 0);
      System.arraycopy(
          /* src= */ tempResultMatrix,
          /* srcPos= */ 0,
          /* dest= */ compositeTransformationMatrixArray,
          /* destPost= */ 0,
          /* length= */ tempResultMatrix.length);
      visiblePolygon =
          MatrixUtil.clipConvexPolygonToNdcRange(
              MatrixUtil.transformPoints(transformationMatrix, visiblePolygon));
      if (visiblePolygon.size() < 3) {
        // Can ignore remaining matrices as there are not enough vertices left to form a polygon.
        return;
      }
    }
    // Calculate the input frame vertices corresponding to the output frame's visible polygon.
    Matrix.invertM(
        tempResultMatrix,
        /* mInvOffset= */ 0,
        compositeTransformationMatrixArray,
        /* mOffset= */ 0);
    visiblePolygon = MatrixUtil.transformPoints(tempResultMatrix, visiblePolygon);
  }

  /**
   * Updates the {@code cachedMatrices} with the {@code newMatrices}. Returns whether a matrix has
   * changed inside the cache.
   *
   * @param cachedMatrices The existing cached matrices. Gets updated if it is out of date.
   * @param newMatrices The new matrices to compare the cached matrices against.
   */
  private static boolean updateMatrixCache(float[][] cachedMatrices, float[][] newMatrices) {
    boolean matrixChanged = false;
    for (int i = 0; i < cachedMatrices.length; i++) {
      float[] cachedMatrix = cachedMatrices[i];
      float[] newMatrix = newMatrices[i];
      if (!Arrays.equals(cachedMatrix, newMatrix)) {
        System.arraycopy(
            /* src= */ newMatrix,
            /* srcPos= */ 0,
            /* dest= */ cachedMatrix,
            /* destPost= */ 0,
            /* length= */ newMatrix.length);
        matrixChanged = true;
      }
    }
    return matrixChanged;
  }
}

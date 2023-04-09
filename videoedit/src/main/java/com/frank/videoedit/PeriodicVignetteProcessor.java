
package com.frank.videoedit;

import static com.google.android.exoplayer2.util.Assertions.checkArgument;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;
import android.util.Pair;

import com.frank.videoedit.effect.SingleFrameGlTextureProcessor;
import com.google.android.exoplayer2.util.FrameProcessingException;
import com.google.android.exoplayer2.util.GlProgram;
import com.google.android.exoplayer2.util.GlUtil;

import java.io.IOException;

/* package */ final class PeriodicVignetteProcessor extends SingleFrameGlTextureProcessor {

  private static final String VERTEX_SHADER_PATH = "vertex_copy_es2.glsl";
  private static final String FRAGMENT_SHADER_PATH = "fragment_vignette_es2.glsl";
  private static final float DIMMING_PERIOD_US = 5_600_000f;

  private GlProgram glProgram;
  private final float minInnerRadius;
  private final float deltaInnerRadius;

  public PeriodicVignetteProcessor(
      Context context,
      boolean useHdr,
      float centerX,
      float centerY,
      float minInnerRadius,
      float maxInnerRadius,
      float outerRadius) {
    super(useHdr);
    checkArgument(minInnerRadius <= maxInnerRadius);
    checkArgument(maxInnerRadius <= outerRadius);
    this.minInnerRadius = minInnerRadius;
    this.deltaInnerRadius = maxInnerRadius - minInnerRadius;
    try {
      glProgram = new GlProgram(context, VERTEX_SHADER_PATH, FRAGMENT_SHADER_PATH);
      glProgram.setFloatsUniform("uCenter", new float[] {centerX, centerY});
      glProgram.setFloatsUniform("uOuterRadius", new float[] {outerRadius});
      // Draw the frame on the entire normalized device coordinate space, from -1 to 1, for x and y.
      glProgram.setBufferAttribute(
              "aFramePosition",
              GlUtil.getNormalizedCoordinateBounds(),
              GlUtil.HOMOGENEOUS_COORDINATE_VECTOR_SIZE);
    } catch (IOException | GlUtil.GlException e) {
      Log.e("VignetteProcessor", "create glProgram error:" + e.getMessage());
    }
  }

  @Override
  public Pair<Integer, Integer> configure(int inputWidth, int inputHeight) {
    return Pair.create(inputWidth, inputHeight);
  }

  @Override
  public void drawFrame(int inputTexId, long presentationTimeUs) {
    try {
      glProgram.use();
      glProgram.setSamplerTexIdUniform("uTexSampler", inputTexId, /* texUnitIndex= */ 0);
      double theta = presentationTimeUs * 2 * Math.PI / DIMMING_PERIOD_US;
      float innerRadius =
          minInnerRadius + deltaInnerRadius * (0.5f - 0.5f * (float) Math.cos(theta));
      glProgram.setFloatsUniform("uInnerRadius", new float[] {innerRadius});
      glProgram.bindAttributesAndUniforms();
      // The four-vertex triangle strip forms a quad.
      GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, /* first= */ 0, /* count= */ 4);
    } catch (GlUtil.GlException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void release() throws FrameProcessingException {
    super.release();
    try {
      glProgram.delete();
    } catch (GlUtil.GlException e) {
      throw new RuntimeException(e);
    }
  }
}

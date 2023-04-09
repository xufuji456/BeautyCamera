package com.frank.videoedit.effect;

import static com.google.android.exoplayer2.util.Assertions.checkArgument;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Pair;

import com.google.android.exoplayer2.util.FrameProcessingException;
import com.google.android.exoplayer2.util.GlProgram;
import com.google.android.exoplayer2.util.GlUtil;

import java.io.IOException;

/* package */ final class ColorLutProcessor extends SingleFrameGlTextureProcessor {
  private static final String VERTEX_SHADER_PATH = "shaders/vertex_shader_transform_es2.glsl";
  private static final String FRAGMENT_SHADER_PATH = "shaders/fragment_lut_es2.glsl";

  private final GlProgram glProgram;
  private final ColorLut colorLut;

  public ColorLutProcessor(Context context, ColorLut colorLut, boolean useHdr)
      throws FrameProcessingException {
    super(useHdr);
    // TODO(b/246315245): Add HDR support.
    checkArgument(!useHdr, "LutProcessor does not support HDR colors.");
    this.colorLut = colorLut;

    try {
      glProgram = new GlProgram(context, VERTEX_SHADER_PATH, FRAGMENT_SHADER_PATH);
    } catch (IOException | GlUtil.GlException e) {
      throw new FrameProcessingException(e);
    }

    // Draw the frame on the entire normalized device coordinate space, from -1 to 1, for x and y.
    glProgram.setBufferAttribute(
        "aFramePosition",
        GlUtil.getNormalizedCoordinateBounds(),
        GlUtil.HOMOGENEOUS_COORDINATE_VECTOR_SIZE);

    float[] identityMatrix = GlUtil.create4x4IdentityMatrix();
    glProgram.setFloatsUniform("uTransformationMatrix", identityMatrix);
    glProgram.setFloatsUniform("uTexTransformationMatrix", identityMatrix);
  }

  @Override
  public Pair<Integer, Integer> configure(int inputWidth, int inputHeight) {
    return Pair.create(inputWidth, inputHeight);
  }

  @Override
  public void drawFrame(int inputTexId, long presentationTimeUs) throws FrameProcessingException {
    try {
      glProgram.use();
      glProgram.setSamplerTexIdUniform("uTexSampler", inputTexId, /* texUnitIndex= */ 0);
      glProgram.setSamplerTexIdUniform(
          "uColorLut", colorLut.getLutTextureId(presentationTimeUs), /* texUnitIndex= */ 1);
      glProgram.setFloatUniform("uColorLutLength", colorLut.getLength(presentationTimeUs));
      glProgram.bindAttributesAndUniforms();

      GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, /* first= */ 0, /* count= */ 4);
    } catch (GlUtil.GlException e) {
      throw new FrameProcessingException(e);
    }
  }

  @Override
  public void release() throws FrameProcessingException {
    super.release();
    try {
      colorLut.release();
      glProgram.delete();
    } catch (GlUtil.GlException e) {
      throw new FrameProcessingException(e);
    }
  }
}

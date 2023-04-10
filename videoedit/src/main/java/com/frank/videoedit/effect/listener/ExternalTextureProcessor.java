package com.frank.videoedit.effect.listener;

public interface ExternalTextureProcessor extends GlTextureProcessor {

  /**
   * Sets the texture transform matrix for converting an external surface texture's coordinates to
   * sampling locations.
   *
   * @param textureTransformMatrix The external surface texture's {@linkplain
   *     android.graphics.SurfaceTexture#getTransformMatrix(float[]) transform matrix}.
   */
  void setTextureTransformMatrix(float[] textureTransformMatrix);
}

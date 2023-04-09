package com.frank.videoedit.effect;

import com.google.android.exoplayer2.C;

/** Contains information describing an OpenGL texture. */
public final class TextureInfo {

  /** A {@link TextureInfo} instance with all fields unset. */
  public static final TextureInfo UNSET =
      new TextureInfo(C.INDEX_UNSET, C.INDEX_UNSET, C.LENGTH_UNSET, C.LENGTH_UNSET);

  /** The OpenGL texture identifier. */
  public final int texId;
  /** Identifier of a framebuffer object associated with the texture. */
  public final int fboId;
  /** The width of the texture, in pixels. */
  public final int width;
  /** The height of the texture, in pixels. */
  public final int height;

  /**
   * Creates a new instance.
   *
   * @param texId The OpenGL texture identifier.
   * @param fboId Identifier of a framebuffer object associated with the texture.
   * @param width The width of the texture, in pixels.
   * @param height The height of the texture, in pixels.
   */
  public TextureInfo(int texId, int fboId, int width, int height) {
    this.texId = texId;
    this.fboId = fboId;
    this.width = width;
    this.height = height;
  }
}

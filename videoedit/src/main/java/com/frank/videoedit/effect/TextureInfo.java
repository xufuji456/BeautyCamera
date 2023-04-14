package com.frank.videoedit.effect;

import com.frank.videoedit.util.CommonUtil;

public final class TextureInfo {

  public static final TextureInfo UNSET =
      new TextureInfo(CommonUtil.INDEX_UNSET, CommonUtil.INDEX_UNSET,
              CommonUtil.LENGTH_UNSET, CommonUtil.LENGTH_UNSET);

  public final int texId;

  public final int fboId;

  public final int width;

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
    this.texId  = texId;
    this.fboId  = fboId;
    this.width  = width;
    this.height = height;
  }
}

package com.frank.videoedit.effect;

import android.content.Context;

import androidx.annotation.WorkerThread;

import com.google.android.exoplayer2.util.FrameProcessingException;
import com.google.android.exoplayer2.util.GlUtil;

/**
 * Specifies color transformations using color lookup tables to apply to each frame in the fragment
 * shader.
 */
public interface ColorLut extends GlEffect {

  /**
   * Returns the OpenGL texture ID of the LUT to apply to the pixels of the frame with the given
   * timestamp.
   */
  int getLutTextureId(long presentationTimeUs);

  /** Returns the length N of the 3D N x N x N LUT cube with the given timestamp. */
  int getLength(long presentationTimeUs);

  /** Releases the OpenGL texture of the LUT. */
  void release() throws GlUtil.GlException;

  /** This method must be executed on the same thread as other GL commands. */
  @Override
  @WorkerThread
  default SingleFrameGlTextureProcessor toGlTextureProcessor(Context context, boolean useHdr)
      throws FrameProcessingException {
    return new ColorLutProcessor(context, /* colorLut= */ this, useHdr);
  }
}

package com.frank.videoedit.effect.listener;

import android.content.Context;

import com.frank.videoedit.util.FrameProcessingException;

public interface GlEffect {

  GlTextureProcessor toGlTextureProcessor(Context context, boolean useHdr)
      throws FrameProcessingException;
}

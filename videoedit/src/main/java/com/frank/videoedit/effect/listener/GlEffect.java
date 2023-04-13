package com.frank.videoedit.effect.listener;

import android.content.Context;

import com.frank.videoedit.util.FrameProcessingException;
import com.google.android.exoplayer2.util.Effect;

public interface GlEffect extends Effect {

  GlTextureProcessor toGlTextureProcessor(Context context, boolean useHdr)
      throws FrameProcessingException;
}

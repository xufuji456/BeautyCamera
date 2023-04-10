package com.frank.videoedit.effect.listener;

import android.content.Context;

import com.google.android.exoplayer2.util.Effect;
import com.google.android.exoplayer2.util.FrameProcessingException;

public interface GlEffect extends Effect {

  GlTextureProcessor toGlTextureProcessor(Context context, boolean useHdr)
      throws FrameProcessingException;
}

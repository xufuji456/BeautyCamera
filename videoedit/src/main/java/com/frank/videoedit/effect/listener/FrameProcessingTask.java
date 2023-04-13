package com.frank.videoedit.effect.listener;

import com.frank.videoedit.util.FrameProcessingException;
import com.google.android.exoplayer2.util.GlUtil;

public interface FrameProcessingTask {
  /** Runs the task. */
  void run() throws FrameProcessingException, GlUtil.GlException;
}

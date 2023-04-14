package com.frank.videoedit.effect.listener;

import com.frank.videoedit.util.FrameProcessingException;
import com.frank.videoedit.effect.util.GlUtil;

public interface FrameProcessingTask {
  /** Runs the task. */
  void run() throws FrameProcessingException, GlUtil.GlException;
}

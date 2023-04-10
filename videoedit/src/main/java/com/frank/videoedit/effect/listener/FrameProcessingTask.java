package com.frank.videoedit.effect.listener;

import com.google.android.exoplayer2.util.FrameProcessingException;
import com.google.android.exoplayer2.util.GlUtil;

/**
 * Interface for tasks that may throw a {@link GlUtil.GlException} or {@link
 * FrameProcessingException}.
 */
public interface FrameProcessingTask {
  /** Runs the task. */
  void run() throws FrameProcessingException, GlUtil.GlException;
}
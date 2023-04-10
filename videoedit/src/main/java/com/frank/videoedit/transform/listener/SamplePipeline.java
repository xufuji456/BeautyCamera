package com.frank.videoedit.transform.listener;

import androidx.annotation.Nullable;

import com.frank.videoedit.transform.TransformationException;
import com.google.android.exoplayer2.decoder.DecoderInputBuffer;

/**
 * Pipeline for processing {@link DecoderInputBuffer DecoderInputBuffers}.
 *
 * <p>This pipeline can be used to implement transformations of audio or video samples.
 */
public interface SamplePipeline {

  /** Returns a buffer if the pipeline is ready to accept input, and {@code null} otherwise. */
  @Nullable
  DecoderInputBuffer dequeueInputBuffer() throws TransformationException;

  /**
   * Informs the pipeline that its input buffer contains new input.
   *
   * <p>Should be called after filling the input buffer from {@link #dequeueInputBuffer()} with new
   * input.
   */
  void queueInputBuffer() throws TransformationException;

  /**
   * Processes the input data and returns whether it may be possible to process more data by calling
   * this method again.
   */
  boolean processData() throws TransformationException;

  /** Returns whether the pipeline has ended. */
  boolean isEnded();

  /** Releases all resources held by the pipeline. */
  void release();
}

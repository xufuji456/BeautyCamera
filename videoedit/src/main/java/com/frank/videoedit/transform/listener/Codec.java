package com.frank.videoedit.transform.listener;

import android.media.MediaCodec.BufferInfo;
import android.view.Surface;

import com.frank.videoedit.transform.TransformationException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.decoder.DecoderInputBuffer;

import java.nio.ByteBuffer;
import java.util.List;

public interface Codec {

  int UNLIMITED_PENDING_FRAME_COUNT = Integer.MAX_VALUE;

  interface DecoderFactory {

    Codec createForAudioDecoding(Format format) throws TransformationException;

    Codec createForVideoDecoding(
        Format format, Surface outputSurface, boolean enableRequestSdrToneMapping)
        throws TransformationException;
  }

  interface EncoderFactory {

    Codec createForAudioEncoding(Format format, List<String> allowedMimeTypes)
        throws TransformationException;

    Codec createForVideoEncoding(Format format, List<String> allowedMimeTypes)
        throws TransformationException;

    default boolean audioNeedsEncoding() {
      return false;
    }

    default boolean videoNeedsEncoding() {
      return false;
    }
  }

  Format getConfigFormat();

  String getName();

  Surface getInputSurface();

  default int getMaxPendingFrameCount() {
    return UNLIMITED_PENDING_FRAME_COUNT;
  }

  boolean maybeDequeueInputBuffer(DecoderInputBuffer inputBuffer) throws TransformationException;

  void queueInputBuffer(DecoderInputBuffer inputBuffer) throws TransformationException;

  void signalEndOfInputStream() throws TransformationException;

  Format getOutputFormat() throws TransformationException;

  ByteBuffer getOutputBuffer() throws TransformationException;

  BufferInfo getOutputBufferInfo() throws TransformationException;

  void releaseOutputBuffer(boolean render) throws TransformationException;

  boolean isEnded();

  void release();
}

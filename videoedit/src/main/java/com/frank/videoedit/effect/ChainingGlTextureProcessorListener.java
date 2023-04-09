package com.frank.videoedit.effect;

import android.util.Pair;

import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.frank.videoedit.effect.GlTextureProcessor.InputListener;
import com.frank.videoedit.effect.GlTextureProcessor.OutputListener;

import java.util.ArrayDeque;
import java.util.Queue;

/* package */ final class ChainingGlTextureProcessorListener
    implements InputListener, OutputListener {

  private final GlTextureProcessor producingGlTextureProcessor;
  private final GlTextureProcessor consumingGlTextureProcessor;
  private final FrameProcessingTaskExecutor frameProcessingTaskExecutor;

  @GuardedBy("this")
  private final Queue<Pair<TextureInfo, Long>> availableFrames;

  @GuardedBy("this")
  private int consumingGlTextureProcessorInputCapacity;

  public ChainingGlTextureProcessorListener(
      GlTextureProcessor producingGlTextureProcessor,
      GlTextureProcessor consumingGlTextureProcessor,
      FrameProcessingTaskExecutor frameProcessingTaskExecutor) {
    this.producingGlTextureProcessor = producingGlTextureProcessor;
    this.consumingGlTextureProcessor = consumingGlTextureProcessor;
    this.frameProcessingTaskExecutor = frameProcessingTaskExecutor;
    availableFrames = new ArrayDeque<>();
  }

  @Override
  public synchronized void onReadyToAcceptInputFrame() {
    @Nullable Pair<TextureInfo, Long> pendingFrame = availableFrames.poll();
    if (pendingFrame == null) {
      consumingGlTextureProcessorInputCapacity++;
      return;
    }

    long presentationTimeUs = pendingFrame.second;
    if (presentationTimeUs == C.TIME_END_OF_SOURCE) {
      frameProcessingTaskExecutor.submit(
          consumingGlTextureProcessor::signalEndOfCurrentInputStream);
    } else {
      frameProcessingTaskExecutor.submit(
          () ->
              consumingGlTextureProcessor.queueInputFrame(
                  /* inputTexture= */ pendingFrame.first, presentationTimeUs));
    }
  }

  @Override
  public void onInputFrameProcessed(TextureInfo inputTexture) {
    frameProcessingTaskExecutor.submit(
        () -> producingGlTextureProcessor.releaseOutputFrame(inputTexture));
  }

  @Override
  public synchronized void onOutputFrameAvailable(
          TextureInfo outputTexture, long presentationTimeUs) {
    if (consumingGlTextureProcessorInputCapacity > 0) {
      frameProcessingTaskExecutor.submit(
          () ->
              consumingGlTextureProcessor.queueInputFrame(
                  /* inputTexture= */ outputTexture, presentationTimeUs));
      consumingGlTextureProcessorInputCapacity--;
    } else {
      availableFrames.add(new Pair<>(outputTexture, presentationTimeUs));
    }
  }

  @Override
  public synchronized void onCurrentOutputStreamEnded() {
    if (!availableFrames.isEmpty()) {
      availableFrames.add(new Pair<>(TextureInfo.UNSET, C.TIME_END_OF_SOURCE));
    } else {
      frameProcessingTaskExecutor.submit(
          consumingGlTextureProcessor::signalEndOfCurrentInputStream);
    }
  }
}

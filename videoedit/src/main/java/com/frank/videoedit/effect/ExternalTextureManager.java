package com.frank.videoedit.effect;

import static com.google.android.exoplayer2.util.Assertions.checkNotNull;

import android.graphics.SurfaceTexture;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.frank.videoedit.effect.listener.ExternalTextureProcessor;
import com.frank.videoedit.effect.listener.GlTextureProcessor.InputListener;
import com.frank.videoedit.effect.entity.FrameInfo;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.util.FrameProcessingException;
import com.google.android.exoplayer2.util.FrameProcessor;
import com.google.android.exoplayer2.util.GlUtil;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
/* package */ class ExternalTextureManager implements InputListener {

  private final FrameProcessingTaskExecutor frameProcessingTaskExecutor;
  private final ExternalTextureProcessor externalTextureProcessor;
  private final int externalTexId;
  private final SurfaceTexture surfaceTexture;
  private final float[] textureTransformMatrix;
  private final Queue<FrameInfo> pendingFrames;

  // Incremented on any thread when a frame becomes available on the surfaceTexture, decremented on
  // the GL thread only.
  private final AtomicInteger availableFrameCount;
  // Incremented on any thread, decremented on the GL thread only.
  private final AtomicInteger externalTextureProcessorInputCapacity;

  // Set to true on any thread. Read on the GL thread only.
  private volatile boolean inputStreamEnded;
  // The frame that is sent downstream and is not done processing yet.
  // Set to null on any thread. Read and set to non-null on the GL thread only.
  @Nullable private volatile FrameInfo currentFrame;

  private long previousStreamOffsetUs;

  public ExternalTextureManager(
      ExternalTextureProcessor externalTextureProcessor,
      FrameProcessingTaskExecutor frameProcessingTaskExecutor)
      throws FrameProcessingException {
    this.externalTextureProcessor = externalTextureProcessor;
    this.frameProcessingTaskExecutor = frameProcessingTaskExecutor;
    try {
      externalTexId = GlUtil.createExternalTexture();
    } catch (GlUtil.GlException e) {
      throw new FrameProcessingException(e);
    }
    surfaceTexture = new SurfaceTexture(externalTexId);
    textureTransformMatrix = new float[16];
    pendingFrames = new ConcurrentLinkedQueue<>();
    availableFrameCount = new AtomicInteger();
    externalTextureProcessorInputCapacity = new AtomicInteger();
    previousStreamOffsetUs = C.TIME_UNSET;
  }

  public SurfaceTexture getSurfaceTexture() {
    surfaceTexture.setOnFrameAvailableListener(
        unused -> {
          availableFrameCount.getAndIncrement();
          frameProcessingTaskExecutor.submit(this::maybeQueueFrameToExternalTextureProcessor);
        });
    return surfaceTexture;
  }

  @Override
  public void onReadyToAcceptInputFrame() {
    externalTextureProcessorInputCapacity.getAndIncrement();
    frameProcessingTaskExecutor.submit(this::maybeQueueFrameToExternalTextureProcessor);
  }

  @Override
  public void onInputFrameProcessed(TextureInfo inputTexture) {
    currentFrame = null;
    frameProcessingTaskExecutor.submit(this::maybeQueueFrameToExternalTextureProcessor);
  }

  /**
   * Notifies the {@code ExternalTextureManager} that a frame with the given {@link FrameInfo} will
   * become available via the {@link SurfaceTexture} eventually.
   *
   * <p>Can be called on any thread. The caller must ensure that frames are registered in the
   * correct order.
   */
  public void registerInputFrame(FrameInfo frame) {
    pendingFrames.add(frame);
  }

  /**
   * Returns the number of {@linkplain #registerInputFrame(FrameInfo) registered} frames that have
   * not been rendered to the external texture yet.
   *
   * <p>Can be called on any thread.
   */
  public int getPendingFrameCount() {
    return pendingFrames.size();
  }

  /**
   * Signals the end of the input.
   *
   * @see FrameProcessor#signalEndOfInput()
   */
  @WorkerThread
  public void signalEndOfInput() {
    inputStreamEnded = true;
    if (pendingFrames.isEmpty() && currentFrame == null) {
      externalTextureProcessor.signalEndOfCurrentInputStream();
    }
  }

  public void release() {
    surfaceTexture.release();
  }

  @WorkerThread
  private void maybeQueueFrameToExternalTextureProcessor() {
    if (externalTextureProcessorInputCapacity.get() == 0
        || availableFrameCount.get() == 0
        || currentFrame != null) {
      return;
    }

    availableFrameCount.getAndDecrement();
    surfaceTexture.updateTexImage();
    this.currentFrame = pendingFrames.remove();

    FrameInfo currentFrame = checkNotNull(this.currentFrame);
    externalTextureProcessorInputCapacity.getAndDecrement();
    surfaceTexture.getTransformMatrix(textureTransformMatrix);
    externalTextureProcessor.setTextureTransformMatrix(textureTransformMatrix);
    long frameTimeNs = surfaceTexture.getTimestamp();
    long streamOffsetUs = currentFrame.streamOffsetUs;
    if (streamOffsetUs != previousStreamOffsetUs) {
      if (previousStreamOffsetUs != C.TIME_UNSET) {
        externalTextureProcessor.signalEndOfCurrentInputStream();
      }
      previousStreamOffsetUs = streamOffsetUs;
    }
    // Correct for the stream offset so processors see original media presentation timestamps.
    long presentationTimeUs = (frameTimeNs / 1000) - streamOffsetUs;
    externalTextureProcessor.queueInputFrame(
        new TextureInfo(
            externalTexId, /* fboId= */ C.INDEX_UNSET, currentFrame.width, currentFrame.height),
        presentationTimeUs);

    if (inputStreamEnded && pendingFrames.isEmpty()) {
      externalTextureProcessor.signalEndOfCurrentInputStream();
    }
  }
}

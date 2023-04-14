package com.frank.videoedit.effect;

import static com.google.common.collect.Iterables.getLast;

import android.content.Context;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.frank.videoedit.effect.listener.ExternalTextureProcessor;
import com.frank.videoedit.effect.listener.GlEffect;
import com.frank.videoedit.effect.listener.GlMatrixTransformation;
import com.frank.videoedit.effect.listener.GlTextureProcessor;
import com.frank.videoedit.effect.util.GlUtil;
import com.frank.videoedit.listener.FrameProcessor;
import com.frank.videoedit.entity.SurfaceInfo;
import com.frank.videoedit.entity.FrameInfo;
import com.frank.videoedit.entity.ColorInfo;
import com.frank.videoedit.util.CommonUtil;
import com.frank.videoedit.util.FrameProcessingException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public final class GlEffectsFrameProcessor implements FrameProcessor {

  /** A factory for {@link GlEffectsFrameProcessor} instances. */
  public static class Factory implements FrameProcessor.Factory {

    @NonNull
    @Override
    public GlEffectsFrameProcessor create(
            @NonNull Context context,
            @NonNull Listener listener,
            @NonNull List<GlEffect> effects,
            @NonNull ColorInfo colorInfo,
            boolean releaseFramesAutomatically)
        throws FrameProcessingException {

      ExecutorService singleThreadExecutorService = CommonUtil.newSingleThreadExecutor(THREAD_NAME);

      Future<GlEffectsFrameProcessor> glFrameProcessorFuture =
          singleThreadExecutorService.submit(
              () ->
                  createOpenGlObjectsAndFrameProcessor(
                      context,
                      listener,
                      effects,
                      colorInfo,
                      releaseFramesAutomatically,
                      singleThreadExecutorService));

      try {
        return glFrameProcessorFuture.get();
      } catch (ExecutionException e) {
        throw new FrameProcessingException(e);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new FrameProcessingException(e);
      }
    }
  }

  @WorkerThread
  private static GlEffectsFrameProcessor createOpenGlObjectsAndFrameProcessor(
      Context context,
      Listener listener,
      List<GlEffect> effects,
      ColorInfo colorInfo,
      boolean releaseFramesAutomatically,
      ExecutorService singleThreadExecutorService)
      throws GlUtil.GlException, FrameProcessingException {

    // TODO(b/237674316): Delay initialization of things requiring the colorInfo, to
    //  configure based on the color info from the decoder output media format instead.
    boolean useHdr = ColorInfo.isTransferHdr(colorInfo);
    EGLDisplay eglDisplay = GlUtil.createEglDisplay();
    int[] configAttributes =
        useHdr ? GlUtil.EGL_CONFIG_ATTRIBUTES_RGBA_1010102 : GlUtil.EGL_CONFIG_ATTRIBUTES_RGBA_8888;
    EGLContext eglContext = GlUtil.createEglContext(eglDisplay, configAttributes);
    GlUtil.createFocusedPlaceholderEglSurface(eglContext, eglDisplay, configAttributes);

    List<GlTextureProcessor> textureProcessors =
        getGlTextureProcessorsForGlEffects(
            context,
            effects,
            eglDisplay,
            eglContext,
            listener,
            colorInfo,
            releaseFramesAutomatically);
    FrameProcessingTaskExecutor frameProcessingTaskExecutor =
        new FrameProcessingTaskExecutor(singleThreadExecutorService, listener);
    chainTextureProcessorsWithListeners(textureProcessors, frameProcessingTaskExecutor, listener);

    return new GlEffectsFrameProcessor(
        eglDisplay,
        eglContext,
        frameProcessingTaskExecutor,
        textureProcessors,
        releaseFramesAutomatically);
  }

  private static List<GlTextureProcessor> getGlTextureProcessorsForGlEffects(
      Context context,
      List<GlEffect> effects,
      EGLDisplay eglDisplay,
      EGLContext eglContext,
      Listener listener,
      ColorInfo colorInfo,
      boolean releaseFramesAutomatically)
      throws FrameProcessingException {
    List<GlTextureProcessor> textureProcessorListBuilder =
        new ArrayList<>();
    List<GlMatrixTransformation> matrixTransformationListBuilder =
        new ArrayList<>();
    boolean sampleFromExternalTexture = true;
    for (int i = 0; i < effects.size(); i++) {
      GlEffect glEffect = (GlEffect) effects.get(i);
      // The following logic may change the order of the RgbMatrix and GlMatrixTransformation
      // effects. This does not influence the output since RgbMatrix only changes the individual
      // pixels and does not take any location in account, which the GlMatrixTransformation
      // may change.
      if (glEffect instanceof GlMatrixTransformation) {
        matrixTransformationListBuilder.add((GlMatrixTransformation) glEffect);
        continue;
      }
      List<GlMatrixTransformation> matrixTransformations =
          matrixTransformationListBuilder;
      if (!matrixTransformations.isEmpty() || sampleFromExternalTexture) {
        MatrixTextureProcessor matrixTextureProcessor;
        if (sampleFromExternalTexture) {
          matrixTextureProcessor =
              MatrixTextureProcessor.createWithExternalSamplerApplyingEotf(
                  context, matrixTransformations, colorInfo);
        } else {
          matrixTextureProcessor =
              MatrixTextureProcessor.create(
                  context, matrixTransformations, ColorInfo.isTransferHdr(colorInfo));
        }
        textureProcessorListBuilder.add(matrixTextureProcessor);
        matrixTransformationListBuilder = new ArrayList<>();
        sampleFromExternalTexture = false;
      }
      textureProcessorListBuilder.add(
          glEffect.toGlTextureProcessor(context, ColorInfo.isTransferHdr(colorInfo)));
    }

    textureProcessorListBuilder.add(
        new MatrixTextureProcessorWrapper(
            context,
            eglDisplay,
            eglContext,
            matrixTransformationListBuilder,
            listener,
            sampleFromExternalTexture,
            colorInfo,
            releaseFramesAutomatically));
    return textureProcessorListBuilder;
  }

  private static void chainTextureProcessorsWithListeners(
      List<GlTextureProcessor> textureProcessors,
      FrameProcessingTaskExecutor frameProcessingTaskExecutor,
      Listener frameProcessorListener) {
    for (int i = 0; i < textureProcessors.size() - 1; i++) {
      GlTextureProcessor producingGlTextureProcessor = textureProcessors.get(i);
      GlTextureProcessor consumingGlTextureProcessor = textureProcessors.get(i + 1);
      ChainTextureProcessorListener chainingGlTextureProcessorListener =
          new ChainTextureProcessorListener(
              producingGlTextureProcessor,
              consumingGlTextureProcessor,
              frameProcessingTaskExecutor);
      producingGlTextureProcessor.setOutputListener(chainingGlTextureProcessorListener);
      producingGlTextureProcessor.setErrorListener(frameProcessorListener::onFrameProcessingError);
      consumingGlTextureProcessor.setInputListener(chainingGlTextureProcessorListener);
    }
  }

  private static final String THREAD_NAME = "Effect:GlThread";
  private static final long RELEASE_WAIT_TIME_MS = 100;

  private final EGLDisplay eglDisplay;
  private final EGLContext eglContext;
  private final FrameProcessingTaskExecutor frameProcessingTaskExecutor;
  private final ExternalTextureManager inputExternalTextureManager;
  private final Surface inputSurface;
  private final boolean releaseFramesAutomatically;
  private final MatrixTextureProcessorWrapper finalTextureProcessorWrapper;
  private final List<GlTextureProcessor> allTextureProcessors;

  private FrameInfo nextInputFrameInfo;
  private boolean inputStreamEnded;
  /**
   * Offset compared to original media presentation time that has been added to incoming frame
   * timestamps, in microseconds.
   */
  private long previousStreamOffsetUs;

  private GlEffectsFrameProcessor(
      EGLDisplay eglDisplay,
      EGLContext eglContext,
      FrameProcessingTaskExecutor frameProcessingTaskExecutor,
      List<GlTextureProcessor> textureProcessors,
      boolean releaseFramesAutomatically)
      throws FrameProcessingException {

    this.eglDisplay = eglDisplay;
    this.eglContext = eglContext;
    this.frameProcessingTaskExecutor = frameProcessingTaskExecutor;
    this.releaseFramesAutomatically  = releaseFramesAutomatically;

    ExternalTextureProcessor inputExternalTextureProcessor =
        (ExternalTextureProcessor) textureProcessors.get(0);
    inputExternalTextureManager =
        new ExternalTextureManager(inputExternalTextureProcessor, frameProcessingTaskExecutor);
    inputExternalTextureProcessor.setInputListener(inputExternalTextureManager);
    inputSurface = new Surface(inputExternalTextureManager.getSurfaceTexture());
    finalTextureProcessorWrapper = (MatrixTextureProcessorWrapper) getLast(textureProcessors);
    allTextureProcessors = textureProcessors;
    previousStreamOffsetUs = CommonUtil.TIME_UNSET;
  }

  @NonNull
  @Override
  public Surface getInputSurface() {
    return inputSurface;
  }

  @Override
  public void setInputFrameInfo(@NonNull FrameInfo inputFrameInfo) {
    nextInputFrameInfo = adjustForPixelWidthHeightRatio(inputFrameInfo);

    if (nextInputFrameInfo.streamOffsetUs != previousStreamOffsetUs) {
      finalTextureProcessorWrapper.appendStream(nextInputFrameInfo.streamOffsetUs);
      previousStreamOffsetUs = nextInputFrameInfo.streamOffsetUs;
    }
  }

  @Override
  public void registerInputFrame() {
    inputExternalTextureManager.registerInputFrame(nextInputFrameInfo);
  }

  @Override
  public int getPendingInputFrameCount() {
    return inputExternalTextureManager.getPendingFrameCount();
  }

  @Override
  public void setOutputSurfaceInfo(@Nullable SurfaceInfo outputSurfaceInfo) {
    finalTextureProcessorWrapper.setOutputSurfaceInfo(outputSurfaceInfo);
  }

  @Override
  public void releaseOutputFrame(long releaseTimeNs) {
    frameProcessingTaskExecutor.submitWithHighPriority(
        () -> finalTextureProcessorWrapper.releaseOutputFrame(releaseTimeNs));
  }

  @Override
  public void signalEndOfInput() {
    inputStreamEnded = true;
    frameProcessingTaskExecutor.submit(inputExternalTextureManager::signalEndOfInput);
  }

  @Override
  public void release() {
    try {
      frameProcessingTaskExecutor.release(
          /* releaseTask= */ this::releaseTextureProcessorsAndDestroyGlContext,
          RELEASE_WAIT_TIME_MS);
    } catch (InterruptedException unexpected) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(unexpected);
    }
    inputExternalTextureManager.release();
    inputSurface.release();
  }

  /**
   * Expands or shrinks the frame based on the {@link FrameInfo#pixelWidthHeightRatio} and returns a
   * new {@link FrameInfo} instance with scaled dimensions and {@link
   * FrameInfo#pixelWidthHeightRatio} of {@code 1}.
   */
  private FrameInfo adjustForPixelWidthHeightRatio(FrameInfo frameInfo) {
    if (frameInfo.pixelWidthHeightRatio > 1f) {
      return new FrameInfo(
          (int) (frameInfo.width * frameInfo.pixelWidthHeightRatio),
          frameInfo.height,
          /* pixelWidthHeightRatio= */ 1,
          frameInfo.streamOffsetUs);
    } else if (frameInfo.pixelWidthHeightRatio < 1f) {
      return new FrameInfo(
          frameInfo.width,
          (int) (frameInfo.height / frameInfo.pixelWidthHeightRatio),
          /* pixelWidthHeightRatio= */ 1,
          frameInfo.streamOffsetUs);
    } else {
      return frameInfo;
    }
  }

  @WorkerThread
  private void releaseTextureProcessorsAndDestroyGlContext()
      throws GlUtil.GlException, FrameProcessingException {
    for (int i = 0; i < allTextureProcessors.size(); i++) {
      allTextureProcessors.get(i).release();
    }
    GlUtil.destroyEglContext(eglDisplay, eglContext);
  }
}

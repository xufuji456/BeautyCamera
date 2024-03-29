package com.frank.videoedit.effect;

import android.content.Context;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.util.Pair;

import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.frank.videoedit.effect.util.MatrixUtil;
import com.frank.videoedit.effect.util.GlUtil;

import com.frank.videoedit.entity.SurfaceInfo;
import com.frank.videoedit.effect.listener.ExternalTextureProcessor;
import com.frank.videoedit.effect.listener.GlMatrixTransformation;
import com.frank.videoedit.listener.FrameProcessor;
import com.frank.videoedit.entity.ColorInfo;
import com.frank.videoedit.util.CommonUtil;
import com.frank.videoedit.util.FrameProcessingException;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/* package */ final class MatrixTextureProcessorWrapper implements ExternalTextureProcessor {

  private final Context context;
  private final List<GlMatrixTransformation> matrixTransformations;
  private final EGLDisplay eglDisplay;
  private final EGLContext eglContext;
  private final FrameProcessor.Listener frameProcessorListener;
  private final boolean sampleFromExternalTexture;
  private final ColorInfo colorInfo;
  private final boolean releaseFramesAutomatically;
  private final float[] textureTransformMatrix;
  private final Queue<Long> streamOffsetUsQueue;
  private final Queue<Pair<TextureInfo, Long>> availableFrames;

  private int inputWidth;
  private int inputHeight;
  @Nullable private MatrixTextureProcessor matrixTextureProcessor;
  private InputListener inputListener;
  private Pair<Integer, Integer> outputSizeBeforeSurfaceTransform;

  private volatile boolean outputSizeOrRotationChanged;

  @GuardedBy("this")
  @Nullable
  private SurfaceInfo outputSurfaceInfo;

  @GuardedBy("this")
  @Nullable
  private EGLSurface outputEglSurface;

  public MatrixTextureProcessorWrapper(
      Context context,
      EGLDisplay eglDisplay,
      EGLContext eglContext,
      List<GlMatrixTransformation> matrixTransformations,
      FrameProcessor.Listener frameProcessorListener,
      boolean sampleFromExternalTexture,
      ColorInfo colorInfo,
      boolean releaseFramesAutomatically) {
    this.context = context;
    this.matrixTransformations = matrixTransformations;
    this.eglDisplay = eglDisplay;
    this.eglContext = eglContext;
    this.frameProcessorListener = frameProcessorListener;
    this.sampleFromExternalTexture = sampleFromExternalTexture;
    this.colorInfo = colorInfo;
    this.releaseFramesAutomatically = releaseFramesAutomatically;

    textureTransformMatrix = GlUtil.create4x4IdentityMatrix();
    streamOffsetUsQueue = new ConcurrentLinkedQueue<>();
    inputListener = new InputListener() {};
    availableFrames = new ConcurrentLinkedQueue<>();
  }

  @Override
  public void setInputListener(InputListener inputListener) {
    this.inputListener = inputListener;
    inputListener.onReadyToAcceptInputFrame();
  }

  @Override
  public void setOutputListener(OutputListener outputListener) {
    // The FrameProcessor.Listener passed to the constructor is used for output-related events.
    throw new UnsupportedOperationException();
  }

  @Override
  public void setErrorListener(ErrorListener errorListener) {
    // The FrameProcessor.Listener passed to the constructor is used for errors.
    throw new UnsupportedOperationException();
  }

  public static <T> T checkNotNull(@Nullable T reference, Object errorMessage) {
    if (reference == null) {
      throw new IllegalStateException(String.valueOf(errorMessage));
    }
    return reference;
  }

  @Override
  public void queueInputFrame(TextureInfo inputTexture, long presentationTimeUs) {
    long streamOffsetUs =
        checkNotNull(streamOffsetUsQueue.peek(), "No input stream specified.");
    long offsetPresentationTimeUs = presentationTimeUs + streamOffsetUs;
    frameProcessorListener.onOutputFrameAvailable(offsetPresentationTimeUs);
    if (releaseFramesAutomatically) {
      renderFrameToSurfaces(
          inputTexture, presentationTimeUs, /* releaseTimeNs= */ offsetPresentationTimeUs * 1000);
    } else {
      availableFrames.add(Pair.create(inputTexture, presentationTimeUs));
    }
    inputListener.onReadyToAcceptInputFrame();
  }

  @Override
  public void releaseOutputFrame(TextureInfo outputTexture) {
    // The final texture processor writes to a surface so there is no texture to release.
    throw new UnsupportedOperationException();
  }

  @WorkerThread
  public void releaseOutputFrame(long releaseTimeNs) {
    Pair<TextureInfo, Long> oldestAvailableFrame = availableFrames.remove();
    renderFrameToSurfaces(
        /* inputTexture= */ oldestAvailableFrame.first,
        /* presentationTimeUs= */ oldestAvailableFrame.second,
        releaseTimeNs);
  }

  @Override
  public void signalEndOfCurrentInputStream() {
    streamOffsetUsQueue.remove();
    if (streamOffsetUsQueue.isEmpty()) {
      frameProcessorListener.onFrameProcessingEnded();
    }
  }

  @Override
  @WorkerThread
  public void release() throws FrameProcessingException {
    if (matrixTextureProcessor != null) {
      matrixTextureProcessor.release();
    }
  }

  @Override
  public void setTextureTransformMatrix(float[] textureTransformMatrix) {
    System.arraycopy(
        /* src= */ textureTransformMatrix,
        /* srcPos= */ 0,
        /* dest= */ this.textureTransformMatrix,
        /* destPost= */ 0,
        /* length= */ textureTransformMatrix.length);

    if (matrixTextureProcessor != null) {
      matrixTextureProcessor.setTextureTransformMatrix(textureTransformMatrix);
    }
  }

  /**
   * Signals that there will be another input stream after all previously appended input streams
   * have {@linkplain #signalEndOfCurrentInputStream() ended}.
   *
   * <p>This method does not need to be called on the GL thread, but the caller must ensure that
   * stream offsets are appended in the correct order.
   *
   * @param streamOffsetUs The presentation timestamp offset, in microseconds.
   */
  public void appendStream(long streamOffsetUs) {
    streamOffsetUsQueue.add(streamOffsetUs);
  }

  /**
   * Sets the output {@link SurfaceInfo}.
   *
   * @see FrameProcessor#setOutputSurfaceInfo(SurfaceInfo)
   */
  public synchronized void setOutputSurfaceInfo(@Nullable SurfaceInfo outputSurfaceInfo) {
    if (!CommonUtil.areEqual(this.outputSurfaceInfo, outputSurfaceInfo)) {
      if (outputSurfaceInfo != null
          && this.outputSurfaceInfo != null
          && !this.outputSurfaceInfo.surface.equals(outputSurfaceInfo.surface)) {
        this.outputEglSurface = null;
      }
      outputSizeOrRotationChanged =
          this.outputSurfaceInfo == null
              || outputSurfaceInfo == null
              || this.outputSurfaceInfo.width != outputSurfaceInfo.width
              || this.outputSurfaceInfo.height != outputSurfaceInfo.height
              || this.outputSurfaceInfo.orientationDegrees != outputSurfaceInfo.orientationDegrees;
      this.outputSurfaceInfo = outputSurfaceInfo;
    }
  }

  private void renderFrameToSurfaces(
          TextureInfo inputTexture, long presentationTimeUs, long releaseTimeNs) {
    try {
      maybeRenderFrameToOutputSurface(inputTexture, presentationTimeUs, releaseTimeNs);
    } catch (FrameProcessingException | GlUtil.GlException e) {
      frameProcessorListener.onFrameProcessingError(
          FrameProcessingException.from(e, presentationTimeUs));
    }
    inputListener.onInputFrameProcessed(inputTexture);
  }

  private synchronized void maybeRenderFrameToOutputSurface(
          TextureInfo inputTexture, long presentationTimeUs, long releaseTimeNs)
      throws FrameProcessingException, GlUtil.GlException {
    if (releaseTimeNs == FrameProcessor.DROP_OUTPUT_FRAME
        || !ensureConfigured(inputTexture.width, inputTexture.height)) {
      return; // Drop frames when requested, or there is no output surface.
    }

    EGLSurface outputEglSurface = this.outputEglSurface;
    SurfaceInfo outputSurfaceInfo = this.outputSurfaceInfo;
    MatrixTextureProcessor matrixTextureProcessor = this.matrixTextureProcessor;

    GlUtil.focusEglSurface(
        eglDisplay,
        eglContext,
        outputEglSurface,
        outputSurfaceInfo.width,
        outputSurfaceInfo.height);
    GlUtil.clearOutputFrame();
    matrixTextureProcessor.drawFrame(inputTexture.texId, presentationTimeUs);

    EGLExt.eglPresentationTimeANDROID(
        eglDisplay,
        outputEglSurface,
        releaseTimeNs == FrameProcessor.RELEASE_OUTPUT_FRAME_IMMEDIATELY
            ? System.nanoTime()
            : releaseTimeNs);
    EGL14.eglSwapBuffers(eglDisplay, outputEglSurface);
  }

  private synchronized boolean ensureConfigured(int inputWidth, int inputHeight)
      throws FrameProcessingException, GlUtil.GlException {

    if (this.inputWidth != inputWidth
        || this.inputHeight != inputHeight
        || this.outputSizeBeforeSurfaceTransform == null) {
      this.inputWidth = inputWidth;
      this.inputHeight = inputHeight;
      Pair<Integer, Integer> outputSizeBeforeSurfaceTransform =
          MatrixUtil.configureAndGetOutputSize(inputWidth, inputHeight, matrixTransformations);
      if (!CommonUtil.areEqual(
          this.outputSizeBeforeSurfaceTransform, outputSizeBeforeSurfaceTransform)) {
        this.outputSizeBeforeSurfaceTransform = outputSizeBeforeSurfaceTransform;
        frameProcessorListener.onOutputSizeChanged(
            outputSizeBeforeSurfaceTransform.first,
            outputSizeBeforeSurfaceTransform.second);
      }
    }

    if (outputSurfaceInfo == null) {
      if (matrixTextureProcessor != null) {
        matrixTextureProcessor.release();
        matrixTextureProcessor = null;
      }
      outputEglSurface = null;
      return false;
    }

    SurfaceInfo outputSurfaceInfo = this.outputSurfaceInfo;
    @Nullable EGLSurface outputEglSurface = this.outputEglSurface;
    if (outputEglSurface == null) {
      boolean colorInfoIsHdr = ColorInfo.isTransferHdr(colorInfo);

      outputEglSurface =
          GlUtil.getEglSurface(
              eglDisplay,
              outputSurfaceInfo.surface,
              colorInfoIsHdr
                  ? GlUtil.EGL_CONFIG_ATTRIBUTES_RGBA_1010102
                  : GlUtil.EGL_CONFIG_ATTRIBUTES_RGBA_8888);
    }

    if (matrixTextureProcessor != null && outputSizeOrRotationChanged) {
      matrixTextureProcessor.release();
      matrixTextureProcessor = null;
      outputSizeOrRotationChanged = false;
    }
    if (matrixTextureProcessor == null) {
      matrixTextureProcessor = createMatrixTextureProcessorForOutputSurface(outputSurfaceInfo);
    }

    this.outputSurfaceInfo = outputSurfaceInfo;
    this.outputEglSurface = outputEglSurface;
    return true;
  }

  private MatrixTextureProcessor createMatrixTextureProcessorForOutputSurface(
      SurfaceInfo outputSurfaceInfo) throws FrameProcessingException {
    List<GlMatrixTransformation> matrixTransformationListBuilder =
        new ArrayList<>();
    matrixTransformationListBuilder.addAll(matrixTransformations);

    matrixTransformationListBuilder.add(
        Presentation.createForWidthAndHeight(
            outputSurfaceInfo.width, outputSurfaceInfo.height, Presentation.LAYOUT_SCALE_TO_FIT));

    MatrixTextureProcessor matrixTextureProcessor;
    List<GlMatrixTransformation> expandedMatrixTransformations =
        matrixTransformationListBuilder;
    if (sampleFromExternalTexture) {
      matrixTextureProcessor =
          MatrixTextureProcessor.createWithExternalSamplerApplyingEotfThenOetf(
              context, expandedMatrixTransformations, colorInfo);
    } else {
      matrixTextureProcessor =
          MatrixTextureProcessor.createApplyingOetf(
              context, expandedMatrixTransformations, colorInfo);
    }

    matrixTextureProcessor.setTextureTransformMatrix(textureTransformMatrix);

    return matrixTextureProcessor;
  }

}

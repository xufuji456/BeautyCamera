package com.frank.videoedit.transform;

import static java.lang.annotation.ElementType.TYPE_USE;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;

import com.frank.videoedit.listener.FrameProcessor;
import com.frank.videoedit.transform.entity.ProgressHolder;
import com.frank.videoedit.transform.listener.Codec;
import com.frank.videoedit.transform.listener.Muxer;
import com.frank.videoedit.transform.util.MediaUtil;
import com.frank.videoedit.util.CommonUtil;
import com.frank.videoedit.effect.GlEffectsFrameProcessor;
import com.frank.videoedit.transform.entity.MediaItem;
import com.frank.videoedit.effect.listener.GlEffect;

import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.util.Clock;
import com.google.android.exoplayer2.util.ListenerSet;

import java.io.File;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

/**
 * A transformer to transform media inputs.
 *
 * <p>The same Transformer instance can be used to transform multiple inputs (sequentially, not
 * concurrently).
 *
 * <p>Transformer instances must be accessed from a single application thread.
 */
public final class Transformer {

  /** A builder for {@link Transformer} instances. */
  public static final class Builder {

    // Mandatory field.
    private final Context context;

    // Optional fields.
    private TransformationRequest transformationRequest;
    private List<GlEffect> videoEffects;
    private final ListenerSet<Listener> listeners;
    private MediaSource.Factory mediaSourceFactory;
    private final Codec.DecoderFactory decoderFactory;
    private Codec.EncoderFactory encoderFactory;
    private final FrameProcessor.Factory frameProcessorFactory;
    private final Muxer.Factory muxerFactory;
    private final Looper looper;
    private final Clock clock;

    /**
     * Creates a builder with default values.
     *
     * @param context The {@link Context}.
     */
    public Builder(Context context) {
      this.context = context.getApplicationContext();
      transformationRequest = new TransformationRequest.Builder().build();
      videoEffects = new ArrayList<>();
      decoderFactory = new DefaultDecoderFactory(this.context);
      encoderFactory = new DefaultEncoderFactory.Builder(this.context).build();
      frameProcessorFactory = new GlEffectsFrameProcessor.Factory();
      muxerFactory = new DefaultMuxer.Factory();
      looper = CommonUtil.getCurrentOrMainLooper();
      clock = Clock.DEFAULT;
      listeners = new ListenerSet<>(looper, clock, (listener, flags) -> {});
    }

    /** Creates a builder with the values of the provided {@link Transformer}. */
    private Builder(Transformer transformer) {
      this.context = transformer.context;
      this.transformationRequest = transformer.transformationRequest;
      this.videoEffects = transformer.videoEffects;
      this.listeners = transformer.listeners;
      this.mediaSourceFactory = transformer.mediaSourceFactory;
      this.decoderFactory = transformer.decoderFactory;
      this.encoderFactory = transformer.encoderFactory;
      this.frameProcessorFactory = transformer.frameProcessorFactory;
      this.muxerFactory = transformer.muxerFactory;
      this.looper = transformer.looper;
      this.clock = transformer.clock;
    }

    public Builder setTransformationRequest(TransformationRequest transformationRequest) {
      this.transformationRequest = transformationRequest;
      return this;
    }

    public Builder setVideoEffects(List<GlEffect> effects) {
      this.videoEffects = List.copyOf(effects);
      return this;
    }

    @Deprecated
    public Builder setListener(Listener listener) {
      this.listeners.clear();
      this.listeners.add(listener);
      return this;
    }

    public Builder addListener(Listener listener) {
      this.listeners.add(listener);
      return this;
    }

    public Builder setEncoderFactory(Codec.EncoderFactory encoderFactory) {
      this.encoderFactory = encoderFactory;
      return this;
    }

    public Transformer build() {
      if (mediaSourceFactory == null) {
        DefaultExtractorsFactory defaultExtractorsFactory = new DefaultExtractorsFactory();
        mediaSourceFactory = new DefaultMediaSourceFactory(context, defaultExtractorsFactory);
      }
      return new Transformer(
          context,
          transformationRequest,
          videoEffects,
          listeners,
          mediaSourceFactory,
          decoderFactory,
          encoderFactory,
          frameProcessorFactory,
          muxerFactory,
          looper,
          clock);
    }

  }

  /** A listener for the transformation events. */
  public interface Listener {

    default void onTransformationCompleted(MediaItem inputMediaItem) {}

    default void onTransformationCompleted(
        MediaItem inputMediaItem, TransformationResult transformationResult) {
      onTransformationCompleted(inputMediaItem);
    }

    /**
     * @deprecated Use {@link #onTransformationError(MediaItem, TransformationException)}.
     */
    @Deprecated
    default void onTransformationError(MediaItem inputMediaItem, Exception exception) {
      onTransformationError(inputMediaItem, (TransformationException) exception);
    }

    /**
     * Called if an exception occurs during the transformation.
     *
     * @param inputMediaItem The {@link MediaItem} for which the exception occurs.
     * @param exception The {@link TransformationException} describing the exception.
     */
    default void onTransformationError(
        MediaItem inputMediaItem, TransformationException exception) {}

    default void onFallbackApplied(
        MediaItem inputMediaItem,
        TransformationRequest originalTransformationRequest,
        TransformationRequest fallbackTransformationRequest) {}
  }

  /**
   * Progress state. One of {@link #PROGRESS_STATE_WAITING_FOR_AVAILABILITY}, {@link
   * #PROGRESS_STATE_AVAILABLE}, {@link #PROGRESS_STATE_UNAVAILABLE}, {@link
   * #PROGRESS_STATE_NO_TRANSFORMATION}
   */
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target(TYPE_USE)
  @IntDef({
    PROGRESS_STATE_WAITING_FOR_AVAILABILITY,
    PROGRESS_STATE_AVAILABLE,
    PROGRESS_STATE_UNAVAILABLE,
    PROGRESS_STATE_NO_TRANSFORMATION
  })
  public @interface ProgressState {}

  /**
   * Indicates that the progress is unavailable for the current transformation, but might become
   * available.
   */
  public static final int PROGRESS_STATE_WAITING_FOR_AVAILABILITY = 0;
  /** Indicates that the progress is available. */
  public static final int PROGRESS_STATE_AVAILABLE = 1;
  /** Indicates that the progress is permanently unavailable for the current transformation. */
  public static final int PROGRESS_STATE_UNAVAILABLE = 2;
  /** Indicates that there is no current transformation. */
  public static final int PROGRESS_STATE_NO_TRANSFORMATION = 4;

  @VisibleForTesting final Codec.DecoderFactory decoderFactory;
  @VisibleForTesting final Codec.EncoderFactory encoderFactory;

  private final Context context;
  private final TransformationRequest transformationRequest;
  private final List<GlEffect> videoEffects;
  private final ListenerSet<Listener> listeners;
  private final MediaSource.Factory mediaSourceFactory;
  private final FrameProcessor.Factory frameProcessorFactory;
  private final Muxer.Factory muxerFactory;
  private final Looper looper;
  private final Clock clock;
  private final ExoPlayerAssetLoader exoPlayerAssetLoader;

  @Nullable private MuxerWrapper muxerWrapper;
  @Nullable private String outputPath;
  @Nullable private ParcelFileDescriptor outputParcelFileDescriptor;
  private boolean transformationInProgress;
  private boolean isCancelling;

  private Transformer(
      Context context,
      TransformationRequest transformationRequest,
      List<GlEffect> videoEffects,
      ListenerSet<Listener> listeners,
      MediaSource.Factory mediaSourceFactory,
      Codec.DecoderFactory decoderFactory,
      Codec.EncoderFactory encoderFactory,
      FrameProcessor.Factory frameProcessorFactory,
      Muxer.Factory muxerFactory,
      Looper looper,
      Clock clock) {
    this.context = context;
    this.transformationRequest = transformationRequest;
    this.videoEffects = videoEffects;
    this.listeners = listeners;
    this.mediaSourceFactory = mediaSourceFactory;
    this.decoderFactory = decoderFactory;
    this.encoderFactory = encoderFactory;
    this.frameProcessorFactory = frameProcessorFactory;
    this.muxerFactory = muxerFactory;
    this.looper = looper;
    this.clock = clock;
    exoPlayerAssetLoader =
        new ExoPlayerAssetLoader(
            context,
            transformationRequest,
            videoEffects,
            mediaSourceFactory,
            decoderFactory,
            encoderFactory,
            frameProcessorFactory,
            looper,
            clock);
  }

  public Builder buildUpon() {
    return new Builder(this);
  }

  public void setListener(Listener listener) {
    verifyApplicationThread();
    this.listeners.clear();
    this.listeners.add(listener);
  }

  public void startTransformation(MediaItem mediaItem, String path) {
    this.outputPath = path;
    this.outputParcelFileDescriptor = null;
    startTransformationInternal(mediaItem);
  }

  @RequiresApi(26)
  public void startTransformation(MediaItem mediaItem, ParcelFileDescriptor parcelFileDescriptor) {
    this.outputParcelFileDescriptor = parcelFileDescriptor;
    this.outputPath = null;
    startTransformationInternal(mediaItem);
  }

  private void startTransformationInternal(MediaItem mediaItem) {
    verifyApplicationThread();
    if (transformationInProgress) {
      throw new IllegalStateException("There is already a transformation in progress.");
    }
    transformationInProgress = true;
    ComponentListener componentListener = new ComponentListener(mediaItem, looper);
    MuxerWrapper muxerWrapper =
        new MuxerWrapper(
            outputPath,
            outputParcelFileDescriptor,
            muxerFactory,
            /* asyncErrorListener= */ componentListener);
    this.muxerWrapper = muxerWrapper;
    FallbackListener fallbackListener =
        new FallbackListener(
            mediaItem,
            listeners,
            clock.createHandler(looper, /* callback= */ null),
            transformationRequest);
    exoPlayerAssetLoader.start(
        mediaItem,
        muxerWrapper,
        /* listener= */ componentListener,
        fallbackListener,
        /* asyncErrorListener= */ componentListener);
  }

  public @ProgressState int getProgress(ProgressHolder progressHolder) {
    verifyApplicationThread();
    return exoPlayerAssetLoader.getProgress(progressHolder);
  }

  /**
   * Cancels the transformation that is currently in progress, if any.
   *
   * @throws IllegalStateException If this method is called from the wrong thread.
   */
  public void cancel() {
    verifyApplicationThread();
    isCancelling = true;
    try {
      releaseResources(/* forCancellation= */ true);
    } catch (TransformationException impossible) {
      throw new IllegalStateException(impossible);
    }
    isCancelling = false;
  }

  /**
   * Releases the resources.
   *
   * @param forCancellation Whether the reason for releasing the resources is the transformation
   *     cancellation.
   * @throws IllegalStateException If this method is called from the wrong thread.
   * @throws TransformationException If the muxer is in the wrong state and {@code forCancellation}
   *     is false.
   */
  private void releaseResources(boolean forCancellation) throws TransformationException {
    transformationInProgress = false;
    exoPlayerAssetLoader.release();
    if (muxerWrapper != null) {
      try {
        muxerWrapper.release(forCancellation);
      } catch (Muxer.MuxerException e) {
        throw TransformationException.createForMuxer(
            e, TransformationException.ERROR_CODE_MUXING_FAILED);
      }
      muxerWrapper = null;
    }
  }

  private void verifyApplicationThread() {
    if (Looper.myLooper() != looper) {
      throw new IllegalStateException("Transformer is accessed on the wrong thread.");
    }
  }

  private long getCurrentOutputFileCurrentSizeBytes() {
    long fileSize = CommonUtil.LENGTH_UNSET;

    if (outputPath != null) {
      fileSize = new File(outputPath).length();
    } else if (outputParcelFileDescriptor != null) {
      fileSize = outputParcelFileDescriptor.getStatSize();
    }

    if (fileSize <= 0) {
      fileSize = CommonUtil.LENGTH_UNSET;
    }

    return fileSize;
  }

  /** Listener for exceptions that occur during a transformation. */
  /* package */ interface AsyncErrorListener {
    /**
     * Called when a {@link TransformationException} occurs.
     *
     * <p>Can be called from any thread.
     */
    void onTransformationException(TransformationException exception);
  }

  private final class ComponentListener
      implements ExoPlayerAssetLoader.Listener, AsyncErrorListener {

    private final MediaItem mediaItem;
    private final Handler handler;

    public ComponentListener(MediaItem mediaItem, Looper looper) {
      this.mediaItem = mediaItem;
      handler = new Handler(looper);
    }

    @Override
    public void onError(Exception e) {
      TransformationException transformationException =
          TransformationException.createForUnexpected(e);
      handleTransformationException(transformationException);
    }

    @Override
    public void onEnded() {
      handleTransformationEnded(/* exception= */ null);
    }

    @Override
    public void onTransformationException(TransformationException exception) {
      if (Looper.myLooper() == looper) {
        handleTransformationException(exception);
      } else {
        handler.post(() -> handleTransformationException(exception));
      }
    }

    private void handleTransformationException(TransformationException transformationException) {
      if (isCancelling) {
        // Resources are already being released.
        listeners.queueEvent(
            /* eventFlag= */ CommonUtil.INDEX_UNSET,
            listener -> listener.onTransformationError(mediaItem, transformationException));
        listeners.flushEvents();
      } else {
        handleTransformationEnded(transformationException);
      }
    }

    private void handleTransformationEnded(@Nullable TransformationException exception) {
      MuxerWrapper muxerWrapper = Transformer.this.muxerWrapper;
      @Nullable TransformationException resourceReleaseException = null;
      try {
        releaseResources(/* forCancellation= */ false);
      } catch (TransformationException e) {
        resourceReleaseException = e;
      } catch (RuntimeException e) {
        resourceReleaseException = TransformationException.createForUnexpected(e);
      }
      if (exception == null) {
        exception = resourceReleaseException;
      }

      if (exception != null) {
        TransformationException finalException = exception;
        listeners.queueEvent(
            /* eventFlag= */ CommonUtil.INDEX_UNSET,
            listener -> listener.onTransformationError(mediaItem, finalException));
      } else {
        TransformationResult result =
            new TransformationResult.Builder()
                .setDurationMs(muxerWrapper.getDurationMs())
                .setAverageAudioBitrate(muxerWrapper.getTrackAverageBitrate(MediaUtil.TRACK_TYPE_AUDIO))
                .setAverageVideoBitrate(muxerWrapper.getTrackAverageBitrate(MediaUtil.TRACK_TYPE_VIDEO))
                .setVideoFrameCount(muxerWrapper.getTrackSampleCount(MediaUtil.TRACK_TYPE_VIDEO))
                .setFileSizeBytes(getCurrentOutputFileCurrentSizeBytes())
                .build();

        listeners.queueEvent(
            /* eventFlag= */ CommonUtil.INDEX_UNSET,
            listener -> listener.onTransformationCompleted(mediaItem, result));
      }
      listeners.flushEvents();
    }
  }
}

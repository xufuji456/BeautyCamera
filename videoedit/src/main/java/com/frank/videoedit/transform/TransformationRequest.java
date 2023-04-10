package com.frank.videoedit.transform;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.util.MimeTypes;

/** A media transformation request. */
public final class TransformationRequest {

  /** A builder for {@link TransformationRequest} instances. */
  public static final class Builder {

    private int outputHeight;
    @Nullable private String audioMimeType;
    @Nullable private String videoMimeType;
    private boolean enableRequestSdrToneMapping;
    private boolean forceInterpretHdrVideoAsSdr;
    private boolean enableHdrEditing;

    /**
     * Creates a new instance with default values.
     *
     * <p>Use {@link TransformationRequest#buildUpon()} to obtain a builder representing an existing
     * {@link TransformationRequest}.
     */
    public Builder() {
      outputHeight = C.LENGTH_UNSET;
    }

    private Builder(TransformationRequest transformationRequest) {
      this.outputHeight  = transformationRequest.outputHeight;
      this.audioMimeType = transformationRequest.audioMimeType;
      this.videoMimeType = transformationRequest.videoMimeType;
      this.enableRequestSdrToneMapping = transformationRequest.enableRequestSdrToneMapping;
      this.forceInterpretHdrVideoAsSdr = transformationRequest.forceInterpretHdrVideoAsSdr;
      this.enableHdrEditing = transformationRequest.enableHdrEditing;
    }

    /**
     * Sets the output resolution using the output height.
     *
     * <p>Output width of the displayed video will scale to preserve the video's aspect ratio after
     * other transformations.
     *
     * <p>For example, a 1920x1440 video can be scaled to 640x480 by calling setResolution(480).
     *
     * @param outputHeight The output height of the displayed video, in pixels.
     * @return This builder.
     */
    public Builder setResolution(int outputHeight) {
      this.outputHeight = outputHeight;
      return this;
    }

    /**
     * Sets the video MIME type of the output.
     *
     * <p>The default value is {@code null} which corresponds to using the same MIME type as the
     * input. Supported MIME types are:
     *
     * <ul>
     *   <li>{@link MimeTypes#VIDEO_H263}
     *   <li>{@link MimeTypes#VIDEO_H264}
     *   <li>{@link MimeTypes#VIDEO_H265} from API level 24
     *   <li>{@link MimeTypes#VIDEO_MP4V}
     * </ul>
     *
     * @param videoMimeType The MIME type of the video samples in the output.
     * @return This builder.
     * @throws IllegalArgumentException If the {@code videoMimeType} is non-null but not a video
     *     {@linkplain MimeTypes MIME type}.
     */
    public Builder setVideoMimeType(@Nullable String videoMimeType) {
      this.videoMimeType = videoMimeType;
      return this;
    }

    /**
     * Sets the audio MIME type of the output.
     *
     * <p>The default value is {@code null} which corresponds to using the same MIME type as the
     * input. Supported MIME types are:
     *
     * <ul>
     *   <li>{@link MimeTypes#AUDIO_AAC}
     *   <li>{@link MimeTypes#AUDIO_AMR_NB}
     *   <li>{@link MimeTypes#AUDIO_AMR_WB}
     * </ul>
     *
     * @param audioMimeType The MIME type of the audio samples in the output.
     * @return This builder.
     * @throws IllegalArgumentException If the {@code audioMimeType} is non-null but not an audio
     *     {@linkplain MimeTypes MIME type}.
     */
    public Builder setAudioMimeType(@Nullable String audioMimeType) {
      this.audioMimeType = audioMimeType;
      return this;
    }

    /**
     * Sets whether to request tone-mapping to standard dynamic range (SDR). If enabled and
     * supported, high dynamic range (HDR) input will be tone-mapped into an SDR opto-electrical
     * transfer function before processing.
     *
     * <p>The default value is {@code true}, which corresponds to tone-mapping output if possible.
     *
     * <p>The setting has no effect if the input is already in SDR, or if tone-mapping is not
     * supported. Currently tone-mapping is only guaranteed to be supported from Android T onwards.
     *
     * <p>Setting this as {@code true} will set {@linkplain #experimental_setEnableHdrEditing} and
     * {@linkplain #forceInterpretHdrVideoAsSdr} to {@code false}.
     *
     * @param enableRequestSdrToneMapping Whether to request tone-mapping down to SDR.
     * @return This builder.
     */
    public Builder setEnableRequestSdrToneMapping(boolean enableRequestSdrToneMapping) {
      this.enableRequestSdrToneMapping = enableRequestSdrToneMapping;
      if (enableRequestSdrToneMapping) {
        forceInterpretHdrVideoAsSdr = false;
        enableHdrEditing = false;
      }
      return this;
    }

    /**
     * Sets whether to interpret HDR video as SDR, resulting in washed out video.
     *
     * <p>The default value is {@code false}, with {@link #setEnableRequestSdrToneMapping} being
     * applied.
     *
     * <p>Use of this flag may result in {@code
     * TransformationException.ERROR_CODE_HDR_DECODING_UNSUPPORTED} or {@code
     * ERROR_CODE_DECODING_FORMAT_UNSUPPORTED}.
     *
     * <p>This method is experimental, and will be renamed or removed in a future release.
     *
     * <p>If enabled, HDR information will be interpreted as SDR, which is much more widely
     * supported than tone mapping or HDR editing. However, as HDR transfer functions and metadata
     * will be ignored, contents will be displayed incorrectly, likely with a washed out look.
     *
     * <p>The setting has no effect if the input is already in SDR.
     *
     * <p>Setting this as {@code true} will set {@linkplain #experimental_setEnableHdrEditing} and
     * {@linkplain #forceInterpretHdrVideoAsSdr} to {@code false}.
     *
     * @param forceInterpretHdrVideoAsSdr Whether to interpret HDR contents as SDR.
     * @return This builder.
     */
    // TODO(http://b/258246130): Use IntDef to select between tone mapping, HDR editing, and this.
    public Builder experimental_setForceInterpretHdrVideoAsSdr(
        boolean forceInterpretHdrVideoAsSdr) {
      this.forceInterpretHdrVideoAsSdr = forceInterpretHdrVideoAsSdr;
      if (forceInterpretHdrVideoAsSdr) {
        enableRequestSdrToneMapping = false;
        enableHdrEditing = false;
      }
      return this;
    }

    /**
     * Sets whether to allow processing high dynamic range (HDR) input video streams as HDR.
     *
     * <p>The default value is {@code false}, with {@link #setEnableRequestSdrToneMapping} being
     * applied.
     *
     * <p>This method is experimental, and will be renamed or removed in a future release. The HDR
     * editing feature is under development and is intended for developing/testing HDR support.
     *
     * <p>Setting this as {@code true} will set {@linkplain #experimental_setEnableHdrEditing} and
     * {@linkplain #forceInterpretHdrVideoAsSdr} to {@code false}.
     *
     * <p>With this flag enabled, HDR streams will correctly edit in HDR, convert via tone-mapping
     * to SDR, or throw an error, based on the device's HDR support. SDR streams will be interpreted
     * the same way regardless of this flag's state.
     *
     * @param enableHdrEditing Whether to attempt to process any input video stream as a high
     *     dynamic range (HDR) signal.
     * @return This builder.
     */
    public Builder experimental_setEnableHdrEditing(boolean enableHdrEditing) {
      this.enableHdrEditing = enableHdrEditing;
      if (enableHdrEditing) {
        enableRequestSdrToneMapping = false;
        forceInterpretHdrVideoAsSdr = false;
      }
      return this;
    }

    /** Builds a {@link TransformationRequest} instance. */
    public TransformationRequest build() {
      return new TransformationRequest(
          outputHeight,
          audioMimeType,
          videoMimeType,
          enableRequestSdrToneMapping,
          forceInterpretHdrVideoAsSdr,
          enableHdrEditing);
    }
  }

  /**
   * The requested height of the output video, or {@link C#LENGTH_UNSET} if inferred from the input.
   *
   * @see Builder#setResolution(int)
   */
  public final int outputHeight;
  /**
   * The requested output audio sample {@linkplain MimeTypes MIME type}, or {@code null} if inferred
   * from the input.
   *
   * @see Builder#setAudioMimeType(String)
   */
  @Nullable public final String audioMimeType;
  /**
   * The requested output video sample {@linkplain MimeTypes MIME type}, or {@code null} if inferred
   * from the input.
   *
   * @see Builder#setVideoMimeType(String)
   */
  @Nullable public final String videoMimeType;
  /** Whether to request tone-mapping to standard dynamic range (SDR). */
  public final boolean enableRequestSdrToneMapping;

  /** Whether to force interpreting HDR video as SDR. */
  public final boolean forceInterpretHdrVideoAsSdr;

  /**
   * Whether to attempt to process any input video stream as a high dynamic range (HDR) signal.
   *
   * @see Builder#experimental_setEnableHdrEditing(boolean)
   */
  public final boolean enableHdrEditing;

  private TransformationRequest(
      int outputHeight,
      @Nullable String audioMimeType,
      @Nullable String videoMimeType,
      boolean enableRequestSdrToneMapping,
      boolean forceInterpretHdrVideoAsSdr,
      boolean enableHdrEditing) {

    this.outputHeight                = outputHeight;
    this.audioMimeType               = audioMimeType;
    this.videoMimeType               = videoMimeType;
    this.enableHdrEditing            = enableHdrEditing;
    this.enableRequestSdrToneMapping = enableRequestSdrToneMapping;
    this.forceInterpretHdrVideoAsSdr = forceInterpretHdrVideoAsSdr;
  }

  /**
   * Returns a new {@link Builder} initialized with the values of this
   * instance.
   */
  public Builder buildUpon() {
    return new Builder(this);
  }
}

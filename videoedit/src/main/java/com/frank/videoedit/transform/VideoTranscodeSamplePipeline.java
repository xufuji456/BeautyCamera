package com.frank.videoedit.transform;

import static com.google.android.exoplayer2.util.Assertions.checkNotNull;
import static com.google.android.exoplayer2.util.Assertions.checkState;
import static com.google.android.exoplayer2.util.Util.SDK_INT;

import android.content.Context;
import android.media.MediaCodec;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.frank.videoedit.listener.FrameProcessor;
import com.frank.videoedit.transform.listener.Codec;
import com.frank.videoedit.effect.Presentation;
import com.frank.videoedit.effect.entity.SurfaceInfo;
import com.frank.videoedit.effect.entity.FrameInfo;
import com.frank.videoedit.effect.entity.ColorInfo;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.decoder.DecoderInputBuffer;
import com.google.android.exoplayer2.util.Effect;
import com.google.android.exoplayer2.util.FrameProcessingException;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.util.Util;
import com.google.common.collect.ImmutableList;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Pipeline to decode video samples, apply transformations on the raw samples, and re-encode them.
 */
/* package */ final class VideoTranscodeSamplePipeline extends BaseSamplePipeline {

  private final int maxPendingFrameCount;

  private final DecoderInputBuffer decoderInputBuffer;
  private final Codec decoder;
  private final ArrayList<Long> decodeOnlyPresentationTimestamps;

  private final FrameProcessor frameProcessor;

  private final EncoderWrapper encoderWrapper;
  private final DecoderInputBuffer encoderOutputBuffer;

  // TODO
  private static ColorInfo convertColorInfo(Format format) {
    if (format == null || format.colorInfo == null) {
      return null;
    }
    return new ColorInfo(format.colorInfo.colorSpace,
            format.colorInfo.colorRange,
            format.colorInfo.colorTransfer,
            format.colorInfo.hdrStaticInfo);
  }

  public VideoTranscodeSamplePipeline(
      Context context,
      Format inputFormat,
      long streamOffsetUs,
      long streamStartPositionUs,
      TransformationRequest transformationRequest,
      ImmutableList<Effect> effects,
      FrameProcessor.Factory frameProcessorFactory,
      Codec.DecoderFactory decoderFactory,
      Codec.EncoderFactory encoderFactory,
      MuxerWrapper muxerWrapper,
      FallbackListener fallbackListener,
      Transformer.AsyncErrorListener asyncErrorListener)
      throws TransformationException {
    super(
        inputFormat,
        streamOffsetUs,
        streamStartPositionUs,
        muxerWrapper);

    ColorInfo colorInfo = convertColorInfo(inputFormat);
    if (ColorInfo.isTransferHdr(/*inputFormat.colorInfo*/colorInfo)) {
      if (transformationRequest.forceInterpretHdrVideoAsSdr) {
        if (SDK_INT < 29) {
          throw TransformationException.createForCodec(
              new IllegalArgumentException("Interpreting HDR video as SDR is not supported."),
              /* isVideo= */ true,
              /* isDecoder= */ true,
              inputFormat,
              /* mediaCodecName= */ null,
              TransformationException.ERROR_CODE_HDR_DECODING_UNSUPPORTED);
        }
        inputFormat = inputFormat.buildUpon().
                setColorInfo(com.google.android.exoplayer2.video.ColorInfo.SDR_BT709_LIMITED).build();
      } else if (SDK_INT < 31 || deviceNeedsNoToneMappingWorkaround()) {
        throw TransformationException.createForCodec(
            new IllegalArgumentException("HDR editing and tone mapping is not supported."),
            /* isVideo= */ true,
            /* isDecoder= */ false,
            inputFormat,
            /* mediaCodecName= */ null,
            TransformationException.ERROR_CODE_HDR_ENCODING_UNSUPPORTED);
      }
    }

    decoderInputBuffer =
        new DecoderInputBuffer(DecoderInputBuffer.BUFFER_REPLACEMENT_MODE_DISABLED);
    encoderOutputBuffer =
        new DecoderInputBuffer(DecoderInputBuffer.BUFFER_REPLACEMENT_MODE_DISABLED);
    decodeOnlyPresentationTimestamps = new ArrayList<>();

    // The decoder rotates encoded frames for display by inputFormat.rotationDegrees.
    int decodedWidth =
        (inputFormat.rotationDegrees % 180 == 0) ? inputFormat.width : inputFormat.height;
    int decodedHeight =
        (inputFormat.rotationDegrees % 180 == 0) ? inputFormat.height : inputFormat.width;

    ImmutableList.Builder<Effect> effectsListBuilder =
        new ImmutableList.Builder<Effect>().addAll(effects);

    if (transformationRequest.outputHeight != C.LENGTH_UNSET) {
      effectsListBuilder.add(Presentation.createForHeight(transformationRequest.outputHeight));
    }

    encoderWrapper =
        new EncoderWrapper(
            encoderFactory,
            inputFormat,
            muxerWrapper.getSupportedSampleMimeTypes(C.TRACK_TYPE_VIDEO),
            transformationRequest,
            fallbackListener);

    try {
      frameProcessor =
          frameProcessorFactory.create(
              context,
              new FrameProcessor.Listener() {
                @Override
                public void onOutputSizeChanged(int width, int height) {
                  try {
                    checkNotNull(frameProcessor)
                        .setOutputSurfaceInfo(encoderWrapper.getSurfaceInfo(width, height));
                  } catch (TransformationException exception) {
                    asyncErrorListener.onTransformationException(exception);
                  }
                }

                @Override
                public void onOutputFrameAvailable(long presentationTimeUs) {
                  // Do nothing as frames are released automatically.
                }

                @Override
                public void onFrameProcessingError(FrameProcessingException exception) {
                  asyncErrorListener.onTransformationException(
                      TransformationException.createForFrameProcessingException(
                          exception, TransformationException.ERROR_CODE_FRAME_PROCESSING_FAILED));
                }

                @Override
                public void onFrameProcessingEnded() {
                  try {
                    encoderWrapper.signalEndOfInputStream();
                  } catch (TransformationException exception) {
                    asyncErrorListener.onTransformationException(exception);
                  }
                }
              },
              effectsListBuilder.build(),
              // HDR colors are only used if the MediaCodec encoder supports FEATURE_HdrEditing.
              // This implies that the OpenGL EXT_YUV_target extension is supported and hence the
              // default FrameProcessor, GlEffectsFrameProcessor, also supports HDR. Otherwise, tone
              // mapping is applied, which ensures the decoder outputs SDR output for an HDR input.
              encoderWrapper.getSupportedInputColor(),
              /* releaseFramesAutomatically= */ true);
    } catch (FrameProcessingException e) {
      throw TransformationException.createForFrameProcessingException(
          e, TransformationException.ERROR_CODE_FRAME_PROCESSING_FAILED);
    }
    frameProcessor.setInputFrameInfo(
        new FrameInfo(
            decodedWidth, decodedHeight, inputFormat.pixelWidthHeightRatio, streamOffsetUs));

    boolean isToneMappingRequired =
        ColorInfo.isTransferHdr(/*inputFormat.colorInfo*/colorInfo)
            && !ColorInfo.isTransferHdr(encoderWrapper.getSupportedInputColor());
    decoder =
        decoderFactory.createForVideoDecoding(
            inputFormat, frameProcessor.getInputSurface(), isToneMappingRequired);
    maxPendingFrameCount = decoder.getMaxPendingFrameCount();
  }

  @Override
  public void release() {
    frameProcessor.release();
    decoder.release();
    encoderWrapper.release();
  }

  @Override
  @Nullable
  protected DecoderInputBuffer dequeueInputBufferInternal() throws TransformationException {
    return decoder.maybeDequeueInputBuffer(decoderInputBuffer) ? decoderInputBuffer : null;
  }

  @Override
  protected void queueInputBufferInternal() throws TransformationException {
    if (decoderInputBuffer.isDecodeOnly()) {
      decodeOnlyPresentationTimestamps.add(decoderInputBuffer.timeUs);
    }
    decoder.queueInputBuffer(decoderInputBuffer);
  }

  @Override
  protected boolean processDataUpToMuxer() throws TransformationException {
    if (decoder.isEnded()) {
      return false;
    }

    boolean processedData = false;
    while (maybeProcessDecoderOutput()) {
      processedData = true;
    }
    if (decoder.isEnded()) {
      frameProcessor.signalEndOfInput();
    }
    // If the decoder produced output, signal that it may be possible to process data again.
    return processedData;
  }

  @Override
  @Nullable
  protected Format getMuxerInputFormat() throws TransformationException {
    return encoderWrapper.getOutputFormat();
  }

  @Override
  @Nullable
  protected DecoderInputBuffer getMuxerInputBuffer() throws TransformationException {
    encoderOutputBuffer.data = encoderWrapper.getOutputBuffer();
    if (encoderOutputBuffer.data == null) {
      return null;
    }
    MediaCodec.BufferInfo bufferInfo = checkNotNull(encoderWrapper.getOutputBufferInfo());
    encoderOutputBuffer.timeUs = bufferInfo.presentationTimeUs;
    encoderOutputBuffer.setFlags(bufferInfo.flags);
    return encoderOutputBuffer;
  }

  @Override
  protected void releaseMuxerInputBuffer() throws TransformationException {
    encoderWrapper.releaseOutputBuffer(/* render= */ false);
  }

  @Override
  protected boolean isMuxerInputEnded() {
    return encoderWrapper.isEnded();
  }

  private static TransformationRequest createSupportedTransformationRequest(
      TransformationRequest transformationRequest,
      boolean hasOutputFormatRotation,
      Format requestedFormat,
      Format supportedFormat,
      boolean isToneMappedToSdr) {
    // TODO(b/210591626): Also update bitrate etc. once encoder configuration and fallback are
    //  implemented.
    if (transformationRequest.enableRequestSdrToneMapping == isToneMappedToSdr
        && Util.areEqual(requestedFormat.sampleMimeType, supportedFormat.sampleMimeType)
        && (hasOutputFormatRotation
            ? requestedFormat.width == supportedFormat.width
            : requestedFormat.height == supportedFormat.height)) {
      return transformationRequest;
    }
    TransformationRequest.Builder transformationRequestBuilder = transformationRequest.buildUpon();
    if (transformationRequest.enableRequestSdrToneMapping != isToneMappedToSdr) {
      checkState(isToneMappedToSdr);
      transformationRequestBuilder
          .setEnableRequestSdrToneMapping(true)
          .experimental_setEnableHdrEditing(false);
    }
    return transformationRequestBuilder
        .setVideoMimeType(supportedFormat.sampleMimeType)
        .setResolution(hasOutputFormatRotation ? requestedFormat.width : requestedFormat.height)
        .build();
  }

  private static boolean deviceNeedsNoToneMappingWorkaround() {
    // Pixel build ID prefix does not support tone mapping. See http://b/249297370#comment8.
    return Util.MANUFACTURER.equals("Google")
        && (
        /* Pixel 6 */ Build.ID.startsWith("TP1A")
            || Build.ID.startsWith(/* Pixel Watch */ "rwd9.220429.053"));
  }

  private boolean maybeProcessDecoderOutput() throws TransformationException {
    @Nullable MediaCodec.BufferInfo decoderOutputBufferInfo = decoder.getOutputBufferInfo();
    if (decoderOutputBufferInfo == null) {
      return false;
    }

    if (isDecodeOnlyBuffer(decoderOutputBufferInfo.presentationTimeUs)) {
      decoder.releaseOutputBuffer(/* render= */ false);
      return true;
    }

    if (maxPendingFrameCount != Codec.UNLIMITED_PENDING_FRAME_COUNT
        && frameProcessor.getPendingInputFrameCount() == maxPendingFrameCount) {
      return false;
    }

    frameProcessor.registerInputFrame();
    decoder.releaseOutputBuffer(/* render= */ true);
    return true;
  }

  private boolean isDecodeOnlyBuffer(long presentationTimeUs) {
    // We avoid using decodeOnlyPresentationTimestamps.remove(presentationTimeUs) because it would
    // box presentationTimeUs, creating a Long object that would need to be garbage collected.
    int size = decodeOnlyPresentationTimestamps.size();
    for (int i = 0; i < size; i++) {
      if (decodeOnlyPresentationTimestamps.get(i) == presentationTimeUs) {
        decodeOnlyPresentationTimestamps.remove(i);
        return true;
      }
    }
    return false;
  }

  @VisibleForTesting
  /* package */ static final class EncoderWrapper {
    private static final String TAG = "EncoderWrapper";

    private final Codec.EncoderFactory encoderFactory;
    private final Format inputFormat;
    private final List<String> allowedOutputMimeTypes;
    private final TransformationRequest transformationRequest;
    private final FallbackListener fallbackListener;
    private final String requestedOutputMimeType;
    private final ImmutableList<String> supportedEncoderNamesForHdrEditing;

    private SurfaceInfo encoderSurfaceInfo;

    private volatile Codec encoder;
    private volatile int outputRotationDegrees;
    private volatile boolean releaseEncoder;

    public EncoderWrapper(Codec.EncoderFactory encoderFactory,
        Format inputFormat,
        List<String> allowedOutputMimeTypes, TransformationRequest transformationRequest,
        FallbackListener fallbackListener) {
      this.encoderFactory = encoderFactory;
      this.inputFormat = inputFormat;
      this.allowedOutputMimeTypes = allowedOutputMimeTypes;
      this.transformationRequest = transformationRequest;
      this.fallbackListener = fallbackListener;

      ColorInfo colorInfo = convertColorInfo(inputFormat);
      requestedOutputMimeType =
          transformationRequest.videoMimeType != null
              ? transformationRequest.videoMimeType
              : checkNotNull(inputFormat.sampleMimeType);
      supportedEncoderNamesForHdrEditing = EncoderUtil.getSupportedEncoderNamesForHdrEditing(
              requestedOutputMimeType, /*inputFormat.colorInfo*/colorInfo);
    }

    /** Returns the {@link ColorInfo} expected from the input surface. */
    public ColorInfo getSupportedInputColor() {
      boolean isHdrEditingEnabled =
          transformationRequest.enableHdrEditing
              && !transformationRequest.enableRequestSdrToneMapping
              && !supportedEncoderNamesForHdrEditing.isEmpty();
      ColorInfo colorInfo = convertColorInfo(inputFormat);
      boolean isInputToneMapped =
          !isHdrEditingEnabled && ColorInfo.isTransferHdr(/*inputFormat.colorInfo*/colorInfo);
      if (isInputToneMapped) {
        // When tone-mapping HDR to SDR is enabled, assume we get BT.709 to avoid having the encoder
        // populate default color info, which depends on the resolution.
        // TODO(b/237674316): Get the color info from the decoder output media format instead.
        return ColorInfo.SDR_BT709_LIMITED;
      }
      if (inputFormat.colorInfo == null) {
        Log.d(TAG, "colorInfo is null. Defaulting to SDR_BT709_LIMITED.");
        return ColorInfo.SDR_BT709_LIMITED;
      }
      return colorInfo;//inputFormat.colorInfo;
    }

    @Nullable
    public SurfaceInfo getSurfaceInfo(int requestedWidth, int requestedHeight)
        throws TransformationException {
      if (releaseEncoder) {
        return null;
      }
      if (encoderSurfaceInfo != null) {
        return encoderSurfaceInfo;
      }

      // Encoders commonly support higher maximum widths than maximum heights. This may rotate the
      // frame before encoding, so the encoded frame's width >= height, and sets
      // rotationDegrees in the output Format to ensure the frame is displayed in the correct
      // orientation.
      boolean flipOrientation = requestedWidth < requestedHeight;
      if (flipOrientation) {
        int temp = requestedWidth;
        requestedWidth = requestedHeight;
        requestedHeight = temp;
        outputRotationDegrees = 90;
      }

      ColorInfo curInfo = getSupportedInputColor();
      com.google.android.exoplayer2.video.ColorInfo newInfo = new com.google.android.exoplayer2.video.ColorInfo(
              curInfo.colorSpace, curInfo.colorRange, curInfo.colorTransfer, curInfo.hdrStaticInfo);
      Format requestedEncoderFormat =
          new Format.Builder()
              .setWidth(requestedWidth)
              .setHeight(requestedHeight)
              .setRotationDegrees(0)
              .setFrameRate(inputFormat.frameRate)
              .setSampleMimeType(requestedOutputMimeType)
              .setColorInfo(/*getSupportedInputColor()*/newInfo)
              .build();

      encoder =
          encoderFactory.createForVideoEncoding(requestedEncoderFormat, allowedOutputMimeTypes);

      Format encoderSupportedFormat = encoder.getConfigFormat();
      ColorInfo colorInfo = convertColorInfo(inputFormat);
      if (ColorInfo.isTransferHdr(/*requestedEncoderFormat.colorInfo*/colorInfo)) {
        if (!requestedOutputMimeType.equals(encoderSupportedFormat.sampleMimeType)) {
          throw createEncodingException(
              new IllegalStateException("MIME type fallback unsupported with HDR editing"),
              encoderSupportedFormat);
        } else if (!supportedEncoderNamesForHdrEditing.contains(encoder.getName())) {
          throw createEncodingException(
              new IllegalStateException("Selected encoder doesn't support HDR editing"),
              encoderSupportedFormat);
        }
      }
      boolean isInputToneMapped =
          ColorInfo.isTransferHdr(/*inputFormat.colorInfo*/colorInfo)
              && !ColorInfo.isTransferHdr(/*requestedEncoderFormat.colorInfo*/colorInfo);
      fallbackListener.onTransformationRequestFinalized(
          createSupportedTransformationRequest(
              transformationRequest,
              /* hasOutputFormatRotation= */ flipOrientation,
              requestedEncoderFormat,
              encoderSupportedFormat,
              isInputToneMapped));

      encoderSurfaceInfo =
          new SurfaceInfo(
              encoder.getInputSurface(),
              encoderSupportedFormat.width,
              encoderSupportedFormat.height,
              outputRotationDegrees);

      if (releaseEncoder) {
        encoder.release();
      }
      return encoderSurfaceInfo;
    }

    public void signalEndOfInputStream() throws TransformationException {
      if (encoder != null) {
        encoder.signalEndOfInputStream();
      }
    }

    @Nullable
    public Format getOutputFormat() throws TransformationException {
      if (encoder == null) {
        return null;
      }
      @Nullable Format outputFormat = encoder.getOutputFormat();
      if (outputFormat != null && outputRotationDegrees != 0) {
        outputFormat = outputFormat.buildUpon().setRotationDegrees(outputRotationDegrees).build();
      }
      return outputFormat;
    }

    @Nullable
    public ByteBuffer getOutputBuffer() throws TransformationException {
      return encoder != null ? encoder.getOutputBuffer() : null;
    }

    @Nullable
    public MediaCodec.BufferInfo getOutputBufferInfo() throws TransformationException {
      return encoder != null ? encoder.getOutputBufferInfo() : null;
    }

    public void releaseOutputBuffer(boolean render) throws TransformationException {
      if (encoder != null) {
        encoder.releaseOutputBuffer(render);
      }
    }

    public boolean isEnded() {
      return encoder != null && encoder.isEnded();
    }

    public void release() {
      if (encoder != null) {
        encoder.release();
      }
      releaseEncoder = true;
    }

    private TransformationException createEncodingException(Exception cause, Format format) {
      return TransformationException.createForCodec(
          cause,
          /* isVideo= */ true,
          /* isDecoder= */ false,
          format,
          checkNotNull(encoder).getName(),
          TransformationException.ERROR_CODE_ENCODING_FAILED);
    }
  }
}

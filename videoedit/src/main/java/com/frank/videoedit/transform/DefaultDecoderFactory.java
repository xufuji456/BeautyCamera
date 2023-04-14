package com.frank.videoedit.transform;

import android.content.Context;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Pair;
import android.view.Surface;

import androidx.annotation.Nullable;

import com.frank.videoedit.entity.ColorInfo;
import com.frank.videoedit.transform.listener.Codec;
import com.frank.videoedit.transform.util.MediaUtil;

import com.google.android.exoplayer2.mediacodec.MediaCodecUtil;

/** A default implementation of {@link Codec.DecoderFactory}. */
/* package */ final class DefaultDecoderFactory implements Codec.DecoderFactory {

  private final Context context;

  private final boolean decoderSupportsKeyAllowFrameDrop;

  public DefaultDecoderFactory(Context context) {
    this.context = context;

    decoderSupportsKeyAllowFrameDrop =
            Build.VERSION.SDK_INT >= 29
            && context.getApplicationContext().getApplicationInfo().targetSdkVersion >= 29;
  }

  @Override
  public Codec createForAudioDecoding(Format format) throws TransformationException {
    MediaFormat mediaFormat =
        MediaFormat.createAudioFormat(
            format.sampleMimeType, format.sampleRate, format.channelCount);
    MediaUtil.maybeSetInteger(
        mediaFormat, MediaFormat.KEY_MAX_INPUT_SIZE, format.maxInputSize);
    MediaUtil.setCsdBuffers(mediaFormat, format.initializationData);

    @Nullable
    String mediaCodecName = EncoderUtil.findCodecForFormat(mediaFormat, /* isDecoder= */ true);
    if (mediaCodecName == null) {
      throw createTransformationException(format);
    }
    return new DefaultCodec(
        context,
        format,
        mediaFormat,
        mediaCodecName,
        /* isDecoder= */ true,
        /* outputSurface= */ null);
  }

  // TODO
  private ColorInfo convertColorInfo(Format format) {
    if (format == null || format.colorInfo == null) {
      return null;
    }
    return new ColorInfo(format.colorInfo.colorSpace,
            format.colorInfo.colorRange,
            format.colorInfo.colorTransfer,
            format.colorInfo.hdrStaticInfo);
  }

  @Override
  public Codec createForVideoDecoding(
      Format format, Surface outputSurface, boolean enableRequestSdrToneMapping)
      throws TransformationException {
    MediaFormat mediaFormat =
        MediaFormat.createVideoFormat(
            format.sampleMimeType, format.width, format.height);
    MediaUtil.maybeSetInteger(mediaFormat, MediaFormat.KEY_ROTATION, format.rotationDegrees);
    MediaUtil.maybeSetInteger(
        mediaFormat, MediaFormat.KEY_MAX_INPUT_SIZE, format.maxInputSize);
    MediaUtil.setCsdBuffers(mediaFormat, format.initializationData);
    MediaUtil.maybeSetColorInfo(mediaFormat, /*format.colorInfo*/convertColorInfo(format));
    if (decoderSupportsKeyAllowFrameDrop) {
      // This key ensures no frame dropping when the decoder's output surface is full. This allows
      // transformer to decode as many frames as possible in one render cycle.
      mediaFormat.setInteger(MediaFormat.KEY_ALLOW_FRAME_DROP, 0);
    }
    if (Build.VERSION.SDK_INT >= 31 && enableRequestSdrToneMapping) {
      mediaFormat.setInteger(
          MediaFormat.KEY_COLOR_TRANSFER_REQUEST, MediaFormat.COLOR_TRANSFER_SDR_VIDEO);
    }

    @Nullable
    Pair<Integer, Integer> codecProfileAndLevel = MediaUtil.getCodecProfileAndLevel(format);
    if (codecProfileAndLevel != null) {
      MediaUtil.maybeSetInteger(
          mediaFormat, MediaFormat.KEY_PROFILE, codecProfileAndLevel.first);
    }

    @Nullable
    String mediaCodecName = EncoderUtil.findCodecForFormat(mediaFormat, /* isDecoder= */ true);
    if (mediaCodecName == null) {
      throw createTransformationException(format);
    }
    return new DefaultCodec(
        context, format, mediaFormat, mediaCodecName, /* isDecoder= */ true, outputSurface);
  }

  private static TransformationException createTransformationException(Format format) {
    return TransformationException.createForCodec(
        new IllegalArgumentException("The requested decoding format is not supported."),
        MediaUtil.isVideo(format.sampleMimeType),
        /* isDecoder= */ true,
        format,
        /* mediaCodecName= */ null,
        TransformationException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED);
  }
}

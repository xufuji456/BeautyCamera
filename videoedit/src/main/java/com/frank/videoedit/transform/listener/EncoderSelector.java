package com.frank.videoedit.transform.listener;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;

import com.frank.videoedit.transform.EncoderUtil;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.common.collect.ImmutableList;

/** Selector of {@link MediaCodec} encoder instances. */
public interface EncoderSelector {

  /**
   * Default implementation of {@link EncoderSelector}, which returns the preferred encoders for the
   * given {@link MimeTypes MIME type}.
   */
  EncoderSelector DEFAULT = EncoderUtil::getSupportedEncoders;

  /**
   * Returns a list of encoders that can encode media in the specified {@code mimeType}, in priority
   * order.
   *
   * @param mimeType The {@linkplain MimeTypes MIME type} for which an encoder is required.
   * @return An immutable list of {@linkplain MediaCodecInfo encoders} that support the {@code
   *     mimeType}. The list may be empty.
   */
  ImmutableList<MediaCodecInfo> selectEncoderInfos(String mimeType);
}

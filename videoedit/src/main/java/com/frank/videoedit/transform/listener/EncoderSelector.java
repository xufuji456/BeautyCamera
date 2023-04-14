package com.frank.videoedit.transform.listener;

import android.media.MediaCodecInfo;

import com.frank.videoedit.transform.EncoderUtil;

import com.google.common.collect.ImmutableList;

public interface EncoderSelector {

  EncoderSelector DEFAULT = EncoderUtil::getSupportedEncoders;

  ImmutableList<MediaCodecInfo> selectEncoderInfos(String mimeType);
}

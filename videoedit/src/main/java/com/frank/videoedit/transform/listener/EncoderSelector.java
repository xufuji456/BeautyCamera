package com.frank.videoedit.transform.listener;

import android.media.MediaCodecInfo;

import com.frank.videoedit.transform.EncoderUtil;

import java.util.List;

public interface EncoderSelector {

  EncoderSelector DEFAULT = EncoderUtil::getSupportedEncoders;

  List<MediaCodecInfo> selectEncoderInfos(String mimeType);
}

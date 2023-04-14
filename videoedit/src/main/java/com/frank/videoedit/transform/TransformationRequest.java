package com.frank.videoedit.transform;

import androidx.annotation.Nullable;

import com.frank.videoedit.util.CommonUtil;

public final class TransformationRequest {

  public static final class Builder {

    private int outputHeight;
    @Nullable private String audioMimeType;
    @Nullable private String videoMimeType;
    private boolean enableRequestSdrToneMapping;
    private boolean forceInterpretHdrVideoAsSdr;
    private boolean enableHdrEditing;

    public Builder() {
      outputHeight = CommonUtil.LENGTH_UNSET;
    }

    private Builder(TransformationRequest transformationRequest) {
      this.outputHeight  = transformationRequest.outputHeight;
      this.audioMimeType = transformationRequest.audioMimeType;
      this.videoMimeType = transformationRequest.videoMimeType;
      this.enableRequestSdrToneMapping = transformationRequest.enableRequestSdrToneMapping;
      this.forceInterpretHdrVideoAsSdr = transformationRequest.forceInterpretHdrVideoAsSdr;
      this.enableHdrEditing = transformationRequest.enableHdrEditing;
    }

    public Builder setResolution(int outputHeight) {
      this.outputHeight = outputHeight;
      return this;
    }

    public Builder setVideoMimeType(@Nullable String videoMimeType) {
      this.videoMimeType = videoMimeType;
      return this;
    }

    public Builder setAudioMimeType(@Nullable String audioMimeType) {
      this.audioMimeType = audioMimeType;
      return this;
    }

    public Builder setEnableRequestSdrToneMapping(boolean enableRequestSdrToneMapping) {
      this.enableRequestSdrToneMapping = enableRequestSdrToneMapping;
      if (enableRequestSdrToneMapping) {
        forceInterpretHdrVideoAsSdr = false;
        enableHdrEditing = false;
      }
      return this;
    }

    public Builder experimental_setForceInterpretHdrVideoAsSdr(
        boolean forceInterpretHdrVideoAsSdr) {
      this.forceInterpretHdrVideoAsSdr = forceInterpretHdrVideoAsSdr;
      if (forceInterpretHdrVideoAsSdr) {
        enableRequestSdrToneMapping = false;
        enableHdrEditing = false;
      }
      return this;
    }

    public Builder experimental_setEnableHdrEditing(boolean enableHdrEditing) {
      this.enableHdrEditing = enableHdrEditing;
      if (enableHdrEditing) {
        enableRequestSdrToneMapping = false;
        forceInterpretHdrVideoAsSdr = false;
      }
      return this;
    }

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

  public final int outputHeight;

  @Nullable public final String audioMimeType;

  @Nullable public final String videoMimeType;

  public final boolean enableRequestSdrToneMapping;

  public final boolean forceInterpretHdrVideoAsSdr;

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

  public Builder buildUpon() {
    return new Builder(this);
  }
}

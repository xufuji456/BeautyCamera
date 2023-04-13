
package com.frank.videoedit.transform;

import android.media.DrmInitData;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.frank.videoedit.entity.ColorInfo;
import com.frank.videoedit.listener.Bundleable;
import com.frank.videoedit.transform.entity.Metadata;
import com.frank.videoedit.transform.util.MediaUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Format implements Bundleable {

  public static final class Builder {

    @Nullable private String id;
    @Nullable private String label;
    @Nullable private String language;
    private int averageBitrate;
    private int peakBitrate;
    @Nullable private String codecs;
    @Nullable private Metadata metadata;

    // Container specific.
    @Nullable private String containerMimeType;

    // Sample specific.
    @Nullable private String sampleMimeType;
    private int maxInputSize;
    @Nullable private List<byte[]> initializationData;
    @Nullable private DrmInitData drmInitData;
    private long subsampleOffsetUs;

    // Video specific.
    private int width;
    private int height;
    private float frameRate;
    private int rotationDegrees;
    private float pixelWidthHeightRatio;
    @Nullable private byte[] projectionData;
    private @MediaUtil.StereoMode int stereoMode;
    @Nullable private ColorInfo colorInfo;

    // Audio specific.
    private int channelCount;
    private int sampleRate;
    private @MediaUtil.PcmEncoding int pcmEncoding;
    private int encoderDelay;
    private int encoderPadding;

    // Text specific.
    private int accessibilityChannel;

    // Image specific
    private int tileCountHorizontal;
    private int tileCountVertical;

    public Builder() {
      averageBitrate = NO_VALUE;
      peakBitrate = NO_VALUE;
      maxInputSize = NO_VALUE;
      subsampleOffsetUs = OFFSET_SAMPLE_RELATIVE;
      width = NO_VALUE;
      height = NO_VALUE;
      frameRate = NO_VALUE;
      pixelWidthHeightRatio = 1.0f;
      stereoMode = NO_VALUE;
      channelCount = NO_VALUE;
      sampleRate = NO_VALUE;
      pcmEncoding = NO_VALUE;
      accessibilityChannel = NO_VALUE;
      tileCountHorizontal = NO_VALUE;
      tileCountVertical = NO_VALUE;
    }

    private Builder(Format format) {
      this.id = format.id;
      this.label = format.label;
      this.language = format.language;
      this.averageBitrate = format.averageBitrate;
      this.peakBitrate = format.peakBitrate;
      this.codecs = format.codecs;
      this.metadata = format.metadata;
      this.containerMimeType = format.containerMimeType;
      this.sampleMimeType = format.sampleMimeType;
      this.maxInputSize = format.maxInputSize;
      this.initializationData = format.initializationData;
      this.drmInitData = format.drmInitData;
      this.subsampleOffsetUs = format.subsampleOffsetUs;
      this.width = format.width;
      this.height = format.height;
      this.frameRate = format.frameRate;
      this.rotationDegrees = format.rotationDegrees;
      this.pixelWidthHeightRatio = format.pixelWidthHeightRatio;
      this.projectionData = format.projectionData;
      this.stereoMode = format.stereoMode;
      this.colorInfo = format.colorInfo;
      this.channelCount = format.channelCount;
      this.sampleRate = format.sampleRate;
      this.pcmEncoding = format.pcmEncoding;
      this.encoderDelay = format.encoderDelay;
      this.encoderPadding = format.encoderPadding;
      this.accessibilityChannel = format.accessibilityChannel;
      this.tileCountHorizontal = format.tileCountHorizontal;
      this.tileCountVertical = format.tileCountVertical;
    }

    public Builder setId(@Nullable String id) {
      this.id = id;
      return this;
    }

    public Builder setId(int id) {
      this.id = Integer.toString(id);
      return this;
    }

    public Builder setLabel(@Nullable String label) {
      this.label = label;
      return this;
    }

    public Builder setLanguage(@Nullable String language) {
      this.language = language;
      return this;
    }

    public Builder setAverageBitrate(int averageBitrate) {
      this.averageBitrate = averageBitrate;
      return this;
    }

    public Builder setPeakBitrate(int peakBitrate) {
      this.peakBitrate = peakBitrate;
      return this;
    }

    public Builder setCodecs(@Nullable String codecs) {
      this.codecs = codecs;
      return this;
    }

    public Builder setMetadata(@Nullable Metadata metadata) {
      this.metadata = metadata;
      return this;
    }

    public Builder setContainerMimeType(@Nullable String containerMimeType) {
      this.containerMimeType = containerMimeType;
      return this;
    }

    public Builder setSampleMimeType(@Nullable String sampleMimeType) {
      this.sampleMimeType = sampleMimeType;
      return this;
    }

    public Builder setMaxInputSize(int maxInputSize) {
      this.maxInputSize = maxInputSize;
      return this;
    }

    public Builder setInitializationData(@Nullable List<byte[]> initializationData) {
      this.initializationData = initializationData;
      return this;
    }

    public Builder setDrmInitData(@Nullable DrmInitData drmInitData) {
      this.drmInitData = drmInitData;
      return this;
    }

    public Builder setSubsampleOffsetUs(long subsampleOffsetUs) {
      this.subsampleOffsetUs = subsampleOffsetUs;
      return this;
    }

    public Builder setWidth(int width) {
      this.width = width;
      return this;
    }

    public Builder setHeight(int height) {
      this.height = height;
      return this;
    }

    public Builder setFrameRate(float frameRate) {
      this.frameRate = frameRate;
      return this;
    }

    public Builder setRotationDegrees(int rotationDegrees) {
      this.rotationDegrees = rotationDegrees;
      return this;
    }

    public Builder setPixelWidthHeightRatio(float pixelWidthHeightRatio) {
      this.pixelWidthHeightRatio = pixelWidthHeightRatio;
      return this;
    }

    public Builder setProjectionData(@Nullable byte[] projectionData) {
      this.projectionData = projectionData;
      return this;
    }

    public Builder setStereoMode(@MediaUtil.StereoMode int stereoMode) {
      this.stereoMode = stereoMode;
      return this;
    }

    public Builder setColorInfo(@Nullable ColorInfo colorInfo) {
      this.colorInfo = colorInfo;
      return this;
    }

    public Builder setChannelCount(int channelCount) {
      this.channelCount = channelCount;
      return this;
    }

    public Builder setSampleRate(int sampleRate) {
      this.sampleRate = sampleRate;
      return this;
    }

    public Builder setPcmEncoding(@MediaUtil.PcmEncoding int pcmEncoding) {
      this.pcmEncoding = pcmEncoding;
      return this;
    }

    public Builder setEncoderDelay(int encoderDelay) {
      this.encoderDelay = encoderDelay;
      return this;
    }

    public Builder setEncoderPadding(int encoderPadding) {
      this.encoderPadding = encoderPadding;
      return this;
    }

    public Builder setAccessibilityChannel(int accessibilityChannel) {
      this.accessibilityChannel = accessibilityChannel;
      return this;
    }

    public Builder setTileCountHorizontal(int tileCountHorizontal) {
      this.tileCountHorizontal = tileCountHorizontal;
      return this;
    }

    public Builder setTileCountVertical(int tileCountVertical) {
      this.tileCountVertical = tileCountVertical;
      return this;
    }

    public Format build() {
      return new Format(/* builder= */ this);
    }
  }

  public static final int NO_VALUE = -1;

  public static final long OFFSET_SAMPLE_RELATIVE = Long.MAX_VALUE;

  private static final Format DEFAULT = new Builder().build();

  @Nullable public final String id;

  @Nullable public final String label;

  @Nullable public final String language;

  public final int averageBitrate;

  public final int peakBitrate;

  public final int bitrate;

  @Nullable public final String codecs;

  @Nullable public final Metadata metadata;

  @Nullable public final String containerMimeType;

  @Nullable public final String sampleMimeType;

  public final int maxInputSize;

  public final List<byte[]> initializationData;

  @Nullable public final DrmInitData drmInitData;

  public final long subsampleOffsetUs;

  public final int width;

  public final int height;

  public final float frameRate;

  public final int rotationDegrees;

  public final float pixelWidthHeightRatio;

  @Nullable public final byte[] projectionData;

  public final @MediaUtil.StereoMode int stereoMode;

  @Nullable public final ColorInfo colorInfo;

  public final int channelCount;

  public final int sampleRate;

  public final @MediaUtil.PcmEncoding int pcmEncoding;

  public final int encoderDelay;

  public final int encoderPadding;

  public final int accessibilityChannel;

  public final int tileCountHorizontal;

  public final int tileCountVertical;

  @Deprecated
  public static Format createVideoSampleFormat(
      @Nullable String id,
      @Nullable String sampleMimeType,
      @Nullable String codecs,
      int bitrate,
      int maxInputSize,
      int width,
      int height,
      float frameRate,
      @Nullable List<byte[]> initializationData,
      @Nullable DrmInitData drmInitData) {
    return new Builder()
        .setId(id)
        .setAverageBitrate(bitrate)
        .setPeakBitrate(bitrate)
        .setCodecs(codecs)
        .setSampleMimeType(sampleMimeType)
        .setMaxInputSize(maxInputSize)
        .setInitializationData(initializationData)
        .setDrmInitData(drmInitData)
        .setWidth(width)
        .setHeight(height)
        .setFrameRate(frameRate)
        .build();
  }

  @Deprecated
  public static Format createVideoSampleFormat(
      @Nullable String id,
      @Nullable String sampleMimeType,
      @Nullable String codecs,
      int bitrate,
      int maxInputSize,
      int width,
      int height,
      float frameRate,
      @Nullable List<byte[]> initializationData,
      int rotationDegrees,
      float pixelWidthHeightRatio,
      @Nullable DrmInitData drmInitData) {
    return new Builder()
        .setId(id)
        .setAverageBitrate(bitrate)
        .setPeakBitrate(bitrate)
        .setCodecs(codecs)
        .setSampleMimeType(sampleMimeType)
        .setMaxInputSize(maxInputSize)
        .setInitializationData(initializationData)
        .setDrmInitData(drmInitData)
        .setWidth(width)
        .setHeight(height)
        .setFrameRate(frameRate)
        .setRotationDegrees(rotationDegrees)
        .setPixelWidthHeightRatio(pixelWidthHeightRatio)
        .build();
  }

  @Deprecated
  public static Format createAudioSampleFormat(
      @Nullable String id,
      @Nullable String sampleMimeType,
      @Nullable String codecs,
      int bitrate,
      int maxInputSize,
      int channelCount,
      int sampleRate,
      @Nullable List<byte[]> initializationData,
      @Nullable String language) {
    return new Builder()
        .setId(id)
        .setLanguage(language)
        .setAverageBitrate(bitrate)
        .setPeakBitrate(bitrate)
        .setCodecs(codecs)
        .setSampleMimeType(sampleMimeType)
        .setMaxInputSize(maxInputSize)
        .setInitializationData(initializationData)
        .setChannelCount(channelCount)
        .setSampleRate(sampleRate)
        .build();
  }

  @Deprecated
  public static Format createAudioSampleFormat(
      @Nullable String id,
      @Nullable String sampleMimeType,
      @Nullable String codecs,
      int bitrate,
      int maxInputSize,
      int channelCount,
      int sampleRate,
      @MediaUtil.PcmEncoding int pcmEncoding,
      @Nullable List<byte[]> initializationData,
      @Nullable String language) {
    return new Builder()
        .setId(id)
        .setLanguage(language)
        .setAverageBitrate(bitrate)
        .setPeakBitrate(bitrate)
        .setCodecs(codecs)
        .setSampleMimeType(sampleMimeType)
        .setMaxInputSize(maxInputSize)
        .setInitializationData(initializationData)
        .setChannelCount(channelCount)
        .setSampleRate(sampleRate)
        .setPcmEncoding(pcmEncoding)
        .build();
  }

  @Deprecated
  public static Format createContainerFormat(
      @Nullable String id,
      @Nullable String label,
      @Nullable String containerMimeType,
      @Nullable String sampleMimeType,
      @Nullable String codecs,
      int bitrate,
      @Nullable String language) {
    return new Builder()
        .setId(id)
        .setLabel(label)
        .setLanguage(language)
        .setAverageBitrate(bitrate)
        .setPeakBitrate(bitrate)
        .setCodecs(codecs)
        .setContainerMimeType(containerMimeType)
        .setSampleMimeType(sampleMimeType)
        .build();
  }

  @Deprecated
  public static Format createSampleFormat(@Nullable String id, @Nullable String sampleMimeType) {
    return new Builder().setId(id).setSampleMimeType(sampleMimeType).build();
  }

  private Format(Builder builder) {
    id = builder.id;
    label = builder.label;
    language = builder.language; //normalizeLanguageCode(builder.language);
    averageBitrate = builder.averageBitrate;
    peakBitrate = builder.peakBitrate;
    bitrate = peakBitrate != NO_VALUE ? peakBitrate : averageBitrate;
    codecs = builder.codecs;
    metadata = builder.metadata;
    containerMimeType = builder.containerMimeType;
    sampleMimeType = builder.sampleMimeType;
    maxInputSize = builder.maxInputSize;
    initializationData =
        builder.initializationData == null ? Collections.emptyList() : builder.initializationData;
    drmInitData = builder.drmInitData;
    subsampleOffsetUs = builder.subsampleOffsetUs;
    width = builder.width;
    height = builder.height;
    frameRate = builder.frameRate;
    rotationDegrees = builder.rotationDegrees == NO_VALUE ? 0 : builder.rotationDegrees;
    pixelWidthHeightRatio =
        builder.pixelWidthHeightRatio == NO_VALUE ? 1 : builder.pixelWidthHeightRatio;
    projectionData = builder.projectionData;
    stereoMode = builder.stereoMode;
    colorInfo = builder.colorInfo;
    channelCount = builder.channelCount;
    sampleRate = builder.sampleRate;
    pcmEncoding = builder.pcmEncoding;
    encoderDelay = builder.encoderDelay == NO_VALUE ? 0 : builder.encoderDelay;
    encoderPadding = builder.encoderPadding == NO_VALUE ? 0 : builder.encoderPadding;
    accessibilityChannel = builder.accessibilityChannel;
    tileCountHorizontal = builder.tileCountHorizontal;
    tileCountVertical = builder.tileCountVertical;
  }

  public Builder buildUpon() {
    return new Builder(this);
  }

  @Deprecated
  public Format copyWithMaxInputSize(int maxInputSize) {
    return buildUpon().setMaxInputSize(maxInputSize).build();
  }

  @Deprecated
  public Format copyWithSubsampleOffsetUs(long subsampleOffsetUs) {
    return buildUpon().setSubsampleOffsetUs(subsampleOffsetUs).build();
  }

  @Deprecated
  public Format copyWithLabel(@Nullable String label) {
    return buildUpon().setLabel(label).build();
  }

  @Deprecated
  public Format copyWithManifestFormatInfo(Format manifestFormat) {
    return withManifestFormatInfo(manifestFormat);
  }

  public Format withManifestFormatInfo(Format manifestFormat) {
    if (this == manifestFormat) {
      return this;
    }

    @MediaUtil.TrackType int trackType = MediaUtil.getTrackType(sampleMimeType);

    @Nullable String id = manifestFormat.id;

    @Nullable String label = manifestFormat.label != null ? manifestFormat.label : this.label;
    @Nullable String language = this.language;
    if ((trackType == MediaUtil.TRACK_TYPE_TEXT || trackType == MediaUtil.TRACK_TYPE_AUDIO)
        && manifestFormat.language != null) {
      language = manifestFormat.language;
    }

    int averageBitrate =
        this.averageBitrate == NO_VALUE ? manifestFormat.averageBitrate : this.averageBitrate;
    int peakBitrate = this.peakBitrate == NO_VALUE ? manifestFormat.peakBitrate : this.peakBitrate;
    @Nullable String codecs = this.codecs;
    if (codecs == null) {
      @Nullable String codecsOfType = MediaUtil.getCodecsOfType(manifestFormat.codecs, trackType);
      if (MediaUtil.splitCodecs(codecsOfType).length == 1) {
        codecs = codecsOfType;
      }
    }

    Metadata metadata =
            this.metadata == null
                    ? manifestFormat.metadata
                    : this.metadata.copyWithAppendedEntriesFrom(manifestFormat.metadata);

    float frameRate = this.frameRate;
    if (frameRate == NO_VALUE && trackType == MediaUtil.TRACK_TYPE_VIDEO) {
      frameRate = manifestFormat.frameRate;
    }

    return buildUpon()
        .setId(id)
        .setLabel(label)
        .setLanguage(language)
        .setAverageBitrate(averageBitrate)
        .setPeakBitrate(peakBitrate)
        .setCodecs(codecs)
        .setMetadata(metadata)
        .setFrameRate(frameRate)
        .build();
  }

  public Format copyWithMetadata(@Nullable Metadata metadata) {
    return buildUpon().setMetadata(metadata).build();
  }

  private static String intToStringMaxRadix(int i) {
    return Integer.toString(i, Character.MAX_RADIX);
  }

  private static final String FIELD_ID = intToStringMaxRadix(0);
  private static final String FIELD_LABEL = intToStringMaxRadix(1);
  private static final String FIELD_LANGUAGE = intToStringMaxRadix(2);
  private static final String FIELD_AVERAGE_BITRATE = intToStringMaxRadix(5);
  private static final String FIELD_PEAK_BITRATE = intToStringMaxRadix(6);
  private static final String FIELD_CODECS = intToStringMaxRadix(7);
  private static final String FIELD_METADATA = intToStringMaxRadix(8);
  private static final String FIELD_CONTAINER_MIME_TYPE = intToStringMaxRadix(9);
  private static final String FIELD_SAMPLE_MIME_TYPE = intToStringMaxRadix(10);
  private static final String FIELD_MAX_INPUT_SIZE = intToStringMaxRadix(11);
  private static final String FIELD_INITIALIZATION_DATA = intToStringMaxRadix(12);
  private static final String FIELD_DRM_INIT_DATA = intToStringMaxRadix(13);
  private static final String FIELD_SUBSAMPLE_OFFSET_US = intToStringMaxRadix(14);
  private static final String FIELD_WIDTH = intToStringMaxRadix(15);
  private static final String FIELD_HEIGHT = intToStringMaxRadix(16);
  private static final String FIELD_FRAME_RATE = intToStringMaxRadix(17);
  private static final String FIELD_ROTATION_DEGREES = intToStringMaxRadix(18);
  private static final String FIELD_PIXEL_WIDTH_HEIGHT_RATIO = intToStringMaxRadix(19);
  private static final String FIELD_PROJECTION_DATA = intToStringMaxRadix(20);
  private static final String FIELD_STEREO_MODE = intToStringMaxRadix(21);
  private static final String FIELD_COLOR_INFO = intToStringMaxRadix(22);
  private static final String FIELD_CHANNEL_COUNT = intToStringMaxRadix(23);
  private static final String FIELD_SAMPLE_RATE = intToStringMaxRadix(24);
  private static final String FIELD_PCM_ENCODING = intToStringMaxRadix(25);
  private static final String FIELD_ENCODER_DELAY = intToStringMaxRadix(26);
  private static final String FIELD_ENCODER_PADDING = intToStringMaxRadix(27);
  private static final String FIELD_ACCESSIBILITY_CHANNEL = intToStringMaxRadix(28);
  private static final String FIELD_TILE_COUNT_HORIZONTAL = intToStringMaxRadix(30);
  private static final String FIELD_TILE_COUNT_VERTICAL = intToStringMaxRadix(31);

  @Override
  public Bundle toBundle() {
    return toBundle(/* excludeMetadata= */ false);
  }

  public Bundle toBundle(boolean excludeMetadata) {
    Bundle bundle = new Bundle();
    bundle.putString(FIELD_ID, id);
    bundle.putString(FIELD_LABEL, label);
    bundle.putString(FIELD_LANGUAGE, language);
    bundle.putInt(FIELD_AVERAGE_BITRATE, averageBitrate);
    bundle.putInt(FIELD_PEAK_BITRATE, peakBitrate);
    bundle.putString(FIELD_CODECS, codecs);
    if (!excludeMetadata) {
      bundle.putParcelable(FIELD_METADATA, metadata);
    }
    bundle.putString(FIELD_CONTAINER_MIME_TYPE, containerMimeType);
    bundle.putString(FIELD_SAMPLE_MIME_TYPE, sampleMimeType);
    bundle.putInt(FIELD_MAX_INPUT_SIZE, maxInputSize);
    for (int i = 0; i < initializationData.size(); i++) {
      bundle.putByteArray(keyForInitializationData(i), initializationData.get(i));
    }
    bundle.putLong(FIELD_SUBSAMPLE_OFFSET_US, subsampleOffsetUs);
    bundle.putInt(FIELD_WIDTH, width);
    bundle.putInt(FIELD_HEIGHT, height);
    bundle.putFloat(FIELD_FRAME_RATE, frameRate);
    bundle.putInt(FIELD_ROTATION_DEGREES, rotationDegrees);
    bundle.putFloat(FIELD_PIXEL_WIDTH_HEIGHT_RATIO, pixelWidthHeightRatio);
    bundle.putByteArray(FIELD_PROJECTION_DATA, projectionData);
    bundle.putInt(FIELD_STEREO_MODE, stereoMode);
    if (colorInfo != null) {
      bundle.putBundle(FIELD_COLOR_INFO, colorInfo.toBundle());
    }
    bundle.putInt(FIELD_CHANNEL_COUNT, channelCount);
    bundle.putInt(FIELD_SAMPLE_RATE, sampleRate);
    bundle.putInt(FIELD_PCM_ENCODING, pcmEncoding);
    bundle.putInt(FIELD_ENCODER_DELAY, encoderDelay);
    bundle.putInt(FIELD_ENCODER_PADDING, encoderPadding);
    bundle.putInt(FIELD_ACCESSIBILITY_CHANNEL, accessibilityChannel);
    bundle.putInt(FIELD_TILE_COUNT_HORIZONTAL, tileCountHorizontal);
    bundle.putInt(FIELD_TILE_COUNT_VERTICAL, tileCountVertical);
    return bundle;
  }

  /** Object that can restore {@code Format} from a {@link Bundle}. */
  public static final Creator<Format> CREATOR = Format::fromBundle;

  private static Format fromBundle(Bundle bundle) {
    Builder builder = new Builder();
    bundle.setClassLoader(Format.class.getClassLoader());
    builder
        .setId(defaultIfNull(bundle.getString(FIELD_ID), DEFAULT.id))
        .setLabel(defaultIfNull(bundle.getString(FIELD_LABEL), DEFAULT.label))
        .setLanguage(defaultIfNull(bundle.getString(FIELD_LANGUAGE), DEFAULT.language))
        .setAverageBitrate(bundle.getInt(FIELD_AVERAGE_BITRATE, DEFAULT.averageBitrate))
        .setPeakBitrate(bundle.getInt(FIELD_PEAK_BITRATE, DEFAULT.peakBitrate))
        .setCodecs(defaultIfNull(bundle.getString(FIELD_CODECS), DEFAULT.codecs))
        .setMetadata(defaultIfNull(bundle.getParcelable(FIELD_METADATA), DEFAULT.metadata))
        .setContainerMimeType(
            defaultIfNull(bundle.getString(FIELD_CONTAINER_MIME_TYPE), DEFAULT.containerMimeType))
        .setSampleMimeType(
            defaultIfNull(bundle.getString(FIELD_SAMPLE_MIME_TYPE), DEFAULT.sampleMimeType))
        .setMaxInputSize(bundle.getInt(FIELD_MAX_INPUT_SIZE, DEFAULT.maxInputSize));

    List<byte[]> initializationData = new ArrayList<>();
    for (int i = 0; ; i++) {
      @Nullable byte[] data = bundle.getByteArray(keyForInitializationData(i));
      if (data == null) {
        break;
      }
      initializationData.add(data);
    }
    builder
        .setInitializationData(initializationData)
        .setDrmInitData(bundle.getParcelable(FIELD_DRM_INIT_DATA))
        .setSubsampleOffsetUs(bundle.getLong(FIELD_SUBSAMPLE_OFFSET_US, DEFAULT.subsampleOffsetUs))
        .setWidth(bundle.getInt(FIELD_WIDTH, DEFAULT.width))
        .setHeight(bundle.getInt(FIELD_HEIGHT, DEFAULT.height))
        .setFrameRate(bundle.getFloat(FIELD_FRAME_RATE, DEFAULT.frameRate))
        .setRotationDegrees(bundle.getInt(FIELD_ROTATION_DEGREES, DEFAULT.rotationDegrees))
        .setPixelWidthHeightRatio(
            bundle.getFloat(FIELD_PIXEL_WIDTH_HEIGHT_RATIO, DEFAULT.pixelWidthHeightRatio))
        .setProjectionData(bundle.getByteArray(FIELD_PROJECTION_DATA))
        .setStereoMode(bundle.getInt(FIELD_STEREO_MODE, DEFAULT.stereoMode));
    Bundle colorInfoBundle = bundle.getBundle(FIELD_COLOR_INFO);
    if (colorInfoBundle != null) {
      builder.setColorInfo(ColorInfo.CREATOR.fromBundle(colorInfoBundle));
    }
    builder
        .setChannelCount(bundle.getInt(FIELD_CHANNEL_COUNT, DEFAULT.channelCount))
        .setSampleRate(bundle.getInt(FIELD_SAMPLE_RATE, DEFAULT.sampleRate))
        .setPcmEncoding(bundle.getInt(FIELD_PCM_ENCODING, DEFAULT.pcmEncoding))
        .setEncoderDelay(bundle.getInt(FIELD_ENCODER_DELAY, DEFAULT.encoderDelay))
        .setEncoderPadding(bundle.getInt(FIELD_ENCODER_PADDING, DEFAULT.encoderPadding))
        .setAccessibilityChannel(
            bundle.getInt(FIELD_ACCESSIBILITY_CHANNEL, DEFAULT.accessibilityChannel))
        .setTileCountHorizontal(
            bundle.getInt(FIELD_TILE_COUNT_HORIZONTAL, DEFAULT.tileCountHorizontal))
        .setTileCountVertical(bundle.getInt(FIELD_TILE_COUNT_VERTICAL, DEFAULT.tileCountVertical));

    return builder.build();
  }

  private static String keyForInitializationData(int initialisationDataIndex) {
    return FIELD_INITIALIZATION_DATA
        + "_"
        + Integer.toString(initialisationDataIndex, Character.MAX_RADIX);
  }

  @Nullable
  private static <T> T defaultIfNull(@Nullable T value, @Nullable T defaultValue) {
    return value != null ? value : defaultValue;
  }
}

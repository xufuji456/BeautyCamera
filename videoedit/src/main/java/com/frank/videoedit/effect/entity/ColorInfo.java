
package com.frank.videoedit.effect.entity;

import static java.lang.annotation.ElementType.TYPE_USE;

import android.media.MediaFormat;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.Format;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public final class ColorInfo {

  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target(TYPE_USE)
  @IntDef({Format.NO_VALUE, COLOR_SPACE_BT601, COLOR_SPACE_BT709, COLOR_SPACE_BT2020})
  public @interface ColorSpace {}

  public static final int COLOR_SPACE_BT601  = MediaFormat.COLOR_STANDARD_BT601_PAL;
  public static final int COLOR_SPACE_BT709  = MediaFormat.COLOR_STANDARD_BT709;
  public static final int COLOR_SPACE_BT2020 = MediaFormat.COLOR_STANDARD_BT2020;

  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target(TYPE_USE)
  @IntDef({Format.NO_VALUE, COLOR_TRANSFER_SDR, COLOR_TRANSFER_ST2084, COLOR_TRANSFER_HLG})
  public @interface ColorTransfer {}

  public static final int COLOR_TRANSFER_SDR    = MediaFormat.COLOR_TRANSFER_SDR_VIDEO;
  public static final int COLOR_TRANSFER_ST2084 = MediaFormat.COLOR_TRANSFER_ST2084;
  public static final int COLOR_TRANSFER_HLG    = MediaFormat.COLOR_TRANSFER_HLG;

  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target(TYPE_USE)
  @IntDef({Format.NO_VALUE, COLOR_RANGE_LIMITED, COLOR_RANGE_FULL})
  public @interface ColorRange {}

  public static final int COLOR_RANGE_LIMITED = MediaFormat.COLOR_RANGE_LIMITED;
  public static final int COLOR_RANGE_FULL    = MediaFormat.COLOR_RANGE_FULL;

  public static final ColorInfo SDR_BT709_LIMITED =
      new ColorInfo(
          COLOR_SPACE_BT709,
          COLOR_RANGE_LIMITED,
          COLOR_TRANSFER_SDR,
              null);

  public static @ColorSpace int isoColorPrimariesToColorSpace(int isoColorPrimaries) {
    switch (isoColorPrimaries) {
      case 1:
        return COLOR_SPACE_BT709;
      case 4: // BT.470M.
      case 5: // BT.470BG.
      case 6: // SMPTE 170M.
      case 7: // SMPTE 240M.
        return COLOR_SPACE_BT601;
      case 9:
        return COLOR_SPACE_BT2020;
      default:
        return Format.NO_VALUE;
    }
  }

  public static @ColorTransfer int isoTransferCharacteristicsToColorTransfer(
      int isoTransferCharacteristics) {
    switch (isoTransferCharacteristics) {
      case 1: // BT.709.
      case 6: // SMPTE 170M.
      case 7: // SMPTE 240M.
        return COLOR_TRANSFER_SDR;
      case 16:
        return COLOR_TRANSFER_ST2084;
      case 18:
        return COLOR_TRANSFER_HLG;
      default:
        return Format.NO_VALUE;
    }
  }

  public static boolean isTransferHdr(@Nullable ColorInfo colorInfo) {
    return colorInfo != null
        && colorInfo.colorTransfer != -1
        && colorInfo.colorTransfer != COLOR_TRANSFER_SDR;
  }

  public final @ColorSpace int colorSpace;

  public final @ColorRange int colorRange;

  public final @ColorTransfer int colorTransfer;

  @Nullable public final byte[] hdrStaticInfo;

  public ColorInfo(
      @ColorSpace int colorSpace,
      @ColorRange int colorRange,
      @ColorTransfer int colorTransfer,
      @Nullable byte[] hdrStaticInfo) {
    this.colorSpace = colorSpace;
    this.colorRange = colorRange;
    this.colorTransfer = colorTransfer;
    this.hdrStaticInfo = hdrStaticInfo;
  }

  public static boolean isValidColorSpace(int colorSpace) {
    return colorSpace     == COLOR_SPACE_BT601
            || colorSpace == COLOR_SPACE_BT709
            || colorSpace == COLOR_SPACE_BT2020
            || colorSpace == Format.NO_VALUE;
  }

  public static boolean isValidColorRange(int colorRange) {
    return colorRange     == COLOR_RANGE_LIMITED
            || colorRange == COLOR_RANGE_FULL
            || colorRange == Format.NO_VALUE;
  }

  public static boolean isValidColorTransfer(int colorTransfer) {
    return colorTransfer     == COLOR_TRANSFER_SDR
            || colorTransfer == COLOR_TRANSFER_ST2084
            || colorTransfer == COLOR_TRANSFER_HLG
            || colorTransfer == Format.NO_VALUE;
  }


}

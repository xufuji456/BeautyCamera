package com.frank.videoedit.effect;

import android.content.Context;

import com.frank.videoedit.effect.listener.GlEffect;
import com.frank.videoedit.util.FrameProcessingException;

/** Adjusts the HSL (Hue, Saturation, and Lightness) of a frame. */
public class HslAdjustment implements GlEffect {

  /** A builder for {@code HslAdjustment} instances. */
  public static final class Builder {
    private float hueAdjustment;
    private float saturationAdjustment;
    private float lightnessAdjustment;

    /** Creates a new instance with the default values. */
    public Builder() {}

    public Builder adjustHue(float hueAdjustmentDegrees) {
      hueAdjustment = hueAdjustmentDegrees % 360;
      return this;
    }

    public Builder adjustSaturation(float saturationAdjustment) {
      if(saturationAdjustment < -100 || saturationAdjustment > 100) {
        throw new IllegalArgumentException("Can adjust the saturation by [-100, 100], but provided "
              + saturationAdjustment);
      }
      this.saturationAdjustment = saturationAdjustment;
      return this;
    }

    public Builder adjustLightness(float lightnessAdjustment) {
      if(lightnessAdjustment < -100 || lightnessAdjustment > 100) {
        throw new IllegalArgumentException("Can adjust the lightness by [-100, 100], but provided "
                + lightnessAdjustment);
      }
      this.lightnessAdjustment = lightnessAdjustment;
      return this;
    }

    /** Creates a new {@link HslAdjustment} instance. */
    public HslAdjustment build() {
      return new HslAdjustment(hueAdjustment, saturationAdjustment, lightnessAdjustment);
    }
  }

  /** Indicates the hue adjustment in degrees. */
  public final float hueAdjustmentDegrees;
  /** Indicates the saturation adjustment. */
  public final float saturationAdjustment;
  /** Indicates the lightness adjustment. */
  public final float lightnessAdjustment;

  private HslAdjustment(
      float hueAdjustmentDegrees, float saturationAdjustment, float lightnessAdjustment) {
    this.hueAdjustmentDegrees = hueAdjustmentDegrees;
    this.saturationAdjustment = saturationAdjustment;
    this.lightnessAdjustment = lightnessAdjustment;
  }

  @Override
  public SingleFrameGlTextureProcessor toGlTextureProcessor(Context context, boolean useHdr)
      throws FrameProcessingException {
    return new HslProcessor(context, /* hslAdjustment= */ this, useHdr);
  }
}

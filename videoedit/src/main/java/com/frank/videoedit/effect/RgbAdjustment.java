package com.frank.videoedit.effect;

import static com.google.android.exoplayer2.util.Assertions.checkArgument;

import android.opengl.Matrix;

import com.google.android.exoplayer2.util.GlUtil;

/** Scales the red, green, and blue color channels of a frame. */
public final class RgbAdjustment implements RgbMatrix {

  /** A builder for {@link RgbAdjustment} instances. */
  public static final class Builder {
    private float redScale;
    private float greenScale;
    private float blueScale;

    public Builder() {
      redScale = 1;
      greenScale = 1;
      blueScale = 1;
    }

    public Builder setRedScale(float redScale) {
      checkArgument(0 <= redScale, "Red scale needs to be non-negative.");
      this.redScale = redScale;
      return this;
    }

    public Builder setGreenScale(float greenScale) {
      checkArgument(0 <= greenScale, "Green scale needs to be non-negative.");
      this.greenScale = greenScale;
      return this;
    }

    public Builder setBlueScale(float blueScale) {
      checkArgument(0 <= blueScale, "Blue scale needs to be non-negative.");
      this.blueScale = blueScale;
      return this;
    }

    /** Creates a new {@link RgbAdjustment} instance. */
    public RgbAdjustment build() {
      float[] rgbMatrix = GlUtil.create4x4IdentityMatrix();
      Matrix.scaleM(
          rgbMatrix, /* smOffset= */ 0, /* x= */ redScale, /* y= */ greenScale, /* z= */ blueScale);

      return new RgbAdjustment(rgbMatrix);
    }
  }

  private final float[] rgbMatrix;

  private RgbAdjustment(float[] rgbMatrix) {
    this.rgbMatrix = rgbMatrix;
  }

  @Override
  public float[] getMatrix(long presentationTimeUs, boolean useHdr) {
    return rgbMatrix;
  }
}

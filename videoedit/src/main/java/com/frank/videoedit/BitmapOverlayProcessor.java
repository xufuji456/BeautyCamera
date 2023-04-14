package com.frank.videoedit;

import static com.google.android.exoplayer2.util.Assertions.checkArgument;
import static com.google.android.exoplayer2.util.Assertions.checkStateNotNull;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;
import android.util.Pair;

import com.frank.videoedit.effect.SingleFrameGlTextureProcessor;
import com.frank.videoedit.effect.util.GlProgram;
import com.frank.videoedit.effect.util.GlUtil;
import com.frank.videoedit.util.CommonUtil;
import com.frank.videoedit.util.FrameProcessingException;

import java.io.IOException;
import java.util.Locale;

/* package */ final class BitmapOverlayProcessor extends SingleFrameGlTextureProcessor {

  private static final String VERTEX_SHADER_PATH = "vertex_copy_es2.glsl";
  private static final String FRAGMENT_SHADER_PATH = "fragment_bitmap_overlay_es2.glsl";

  private static final int BITMAP_WIDTH_HEIGHT = 512;

  private final Paint paint;
  private final Bitmap overlayBitmap;
  private final Bitmap logoBitmap;
  private final Canvas overlayCanvas;
  private GlProgram glProgram;

  private float bitmapScaleX;
  private float bitmapScaleY;
  private int bitmapTexId;

  public BitmapOverlayProcessor(Context context, boolean useHdr) {
    super(useHdr);
    checkArgument(!useHdr, "BitmapOverlayProcessor does not support HDR colors.");
    paint = new Paint();
    paint.setTextSize(64);
    paint.setAntiAlias(true);
    paint.setARGB(0xFF, 0xFF, 0xFF, 0xFF);
    paint.setColor(Color.GRAY);
    overlayBitmap =
        Bitmap.createBitmap(BITMAP_WIDTH_HEIGHT, BITMAP_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
    overlayCanvas = new Canvas(overlayBitmap);

    try {
      logoBitmap =
          ((BitmapDrawable)
                  context.getPackageManager().getApplicationIcon(context.getPackageName()))
              .getBitmap();
    } catch (PackageManager.NameNotFoundException e) {
      throw new IllegalStateException(e);
    }
    try {
      bitmapTexId =
          GlUtil.createTexture(
              BITMAP_WIDTH_HEIGHT,
              BITMAP_WIDTH_HEIGHT,
              /* useHighPrecisionColorComponents= */ false);
      GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, /* level= */ 0, overlayBitmap, /* border= */ 0);

      glProgram = new GlProgram(context, VERTEX_SHADER_PATH, FRAGMENT_SHADER_PATH);
    } catch (GlUtil.GlException | IOException e) {
      Log.e("BitmapOverlayProcessor", "create program error:" + e.getMessage());
      return;
    }
    // Draw the frame on the entire normalized device coordinate space, from -1 to 1, for x and y.
    glProgram.setBufferAttribute(
        "aFramePosition",
        GlUtil.getNormalizedCoordinateBounds(),
        GlUtil.HOMOGENEOUS_COORDINATE_VECTOR_SIZE);
    glProgram.setSamplerTexIdUniform("uTexSampler1", bitmapTexId, /* texUnitIndex= */ 1);
  }

  @Override
  public Pair<Integer, Integer> configure(int inputWidth, int inputHeight) {
    if (inputWidth > inputHeight) {
      bitmapScaleX = inputWidth / (float) inputHeight;
      bitmapScaleY = 1f;
    } else {
      bitmapScaleX = 1f;
      bitmapScaleY = inputHeight / (float) inputWidth;
    }

    glProgram.setFloatUniform("uScaleX", bitmapScaleX);
    glProgram.setFloatUniform("uScaleY", bitmapScaleY);

    return Pair.create(inputWidth, inputHeight);
  }

  @Override
  public void drawFrame(int inputTexId, long presentationTimeUs) {
    try {
      glProgram.use();

      // Draw to the canvas and store it in a texture.
      String text =
          String.format(Locale.US, "%.02f", presentationTimeUs / (float) CommonUtil.MICROS_PER_SECOND);
      overlayBitmap.eraseColor(Color.TRANSPARENT);
      overlayCanvas.drawBitmap(checkStateNotNull(logoBitmap), /* left= */ 3, /* top= */ 378, paint);
      overlayCanvas.drawText(text, /* x= */ 160, /* y= */ 466, paint);
      GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, bitmapTexId);
      GLUtils.texSubImage2D(
          GLES20.GL_TEXTURE_2D,
          /* level= */ 0,
          /* xoffset= */ 0,
          /* yoffset= */ 0,
          flipBitmapVertically(overlayBitmap));
      GlUtil.checkGlError();

      glProgram.setSamplerTexIdUniform("uTexSampler0", inputTexId, /* texUnitIndex= */ 0);
      glProgram.bindAttributesAndUniforms();
      // The four-vertex triangle strip forms a quad.
      GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, /* first= */ 0, /* count= */ 4);
      GlUtil.checkGlError();
    } catch (GlUtil.GlException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void release() throws FrameProcessingException {
    super.release();
    try {
      glProgram.delete();
    } catch (GlUtil.GlException e) {
      throw new RuntimeException(e);
    }
  }

  private static Bitmap flipBitmapVertically(Bitmap bitmap) {
    Matrix flip = new Matrix();
    flip.postScale(1f, -1f);
    return Bitmap.createBitmap(
        bitmap,
        /* x= */ 0,
        /* y= */ 0,
        bitmap.getWidth(),
        bitmap.getHeight(),
        flip,
        /* filter= */ true);
  }
}

package com.frank.videoedit;

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

  private final Matrix matrix = new Matrix();

  public BitmapOverlayProcessor(Context context, boolean useHdr) {
    super(useHdr);

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
          GlUtil.createTexture(BITMAP_WIDTH_HEIGHT, BITMAP_WIDTH_HEIGHT, false);
      GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, overlayBitmap, 0);

      glProgram = new GlProgram(context, VERTEX_SHADER_PATH, FRAGMENT_SHADER_PATH);
    } catch (GlUtil.GlException | IOException e) {
      Log.e("BitmapOverlayProcessor", "create program error:" + e.getMessage());
      return;
    }

    glProgram.setBufferAttribute(
        "aFramePosition",
        GlUtil.getNormalizedCoordinateBounds(),
        GlUtil.HOMOGENEOUS_COORDINATE_VECTOR_SIZE);
    glProgram.setSamplerTexIdUniform("uTexSampler1", bitmapTexId, 1);
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

      String text =
          String.format(Locale.US, "%.02f", presentationTimeUs / (float) CommonUtil.MICROS_PER_SECOND);
      overlayBitmap.eraseColor(Color.TRANSPARENT);
      overlayCanvas.drawBitmap(logoBitmap, 3, 378, paint);
      overlayCanvas.drawText(text, 160, 466, paint);
      GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, bitmapTexId);
      GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, flipBitmap(overlayBitmap));
      GlUtil.checkGlError();

      glProgram.setSamplerTexIdUniform("uTexSampler0", inputTexId, 0);
      glProgram.bindAttributesAndUniforms();

      GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
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

  private Bitmap flipBitmap(Bitmap bitmap) {
    matrix.reset();
    matrix.postScale(1f, -1f);
    return Bitmap.createBitmap(
        bitmap, 0, 0,
        bitmap.getWidth(),
        bitmap.getHeight(),
        matrix, true);
  }
}

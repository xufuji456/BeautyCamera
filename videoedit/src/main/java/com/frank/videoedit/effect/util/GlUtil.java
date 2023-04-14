
package com.frank.videoedit.effect.util;

import static android.opengl.GLU.gluErrorString;

import android.content.Context;
import android.content.pm.PackageManager;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.os.Build;

import androidx.annotation.DoNotInline;
import androidx.annotation.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.List;

import javax.microedition.khronos.egl.EGL10;

public final class GlUtil {

  public static final class GlException extends Exception {
    public GlException(String message) {
      super(message);
    }
  }

  public static final int HOMOGENEOUS_COORDINATE_VECTOR_SIZE = 4;

  public static final float LENGTH_NDC = 2f;

  public static final int[] EGL_CONFIG_ATTRIBUTES_RGBA_8888 =
      new int[] {
        EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
        EGL14.EGL_RED_SIZE,     8,
        EGL14.EGL_GREEN_SIZE,   8,
        EGL14.EGL_BLUE_SIZE,    8,
        EGL14.EGL_ALPHA_SIZE,   8,
        EGL14.EGL_DEPTH_SIZE,   0,
        EGL14.EGL_STENCIL_SIZE, 0,
        EGL14.EGL_NONE
      };

  public static final int[] EGL_CONFIG_ATTRIBUTES_RGBA_1010102 =
      new int[] {
        EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
        EGL14.EGL_RED_SIZE,     10,
        EGL14.EGL_GREEN_SIZE,   10,
        EGL14.EGL_BLUE_SIZE,    10,
        EGL14.EGL_ALPHA_SIZE,   2,
        EGL14.EGL_DEPTH_SIZE,   0,
        EGL14.EGL_STENCIL_SIZE, 0,
        EGL14.EGL_NONE
      };

  // https://www.khronos.org/registry/EGL/extensions/EXT/EGL_EXT_protected_content.txt
  private static final String EXTENSION_PROTECTED_CONTENT = "EGL_EXT_protected_content";
  // https://www.khronos.org/registry/EGL/extensions/KHR/EGL_KHR_surfaceless_context.txt
  private static final String EXTENSION_SURFACELESS_CONTEXT = "EGL_KHR_surfaceless_context";
  // https://www.khronos.org/registry/OpenGL/extensions/EXT/EXT_YUV_target.txt
  private static final String EXTENSION_YUV_TARGET = "GL_EXT_YUV_target";

  private static final int[] EGL_WINDOW_SURFACE_ATTRIBUTES_NONE = new int[] {EGL14.EGL_NONE};

  private GlUtil() {}

  public static float[] getNormalizedCoordinateBounds() {
    return new float[] {
      -1, -1, 0, 1,
      1, -1, 0, 1,
      -1, 1, 0, 1,
      1, 1, 0, 1
    };
  }

  public static float[] getTextureCoordinateBounds() {
    return new float[] {
      0, 0, 0, 1,
      1, 0, 0, 1,
      0, 1, 0, 1,
      1, 1, 0, 1
    };
  }

  public static float[] create4x4IdentityMatrix() {
    float[] matrix = new float[16];
    setToIdentity(matrix);
    return matrix;
  }

  public static void setToIdentity(float[] matrix) {
    Matrix.setIdentityM(matrix, /* smOffset= */ 0);
  }

  public static float[] createVertexBuffer(List<float[]> vertexList) {
    float[] vertexBuffer = new float[HOMOGENEOUS_COORDINATE_VECTOR_SIZE * vertexList.size()];
    for (int i = 0; i < vertexList.size(); i++) {
      System.arraycopy(
          vertexList.get(i), 0, vertexBuffer,
              HOMOGENEOUS_COORDINATE_VECTOR_SIZE * i,
          HOMOGENEOUS_COORDINATE_VECTOR_SIZE);
    }
    return vertexBuffer;
  }

  public static boolean isProtectedContentExtensionSupported(Context context) {
    if (Build.VERSION.SDK_INT < 24) {
      return false;
    }
    if (Build.VERSION.SDK_INT < 26 && ("samsung".equals(Build.MANUFACTURER) || "XT1650".equals(Build.MODEL))) {
      return false;
    }
    if (Build.VERSION.SDK_INT < 26
        && !context
            .getPackageManager()
            .hasSystemFeature(PackageManager.FEATURE_VR_MODE_HIGH_PERFORMANCE)) {
      return false;
    }

    EGLDisplay display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
    @Nullable String eglExtensions = EGL14.eglQueryString(display, EGL10.EGL_EXTENSIONS);
    return eglExtensions != null && eglExtensions.contains(EXTENSION_PROTECTED_CONTENT);
  }

  public static boolean isSurfacelessContextExtensionSupported() {
    EGLDisplay display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
    @Nullable String eglExtensions = EGL14.eglQueryString(display, EGL10.EGL_EXTENSIONS);
    return eglExtensions != null && eglExtensions.contains(EXTENSION_SURFACELESS_CONTEXT);
  }

  /**
   * Returns whether the {@value #EXTENSION_YUV_TARGET} extension is supported.
   *
   * <p>This extension allows sampling raw YUV values from an external texture, which is required
   * for HDR.
   */
  public static boolean isYuvTargetExtensionSupported() {
    @Nullable String glExtensions;
    if (EGL14.eglGetCurrentContext().equals(EGL14.EGL_NO_CONTEXT)) {
      try {
        EGLDisplay eglDisplay = createEglDisplay();
        EGLContext eglContext = createEglContext(eglDisplay);
        focusPlaceholderEglSurface(eglContext, eglDisplay);
        glExtensions = GLES20.glGetString(GLES20.GL_EXTENSIONS);
        destroyEglContext(eglDisplay, eglContext);
      } catch (GlException e) {
        return false;
      }
    } else {
      glExtensions = GLES20.glGetString(GLES20.GL_EXTENSIONS);
    }

    return glExtensions != null && glExtensions.contains(EXTENSION_YUV_TARGET);
  }

  public static EGLDisplay createEglDisplay() throws GlException {
    return Api17.createEglDisplay();
  }

  public static EGLContext createEglContext(EGLDisplay eglDisplay) throws GlException {
    return createEglContext(eglDisplay, EGL_CONFIG_ATTRIBUTES_RGBA_8888);
  }

  public static EGLContext createEglContext(EGLDisplay eglDisplay, int[] configAttributes)
      throws GlException {
    return Api17.createEglContext(
        eglDisplay,
        Arrays.equals(configAttributes, EGL_CONFIG_ATTRIBUTES_RGBA_1010102) ? 3 : 2,
        configAttributes);
  }

  public static EGLSurface getEglSurface(EGLDisplay eglDisplay, Object surface) throws GlException {
    return Api17.getEglSurface(
        eglDisplay, surface, EGL_CONFIG_ATTRIBUTES_RGBA_8888, EGL_WINDOW_SURFACE_ATTRIBUTES_NONE);
  }

  public static EGLSurface getEglSurface(
      EGLDisplay eglDisplay, Object surface, int[] configAttributes) throws GlException {
    return Api17.getEglSurface(
        eglDisplay, surface, configAttributes, EGL_WINDOW_SURFACE_ATTRIBUTES_NONE);
  }

  private static EGLSurface createPbufferSurface(
      EGLDisplay eglDisplay, int width, int height, int[] configAttributes) throws GlException {
    int[] bufferAttributes =
        new int[] {
          EGL14.EGL_WIDTH, width,
          EGL14.EGL_HEIGHT, height,
          EGL14.EGL_NONE
        };
    return Api17.createEglPbufferSurface(eglDisplay, configAttributes, bufferAttributes);
  }

  public static EGLSurface focusPlaceholderEglSurface(EGLContext eglContext, EGLDisplay eglDisplay)
      throws GlException {
    return createFocusedPlaceholderEglSurface(
        eglContext, eglDisplay, EGL_CONFIG_ATTRIBUTES_RGBA_8888);
  }

  public static EGLSurface createFocusedPlaceholderEglSurface(
      EGLContext eglContext, EGLDisplay eglDisplay, int[] configAttributes) throws GlException {
    EGLSurface eglSurface =
        isSurfacelessContextExtensionSupported()
            ? EGL14.EGL_NO_SURFACE
            : createPbufferSurface(eglDisplay, /* width= */ 1, /* height= */ 1, configAttributes);

    focusEglSurface(eglDisplay, eglContext, eglSurface, /* width= */ 1, /* height= */ 1);
    return eglSurface;
  }

  public static void checkGlError() throws GlException {
    StringBuilder errorMessageBuilder = new StringBuilder();
    boolean foundError = false;
    int error;
    while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
      if (foundError) {
        errorMessageBuilder.append('\n');
      }
      errorMessageBuilder.append("glError: ").append(gluErrorString(error));
      foundError = true;
    }
    if (foundError) {
      throw new GlException(errorMessageBuilder.toString());
    }
  }

  private static void assertValidTextureSize(int width, int height) throws GlException {
    int[] maxTextureSizeBuffer = new int[1];
    GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, maxTextureSizeBuffer, 0);
    int maxTextureSize = maxTextureSizeBuffer[0];

    if (width < 0 || height < 0) {
      throw new GlException("width or height is less than 0");
    }
    if (width > maxTextureSize || height > maxTextureSize) {
      throw new GlException(
          "width or height is greater than GL_MAX_TEXTURE_SIZE " + maxTextureSize);
    }
  }

  public static void clearOutputFrame() throws GlException {
    GLES20.glClearColor(/* red= */ 0, /* green= */ 0, /* blue= */ 0, /* alpha= */ 0);
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
    GlUtil.checkGlError();
  }

  public static void focusEglSurface(
      EGLDisplay eglDisplay, EGLContext eglContext, EGLSurface eglSurface, int width, int height)
      throws GlException {
    Api17.focusRenderTarget(
        eglDisplay, eglContext, eglSurface, /* framebuffer= */ 0, width, height);
  }

  public static void focusFramebuffer(
      EGLDisplay eglDisplay,
      EGLContext eglContext,
      EGLSurface eglSurface,
      int framebuffer,
      int width,
      int height)
      throws GlException {
    Api17.focusRenderTarget(eglDisplay, eglContext, eglSurface, framebuffer, width, height);
  }

  public static void focusFramebufferUsingCurrentContext(int framebuffer, int width, int height)
      throws GlException {
    Api17.focusFramebufferUsingCurrentContext(framebuffer, width, height);
  }

  public static void deleteTexture(int textureId) throws GlException {
    GLES20.glDeleteTextures(/* n= */ 1, new int[] {textureId}, /* offset= */ 0);
    checkGlError();
  }

  public static void destroyEglContext(
      @Nullable EGLDisplay eglDisplay, @Nullable EGLContext eglContext) throws GlException {
    Api17.destroyEglContext(eglDisplay, eglContext);
  }

  public static FloatBuffer createBuffer(float[] data) {
    return (FloatBuffer) createBuffer(data.length).put(data).flip();
  }

  private static FloatBuffer createBuffer(int capacity) {
    ByteBuffer byteBuffer = ByteBuffer.allocateDirect(capacity * 4);
    return byteBuffer.order(ByteOrder.nativeOrder()).asFloatBuffer();
  }

  public static int createExternalTexture() throws GlException {
    int texId = generateTexture();
    bindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texId);
    return texId;
  }

  public static int createTexture(int width, int height, boolean useHighPrecisionColorComponents)
      throws GlException {
    if (useHighPrecisionColorComponents) {
      return createTexture(width, height, GLES30.GL_RGBA16F, GLES30.GL_HALF_FLOAT);
    }
    return createTexture(width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE);
  }

  private static int createTexture(int width, int height, int internalFormat, int type)
      throws GlException {
    assertValidTextureSize(width, height);
    int texId = generateTexture();
    bindTexture(GLES20.GL_TEXTURE_2D, texId);
    ByteBuffer byteBuffer = ByteBuffer.allocateDirect(width * height * 4);
    GLES20.glTexImage2D(
        GLES20.GL_TEXTURE_2D, 0, internalFormat, width, height, 0,
            GLES20.GL_RGBA, type, byteBuffer);
    checkGlError();
    return texId;
  }

  private static int generateTexture() throws GlException {
    checkGlException(
        !EGL14.eglGetCurrentContext().equals(EGL14.EGL_NO_CONTEXT), "No current context");

    int[] texId = new int[1];
    GLES20.glGenTextures(1, texId,  0);
    checkGlError();
    return texId[0];
  }

  public static void bindTexture(int textureTarget, int texId) throws GlException {
    GLES20.glBindTexture(textureTarget, texId);
    checkGlError();
    GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
    checkGlError();
    GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
    checkGlError();
    GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
    checkGlError();
    GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    checkGlError();
  }

  public static int createFboForTexture(int texId) throws GlException {
    checkGlException(
        !EGL14.eglGetCurrentContext().equals(EGL14.EGL_NO_CONTEXT), "No current context");

    int[] fboId = new int[1];
    GLES20.glGenFramebuffers(/* n= */ 1, fboId, /* offset= */ 0);
    checkGlError();
    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId[0]);
    checkGlError();
    GLES20.glFramebufferTexture2D(
        GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, texId, 0);
    checkGlError();
    return fboId[0];
  }

  public static void checkGlException(boolean expression, String errorMessage) throws GlException {
    if (!expression) {
      throw new GlException(errorMessage);
    }
  }

  private static void checkEglException(String errorMessage) throws GlException {
    int error = EGL14.eglGetError();
    checkGlException(error == EGL14.EGL_SUCCESS, errorMessage + ", error code: " + error);
  }

  private static final class Api17 {
    private Api17() {}

    @DoNotInline
    public static EGLDisplay createEglDisplay() throws GlException {
      EGLDisplay eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
      checkGlException(!eglDisplay.equals(EGL14.EGL_NO_DISPLAY), "No EGL display.");
      checkGlException(
          EGL14.eglInitialize(eglDisplay,
                  new int[1], 0, new int[1], 0), "Error in eglInitialize.");
      checkGlError();
      return eglDisplay;
    }

    @DoNotInline
    public static EGLContext createEglContext(
        EGLDisplay eglDisplay, int version, int[] configAttributes) throws GlException {
      int[] contextAttributes = {EGL14.EGL_CONTEXT_CLIENT_VERSION, version, EGL14.EGL_NONE};
      EGLContext eglContext =
          EGL14.eglCreateContext(
              eglDisplay,
              getEglConfig(eglDisplay, configAttributes),
              EGL14.EGL_NO_CONTEXT,
              contextAttributes,
              0);
      if (eglContext == null) {
        EGL14.eglTerminate(eglDisplay);
        throw new GlException(
            "eglCreateContext() failed to create a valid context. The device may not support EGL"
                + " version "
                + version);
      }
      checkGlError();
      return eglContext;
    }

    @DoNotInline
    public static EGLSurface getEglSurface(
        EGLDisplay eglDisplay,
        Object surface,
        int[] configAttributes,
        int[] windowSurfaceAttributes)
        throws GlException {
      EGLSurface eglSurface =
          EGL14.eglCreateWindowSurface(
              eglDisplay,
              getEglConfig(eglDisplay, configAttributes),
              surface,
              windowSurfaceAttributes,
              0);
      checkEglException("Error creating surface");
      return eglSurface;
    }

    @DoNotInline
    public static EGLSurface createEglPbufferSurface(
        EGLDisplay eglDisplay, int[] configAttributes, int[] bufferAttributes) throws GlException {
      EGLSurface eglSurface =
          EGL14.eglCreatePbufferSurface(
              eglDisplay,
              getEglConfig(eglDisplay, configAttributes),
              bufferAttributes,
              0);
      checkEglException("Error creating surface");
      return eglSurface;
    }

    @DoNotInline
    public static void focusRenderTarget(
        EGLDisplay eglDisplay,
        EGLContext eglContext,
        EGLSurface eglSurface,
        int framebuffer,
        int width,
        int height)
        throws GlException {
      EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext);
      checkEglException("Error making context current");
      focusFramebufferUsingCurrentContext(framebuffer, width, height);
    }

    @DoNotInline
    public static void focusFramebufferUsingCurrentContext(int framebuffer, int width, int height)
        throws GlException {
      checkGlException(
          !EGL14.eglGetCurrentContext().equals(EGL14.EGL_NO_CONTEXT), "No current context");

      int[] boundFramebuffer = new int[1];
      GLES20.glGetIntegerv(GLES20.GL_FRAMEBUFFER_BINDING, boundFramebuffer, /* offset= */ 0);
      if (boundFramebuffer[0] != framebuffer) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebuffer);
      }
      checkGlError();
      GLES20.glViewport(0, 0, width, height);
      checkGlError();
    }

    @DoNotInline
    public static void destroyEglContext(
        @Nullable EGLDisplay eglDisplay, @Nullable EGLContext eglContext) throws GlException {
      if (eglDisplay == null) {
        return;
      }
      EGL14.eglMakeCurrent(
          eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
      checkEglException("Error releasing context");
      if (eglContext != null) {
        EGL14.eglDestroyContext(eglDisplay, eglContext);
        checkEglException("Error destroying context");
      }
      EGL14.eglReleaseThread();
      checkEglException("Error releasing thread");
      EGL14.eglTerminate(eglDisplay);
      checkEglException("Error terminating display");
    }

    @DoNotInline
    private static EGLConfig getEglConfig(EGLDisplay eglDisplay, int[] attributes)
        throws GlException {
      EGLConfig[] eglConfigs = new EGLConfig[1];
      if (!EGL14.eglChooseConfig(
          eglDisplay, attributes, 0, eglConfigs,
              0, 1, new int[1], 0)) {
        throw new GlException("eglChooseConfig failed.");
      }
      return eglConfigs[0];
    }
  }

}
